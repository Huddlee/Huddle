import React, { useEffect, useState } from 'react';

interface ChatBubbleProps {
    sender: string;
    text: string;
    onDismiss: () => void;
    onClick: () => void;
}

const ChatBubble: React.FC<ChatBubbleProps> = ({ sender, text, onDismiss, onClick }) => {
    const [visible, setVisible] = useState(false);

    useEffect(() => {
        // Animate in
        requestAnimationFrame(() => setVisible(true));

        const timer = setTimeout(() => {
            setVisible(false);
            setTimeout(onDismiss, 300); // Wait for exit animation
        }, 5000);

        return () => clearTimeout(timer);
    }, [onDismiss]);

    const preview = text.length > 60 ? text.slice(0, 60) + '…' : text;

    return (
        <div
            onClick={onClick}
            className={`fixed bottom-24 right-6 z-50 max-w-xs cursor-pointer transition-all duration-300 ${visible ? 'opacity-100 translate-y-0' : 'opacity-0 translate-y-4'
                }`}
        >
            <div className="bg-[#1C1C2E]/95 backdrop-blur-xl border border-indigo-500/30 rounded-2xl px-4 py-3 shadow-xl shadow-indigo-500/5 hover:border-indigo-400/50 transition-colors">
                <p className="text-xs font-semibold text-indigo-400 mb-1">{sender}</p>
                <p className="text-sm text-gray-200 leading-snug">{preview}</p>
            </div>
        </div>
    );
};

export default ChatBubble;
