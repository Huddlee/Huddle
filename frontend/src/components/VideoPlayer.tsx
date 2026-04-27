import React, { useEffect, useRef, useState } from 'react';

interface VideoPlayerProps {
    stream: MediaStream | null;
    muted?: boolean;
    className?: string;
    isEnabled?: boolean;
}

const VideoPlayer: React.FC<VideoPlayerProps> = ({ stream, muted = false, className = '', isEnabled }) => {
    const videoRef = useRef<HTMLVideoElement>(null);
    const [isVideoTrackEnabled, setIsVideoTrackEnabled] = useState(false);

    useEffect(() => {
        if (videoRef.current && stream) {
            videoRef.current.srcObject = stream;
        }
    }, [stream]);

    // Reactively track whether the video track is enabled (handles remote mute/unmute)
    useEffect(() => {
        if (!stream) {
            setIsVideoTrackEnabled(false);
            return;
        }

        const videoTrack = stream.getVideoTracks()[0];
        if (!videoTrack) {
            setIsVideoTrackEnabled(false);
            return;
        }

        // Set initial state
        setIsVideoTrackEnabled(videoTrack.enabled && !videoTrack.muted);

        const handleMute = () => setIsVideoTrackEnabled(false);
        const handleUnmute = () => setIsVideoTrackEnabled(true);

        videoTrack.addEventListener('mute', handleMute);
        videoTrack.addEventListener('unmute', handleUnmute);

        return () => {
            videoTrack.removeEventListener('mute', handleMute);
            videoTrack.removeEventListener('unmute', handleUnmute);
        };
    }, [stream]);

    // Use prop if provided, otherwise fallback to track state
    const effectiveVideoEnabled = isEnabled !== undefined ? isEnabled : isVideoTrackEnabled;
    const showPlaceholder = !stream || !effectiveVideoEnabled;

    return (
        <div className={`relative overflow-hidden rounded-2xl bg-gray-900 border border-gray-800 shadow-lg ${className}`}>
            <video
                ref={videoRef}
                autoPlay
                playsInline
                muted={muted}
                className={`w-full h-full object-cover ${showPlaceholder ? 'invisible' : ''}`}
            />
            {showPlaceholder && (
                <div className="absolute inset-0 flex items-center justify-center bg-[#121214]">
                    <div className="w-20 h-20 bg-gray-800/80 rounded-full flex items-center justify-center shadow-lg border border-gray-700/50 backdrop-blur-md">
                        <svg className="w-10 h-10 text-gray-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M15 10l4.553-2.276A1 1 0 0121 8.618v6.764a1 1 0 01-1.447.894L15 14M5 18h8a2 2 0 002-2V8a2 2 0 00-2-2H5a2 2 0 00-2 2v8a2 2 0 002 2z"></path>
                            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M3 3l18 18"></path>
                        </svg>
                    </div>
                </div>
            )}
        </div>
    );
};

export default VideoPlayer;
