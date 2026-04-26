import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import LiquidEther from '../components/LiquidEther/LiquidEther';
import { guestLogin } from '../utils/api';
import Toast from '../components/Toast';

const ETHER_COLORS = ['#5227FF', '#FF9FFC', '#B497CF'];

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
        <div className="min-h-screen bg-[#0A0A0B] bg-[radial-gradient(ellipse_at_top,_var(--tw-gradient-stops))] from-indigo-900/20 via-[#0A0A0B] to-[#0A0A0B] flex flex-col items-center justify-center text-white p-6 relative overflow-hidden">
            <div className="absolute inset-0 pointer-events-none z-0">
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
            {/* Subtle Grid Background */}
            <div className="absolute inset-0 bg-[linear-gradient(rgba(255,255,255,0.03)_1px,transparent_1px),linear-gradient(90deg,rgba(255,255,255,0.03)_1px,transparent_1px)] bg-[size:40px_40px] pointer-events-none"></div>

            {error && <Toast message={error} type="error" onClose={() => setError(null)} />}

            <div className="z-10 bg-transparent backdrop-blur-xl border border-white/10 p-10 rounded-3xl shadow-2xl flex flex-col items-center max-w-md w-full transition-transform hover:scale-[1.02] duration-300">
                <div className="mb-8 p-4 bg-white/5 rounded-full ring-1 ring-white/20 shadow-inner">
                    <svg className="w-16 h-16 text-indigo-400 drop-shadow-[0_0_10px_rgba(99,102,241,0.5)]" fill="none" stroke="currentColor" viewBox="0 0 24 24" xmlns="http://www.w3.org/2000/svg">
                        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="1.5" d="M15 10l4.553-2.276A1 1 0 0121 8.618v6.764a1 1 0 01-1.447.894L15 14M5 18h8a2 2 0 002-2V8a2 2 0 00-2-2H5a2 2 0 00-2 2v8a2 2 0 002 2z"></path>
                    </svg>
                </div>

                <h1 className="text-7xl md:text-8xl font-extrabold tracking-tight mb-4 text-transparent bg-clip-text bg-gradient-to-r from-indigo-200 to-pink-200">
                    Huddle
                </h1>
                <p className="text-lg text-indigo-100/70 mb-10 text-center font-medium">
                    Connect instantly. No friction, pure collaboration.
                </p>

                <button
                    id="landing-guest"
                    onClick={handleJoinAsGuest}
                    disabled={loading}
                    className="group relative w-full flex items-center justify-center gap-3 bg-black hover:bg-gray-900 border border-white/10 text-white font-semibold text-lg py-4 px-8 rounded-2xl shadow-lg hover:shadow-xl transition-all duration-300 ease-out active:scale-95 disabled:opacity-70 disabled:cursor-not-allowed"
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
