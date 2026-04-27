import React, { useEffect, useState, useRef, useCallback } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import useDocumentTitle from '../hooks/useDocumentTitle';
import { getOrCreateDisplayName } from '../utils/nameGenerator';
import LiquidEther from '../components/LiquidEther/LiquidEther';

const ETHER_COLORS = ['#5227FF', '#FF9FFC', '#B497CF'];

const PreJoinPage: React.FC = () => {
    useDocumentTitle('One step away | HUDDLE');
    const { roomCode } = useParams<{ roomCode: string }>();
    const navigate = useNavigate();

    const displayName = getOrCreateDisplayName();

    const [localStream, setLocalStream] = useState<MediaStream | null>(null);
    const [isCameraOn, setIsCameraOn] = useState(true);
    const [isMicOn, setIsMicOn] = useState(true);
    const [permissionError, setPermissionError] = useState<string | null>(null);
    const [isJoining, setIsJoining] = useState(false);

    const videoRef = useRef<HTMLVideoElement>(null);
    const streamRef = useRef<MediaStream | null>(null);
    const animFrameRef = useRef<number>(0);

    // Request media permissions and set up stream
    const initMedia = useCallback(async () => {
        try {
            const stream = await navigator.mediaDevices.getUserMedia({
                video: true,
                audio: true,
            });
            setLocalStream(stream);
            streamRef.current = stream;
            setPermissionError(null);
        } catch (err: unknown) {
            console.error('Media permission error:', err);
            if (err instanceof DOMException) {
                if (err.name === 'NotAllowedError') {
                    setPermissionError('Camera and microphone access was denied. Please allow permissions in your browser settings.');
                } else if (err.name === 'NotFoundError') {
                    setPermissionError('No camera or microphone found. Please connect a device and try again.');
                } else {
                    setPermissionError(`Could not access media devices: ${err.message}`);
                }
            } else {
                setPermissionError('An unexpected error occurred while accessing media devices.');
            }
        }
    }, []);

    useEffect(() => {
        // Validate session
        const token = localStorage.getItem('huddle_token');
        if (!token) {
            navigate('/');
            return;
        }
        const activeRoom = sessionStorage.getItem('active_room');
        if (activeRoom !== roomCode) {
            navigate('/');
            return;
        }

        initMedia();

        return () => {
            // Cleanup on unmount (only if NOT joining — if joining, RoomPage takes over the stream)
            cancelAnimationFrame(animFrameRef.current);
            // Stream cleanup is handled conditionally
        };
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, []);

    // Attach stream to video element
    useEffect(() => {
        if (videoRef.current && localStream) {
            videoRef.current.srcObject = localStream;
        }
    }, [localStream]);

    const toggleCamera = () => {
        if (!localStream) return;
        const videoTrack = localStream.getVideoTracks()[0];
        if (videoTrack) {
            if (isCameraOn) {
                videoTrack.enabled = false;
            } else {
                videoTrack.enabled = true;
            }
            setIsCameraOn(!isCameraOn);
        }
    };

    const toggleMic = () => {
        if (!localStream) return;
        const audioTrack = localStream.getAudioTracks()[0];
        if (audioTrack) {
            audioTrack.enabled = !audioTrack.enabled;
            setIsMicOn(audioTrack.enabled);
        }
    };

    const handleJoinRoom = () => {
        setIsJoining(true);
        // Store the user's media preferences so RoomPage can pick them up
        sessionStorage.setItem('prejoin_camera', isCameraOn ? 'on' : 'off');
        sessionStorage.setItem('prejoin_mic', isMicOn ? 'on' : 'off');

        // Stop the preview stream — RoomPage will create its own
        cancelAnimationFrame(animFrameRef.current);
        if (streamRef.current) {
            streamRef.current.getTracks().forEach(track => track.stop());
        }

        navigate(`/room/${roomCode}`, { replace: true });
    };

    const handleGoBack = () => {
        // Clean up stream
        cancelAnimationFrame(animFrameRef.current);
        if (streamRef.current) {
            streamRef.current.getTracks().forEach(track => track.stop());
        }
        sessionStorage.removeItem('active_room');
        navigate('/');
    };


    return (
        <div className="h-screen bg-black text-white flex flex-col items-center justify-center font-sans relative overflow-hidden">
            {/* LiquidEther Background */}
            <div className="absolute inset-0 z-0">
                <LiquidEther
                    colors={ETHER_COLORS}
                    mouseForce={20}
                    cursorSize={100}
                    isViscous={false}
                    viscous={30}
                    iterationsViscous={32}
                    iterationsPoisson={32}
                    resolution={0.5}
                    isBounce={false}
                    autoDemo={true}
                    autoSpeed={0.5}
                    autoIntensity={2.2}
                    takeoverDuration={0.25}
                    autoResumeDelay={3000}
                    autoRampDuration={0.6}
                />
            </div>

            {/* Main Content */}
            <div className="relative z-10 flex flex-col items-center w-full max-w-2xl px-6">
                {/* Header */}
                <div className="mb-8 text-center">
                    <h1 className="text-3xl font-extrabold tracking-tight mb-2">
                        Ready to join?
                    </h1>
                    <p className="text-gray-400 text-sm">
                        Check your camera and microphone before entering the room
                    </p>
                </div>

                {/* Video Preview Card */}
                <div className="w-full max-w-lg bg-[#121214] rounded-3xl overflow-hidden ring-1 ring-white/10 shadow-2xl shadow-black/40 relative">
                    {/* Video Area */}
                    <div className="relative aspect-video bg-[#0D0D0F] overflow-hidden">
                        <video
                            ref={videoRef}
                            autoPlay
                            playsInline
                            muted
                            className={`w-full h-full object-cover scale-x-[-1] ${!isCameraOn ? 'invisible' : ''}`}
                        />

                        {/* Camera off overlay */}
                        {!isCameraOn && (
                            <div className="absolute inset-0 flex flex-col items-center justify-center bg-[#0D0D0F]">
                                <div className="w-24 h-24 bg-gradient-to-br from-indigo-500/20 to-purple-500/20 rounded-full flex items-center justify-center mb-4 ring-1 ring-white/10">
                                    <span className="text-3xl font-bold text-indigo-300 uppercase">
                                        {displayName.charAt(0)}
                                    </span>
                                </div>
                                <p className="text-gray-500 text-sm font-medium">Camera is off</p>
                            </div>
                        )}

                        {/* Permission error overlay */}
                        {permissionError && (
                            <div className="absolute inset-0 flex flex-col items-center justify-center bg-[#0D0D0F] px-8">
                                <div className="w-16 h-16 bg-red-500/15 rounded-full flex items-center justify-center mb-4">
                                    <svg className="w-8 h-8 text-red-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-2.5L13.732 4c-.77-.833-1.964-.833-2.732 0L4.082 16.5c-.77.833.192 2.5 1.732 2.5z" />
                                    </svg>
                                </div>
                                <p className="text-red-300 text-sm text-center font-medium leading-relaxed">{permissionError}</p>
                                <button
                                    onClick={initMedia}
                                    className="mt-4 text-sm text-indigo-400 hover:text-indigo-300 font-medium transition-colors underline underline-offset-2"
                                >
                                    Retry
                                </button>
                            </div>
                        )}

                        {/* Name badge */}
                        <div className="absolute bottom-3 left-3 bg-black/60 px-3 py-1.5 rounded-lg text-sm font-semibold backdrop-blur-md border border-white/10 flex items-center gap-2">
                            <span className="text-indigo-300">{displayName}</span>
                            {!isMicOn && (
                                <svg className="w-4 h-4 text-red-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M5.586 15H4a1 1 0 01-1-1v-4a1 1 0 011-1h1.586l4.707-4.707C10.923 3.663 12 4.109 12 5v14c0 .891-1.077 1.337-1.707.707L5.586 15z" />
                                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M17 14l2-2m0 0l2-2m-2 2l-2-2m2 2l2 2" />
                                </svg>
                            )}
                        </div>

                        {/* Room code badge */}
                        <div className="absolute top-3 right-3 bg-black/60 px-3 py-1.5 rounded-lg text-xs font-mono backdrop-blur-md border border-white/10 text-gray-400">
                            Room: <span className="text-indigo-400">{roomCode}</span>
                        </div>
                    </div>

                    {/* Controls Bar */}
                    <div className="px-6 py-4 flex items-center justify-between bg-[#161618] border-t border-white/5">
                        <div className="flex items-center gap-3">
                            {/* Mic Toggle */}
                            <button
                                onClick={toggleMic}
                                disabled={!!permissionError}
                                className={`relative p-3 rounded-full transition-all duration-200 ${isMicOn && !permissionError
                                    ? 'bg-[#1C1C1E] hover:bg-[#252528] text-white ring-1 ring-white/10'
                                    : 'bg-red-500/15 text-red-400 ring-1 ring-red-500/30 hover:bg-red-500/25'
                                    } disabled:opacity-40 disabled:cursor-not-allowed`}
                            >
                                {isMicOn ? (
                                    <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M19 11a7 7 0 01-7 7m0 0a7 7 0 01-7-7m7 7v4m0 0H8m4 0h4m-4-8a3 3 0 01-3-3V5a3 3 0 116 0v6a3 3 0 01-3 3z" />
                                    </svg>
                                ) : (
                                    <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M5.586 15H4a1 1 0 01-1-1v-4a1 1 0 011-1h1.586l4.707-4.707C10.923 3.663 12 4.109 12 5v14c0 .891-1.077 1.337-1.707.707L5.586 15z" />
                                        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M17 14l2-2m0 0l2-2m-2 2l-2-2m2 2l2 2" />
                                    </svg>
                                )}
                            </button>


                            {/* Camera Toggle */}
                            <button
                                onClick={toggleCamera}
                                disabled={!!permissionError}
                                className={`relative p-3 rounded-full transition-all duration-200 ${isCameraOn && !permissionError
                                    ? 'bg-[#1C1C1E] hover:bg-[#252528] text-white ring-1 ring-white/10'
                                    : 'bg-red-500/15 text-red-400 ring-1 ring-red-500/30 hover:bg-red-500/25'
                                    } disabled:opacity-40 disabled:cursor-not-allowed`}
                            >
                                {isCameraOn ? (
                                    <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M15 10l4.553-2.276A1 1 0 0121 8.618v6.764a1 1 0 01-1.447.894L15 14M5 18h8a2 2 0 002-2V8a2 2 0 00-2-2H5a2 2 0 00-2 2v8a2 2 0 002 2z" />
                                    </svg>
                                ) : (
                                    <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M15 10l4.553-2.276A1 1 0 0121 8.618v6.764a1 1 0 01-1.447.894L15 14M5 18h8a2 2 0 002-2V8a2 2 0 00-2-2H5a2 2 0 00-2 2v8a2 2 0 002 2z" />
                                        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M3 3l18 18" />
                                    </svg>
                                )}
                            </button>
                        </div>

                        {/* Status Labels */}
                        <div className="flex items-center gap-4 text-xs text-gray-500 font-medium">
                            <span className={isMicOn && !permissionError ? 'text-emerald-400' : 'text-red-400'}>
                                {isMicOn && !permissionError ? 'Mic on' : 'Mic off'}
                            </span>
                            <span className={isCameraOn && !permissionError ? 'text-emerald-400' : 'text-red-400'}>
                                {isCameraOn && !permissionError ? 'Camera on' : 'Camera off'}
                            </span>
                        </div>
                    </div>
                </div>

                {/* Action Buttons */}
                <div className="mt-8 flex items-center gap-4 w-full max-w-lg">
                    <button
                        onClick={handleGoBack}
                        className="flex-1 py-3.5 px-6 rounded-xl text-white font-medium bg-red-600 hover:bg-red-700 border border-red-500/30 transition-all shadow-lg shadow-red-500/10"
                    >
                        Go Back
                    </button>
                    <button
                        onClick={handleJoinRoom}
                        disabled={isJoining}
                        className="flex-[2] py-3.5 px-6 rounded-xl text-white font-semibold bg-black hover:bg-gray-900 border border-white/10 transition-all shadow-lg hover:shadow-xl disabled:opacity-50 disabled:cursor-not-allowed flex items-center justify-center gap-2"
                    >
                        {isJoining ? (
                            <>
                                <svg className="animate-spin h-5 w-5" fill="none" viewBox="0 0 24 24">
                                    <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4" />
                                    <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z" />
                                </svg>
                                Joining...
                            </>
                        ) : (
                            <>
                                Join Room
                                <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M14 5l7 7m0 0l-7 7m7-7H3" />
                                </svg>
                            </>
                        )}
                    </button>
                </div>

                {/* Tip */}
                <p className="mt-6 text-xs text-gray-600 text-center">
                    You can always adjust your camera and microphone inside the room
                </p>
            </div>
        </div>
    );
};

export default PreJoinPage;
