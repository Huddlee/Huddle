import React, { useState } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import { registerUser } from '../utils/api';
import Toast from '../components/Toast';

const RegisterPage: React.FC = () => {
    const [username, setUsername] = useState('');
    const [email, setEmail] = useState('');
    const [password, setPassword] = useState('');
    const [loading, setLoading] = useState(false);
    const [toast, setToast] = useState<{ message: string; type: 'success' | 'error' | 'info' } | null>(null);
    const navigate = useNavigate();

    const handleSubmit = async (e: React.FormEvent) => {
        e.preventDefault();
        try {
            setLoading(true);
            await registerUser({ username, password, email });
            setToast({ message: 'User registered successfully!', type: 'success' });
            setTimeout(() => navigate('/login'), 1500);
        } catch (err: any) {
            const errorMsg =
                err.response?.data || 'Registration failed. Please try again.';
            setToast({ message: String(errorMsg), type: 'error' });
        } finally {
            setLoading(false);
        }
    };

    return (
        <div className="min-h-screen bg-gradient-to-br from-indigo-900 via-purple-900 to-black flex flex-col items-center justify-center text-white p-6 relative overflow-hidden">
            {toast && <Toast message={toast.message} type={toast.type} onClose={() => setToast(null)} />}

            {/* Abstract Background Shapes */}
            <div className="absolute top-[-10%] left-[-10%] w-96 h-96 bg-pink-500 rounded-full mix-blend-multiply filter blur-3xl opacity-30 animate-blob"></div>
            <div className="absolute top-[20%] right-[-10%] w-96 h-96 bg-purple-500 rounded-full mix-blend-multiply filter blur-3xl opacity-30 animate-blob animation-delay-2000"></div>
            <div className="absolute bottom-[-10%] left-[20%] w-96 h-96 bg-indigo-500 rounded-full mix-blend-multiply filter blur-3xl opacity-30 animate-blob animation-delay-4000"></div>

            <div className="z-10 bg-white/10 backdrop-blur-xl border border-white/20 p-10 rounded-3xl shadow-2xl flex flex-col items-center max-w-md w-full transition-transform hover:scale-[1.02] duration-300">
                <div className="mb-6 p-4 bg-white/5 rounded-full ring-1 ring-white/20 shadow-inner">
                    <svg className="w-12 h-12 text-indigo-400 drop-shadow-[0_0_10px_rgba(99,102,241,0.5)]" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="1.5" d="M18 9v3m0 0v3m0-3h3m-3 0h-3m-2-5a4 4 0 11-8 0 4 4 0 018 0zM3 20a6 6 0 0112 0v1H3v-1z" />
                    </svg>
                </div>

                <h1 className="text-3xl md:text-4xl font-extrabold tracking-tight mb-2 text-transparent bg-clip-text bg-gradient-to-r from-indigo-200 to-pink-200">
                    Create Account
                </h1>
                <p className="text-sm text-indigo-100/60 mb-8 text-center font-medium">
                    Join Huddle and start collaborating
                </p>

                <form onSubmit={handleSubmit} className="w-full flex flex-col gap-4">
                    <div className="relative">
                        <input
                            id="register-username"
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
                            id="register-email"
                            type="email"
                            placeholder="Email"
                            value={email}
                            onChange={(e) => setEmail(e.target.value)}
                            required
                            className="w-full bg-white/5 border border-white/15 rounded-xl px-4 py-3.5 text-white placeholder-indigo-200/40 focus:outline-none focus:ring-2 focus:ring-indigo-500/50 focus:border-indigo-500/50 transition-all duration-200"
                        />
                    </div>
                    <div className="relative">
                        <input
                            id="register-password"
                            type="password"
                            placeholder="Password"
                            value={password}
                            onChange={(e) => setPassword(e.target.value)}
                            required
                            className="w-full bg-white/5 border border-white/15 rounded-xl px-4 py-3.5 text-white placeholder-indigo-200/40 focus:outline-none focus:ring-2 focus:ring-indigo-500/50 focus:border-indigo-500/50 transition-all duration-200"
                        />
                    </div>

                    <button
                        id="register-submit"
                        type="submit"
                        disabled={loading}
                        className="group relative w-full flex items-center justify-center gap-3 bg-gradient-to-r from-indigo-500 to-purple-600 hover:from-indigo-400 hover:to-purple-500 text-white font-semibold text-lg py-4 px-8 rounded-2xl shadow-[0_0_20px_rgba(99,102,241,0.4)] hover:shadow-[0_0_30px_rgba(99,102,241,0.6)] transition-all duration-300 ease-out active:scale-95 disabled:opacity-70 disabled:cursor-not-allowed mt-2"
                    >
                        {loading ? (
                            <svg className="animate-spin h-6 w-6 text-white" xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24">
                                <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4"></circle>
                                <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path>
                            </svg>
                        ) : (
                            <>
                                <span>Sign Up</span>
                                <svg className="w-5 h-5 group-hover:translate-x-1 transition-transform" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M14 5l7 7m0 0l-7 7m7-7H3" />
                                </svg>
                            </>
                        )}
                    </button>
                </form>

                <p className="mt-6 text-sm text-indigo-200/50">
                    Already have an account?{' '}
                    <Link to="/login" className="text-indigo-300 hover:text-indigo-200 font-semibold transition-colors underline underline-offset-2">
                        Log In
                    </Link>
                </p>
            </div>

            <div className="absolute bottom-6 text-sm text-indigo-200/50 font-medium">
                Premium video collaboration
            </div>
        </div>
    );
};

export default RegisterPage;
