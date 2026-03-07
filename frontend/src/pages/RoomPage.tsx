import React, { useEffect, useState, useRef, useCallback } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import VideoPlayer from '../components/VideoPlayer';
import ChatPanel, { type ChatMessage } from '../components/ChatPanel';
import ChatBubble from '../components/ChatBubble';
import { useSignaling } from '../hooks/useSignaling';
import { getOrCreateDisplayName } from '../utils/nameGenerator';

const RoomPage: React.FC = () => {
    const { roomCode } = useParams<{ roomCode: string }>();
    const navigate = useNavigate();

    const [localStream, setLocalStream] = useState<MediaStream | null>(null);
    const localStreamRef = useRef<MediaStream | null>(null);
    const [isCameraOn, setIsCameraOn] = useState(true);
    const [isMicOn, setIsMicOn] = useState(true);

    const myUserIdRef = useRef<string>('');
    const peersRef = useRef<Record<string, RTCPeerConnection>>({});
    const iceCandidateQueue = useRef<Record<string, RTCIceCandidateInit[]>>({});
    const [remoteStreams, setRemoteStreams] = useState<Record<string, MediaStream>>({});

    // Anonymous display names
    const myDisplayName = useRef(getOrCreateDisplayName()).current;
    const [peerNames, setPeerNames] = useState<Record<string, string>>({});
    const [peerCameraStates, setPeerCameraStates] = useState<Record<string, boolean>>({});
    const isCameraOnRef = useRef(true);
    const dataChannelsRef = useRef<Record<string, RTCDataChannel>>({});

    // Chat state
    const [messages, setMessages] = useState<ChatMessage[]>([]);
    const [isChatOpen, setIsChatOpen] = useState(false);
    const [hasUnread, setHasUnread] = useState(false);
    const [latestBubble, setLatestBubble] = useState<{ id: string, sender: string, text: string } | null>(null);

    const isChatOpenRef = useRef(isChatOpen);
    useEffect(() => {
        isChatOpenRef.current = isChatOpen;
    }, [isChatOpen]);

    // Layout: compute optimal tile sizes like Google Meet
    const containerRef = useRef<HTMLDivElement>(null);
    const [containerSize, setContainerSize] = useState({ width: 0, height: 0 });

    const [mediaReady, setMediaReady] = useState(false);

    // Establish WebSocket signaling connection ONLY after local media is acquired
    const { sendMessage, setOnMessageListener, connectionStatus } = useSignaling(mediaReady ? roomCode : undefined);

    // Refs to avoid stale closures in WebRTC callbacks (the root cause of the asymmetric peer bug)
    const sendMessageRef = useRef(sendMessage);
    sendMessageRef.current = sendMessage;

    // Setup handler for RTCDataChannel (used for camera state signaling)
    const setupDataChannel = useCallback((dc: RTCDataChannel, peerUid: string) => {
        dc.onopen = () => {
            dataChannelsRef.current[peerUid] = dc;
            // Send current camera state immediately so new peer knows
            dc.send(JSON.stringify({ type: 'CAMERA_STATE', cameraOn: isCameraOnRef.current }));
        };
        dc.onmessage = (event) => {
            try {
                const data = JSON.parse(event.data);
                if (data.type === 'CAMERA_STATE') {
                    setPeerCameraStates(prev => ({ ...prev, [peerUid]: data.cameraOn }));
                } else if (data.type === 'CHAT_MSG') {
                    const newMsg: ChatMessage = {
                        id: data.id,
                        sender: data.senderName || `Guest ${peerUid.slice(0, 4)}`,
                        text: data.text,
                        timestamp: data.timestamp,
                        isMe: false
                    };
                    setMessages(prev => [...prev.slice(-99), newMsg]);

                    if (!isChatOpenRef.current) {
                        setHasUnread(true);
                        setLatestBubble({ id: newMsg.id, sender: newMsg.sender, text: newMsg.text });
                    }
                }
            } catch (e) {
                console.error('Error parsing data channel message:', e);
            }
        };
        dc.onclose = () => {
            delete dataChannelsRef.current[peerUid];
        };
    }, []);

    const createPeerConnection = useCallback((targetUid: string) => {
        const pc = new RTCPeerConnection({
            iceServers: [
                { urls: 'stun:stun.l.google.com:19302' }
            ]
        });

        if (localStreamRef.current) {
            localStreamRef.current.getTracks().forEach((track: MediaStreamTrack) => {
                pc.addTrack(track, localStreamRef.current!);
            });
        }

        pc.onicecandidate = (event) => {
            if (event.candidate) {
                sendMessageRef.current({
                    type: 'ICE',
                    to: targetUid,
                    message: JSON.stringify(event.candidate)
                });
            }
        };

        pc.ontrack = (event) => {
            let stream = event.streams?.[0];
            if (!stream) {
                stream = new MediaStream();
                stream.addTrack(event.track);
            }
            setRemoteStreams(prev => {
                if (prev[targetUid] === stream) return prev;
                return { ...prev, [targetUid]: stream };
            });
        };

        // Callee side: receive data channel created by caller
        pc.ondatachannel = (event) => {
            setupDataChannel(event.channel, targetUid);
        };

        pc.onconnectionstatechange = () => {
            console.log(`[PC ${targetUid.slice(0, 6)}] connectionState: ${pc.connectionState}`);
        };

        peersRef.current[targetUid] = pc;
        return pc;
    }, [setupDataChannel]);  // setupDataChannel is stable (useCallback with [])

    useEffect(() => {
        setOnMessageListener(async (data) => {
            const { responseType, message, from } = data;

            try {
                if (responseType === 'USER_ID') {
                    myUserIdRef.current = message;
                }
                else if (responseType === 'PEER_LIST') {
                    const peers = message.replace(/[\[\]\s]/g, '').split(',').filter(Boolean);
                    for (const peerUid of peers) {
                        if (peerUid === myUserIdRef.current) continue;
                        const pc = createPeerConnection(peerUid);
                        // Caller: create data channel BEFORE offer so it's in the SDP
                        const dc = pc.createDataChannel('metadata');
                        setupDataChannel(dc, peerUid);
                        const offer = await pc.createOffer();
                        await pc.setLocalDescription(offer);
                        sendMessageRef.current({
                            type: 'OFFER',
                            to: peerUid,
                            message: JSON.stringify({ sdp: pc.localDescription, displayName: myDisplayName })
                        });
                    }
                }
                else if (responseType === 'PEER_JOIN') {
                    const joinedUid = message.replace("Joined the room", "").trim();
                    if (joinedUid === myUserIdRef.current) return;
                    console.log(`${joinedUid} joined the room`);
                    // Callee: Create peer connection but DO NOT create an offer. Wait for their offer.
                    if (!peersRef.current[joinedUid]) {
                        createPeerConnection(joinedUid);
                    }
                }
                else if (responseType === 'PEER_DC') {
                    const leftUid = message.replace(" disconnected", "").trim();
                    if (leftUid === myUserIdRef.current) return;

                    if (peersRef.current[leftUid]) {
                        peersRef.current[leftUid].close();
                        delete peersRef.current[leftUid];
                    }
                    delete dataChannelsRef.current[leftUid];
                    setRemoteStreams(prev => {
                        const updated = { ...prev };
                        delete updated[leftUid];
                        return updated;
                    });
                    setPeerNames(prev => {
                        const updated = { ...prev };
                        delete updated[leftUid];
                        return updated;
                    });
                    setPeerCameraStates(prev => {
                        const updated = { ...prev };
                        delete updated[leftUid];
                        return updated;
                    });
                }
                else if (responseType === 'OFFER') {
                    if (from === myUserIdRef.current) return;
                    const payload = JSON.parse(message);

                    // Extract peer display name from the offer
                    if (payload.displayName) {
                        setPeerNames(prev => ({ ...prev, [from]: payload.displayName }));
                    }

                    // Use connection created during PEER_JOIN if it exists, otherwise create
                    let pc = peersRef.current[from];
                    if (!pc) {
                        pc = createPeerConnection(from);
                    }

                    await pc.setRemoteDescription(new RTCSessionDescription(payload.sdp));
                    const answer = await pc.createAnswer();
                    await pc.setLocalDescription(answer);
                    sendMessageRef.current({
                        type: 'ANSWER',
                        to: from,
                        message: JSON.stringify({ sdp: pc.localDescription, displayName: myDisplayName })
                    });

                    // Flush ICE queue
                    if (iceCandidateQueue.current[from]) {
                        for (const candidate of iceCandidateQueue.current[from]) {
                            await pc.addIceCandidate(new RTCIceCandidate(candidate)).catch(console.error);
                        }
                        delete iceCandidateQueue.current[from];
                    }
                }
                else if (responseType === 'ANSWER') {
                    if (from === myUserIdRef.current) return;
                    const payload = JSON.parse(message);

                    // Extract peer display name from the answer
                    if (payload.displayName) {
                        setPeerNames(prev => ({ ...prev, [from]: payload.displayName }));
                    }

                    const pc = peersRef.current[from];
                    if (pc) {
                        await pc.setRemoteDescription(new RTCSessionDescription(payload.sdp));
                        // Flush ICE queue
                        if (iceCandidateQueue.current[from]) {
                            for (const candidate of iceCandidateQueue.current[from]) {
                                await pc.addIceCandidate(new RTCIceCandidate(candidate)).catch(console.error);
                            }
                            delete iceCandidateQueue.current[from];
                        }
                    }
                }
                else if (responseType === 'ICE') {
                    if (from === myUserIdRef.current) return;
                    const payload = JSON.parse(message);
                    const pc = peersRef.current[from];

                    if (payload) {
                        if (pc && pc.remoteDescription) {
                            await pc.addIceCandidate(new RTCIceCandidate(payload)).catch(console.error);
                        } else {
                            if (!iceCandidateQueue.current[from]) {
                                iceCandidateQueue.current[from] = [];
                            }
                            iceCandidateQueue.current[from].push(payload);
                        }
                    }
                }
                // CAMERA_STATE handled via RTCDataChannel, not WebSocket
            } catch (error) {
                console.error(`Error handling ${responseType}:`, error);
            }
        });
    }, [setOnMessageListener, createPeerConnection, setupDataChannel]);


    useEffect(() => {
        const activeRoom = sessionStorage.getItem('active_room');
        if (activeRoom !== roomCode) {
            navigate('/dashboard');
            return;
        }

        const token = localStorage.getItem('huddle_token');
        if (!token) {
            navigate('/');
            return;
        }

        let isMounted = true;

        const startLocalMedia = async () => {
            try {
                const stream = await navigator.mediaDevices.getUserMedia({
                    video: true,
                    audio: true
                });
                if (!isMounted) {
                    // Component unmounted (StrictMode cleanup) before getUserMedia resolved
                    stream.getTracks().forEach(track => track.stop());
                    return;
                }
                setLocalStream(stream);
                localStreamRef.current = stream;
                setMediaReady(true);
            } catch (error) {
                console.error("Error accessing media devices.", error);
            }
        };

        startLocalMedia();

        return () => {
            isMounted = false;
            // Cleanup: stop all tracks and close peer connections when unmounting
            if (localStreamRef.current) {
                localStreamRef.current.getTracks().forEach(track => track.stop());
            }
            Object.values(peersRef.current).forEach(pc => pc.close());
            peersRef.current = {};
        };
    }, [navigate]);

    // Cleanup streams on unmount when stream changes (handle race conditions)
    useEffect(() => {
        return () => {
            if (localStream) {
                localStream.getTracks().forEach(track => track.stop());
            }
        };
    }, [localStream]);

    // ResizeObserver to track main container dimensions for tile sizing
    useEffect(() => {
        const el = containerRef.current;
        if (!el) return;
        const observer = new ResizeObserver(entries => {
            const { width, height } = entries[0].contentRect;
            setContainerSize({ width, height });
        });
        observer.observe(el);
        return () => observer.disconnect();
    }, []);


    const toggleCamera = () => {
        if (localStream) {
            const videoTrack = localStream.getVideoTracks()[0];
            if (videoTrack) {
                videoTrack.enabled = !videoTrack.enabled;
                const newState = videoTrack.enabled;
                setIsCameraOn(newState);
                isCameraOnRef.current = newState;

                // Broadcast camera state via RTCDataChannel to all peers
                Object.values(dataChannelsRef.current).forEach(dc => {
                    if (dc.readyState === 'open') {
                        dc.send(JSON.stringify({ type: 'CAMERA_STATE', cameraOn: newState }));
                    }
                });
            }
        }
    };

    const toggleMic = () => {
        if (localStream) {
            const audioTrack = localStream.getAudioTracks()[0];
            if (audioTrack) {
                audioTrack.enabled = !audioTrack.enabled;
                setIsMicOn(audioTrack.enabled);
            }
        }
    };

    const handleLeaveRoom = () => {
        // Close all peer connections before leaving
        Object.values(peersRef.current).forEach(pc => pc.close());
        peersRef.current = {};
        if (localStream) {
            localStream.getTracks().forEach(track => track.stop());
            setLocalStream(null);
        }
        navigate('/dashboard');
    };

    const handleCopyRoomCode = async () => {
        if (roomCode) {
            try {
                await navigator.clipboard.writeText(roomCode);
                setCopied(true);
                setTimeout(() => setCopied(false), 2000);
            } catch {
                // Fallback for older browsers
                const textArea = document.createElement('textarea');
                textArea.value = roomCode;
                document.body.appendChild(textArea);
                textArea.select();
                document.execCommand('copy');
                document.body.removeChild(textArea);
                setCopied(true);
                setTimeout(() => setCopied(false), 2000);
            }
        }
    };

    const handleSendChat = (text: string) => {
        const newMsg: ChatMessage = {
            id: Date.now().toString() + Math.random().toString(36).substring(2, 9),
            sender: myDisplayName,
            text,
            timestamp: Date.now(),
            isMe: true
        };
        setMessages(prev => [...prev.slice(-99), newMsg]);

        // Broadcast chat message to all peers via RTCDataChannel
        Object.values(dataChannelsRef.current).forEach(dc => {
            if (dc.readyState === 'open') {
                dc.send(JSON.stringify({
                    type: 'CHAT_MSG',
                    id: newMsg.id,
                    text: newMsg.text,
                    timestamp: newMsg.timestamp,
                    senderName: myDisplayName
                }));
            }
        });
    };

    const toggleChat = () => {
        setIsChatOpen(prev => {
            if (!prev) setHasUnread(false);
            return !prev;
        });
    };

    const [copied, setCopied] = useState(false);
    const peerCount = 1 + Object.keys(remoteStreams).length;

    // Compute optimal tile size based on container dimensions and peer count
    const computeTileSize = () => {
        const { width: cw, height: ch } = containerSize;
        if (cw === 0 || ch === 0) return null;

        const GAP = 12; // gap-3 = 0.75rem = 12px
        const PADDING = 32; // 16px padding on all sides to prevent shadow clipping
        let cols: number, rows: number;

        if (peerCount === 1) { cols = 1; rows = 1; }
        else if (peerCount === 2) { cols = 2; rows = 1; }
        else if (peerCount <= 4) { cols = 2; rows = 2; }
        else if (peerCount <= 6) { cols = 3; rows = 2; }
        else { cols = 4; rows = 2; }

        const availW = cw - PADDING - GAP * (cols - 1);
        const availH = ch - PADDING - GAP * (rows - 1);

        let tileW = availW / cols;
        let tileH = tileW * 9 / 16;

        // If tiles would exceed available height, constrain by height instead
        if (tileH > availH / rows) {
            tileH = availH / rows;
            tileW = tileH * 16 / 9;
        }

        return { width: Math.floor(tileW), height: Math.floor(tileH) };
    };

    const tileSize = computeTileSize();
    const tileStyle = tileSize ? { width: tileSize.width, height: tileSize.height } : undefined;

    return (
        <div className="h-screen bg-[#0A0A0B] text-white flex font-sans overflow-hidden">
            {/* Main Room Content */}
            <div className="flex-1 flex flex-col p-4 h-full min-w-0 relative">
                {/* WebSocket connection status banners */}
                {connectionStatus === 'reconnecting' && (
                    <div className="fixed top-0 left-0 right-0 z-50 bg-yellow-500/90 text-black text-sm font-medium text-center py-2 flex items-center justify-center gap-2 backdrop-blur-md">
                        <svg className="w-4 h-4 animate-spin" fill="none" viewBox="0 0 24 24">
                            <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4"></circle>
                            <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path>
                        </svg>
                        Reconnecting to server...
                    </div>
                )}
                {connectionStatus === 'disconnected' && (
                    <div className="fixed top-0 left-0 right-0 z-50 bg-red-500/90 text-white text-sm font-medium text-center py-2 backdrop-blur-md">
                        Disconnected from server. Please leave and rejoin the room.
                    </div>
                )}
                <header className="flex justify-between items-center mb-4 z-10 shrink-0">
                    <div>
                        <h1 className="text-3xl font-extrabold tracking-tight">Huddle</h1>
                        <div className="text-sm text-gray-400 mt-1 font-medium flex items-center gap-2">
                            Room Code: <span className="font-mono text-indigo-400 bg-indigo-500/10 px-2 py-0.5 rounded-md ml-1">{roomCode}</span>
                            <button
                                onClick={handleCopyRoomCode}
                                className="ml-1 text-gray-500 hover:text-indigo-400 transition-colors"
                                title="Copy room code"
                            >
                                {copied ? (
                                    <svg className="w-4 h-4 text-green-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M5 13l4 4L19 7"></path>
                                    </svg>
                                ) : (
                                    <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M8 16H6a2 2 0 01-2-2V6a2 2 0 012-2h8a2 2 0 012 2v2m-6 12h8a2 2 0 002-2v-8a2 2 0 00-2-2h-8a2 2 0 00-2 2v8a2 2 0 002 2z"></path>
                                    </svg>
                                )}
                            </button>
                        </div>
                    </div>
                    <div className="flex items-center gap-3">
                        <div className="flex items-center gap-2 bg-[#1C1C1E] border border-gray-800 rounded-full px-4 py-2 text-sm">
                            <svg className="w-4 h-4 text-indigo-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M17 20h5v-2a3 3 0 00-5.356-1.857M17 20H7m10 0v-2c0-.656-.126-1.283-.356-1.857M7 20H2v-2a3 3 0 015.356-1.857M7 20v-2c0-.656.126-1.283.356-1.857m0 0a5.002 5.002 0 019.288 0M15 7a3 3 0 11-6 0 3 3 0 016 0z"></path>
                            </svg>
                            <span className="text-white font-medium">{peerCount}</span>
                            <span className="text-gray-400">in room</span>
                        </div>
                    </div>
                </header>

                <main className="flex-1 w-full relative overflow-hidden min-h-0 flex">
                    {/* Background ambient light (behind everything) */}
                    <div className="absolute top-1/2 left-1/2 -translate-x-1/2 -translate-y-1/2 w-[800px] h-[600px] bg-indigo-900/10 blur-[120px] rounded-full pointer-events-none"></div>

                    <div ref={containerRef} className="flex-1 h-full flex flex-wrap justify-center items-center content-center gap-3 relative z-10 transition-all duration-300">
                        {/* Local tile */}
                        <div
                            style={tileStyle}
                            className="relative overflow-hidden rounded-2xl bg-[#121214] ring-1 ring-white/10 shadow-xl transition-all hover:ring-indigo-500/50 hover:shadow-indigo-500/10"
                        >
                            <VideoPlayer
                                stream={localStream}
                                muted={true}
                                className="w-full h-full object-cover"
                            />
                            <div className="absolute bottom-3 left-3 bg-black/60 px-3 py-1.5 rounded-lg text-sm font-semibold backdrop-blur-md shadow-lg border border-white/10 z-20">
                                <span className="text-indigo-300">{myDisplayName}</span> <span className="text-gray-500 text-xs">(You)</span>
                            </div>

                            {!isCameraOn && (
                                <div className="absolute inset-0 flex items-center justify-center bg-[#121214] z-10">
                                    <div className="w-20 h-20 bg-gray-800/80 rounded-full flex items-center justify-center shadow-lg border border-gray-700/50 backdrop-blur-md">
                                        <svg className="w-10 h-10 text-gray-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M15 10l4.553-2.276A1 1 0 0121 8.618v6.764a1 1 0 01-1.447.894L15 14M5 18h8a2 2 0 002-2V8a2 2 0 00-2-2H5a2 2 0 00-2 2v8a2 2 0 002 2z"></path>
                                            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M3 3l18 18"></path>
                                        </svg>
                                    </div>
                                </div>
                            )}
                        </div>

                        {/* Remote tiles */}
                        {Object.entries(remoteStreams).map(([uid, stream]) => (
                            <div
                                key={uid}
                                style={tileStyle}
                                className="relative overflow-hidden rounded-2xl bg-[#121214] ring-1 ring-white/10 shadow-xl transition-all hover:ring-purple-500/50 hover:shadow-purple-500/10"
                            >
                                <VideoPlayer
                                    stream={stream}
                                    muted={false}
                                    className="w-full h-full object-cover"
                                />
                                <div className="absolute bottom-3 left-3 bg-black/60 px-3 py-1.5 rounded-lg text-sm font-semibold backdrop-blur-md shadow-lg border border-white/10 z-20">
                                    <span className="text-purple-300">{peerNames[uid] || `Guest ${uid.slice(0, 4).toUpperCase()}`}</span>
                                </div>

                                {peerCameraStates[uid] === false && (
                                    <div className="absolute inset-0 flex items-center justify-center bg-[#121214] z-10">
                                        <div className="w-20 h-20 bg-gray-800/80 rounded-full flex items-center justify-center shadow-lg border border-gray-700/50 backdrop-blur-md">
                                            <svg className="w-10 h-10 text-gray-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M15 10l4.553-2.276A1 1 0 0121 8.618v6.764a1 1 0 01-1.447.894L15 14M5 18h8a2 2 0 002-2V8a2 2 0 00-2-2H5a2 2 0 00-2 2v8a2 2 0 002 2z"></path>
                                                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M3 3l18 18"></path>
                                            </svg>
                                        </div>
                                    </div>
                                )}
                            </div>
                        ))}
                    </div>

                    {/* Floating Chat Bubble Notification */}
                    {!isChatOpen && latestBubble && (
                        <ChatBubble
                            key={latestBubble.id}
                            sender={latestBubble.sender}
                            text={latestBubble.text}
                            onDismiss={() => setLatestBubble(null)}
                            onClick={() => {
                                setLatestBubble(null);
                                setIsChatOpen(true);
                            }}
                        />
                    )}
                </main>

                <footer className="mt-3 flex justify-center items-center gap-6 pb-2 shrink-0">
                    <button
                        onClick={toggleMic}
                        className={`p-4 rounded-full transition-all ${isMicOn ? 'bg-gray-800 hover:bg-gray-700 text-white' : 'bg-red-500/20 text-red-500 hover:bg-red-500/30'
                            }`}
                    >
                        {isMicOn ? (
                            <svg className="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M19 11a7 7 0 01-7 7m0 0a7 7 0 01-7-7m7 7v4m0 0H8m4 0h4m-4-8a3 3 0 01-3-3V5a3 3 0 116 0v6a3 3 0 01-3 3z"></path>
                            </svg>
                        ) : (
                            <svg className="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M5.586 15H4a1 1 0 01-1-1v-4a1 1 0 011-1h1.586l4.707-4.707C10.923 3.663 12 4.109 12 5v14c0 .891-1.077 1.337-1.707.707L5.586 15z"></path>
                                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M17 14l2-2m0 0l2-2m-2 2l-2-2m2 2l2 2"></path>
                            </svg>
                        )}
                    </button>

                    <button
                        onClick={toggleCamera}
                        className={`p-4 rounded-full transition-all ${isCameraOn ? 'bg-gray-800 hover:bg-gray-700 text-white' : 'bg-red-500/20 text-red-500 hover:bg-red-500/30'
                            }`}
                    >
                        {isCameraOn ? (
                            <svg className="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M15 10l4.553-2.276A1 1 0 0121 8.618v6.764a1 1 0 01-1.447.894L15 14M5 18h8a2 2 0 002-2V8a2 2 0 00-2-2H5a2 2 0 00-2 2v8a2 2 0 002 2z"></path>
                            </svg>
                        ) : (
                            <svg className="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M15 10l4.553-2.276A1 1 0 0121 8.618v6.764a1 1 0 01-1.447.894L15 14M5 18h8a2 2 0 002-2V8a2 2 0 00-2-2H5a2 2 0 00-2 2v8a2 2 0 002 2z"></path>
                                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M3 3l18 18"></path>
                            </svg>
                        )}
                    </button>

                    <button
                        onClick={toggleChat}
                        className={`relative p-4 rounded-full transition-all ${isChatOpen ? 'bg-indigo-500 hover:bg-indigo-600 text-white shadow-lg shadow-indigo-500/20' : 'bg-gray-800 hover:bg-gray-700 text-white'}`}
                    >
                        <svg className="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M8 12h.01M12 12h.01M16 12h.01M21 12c0 4.418-4.03 8-9 8a9.863 9.863 0 01-4.255-.949L3 20l1.395-3.72C3.512 15.042 3 13.574 3 12c0-4.418 4.03-8 9-8s9 3.582 9 8z"></path>
                        </svg>
                        {hasUnread && !isChatOpen && (
                            <span className="absolute top-3 right-3 w-3 h-3 bg-red-500 rounded-full border-2 border-[#0A0A0B]"></span>
                        )}
                    </button>

                    <button
                        onClick={handleLeaveRoom}
                        className="bg-red-500 hover:bg-red-600 text-white font-medium py-3 px-8 rounded-full transition-colors shadow-lg shadow-red-500/20"
                    >
                        Leave Room
                    </button>
                </footer>
            </div>

            {/* Side Chat Panel (Full right side) */}
            <div
                className={`transition-all duration-300 ease-in-out h-full overflow-hidden z-30 shrink-0 bg-[#0A0A0B] ${isChatOpen ? 'w-[340px] opacity-100 border-l border-white/10' : 'w-0 opacity-0 border-none'
                    }`}
            >
                {isChatOpen && (
                    <ChatPanel
                        messages={messages}
                        onSend={handleSendChat}
                        onClose={() => setIsChatOpen(false)}
                    />
                )}
            </div>
        </div>
    );
};

export default RoomPage;
