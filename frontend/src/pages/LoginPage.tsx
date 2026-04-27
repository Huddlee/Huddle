import React, { useState, useEffect } from 'react';
import { useNavigate, Link, useSearchParams } from 'react-router-dom';
import useDocumentTitle from '../hooks/useDocumentTitle';
import LiquidEther from '../components/LiquidEther/LiquidEther';
import { loginUser } from '../utils/api';
import Toast from '../components/Toast';

const ETHER_COLORS = ['#5227FF', '#FF9FFC', '#B497CF'];

const LoginPage: React.FC = () => {
    useDocumentTitle('Login | HUDDLE');
    const [username, setUsername] = useState('');
    const [password, setPassword] = useState('');
    const [loading, setLoading] = useState(false);
    const [toast, setToast] = useState<{ message: string; type: 'success' | 'error' | 'info' } | null>(null);
    const navigate = useNavigate();
    const [searchParams] = useSearchParams();

    useEffect(() => {
        if (searchParams.get('expired') === 'true') {
            setToast({ message: 'Session expired. Please log in again.', type: 'error' });
        }
    }, [searchParams]);

    const handleSubmit = async (e: React.FormEvent) => {
        e.preventDefault();
        try {
            setLoading(true);
            const { token } = await loginUser({ username, password });
            localStorage.setItem('huddle_token', token);
            navigate('/');
        } catch (err: any) {
            setToast({ message: 'Invalid credentials. Please try again.', type: 'error' });
        } finally {
            setLoading(false);
        }
    };

    return (
        <div className="min-h-screen bg-black flex flex-col items-center justify-center text-white p-6 relative overflow-hidden">
            {toast && <Toast message={toast.message} type={toast.type} onClose={() => setToast(null)} />}

            {/* Background */}
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

            <div className="z-10 bg-white/10 backdrop-blur-xl border border-white/20 p-10 rounded-3xl shadow-2xl flex flex-col items-center max-w-md w-full transition-transform hover:scale-[1.02] duration-300">
                <div className="mb-6 p-4 bg-white/5 rounded-full ring-1 ring-white/20 shadow-inner">
                    <svg className="w-12 h-12 text-indigo-400 drop-shadow-[0_0_10px_rgba(99,102,241,0.5)]" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="1.5" d="M11 16l-4-4m0 0l4-4m-4 4h14m-5 4v1a3 3 0 01-3 3H6a3 3 0 01-3-3V7a3 3 0 013-3h7a3 3 0 013 3v1" />
                    </svg>
                </div>

                <h1 className="text-3xl md:text-4xl font-extrabold tracking-tight mb-2 text-transparent bg-clip-text bg-gradient-to-r from-indigo-200 to-pink-200">
                    Welcome Back
                </h1>
                <p className="text-sm text-indigo-100/60 mb-8 text-center font-medium">
                    Sign in to your Huddle account
                </p>

                <form onSubmit={handleSubmit} className="w-full flex flex-col gap-4">
                    <div className="relative">
                        <input
                            id="login-username"
                            type="text"
                            placeholder="Username"
                            value={username}
                            onChange={(e) => setUsername(e.target.value)}
                            required
                            className="w-full bg-white/5 border border-white/15 rounded-xl px-4 py-3.5 text-white placeholder-indigo-200/40 focus:outline-none focus:ring-2 focus:ring-indigo-500/50 focus:border-indigo-500/50 transition-all duration-200"
                        />
                    </div>
                    <div className="relative">
                        <input
                            id="login-password"
                            type="password"
                            placeholder="Password"
                            value={password}
                            onChange={(e) => setPassword(e.target.value)}
                            required
                            className="w-full bg-white/5 border border-white/15 rounded-xl px-4 py-3.5 text-white placeholder-indigo-200/40 focus:outline-none focus:ring-2 focus:ring-indigo-500/50 focus:border-indigo-500/50 transition-all duration-200"
                        />
                    </div>

                    <button
                        id="login-submit"
                        type="submit"
                        disabled={loading}
                        className="group relative w-full flex items-center justify-center gap-3 bg-black hover:bg-gray-900 border border-white/10 text-white font-semibold text-lg py-4 px-8 rounded-2xl shadow-lg hover:shadow-xl transition-all duration-300 ease-out active:scale-95 disabled:opacity-70 disabled:cursor-not-allowed mt-2"
                    >
                        {loading ? (
                            <svg className="animate-spin h-6 w-6 text-white" xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24">
                                <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4"></circle>
                                <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path>
                            </svg>
                        ) : (
                            <>
                                <span>Log In</span>
                                <svg className="w-5 h-5 group-hover:translate-x-1 transition-transform" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M14 5l7 7m0 0l-7 7m7-7H3" />
                                </svg>
                            </>
                        )}
                    </button>
                </form>

                <p className="mt-6 text-sm text-indigo-200/50">
                    Don't have an account?{' '}
                    <Link to="/register" className="text-indigo-300 hover:text-indigo-200 font-semibold transition-colors underline underline-offset-2">
                        Sign Up
                    </Link>
                </p>
            </div>

            <div className="absolute bottom-6 text-sm text-indigo-200/50 font-medium">
                Premium video collaboration
            </div>
        </div>
    );
};

export default LoginPage;
