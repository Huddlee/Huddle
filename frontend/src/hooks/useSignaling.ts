import { useEffect, useRef, useCallback, useState } from 'react';
import { joinRoom } from '../utils/api';

export interface WebRequest {
    type: "JOIN" | "LEAVE" | "OFFER" | "ANSWER" | "ICE";
    roomCode: string;
    to: string | null;
    message: string | null;
}

export interface WsResponse {
    responseType: "USER_ID" | "PEER_LIST" | "PEER_JOIN" | "PEER_DC" | "ERROR" | "OFFER" | "ANSWER" | "ICE" | "CAMERA_STATE";
    message: string;
    from: string;
}

export type ConnectionStatus = 'connecting' | 'connected' | 'reconnecting' | 'disconnected';

const MAX_RECONNECT_ATTEMPTS = 3;
const BASE_RECONNECT_DELAY_MS = 1000;

export const useSignaling = (roomCode: string | undefined) => {
    const wsRef = useRef<WebSocket | null>(null);
    const [connectionStatus, setConnectionStatus] = useState<ConnectionStatus>('connecting');

    // Keep a stable ref to the callback to avoid re-binding WebSocket events
    const onMessageRef = useRef<((data: WsResponse) => void) | null>(null);

    // Reconnection state tracked via refs to avoid re-triggering effects
    const reconnectAttemptRef = useRef(0);
    const reconnectTimerRef = useRef<ReturnType<typeof setTimeout> | null>(null);
    const intentionalCloseRef = useRef(false);

    const setOnMessageListener = useCallback((listener: (data: WsResponse) => void) => {
        onMessageRef.current = listener;
    }, []);

    const sendMessage = useCallback((req: Omit<WebRequest, 'roomCode'>) => {
        if (wsRef.current && wsRef.current.readyState === WebSocket.OPEN && roomCode) {
            const request: WebRequest = {
                ...req,
                roomCode
            };
            wsRef.current.send(JSON.stringify(request));
        } else {
            console.warn("WebSocket is not open. Cannot send message:", req.type);
        }
    }, [roomCode]);

    useEffect(() => {
        let isMounted = true;

        if (!roomCode) return;

        const token = localStorage.getItem('huddle_token');
        if (!token) {
            console.error("No huddle_token found in localStorage");
            return;
        }

        const connectWebSocket = () => {
            if (!isMounted) return;

            const wsUrl = `ws://localhost:8080/ws?token=${token}`;
            console.log(`Attempting to connect to WebSocket: ${wsUrl}`);
            const ws = new WebSocket(wsUrl);
            wsRef.current = ws;

            ws.onopen = () => {
                if (!isMounted) return;
                console.log("WebSocket connected. Sending JOIN message.");
                reconnectAttemptRef.current = 0;
                setConnectionStatus('connected');

                // Immediately send JOIN message
                const joinMessage: WebRequest = {
                    type: 'JOIN',
                    roomCode,
                    to: null,
                    message: null
                };
                ws.send(JSON.stringify(joinMessage));
            };

            // Serialize message processing to prevent race conditions
            // (e.g., OFFER arriving while PEER_JOIN handler hasn't finished)
            const messageQueue: WsResponse[] = [];
            let isProcessing = false;

            const processQueue = async () => {
                if (isProcessing) return;
                isProcessing = true;
                while (messageQueue.length > 0) {
                    const data = messageQueue.shift()!;
                    try {
                        if (onMessageRef.current) {
                            await onMessageRef.current(data);
                        }
                    } catch (error) {
                        console.error("Error processing WS message:", error);
                    }
                }
                isProcessing = false;
            };

            ws.onmessage = (event) => {
                try {
                    const data: WsResponse = JSON.parse(event.data);
                    console.log(`[WS RCV] ${data.responseType}`, data);
                    messageQueue.push(data);
                    processQueue();
                } catch (error) {
                    console.error("Error parsing WebSocket message:", error, event.data);
                }
            };

            ws.onerror = (error) => {
                console.error("WebSocket error:", error);
            };

            ws.onclose = (event) => {
                console.log("WebSocket connection closed:", event);
                wsRef.current = null;

                if (!isMounted || intentionalCloseRef.current) {
                    setConnectionStatus('disconnected');
                    return;
                }

                // Attempt reconnection with exponential backoff
                if (reconnectAttemptRef.current < MAX_RECONNECT_ATTEMPTS) {
                    const delay = BASE_RECONNECT_DELAY_MS * Math.pow(2, reconnectAttemptRef.current);
                    reconnectAttemptRef.current += 1;
                    setConnectionStatus('reconnecting');
                    console.log(`Reconnecting in ${delay}ms (attempt ${reconnectAttemptRef.current}/${MAX_RECONNECT_ATTEMPTS})...`);

                    reconnectTimerRef.current = setTimeout(() => {
                        if (isMounted) {
                            connectWebSocket();
                        }
                    }, delay);
                } else {
                    console.error("Max reconnection attempts reached. Giving up.");
                    setConnectionStatus('disconnected');
                }
            };
        };

        const connectToRoom = async () => {
            try {
                // Determine if we are allowed to join
                await joinRoom(roomCode, token);
                if (!isMounted) return;

                setConnectionStatus('connecting');
                intentionalCloseRef.current = false;
                connectWebSocket();

            } catch (error) {
                console.error("Failed to join room via API before opening WebSocket:", error);
                if (isMounted) setConnectionStatus('disconnected');
            }
        };

        connectToRoom();

        return () => {
            isMounted = false;
            intentionalCloseRef.current = true;

            if (reconnectTimerRef.current) {
                clearTimeout(reconnectTimerRef.current);
                reconnectTimerRef.current = null;
            }

            if (wsRef.current) {
                console.log("Cleaning up WebSocket connection...");
                wsRef.current.close();
                wsRef.current = null;
            }
        };
    }, [roomCode]);

    return { sendMessage, setOnMessageListener, connectionStatus };
};
