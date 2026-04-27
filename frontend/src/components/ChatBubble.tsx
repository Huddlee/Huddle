import React, { useEffect, useState } from 'react';

export interface BubbleMessage {
    id: string;
    sender: string;
    text: string;
}

interface ChatBubbleStackProps {
    bubbles: BubbleMessage[];
    onDismiss: (id: string) => void;
    onClick: () => void;
}

const ChatBubbleItem: React.FC<{
    bubble: BubbleMessage;
    index: number;
    total: number;
    onDismiss: (id: string) => void;
    onClick: () => void;
}> = ({ bubble, index, total, onDismiss, onClick }) => {
    const [visible, setVisible] = useState(false);

    useEffect(() => {
        requestAnimationFrame(() => setVisible(true));

        const timer = setTimeout(() => {
            setVisible(false);
            setTimeout(() => onDismiss(bubble.id), 300);
        }, 5000);

        return () => clearTimeout(timer);
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, []);

    const preview = bubble.text.length > 60 ? bubble.text.slice(0, 60) + '…' : bubble.text;

    // Responsive: reduce the gap between stacked bubbles on small screens
    const stackGap = typeof window !== 'undefined' && window.innerWidth < 640 ? 56 : 68;

    // Stack positioning: newest (index = total-1) is at the bottom, older ones stack above
    const offset = (total - 1 - index);
    const translateY = -(offset * stackGap); // px gap between stacked bubbles
    const scale = 1 - offset * 0.04;
    const opacity = 1 - offset * 0.15;

    return (
        <div
            onClick={onClick}
            style={{
                transform: visible
                    ? `translateY(${translateY}px) scale(${scale})`
                    : `translateY(${translateY + 16}px) scale(${scale * 0.95})`,
                opacity: visible ? opacity : 0,
                zIndex: index,
                transition: 'all 300ms cubic-bezier(0.16, 1, 0.3, 1)',
                position: 'absolute',
                bottom: 0,
                right: 0,
                width: '100%',
            }}
            className="cursor-pointer"
        >
            <div className="bg-[#1C1C2E]/95 backdrop-blur-xl border border-indigo-500/30 rounded-2xl px-3 py-2 sm:px-4 sm:py-3 shadow-xl shadow-indigo-500/5 hover:border-indigo-400/50 transition-colors">
                <p className="text-[11px] sm:text-xs font-semibold text-indigo-400 mb-0.5 sm:mb-1">{bubble.sender}</p>
                <p className="text-xs sm:text-sm text-gray-200 leading-snug">{preview}</p>
            </div>
        </div>
    );
};

const ChatBubbleStack: React.FC<ChatBubbleStackProps> = ({ bubbles, onDismiss, onClick }) => {
    if (bubbles.length === 0) return null;

    return (
        <div className="fixed bottom-24 right-4 sm:right-6 z-50 w-52 sm:w-64 md:w-72" style={{ height: 0 }}>
            <div className="relative">
                {bubbles.map((bubble, index) => (
                    <ChatBubbleItem
                        key={bubble.id}
                        bubble={bubble}
                        index={index}
                        total={bubbles.length}
                        onDismiss={onDismiss}
                        onClick={onClick}
                    />
                ))}
            </div>
        </div>
    );
};

export default ChatBubbleStack;
