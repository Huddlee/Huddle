import React from 'react';
import { Link } from 'react-router-dom';
import useDocumentTitle from '../hooks/useDocumentTitle';
import Silk from '../components/Silk/Silk';

const AboutPage: React.FC = () => {
    useDocumentTitle('About us | HUDDLE');
    return (
        <div className="min-h-screen bg-[#0A0A0B] text-white font-sans relative">
            {/* Silk Background */}
            <div className="absolute inset-0 pointer-events-none z-0">
                <Silk
                    speed={5}
                    scale={1}
                    color="#1C1C1E"
                    noiseIntensity={1.5}
                    rotation={0}
                />
            </div>

            {/* Nav */}
            <nav className="z-10 relative w-full px-8 py-6 flex items-center justify-between">
                <Link to="/" className="group flex items-center gap-3 transition-all duration-200 hover:opacity-80">
                    <div className="w-10 h-10 bg-gradient-to-br from-indigo-500 to-purple-600 rounded-xl flex items-center justify-center shadow-[0_0_15px_rgba(99,102,241,0.3)] group-hover:shadow-[0_0_20px_rgba(99,102,241,0.5)] transition-shadow">
                        <svg className="w-5 h-5 text-white" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M15 10l4.553-2.276A1 1 0 0121 8.618v6.764a1 1 0 01-1.447.894L15 14M5 18h8a2 2 0 002-2V8a2 2 0 00-2-2H5a2 2 0 00-2 2v8a2 2 0 002 2z" />
                        </svg>
                    </div>
                    <span className="text-xl font-bold tracking-tight text-transparent bg-clip-text bg-gradient-to-r from-indigo-300 to-purple-300">Huddle</span>
                </Link>
                <Link
                    to="/"
                    className="text-sm text-gray-300 hover:text-white font-medium px-4 py-2 rounded-xl border border-gray-700/50 hover:border-gray-600 transition-all flex items-center gap-2"
                >
                    <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M10 19l-7-7m0 0l7-7m-7 7h18" />
                    </svg>
                    Back to app
                </Link>
            </nav>

            {/* Content */}
            <div className="z-10 relative max-w-3xl mx-auto px-8 py-12">
                {/* Hero */}
                <div className="mb-16">
                    <h1 className="text-4xl md:text-5xl font-extrabold tracking-tight mb-6 text-transparent bg-clip-text bg-gradient-to-r from-indigo-200 to-purple-200">
                        Privacy is not a feature.<br />It's the foundation.
                    </h1>
                    <p className="text-lg text-gray-400 leading-relaxed max-w-2xl">
                        Huddle was built with one core belief: your conversations are yours alone.
                        No servers watching, no recordings, no middlemen. Just you and the people you choose to talk to.
                    </p>
                </div>

                {/* How it works */}
                <section className="mb-14">
                    <h2 className="text-2xl font-bold mb-6 flex items-center gap-3">
                        <div className="w-8 h-8 bg-indigo-500/20 text-indigo-400 rounded-lg flex items-center justify-center">
                            <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M13 10V3L4 14h7v7l9-11h-7z" />
                            </svg>
                        </div>
                        How Huddle Works
                    </h2>
                    <div className="space-y-6">
                        <div className="bg-[#1C1C1E]/60 border border-gray-800 rounded-2xl p-6">
                            <h3 className="font-semibold text-lg mb-2 text-indigo-300">WebRTC Mesh Topology</h3>
                            <p className="text-gray-400 text-sm leading-relaxed">
                                Unlike most video platforms that route your data through a central server (SFU),
                                Huddle establishes <strong className="text-gray-300">direct peer-to-peer connections</strong> between every participant.
                                Your audio, video, and chat messages travel directly from your device to each participant's device —
                                no server ever sees or stores your media streams.
                            </p>
                        </div>
                        <div className="bg-[#1C1C1E]/60 border border-gray-800 rounded-2xl p-6">
                            <h3 className="font-semibold text-lg mb-2 text-emerald-300">End-to-End by Architecture</h3>
                            <p className="text-gray-400 text-sm leading-relaxed">
                                Because we use mesh topology, privacy isn't just a setting you toggle on — it's built into the very way Huddle works.
                                Even we, as the developers, cannot access your video or audio streams. The server only facilitates the initial handshake
                                (signaling), then steps away entirely.
                            </p>
                        </div>
                        <div className="bg-[#1C1C1E]/60 border border-gray-800 rounded-2xl p-6">
                            <h3 className="font-semibold text-lg mb-2 text-purple-300">Anonymous Identity</h3>
                            <p className="text-gray-400 text-sm leading-relaxed">
                                Every participant is assigned a fun, randomly generated anonymous name (like "Swift Panda" or "Wild Beer").
                                This means even within a call, your real identity stays hidden unless you choose to reveal it.
                                Guest access is available for those who prefer not to create an account at all.
                            </p>
                        </div>
                    </div>
                </section>

                {/* The tradeoff */}
                <section className="mb-14">
                    <h2 className="text-2xl font-bold mb-6 flex items-center gap-3">
                        <div className="w-8 h-8 bg-amber-500/20 text-amber-400 rounded-lg flex items-center justify-center">
                            <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-2.5L13.732 4c-.77-.833-1.964-.833-2.732 0L3.34 16.5c-.77.833.192 2.5 1.732 2.5z" />
                            </svg>
                        </div>
                        The Honest Tradeoff
                    </h2>
                    <div className="bg-[#1C1C1E]/60 border border-gray-800 rounded-2xl p-6">
                        <p className="text-gray-400 text-sm leading-relaxed">
                            Mesh topology means every device connects to every other device directly. This works beautifully for small,
                            focused groups (up to ~4-6 participants) but doesn't scale to large meetings like a centralized server would.
                            We made this choice intentionally — <strong className="text-gray-300">Huddle is designed for intimate, private conversations</strong>,
                            not 100-person webinars. Privacy is the priority.
                        </p>
                    </div>
                </section>

                {/* What the server does */}
                <section className="mb-14">
                    <h2 className="text-2xl font-bold mb-6 flex items-center gap-3">
                        <div className="w-8 h-8 bg-cyan-500/20 text-cyan-400 rounded-lg flex items-center justify-center">
                            <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M5 12h14M5 12a2 2 0 01-2-2V6a2 2 0 012-2h14a2 2 0 012 2v4a2 2 0 01-2 2M5 12a2 2 0 00-2 2v4a2 2 0 002 2h14a2 2 0 002-2v-4a2 2 0 00-2-2" />
                            </svg>
                        </div>
                        What Our Server Actually Does
                    </h2>
                    <div className="grid gap-4 md:grid-cols-2">
                        <div className="bg-[#1C1C1E]/60 border border-gray-800 rounded-2xl p-5">
                            <div className="flex items-center gap-2 mb-2">
                                <svg className="w-4 h-4 text-emerald-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M5 13l4 4L19 7" />
                                </svg>
                                <span className="text-sm font-semibold text-gray-200">Authentication</span>
                            </div>
                            <p className="text-gray-500 text-xs leading-relaxed">Handles login/register and issues JWT tokens for session management.</p>
                        </div>
                        <div className="bg-[#1C1C1E]/60 border border-gray-800 rounded-2xl p-5">
                            <div className="flex items-center gap-2 mb-2">
                                <svg className="w-4 h-4 text-emerald-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M5 13l4 4L19 7" />
                                </svg>
                                <span className="text-sm font-semibold text-gray-200">Room Management</span>
                            </div>
                            <p className="text-gray-500 text-xs leading-relaxed">Creates room codes and validates joining so only authorized users can enter.</p>
                        </div>
                        <div className="bg-[#1C1C1E]/60 border border-gray-800 rounded-2xl p-5">
                            <div className="flex items-center gap-2 mb-2">
                                <svg className="w-4 h-4 text-emerald-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M5 13l4 4L19 7" />
                                </svg>
                                <span className="text-sm font-semibold text-gray-200">Signaling</span>
                            </div>
                            <p className="text-gray-500 text-xs leading-relaxed">Relays WebRTC offers/answers/ICE candidates to establish peer connections.</p>
                        </div>
                        <div className="bg-[#1C1C1E]/60 border border-gray-800 rounded-2xl p-5">
                            <div className="flex items-center gap-2 mb-2">
                                <svg className="w-4 h-4 text-red-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M6 18L18 6M6 6l12 12" />
                                </svg>
                                <span className="text-sm font-semibold text-gray-200">Media Streams</span>
                            </div>
                            <p className="text-gray-500 text-xs leading-relaxed">Never touches your audio, video, or chat content. Zero access, zero storage.</p>
                        </div>
                    </div>
                </section>

                {/* CTA */}
                <div className="text-center pt-4 pb-12">
                    <Link
                        to="/"
                        className="inline-flex items-center gap-2 bg-black hover:bg-gray-900 border border-white/10 text-white font-semibold text-lg py-4 px-8 rounded-2xl shadow-lg hover:shadow-xl transition-all duration-300 active:scale-95"
                    >
                        Start a Private Session
                        <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M14 5l7 7m0 0l-7 7m7-7H3" />
                        </svg>
                    </Link>
                </div>
            </div>
        </div>
    );
};

export default AboutPage;
