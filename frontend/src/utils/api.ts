import axios from 'axios';

const API_BASE_URL = import.meta.env.VITE_BACKEND_URL;

const apiClient = axios.create({
    baseURL: API_BASE_URL,
});

// Global interceptor: redirect to login on 403 (session expired / forbidden)
apiClient.interceptors.response.use(
    (response) => response,
    (error) => {
        if (error.response?.status === 403) {
            localStorage.removeItem('huddle_token');
            window.location.href = '/login?expired=true';
        }
        return Promise.reject(error);
    }
);

// --- DTOs ---

export interface LoginRequest {
    username: string;
    password: string;
}

export interface RegisterRequest {
    username: string;
    password: string;
    email: string;
}

export interface JwtAuthResponse {
    token: string;
}

// --- Auth API ---

export const registerUser = async (data: RegisterRequest): Promise<string> => {
    const response = await apiClient.post('/api/auth/register', data, {
        // Backend returns plain text, not JSON
        responseType: 'text',
    });
    return response.data as string; // "User registered"
};

export const loginUser = async (data: LoginRequest): Promise<JwtAuthResponse> => {
    const response = await apiClient.post<JwtAuthResponse>('/api/auth/login', data);
    return response.data;
};

// --- Guest Auth ---

export const guestLogin = async (): Promise<string> => {
    const response = await apiClient.post('/api/auth/guest/register');
    if (response.data.token) {
        return response.data.token;
    }
    return response.data;
};

// --- Room API ---

export const createRoom = async (token: string): Promise<string> => {
    const response = await apiClient.get('/api/room/create', {
        headers: {
            Authorization: `Bearer ${token}`,
        },
    });
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
