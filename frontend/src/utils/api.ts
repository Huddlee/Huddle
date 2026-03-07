import axios from 'axios';

const API_BASE_URL = 'http://localhost:8080';

const apiClient = axios.create({
    baseURL: API_BASE_URL,
});

export const guestLogin = async (): Promise<string> => {
    const response = await apiClient.post('/api/auth/guest/register');
    // Assuming the backend returns the token directly as a string, or in an object like { token: "..." }
    // Adjust based on actual backend response structure.
    if (response.data.token) {
        return response.data.token;
    }
    return response.data;
};

export const createRoom = async (token: string): Promise<string> => {
    const response = await apiClient.get('/api/room/create', {
        headers: {
            Authorization: `Bearer ${token}`,
        },
    });
    // Assuming backend returns room code in { roomCode: "..." } or similar
    if (response.data.roomCode) {
        return response.data.roomCode;
    }
    return response.data;
};

export const joinRoom = async (roomCode: string, token: string): Promise<any> => {
    const response = await apiClient.get(`/api/room/join/${roomCode}`, {
        headers: {
            Authorization: `Bearer ${token}`
        }
    });
    return response.data;
};
