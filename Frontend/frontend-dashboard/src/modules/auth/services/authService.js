import api from '../../../api/api';

export const loginUser = async (username, password) => {
    const response = await api.post('/auth/login', { username, password });
    return response.data?.data || response.data;
};

export const registerUser = async (userData) => {
    const response = await api.post('/auth/register', userData);
    return response.data?.data || response.data;
};

export const refreshAccessToken = async (refreshToken) => {
    const response = await api.post('/auth/refresh', { refreshToken });
    return response.data?.data || response.data;
};

export const logoutUser = async (refreshToken) => {
    try {
        await api.post('/auth/logout', { refreshToken });
    } catch (e) {
        console.warn("Logout request failed:", e);
    }
};

export const getCurrentUserProfile = async () => {
    const response = await api.get('/auth/me');
    return response.data?.data || response.data;
};
