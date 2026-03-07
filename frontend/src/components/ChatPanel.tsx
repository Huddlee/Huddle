import React, { useRef, useEffect, useState } from 'react';

export interface ChatMessage {
    id: string;
    sender: string;
    text: string;
    timestamp: number;
    isMe: boolean;
}

interface ChatPanelProps {
    messages: ChatMessage[];
    onSend: (text: string) => void;
    onClose: () => void;
}

const getPeerColor = (sender: string) => {
    // Array of 8 distinct bright colors to ensure low chance of collision for 4 peers
    const colors = [
        'text-emerald-400',
        'text-amber-400',
        'text-pink-400',
        'text-cyan-400',
        'text-fuchsia-400',
        'text-lime-400',
        'text-blue-400',
        'text-orange-400'
    ];
    let hash = 0;
    for (let i = 0; i < sender.length; i++) {
        hash = sender.charCodeAt(i) + ((hash << 5) - hash);
    }
    return colors[Math.abs(hash) % colors.length];
};

const ChatPanel: React.FC<ChatPanelProps> = ({ messages, onSend, onClose }) => {
    const [input, setInput] = useState('');
    const bottomRef = useRef<HTMLDivElement>(null);

    useEffect(() => {
        bottomRef.current?.scrollIntoView({ behavior: 'smooth' });
    }, [messages]);

    const handleSend = () => {
        const trimmed = input.trim();
        if (!trimmed) return;
        onSend(trimmed);
        setInput('');
    };

    const handleKeyDown = (e: React.KeyboardEvent) => {
        if (e.key === 'Enter' && !e.shiftKey) {
            e.preventDefault();
            handleSend();
        }
    };

    const formatTime = (ts: number) => {
        const d = new Date(ts);
        return d.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });
    };

    return (
        <div className="w-[340px] h-full flex flex-col bg-[#111113]/95 backdrop-blur-xl shrink-0">
            {/* Header */}
            <div className="flex items-center justify-between px-4 py-3 border-b border-white/10">
                <h2 className="text-base font-semibold text-white tracking-tight">Chat</h2>
                <button
                    onClick={onClose}
                    className="p-1.5 rounded-lg text-gray-400 hover:text-white hover:bg-white/10 transition-colors"
                    title="Close chat"
                >
                    <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M6 18L18 6M6 6l12 12" />
                    </svg>
                </button>
            </div>

            {/* Messages */}
            <div className="flex-1 overflow-y-auto px-4 py-3 space-y-2 [&::-webkit-scrollbar]:w-1.5 [&::-webkit-scrollbar-track]:bg-transparent [&::-webkit-scrollbar-thumb]:bg-white/10 [&::-webkit-scrollbar-thumb]:rounded-full hover:[&::-webkit-scrollbar-thumb]:bg-white/20">
                {messages.length === 0 && (
                    <div className="flex flex-col items-center justify-center h-full text-gray-500 text-sm gap-2 select-none">
                        <svg className="w-10 h-10 opacity-40" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="1.5" d="M8 12h.01M12 12h.01M16 12h.01M21 12c0 4.418-4.03 8-9 8a9.863 9.863 0 01-4.255-.949L3 20l1.395-3.72C3.512 15.042 3 13.574 3 12c0-4.418 4.03-8 9-8s9 3.582 9 8z" />
                        </svg>
                        <p>No messages yet</p>
                        <p className="text-xs text-gray-600">Messages are peer-to-peer</p>
                    </div>
                )}
                {messages.map((msg, index) => {
                    const isSequential = index > 0 && messages[index - 1].sender === msg.sender;

                    return (
                        <div key={msg.id} className={`flex flex-col ${msg.isMe ? 'items-end' : 'items-start'} ${isSequential ? '-mt-1' : ''}`}>
                            {!isSequential && (
                                <span className={`text-[11px] font-medium mb-1 ${msg.isMe ? 'text-indigo-400' : getPeerColor(msg.sender)}`}>
                                    {msg.isMe ? 'You' : msg.sender}
                                </span>
                            )}
                            <div
                                className={`max-w-[85%] px-3 py-2 rounded-2xl text-sm leading-relaxed break-words ${msg.isMe
                                    ? 'bg-indigo-500/20 text-indigo-100 rounded-bl-md'
                                    : 'bg-white/[0.07] text-gray-200 rounded-br-md'
                                    } ${isSequential ? (msg.isMe ? 'rounded-tr-md' : 'rounded-tl-md') : ''}`}
                            >
                                {msg.text}
                            </div>
                            {(!messages[index + 1] || messages[index + 1].sender !== msg.sender) && (
                                <span className="text-[10px] text-gray-600 mt-1">{formatTime(msg.timestamp)}</span>
                            )}
                        </div>
                    );
                })}
                <div ref={bottomRef} />
            </div>

            {/* Input */}
            <div className="px-3 py-3 border-t border-white/10">
                <div className="flex items-center gap-2 bg-white/[0.06] rounded-xl px-3 py-2 focus-within:ring-1 focus-within:ring-indigo-500/50 transition-all">
                    <input
                        type="text"
                        value={input}
                        onChange={(e) => setInput(e.target.value)}
                        onKeyDown={handleKeyDown}
                        placeholder="Type a message..."
                        className="flex-1 bg-transparent text-sm text-white placeholder-gray-500 outline-none"
                    />
                    <button
                        onClick={handleSend}
                        disabled={!input.trim()}
                        className="p-1.5 rounded-lg text-indigo-400 hover:text-indigo-300 hover:bg-indigo-500/10 transition-colors disabled:opacity-30 disabled:cursor-default"
                    >
                        <svg className="w-5 h-5 rotate-90" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M12 19l9 2-9-18-9 18 9-2zm0 0v-8" />
                        </svg>
                    </button>
                </div>
            </div>
        </div>
    );
};

export default ChatPanel;
