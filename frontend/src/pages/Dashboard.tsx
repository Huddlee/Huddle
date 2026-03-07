import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { createRoom, joinRoom } from '../utils/api';
import Toast from '../components/Toast';
import { getOrCreateDisplayName, generateDisplayName } from '../utils/nameGenerator';

const Dashboard: React.FC = () => {
    const [roomCode, setRoomCode] = useState('');
    const [loadingCreate, setLoadingCreate] = useState(false);
    const [loadingJoin, setLoadingJoin] = useState(false);
    const [error, setError] = useState<string | null>(null);
    const [displayName, setDisplayName] = useState(getOrCreateDisplayName());
    const navigate = useNavigate();

    useEffect(() => {
        const token = localStorage.getItem('huddle_token');
        if (!token) {
            navigate('/');
        }
    }, [navigate]);

    const handleRegenerateName = () => {
        const newName = generateDisplayName();
        sessionStorage.setItem('huddle_display_name', newName);
        setDisplayName(newName);
    };

    const handleCreateRoom = async () => {
        try {
            setLoadingCreate(true);
            const token = localStorage.getItem('huddle_token');
            if (!token) throw new Error('No token found');

            const newRoomCode = await createRoom(token);
            sessionStorage.setItem('active_room', newRoomCode);
            navigate(`/room/${newRoomCode}`);
        } catch (error) {
            console.error('Failed to create room', error);
            setError('Failed to create room. Please try again.');
        } finally {
            setLoadingCreate(false);
        }
    };

    const handleJoinRoom = async (e: React.FormEvent) => {
        e.preventDefault();
        const trimmedCode = roomCode.trim();
        if (!trimmedCode) return;

        try {
            setLoadingJoin(true);
            const token = localStorage.getItem('huddle_token');
            if (!token) throw new Error('No token found');

            await joinRoom(trimmedCode, token);
            sessionStorage.setItem('active_room', trimmedCode);
            navigate(`/room/${trimmedCode}`);
        } catch (error) {
            console.error('Failed to join room', error);
            setError('Failed to join room. Please check the code and try again.');
        } finally {
            setLoadingJoin(false);
        }
    };

    return (
        <div className="min-h-screen bg-[#0A0A0B] text-white flex flex-col items-center pt-24 p-6 font-sans">
            {error && <Toast message={error} type="error" onClose={() => setError(null)} />}

            {/* Subtle Grid Background */}
            <div className="absolute inset-0 bg-[linear-gradient(rgba(255,255,255,0.03)_1px,transparent_1px),linear-gradient(90deg,rgba(255,255,255,0.03)_1px,transparent_1px)] bg-[size:40px_40px] pointer-events-none"></div>

            <div className="z-10 w-full max-w-4xl flex flex-col items-center">
                <header className="mb-10 text-center">
                    <h1 className="text-4xl md:text-5xl font-bold mb-4 tracking-tight">
                        Welcome back.
                    </h1>
                    <p className="text-gray-400 text-lg">
                        Create a new space or join an existing session.
                    </p>
                </header>

                {/* Anonymous Name Preview */}
                <div className="mb-10 w-full max-w-md bg-[#1C1C1E]/60 backdrop-blur-md border border-gray-800 rounded-2xl p-5 flex items-center justify-between">
                    <div>
                        <p className="text-xs text-gray-500 uppercase tracking-wider font-medium mb-1">Your anonymous name</p>
                        <p className="text-lg font-bold text-transparent bg-clip-text bg-gradient-to-r from-indigo-300 to-purple-300">{displayName}</p>
                    </div>
                    <button
                        onClick={handleRegenerateName}
                        className="flex items-center gap-2 bg-[#2A2A2D] hover:bg-[#353538] text-gray-300 hover:text-white text-sm font-medium px-4 py-2.5 rounded-xl transition-all border border-gray-700/50"
                        title="Generate a new anonymous name"
                    >
                        <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M4 4v5h.582m15.356 2A8.001 8.001 0 004.582 9m0 0H9m11 11v-5h-.581m0 0a8.003 8.003 0 01-15.357-2m15.357 2H15"></path>
                        </svg>
                        Shuffle
                    </button>
                </div>


                <div className="grid grid-cols-1 md:grid-cols-2 gap-6 w-full max-w-3xl">
                    {/* Create Room Card */}
                    <div className="bg-[#1C1C1E]/80 backdrop-blur-md border border-gray-800 rounded-3xl p-8 transition-all hover:bg-[#1C1C1E] hover:border-indigo-500/50 hover:shadow-[0_0_30px_rgba(99,102,241,0.1)] flex flex-col">
                        <div className="w-12 h-12 bg-indigo-500/20 text-indigo-400 rounded-xl flex items-center justify-center mb-6">
                            <svg className="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M12 4v16m8-8H4"></path>
                            </svg>
                        </div>
                        <h2 className="text-2xl font-bold mb-3">Create Room</h2>
                        <p className="text-gray-400 mb-8 flex-grow">
                            Start a new secure session and invite others to join instantly.
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
                                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M17 20h5v-2a3 3 0 00-5.356-1.857M17 20H7m10 0v-2c0-.656-.126-1.283-.356-1.857M7 20H2v-2a3 3 0 015.356-1.857M7 20v-2c0-.656.126-1.283.356-1.857m0 0a5.002 5.002 0 019.288 0M15 7a3 3 0 11-6 0 3 3 0 016 0zm6 3a2 2 0 11-4 0 2 2 0 014 0zM7 10a2 2 0 11-4 0 2 2 0 014 0z"></path>
                            </svg>
                        </div>
                        <h2 className="text-2xl font-bold mb-3">Join Room</h2>
                        <p className="text-gray-400 mb-8 flex-grow">
                            Enter a provided code to connect to an existing session.
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
            </div>
        </div >
    );
};

export default Dashboard;
