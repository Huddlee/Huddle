import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { guestLogin } from '../utils/api';
import Toast from '../components/Toast';

const LandingPage: React.FC = () => {
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState<string | null>(null);
    const navigate = useNavigate();

    const handleJoinAsGuest = async () => {
        try {
            setLoading(true);
            const token = await guestLogin();
            localStorage.setItem('huddle_token', token);
            navigate('/');
        } catch (error) {
            console.error('Login failed', error);
            setError('Failed to login as guest. Please ensure the backend is running.');
        } finally {
            setLoading(false);
        }
    };

    return (
        <div className="min-h-screen bg-gradient-to-br from-indigo-900 via-purple-900 to-black flex flex-col items-center justify-center text-white p-6 relative overflow-hidden">
            {error && <Toast message={error} type="error" onClose={() => setError(null)} />}

            {/* Abstract Background Shapes */}
            <div className="absolute top-[-10%] left-[-10%] w-96 h-96 bg-pink-500 rounded-full mix-blend-multiply filter blur-3xl opacity-30 animate-blob"></div>
            <div className="absolute top-[20%] right-[-10%] w-96 h-96 bg-purple-500 rounded-full mix-blend-multiply filter blur-3xl opacity-30 animate-blob animation-delay-2000"></div>
            <div className="absolute bottom-[-10%] left-[20%] w-96 h-96 bg-indigo-500 rounded-full mix-blend-multiply filter blur-3xl opacity-30 animate-blob animation-delay-4000"></div>

            <div className="z-10 bg-white/10 backdrop-blur-xl border border-white/20 p-10 rounded-3xl shadow-2xl flex flex-col items-center max-w-md w-full transition-transform hover:scale-[1.02] duration-300">
                <div className="mb-8 p-4 bg-white/5 rounded-full ring-1 ring-white/20 shadow-inner">
                    <svg className="w-16 h-16 text-indigo-400 drop-shadow-[0_0_10px_rgba(99,102,241,0.5)]" fill="none" stroke="currentColor" viewBox="0 0 24 24" xmlns="http://www.w3.org/2000/svg">
                        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="1.5" d="M15 10l4.553-2.276A1 1 0 0121 8.618v6.764a1 1 0 01-1.447.894L15 14M5 18h8a2 2 0 002-2V8a2 2 0 00-2-2H5a2 2 0 00-2 2v8a2 2 0 002 2z"></path>
                    </svg>
                </div>

                <h1 className="text-4xl md:text-5xl font-extrabold tracking-tight mb-4 text-transparent bg-clip-text bg-gradient-to-r from-indigo-200 to-pink-200">
                    Huddle.
                </h1>
                <p className="text-lg text-indigo-100/70 mb-10 text-center font-medium">
                    Connect instantly. No friction, pure collaboration.
                </p>

                <button
                    id="landing-guest"
                    onClick={handleJoinAsGuest}
                    disabled={loading}
                    className="group relative w-full flex items-center justify-center gap-3 bg-gradient-to-r from-indigo-500 to-purple-600 hover:from-indigo-400 hover:to-purple-500 text-white font-semibold text-lg py-4 px-8 rounded-2xl shadow-[0_0_20px_rgba(99,102,241,0.4)] hover:shadow-[0_0_30px_rgba(99,102,241,0.6)] transition-all duration-300 ease-out active:scale-95 disabled:opacity-70 disabled:cursor-not-allowed"
                >
                    {loading ? (
                        <svg className="animate-spin h-6 w-6 text-white" xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24">
                            <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4"></circle>
                            <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path>
                        </svg>
                    ) : (
                        <>
                            <span>Join as Guest</span>
                            <svg className="w-5 h-5 group-hover:translate-x-1 transition-transform" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M14 5l7 7m0 0l-7 7m7-7H3"></path>
                            </svg>
                        </>
                    )}
                </button>

                {/* Divider */}
                <div className="flex items-center gap-3 w-full my-4">
                    <div className="flex-1 h-px bg-white/15"></div>
                    <span className="text-xs text-indigo-200/40 font-medium uppercase tracking-wider">or</span>
                    <div className="flex-1 h-px bg-white/15"></div>
                </div>

                {/* Auth Buttons */}
                <div className="flex gap-3 w-full">
                    <button
                        id="landing-login"
                        onClick={() => navigate('/login')}
                        className="flex-1 flex items-center justify-center gap-2 bg-white/5 border border-white/15 hover:bg-white/10 hover:border-white/25 text-white font-semibold py-3.5 px-6 rounded-2xl transition-all duration-300 ease-out active:scale-95"
                    >
                        <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M11 16l-4-4m0 0l4-4m-4 4h14m-5 4v1a3 3 0 01-3 3H6a3 3 0 01-3-3V7a3 3 0 013-3h7a3 3 0 013 3v1" />
                        </svg>
                        <span>Log In</span>
                    </button>
                    <button
                        id="landing-signup"
                        onClick={() => navigate('/register')}
                        className="flex-1 flex items-center justify-center gap-2 bg-white/5 border border-white/15 hover:bg-white/10 hover:border-white/25 text-white font-semibold py-3.5 px-6 rounded-2xl transition-all duration-300 ease-out active:scale-95"
                    >
                        <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M18 9v3m0 0v3m0-3h3m-3 0h-3m-2-5a4 4 0 11-8 0 4 4 0 018 0zM3 20a6 6 0 0112 0v1H3v-1z" />
                        </svg>
                        <span>Sign Up</span>
                    </button>
                </div>
            </div>

            <div className="absolute bottom-6 text-sm text-indigo-200/50 font-medium">
                Premium video collaboration
            </div>
        </div>
    );
};

export default LandingPage;
