import axios from 'axios';

const api = axios.create({
    baseURL: import.meta.env.VITE_INVENTORY_API_URL || '/api/inventory',
    headers: {
        'Content-Type': 'application/json'
    }
});

// Request Interceptor: Attach Access Token
api.interceptors.request.use(
    (config) => {
        const token = localStorage.getItem('erp_access_token');
        if (token) {
            config.headers.Authorization = `Bearer ${token}`;
        }
        return config;
    },
    (error) => Promise.reject(error)
);

// Response Interceptor: Silent Token Refresh on 401
let isRefreshing = false;
let failedQueue = [];

const processQueue = (error, token = null) => {
    failedQueue.forEach(prom => {
        if (error) {
            prom.reject(error);
        } else {
            prom.resolve(token);
        }
    });
    failedQueue = [];
};

api.interceptors.response.use(
    (response) => response,
    async (error) => {
        const originalRequest = error.config;

        // Bypass refresh on auth endpoints (login/refresh/logout) to prevent infinite loops
        if (originalRequest?.url?.includes('/auth/')) {
            return Promise.reject(error);
        }

        if (error.response?.status === 401 && !originalRequest._retry) {
            if (isRefreshing) {
                return new Promise((resolve, reject) => {
                    failedQueue.push({ resolve, reject });
                })
                    .then((token) => {
                        originalRequest.headers.Authorization = `Bearer ${token}`;
                        return api(originalRequest);
                    })
                    .catch((err) => Promise.reject(err));
            }

            originalRequest._retry = true;
            isRefreshing = true;

            const storedRefreshToken = localStorage.getItem('erp_refresh_token');

            if (!storedRefreshToken) {
                isRefreshing = false;
                localStorage.removeItem('erp_access_token');
                localStorage.removeItem('erp_user');
                return Promise.reject(error);
            }

            try {
                // Call refresh endpoint directly using clean axios instance to avoid circular interceptor loop
                const response = await axios.post(`${import.meta.env.VITE_INVENTORY_API_URL || '/api/inventory'}/auth/refresh`, {
                    refreshToken: storedRefreshToken
                });

                const data = response.data?.data || response.data;
                const newAccessToken = data.accessToken;
                const newRefreshToken = data.refreshToken;

                localStorage.setItem('erp_access_token', newAccessToken);
                if (newRefreshToken) {
                    localStorage.setItem('erp_refresh_token', newRefreshToken);
                }

                api.defaults.headers.common.Authorization = `Bearer ${newAccessToken}`;
                originalRequest.headers.Authorization = `Bearer ${newAccessToken}`;

                processQueue(null, newAccessToken);
                return api(originalRequest);
            } catch (refreshErr) {
                processQueue(refreshErr, null);
                localStorage.removeItem('erp_access_token');
                localStorage.removeItem('erp_refresh_token');
                localStorage.removeItem('erp_user');
                window.dispatchEvent(new Event('auth:expired'));
                return Promise.reject(refreshErr);
            } finally {
                isRefreshing = false;
            }
        }

        return Promise.reject(error);
    }
);

export default api;
