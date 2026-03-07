import React, { useState, useEffect } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import { createRoom, joinRoom } from '../utils/api';
import Toast from '../components/Toast';
import { getOrCreateDisplayName, generateDisplayName } from '../utils/nameGenerator';

const Dashboard: React.FC = () => {
    const [roomCode, setRoomCode] = useState('');
    const [loadingCreate, setLoadingCreate] = useState(false);
    const [loadingJoin, setLoadingJoin] = useState(false);
    const [toast, setToast] = useState<{ message: string; type: 'success' | 'error' | 'info' } | null>(null);
    const [displayName, setDisplayName] = useState(getOrCreateDisplayName());
    const [isAuthenticated, setIsAuthenticated] = useState(false);
    const navigate = useNavigate();

    useEffect(() => {
        const token = localStorage.getItem('huddle_token');
        if (!token) {
            setIsAuthenticated(false);
        } else {
            setIsAuthenticated(true);
        }
    }, []);

    const handleRegenerateName = () => {
        const newName = generateDisplayName();
        sessionStorage.setItem('huddle_display_name', newName);
        setDisplayName(newName);
    };



    const handleCreateRoom = async () => {
        if (!isAuthenticated) {
            navigate('/auth');
            return;
        }
        try {
            setLoadingCreate(true);
            const token = localStorage.getItem('huddle_token');
            if (!token) throw new Error('No token found');
            const newRoomCode = await createRoom(token);
            sessionStorage.setItem('active_room', newRoomCode);
            navigate(`/room/${newRoomCode}`);
        } catch (error) {
            console.error('Failed to create room', error);
            setToast({ message: 'Failed to create room. Please try again.', type: 'error' });
        } finally {
            setLoadingCreate(false);
        }
    };

    const handleJoinRoom = async (e: React.FormEvent) => {
        e.preventDefault();
        const trimmedCode = roomCode.trim();
        if (!trimmedCode) return;
        if (!isAuthenticated) {
            navigate('/auth');
            return;
        }
        try {
            setLoadingJoin(true);
            const token = localStorage.getItem('huddle_token');
            if (!token) throw new Error('No token found');
            await joinRoom(trimmedCode, token);
            sessionStorage.setItem('active_room', trimmedCode);
            navigate(`/room/${trimmedCode}`);
        } catch (error) {
            console.error('Failed to join room', error);
            setToast({ message: 'Failed to join room. Please check the code and try again.', type: 'error' });
        } finally {
            setLoadingJoin(false);
        }
    };

    return (
        <div className="min-h-screen bg-[#0A0A0B] text-white flex flex-col font-sans relative">
            {toast && <Toast message={toast.message} type={toast.type} onClose={() => setToast(null)} />}

            {/* Subtle Grid Background */}
            <div className="absolute inset-0 bg-[linear-gradient(rgba(255,255,255,0.03)_1px,transparent_1px),linear-gradient(90deg,rgba(255,255,255,0.03)_1px,transparent_1px)] bg-[size:40px_40px] pointer-events-none"></div>

            {/* Top-left Huddle Logo */}
            <nav className="z-10 w-full px-8 py-6 flex items-center justify-between">
                <Link to="/about" className="group flex items-center gap-3 transition-all duration-200 hover:opacity-80">
                    <div className="w-10 h-10 bg-gradient-to-br from-indigo-500 to-purple-600 rounded-xl flex items-center justify-center shadow-[0_0_15px_rgba(99,102,241,0.3)] group-hover:shadow-[0_0_20px_rgba(99,102,241,0.5)] transition-shadow">
                        <svg className="w-5 h-5 text-white" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M15 10l4.553-2.276A1 1 0 0121 8.618v6.764a1 1 0 01-1.447.894L15 14M5 18h8a2 2 0 002-2V8a2 2 0 00-2-2H5a2 2 0 00-2 2v8a2 2 0 002 2z" />
                        </svg>
                    </div>
                    <span className="text-xl font-bold tracking-tight text-transparent bg-clip-text bg-gradient-to-r from-indigo-300 to-purple-300">Huddle</span>
                </Link>

                {/* Auth Actions */}
                <div className="flex items-center gap-3">
                    {isAuthenticated ? (
                        <button
                            onClick={() => {
                                localStorage.removeItem('huddle_token');
                                setIsAuthenticated(false);
                                setToast({ message: 'Logged out successfully.', type: 'info' });
                            }}
                            className="text-sm text-gray-400 hover:text-white font-medium px-4 py-2 rounded-xl border border-gray-700/50 hover:border-gray-600 transition-all"
                        >
                            Log Out
                        </button>
                    ) : (
                        <>
                            <button
                                onClick={() => navigate('/login')}
                                className="text-sm text-gray-300 hover:text-white font-medium px-4 py-2 rounded-xl transition-all"
                            >
                                Log In
                            </button>
                            <button
                                onClick={() => navigate('/register')}
                                className="text-sm text-white font-medium px-4 py-2 rounded-xl bg-indigo-600 hover:bg-indigo-500 transition-all"
                            >
                                Sign Up
                            </button>
                        </>
                    )}
                </div>
            </nav>

            {/* Main Content */}
            <div className="z-10 flex-1 flex flex-col items-center justify-center px-6 pb-20 -mt-8">


                {/* Anonymous Name */}
                <div className="mb-8 w-full max-w-md bg-[#1C1C1E]/60 backdrop-blur-md border border-gray-800 rounded-2xl p-5 flex items-center justify-between gap-3">
                    <div className="flex-1">
                        <p className="text-xs text-gray-500 uppercase tracking-wider font-medium mb-2">Your display name</p>
                        <input
                            type="text"
                            value={displayName}
                            onChange={(e) => {
                                setDisplayName(e.target.value);
                                sessionStorage.setItem('huddle_display_name', e.target.value);
                            }}
                            className="w-full bg-[#2A2A2D] border border-gray-700 text-white text-base font-bold rounded-xl px-4 py-2.5 focus:outline-none focus:border-indigo-500 focus:ring-1 focus:ring-indigo-500 transition-all"
                            placeholder="Enter your name"
                        />
                    </div>
                    <button
                        onClick={handleRegenerateName}
                        className="flex items-center gap-2 bg-[#2A2A2D] hover:bg-[#353538] text-gray-300 hover:text-white text-sm font-medium px-4 py-2.5 rounded-xl transition-all border border-gray-700/50 self-end"
                        title="Generate a random name"
                    >
                        <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M4 4v5h.582m15.356 2A8.001 8.001 0 004.582 9m0 0H9m11 11v-5h-.581m0 0a8.003 8.003 0 01-15.357-2m15.357 2H15" />
                        </svg>
                        Shuffle
                    </button>
                </div>

                {/* Create / Join Cards */}
                <div className="grid grid-cols-1 md:grid-cols-2 gap-6 w-full max-w-3xl">
                    {/* Create Room Card */}
                    <div className="bg-[#1C1C1E]/80 backdrop-blur-md border border-gray-800 rounded-3xl p-8 transition-all hover:bg-[#1C1C1E] hover:border-indigo-500/50 hover:shadow-[0_0_30px_rgba(99,102,241,0.1)] flex flex-col">
                        <div className="w-12 h-12 bg-indigo-500/20 text-indigo-400 rounded-xl flex items-center justify-center mb-6">
                            <svg className="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M12 4v16m8-8H4" />
                            </svg>
                        </div>
                        <h2 className="text-2xl font-bold mb-3">Create Room</h2>
                        <p className="text-gray-400 mb-8 flex-grow">
                            Start a new secure, peer-to-peer session and invite others to join.
                        </p>
                        <button
                            onClick={handleCreateRoom}
                            disabled={loadingCreate}
                            className="w-full bg-indigo-600 hover:bg-indigo-500 text-white font-medium py-3.5 px-6 rounded-xl transition-colors disabled:opacity-50 flex justify-center items-center h-[52px]"
                        >
                            {loadingCreate ? (
                                <svg className="animate-spin h-5 w-5" xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24">
                                    <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4"></circle>
                                    <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path>
                                </svg>
                            ) : "New Meeting"}
                        </button>
                    </div>

                    {/* Join Room Card */}
                    <div className="bg-[#1C1C1E]/80 backdrop-blur-md border border-gray-800 rounded-3xl p-8 transition-all hover:bg-[#1C1C1E] hover:border-purple-500/50 hover:shadow-[0_0_30px_rgba(168,85,247,0.1)] flex flex-col">
                        <div className="w-12 h-12 bg-purple-500/20 text-purple-400 rounded-xl flex items-center justify-center mb-6">
                            <svg className="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M17 20h5v-2a3 3 0 00-5.356-1.857M17 20H7m10 0v-2c0-.656-.126-1.283-.356-1.857M7 20H2v-2a3 3 0 015.356-1.857M7 20v-2c0-.656.126-1.283.356-1.857m0 0a5.002 5.002 0 019.288 0M15 7a3 3 0 11-6 0 3 3 0 016 0zm6 3a2 2 0 11-4 0 2 2 0 014 0zM7 10a2 2 0 11-4 0 2 2 0 014 0z" />
                            </svg>
                        </div>
                        <h2 className="text-2xl font-bold mb-3">Join Room</h2>
                        <p className="text-gray-400 mb-8 flex-grow">
                            Enter a room code to connect directly, peer-to-peer.
                        </p>
                        <form onSubmit={handleJoinRoom} className="flex gap-3 h-[52px]">
                            <input
                                type="text"
                                placeholder="Room code"
                                value={roomCode}
                                onChange={(e) => setRoomCode(e.target.value)}
                                className="flex-grow bg-[#2A2A2D] border border-gray-700 text-white text-base rounded-xl px-4 py-3 focus:outline-none focus:border-purple-500 focus:ring-1 focus:ring-purple-500 transition-all font-mono"
                            />
                            <button
                                type="submit"
                                disabled={!roomCode.trim() || loadingJoin}
                                className="bg-purple-600 hover:bg-purple-500 text-white font-medium px-6 rounded-xl transition-colors disabled:opacity-50 disabled:cursor-not-allowed flex justify-center items-center min-w-[80px]"
                            >
                                {loadingJoin ? (
                                    <svg className="animate-spin h-5 w-5" xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24">
                                        <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4"></circle>
                                        <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path>
                                    </svg>
                                ) : "Join"}
                            </button>
                        </form>
                    </div>
                </div>

                {/* Privacy-first description */}
                <div className="mt-12 w-full max-w-3xl">
                    <div className="bg-[#1C1C1E]/40 backdrop-blur-md border border-gray-800/60 rounded-2xl p-8">
                        <div className="flex items-start gap-4">
                            <div className="w-10 h-10 bg-emerald-500/15 text-emerald-400 rounded-xl flex items-center justify-center shrink-0 mt-0.5">
                                <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M12 15v2m-6 4h12a2 2 0 002-2v-6a2 2 0 00-2-2H6a2 2 0 00-2 2v6a2 2 0 002 2zm10-10V7a4 4 0 00-8 0v4h8z" />
                                </svg>
                            </div>
                            <div>
                                <h3 className="text-lg font-bold mb-2 text-white">Privacy by Design</h3>
                                <p className="text-gray-400 text-sm leading-relaxed">
                                    Huddle uses <strong className="text-gray-300">WebRTC mesh topology</strong> — your video and audio streams travel directly between participants, never through a central server.
                                    No one can intercept, record, or monitor your conversations. Your data stays yours.
                                </p>
                                <Link to="/about" className="inline-flex items-center gap-1.5 text-indigo-400 hover:text-indigo-300 text-sm font-medium mt-3 transition-colors group">
                                    Learn how Huddle works
                                    <svg className="w-3.5 h-3.5 group-hover:translate-x-0.5 transition-transform" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M14 5l7 7m0 0l-7 7m7-7H3" />
                                    </svg>
                                </Link>
                            </div>
                        </div>
                    </div>
                </div>
            </div>
        </div>
    );
};

export default Dashboard;
