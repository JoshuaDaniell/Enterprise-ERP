/* eslint-disable react-refresh/only-export-components */
import { createContext, useContext, useState, useEffect, useCallback } from 'react';
import { getCurrentUserProfile, loginUser, logoutUser, refreshAccessToken } from '../services/authService';

const AuthContext = createContext(null);

export const AuthProvider = ({ children }) => {
    const [user, setUser] = useState(null);

    const [isLoading, setIsLoading] = useState(true);
    const [authError, setAuthError] = useState(null);

    const saveAuthData = (data) => {
        if (data?.accessToken) {
            localStorage.setItem('erp_access_token', data.accessToken);
        }
        if (data?.refreshToken) {
            localStorage.setItem('erp_refresh_token', data.refreshToken);
        }
        const userInfo = {
            username: data.username,
            email: data.email,
            role: data.role
        };
        localStorage.setItem('erp_user', JSON.stringify(userInfo));
        setUser(userInfo);
        setAuthError(null);
    };

    const login = async (username, password) => {
        setIsLoading(true);
        setAuthError(null);
        try {
            const data = await loginUser(username, password);
            saveAuthData(data);
            return data;
        } catch (err) {
            const msg = err.response?.data?.message || err.message || 'Invalid username or password';
            setAuthError(msg);
            throw new Error(msg, { cause: err });
        } finally {
            setIsLoading(false);
        }
    };

    const logout = async () => {
        const storedRefreshToken = localStorage.getItem('erp_refresh_token');
        if (storedRefreshToken) {
            try {
                await logoutUser(storedRefreshToken);
            } catch (err) {
                console.warn('Logout notification error:', err);
            }
        }
        localStorage.removeItem('erp_access_token');
        localStorage.removeItem('erp_refresh_token');
        localStorage.removeItem('erp_user');
        setUser(null);
    };

    useEffect(() => {
        localStorage.removeItem('erp_cache_orders');
        localStorage.removeItem('erp_cache_customers');
        localStorage.removeItem('erp_cache_invoices');
        localStorage.removeItem('erp_cache_finance_dashboard');
        const restoreSession = async () => {
            try {
                if (!localStorage.getItem('erp_access_token')) throw new Error('No saved access token');
                let profile;
                try {
                    profile = await getCurrentUserProfile();
                } catch (error) {
                    const refreshToken = localStorage.getItem('erp_refresh_token');
                    if (!refreshToken) throw error;
                    const refreshed = await refreshAccessToken(refreshToken);
                    localStorage.setItem('erp_access_token', refreshed.accessToken);
                    localStorage.setItem('erp_refresh_token', refreshed.refreshToken);
                    profile = await getCurrentUserProfile();
                }
                const userInfo = { id: profile.id, username: profile.username, email: profile.email, role: profile.role };
                localStorage.setItem('erp_user', JSON.stringify(userInfo));
                setUser(userInfo);
            } catch {
                localStorage.removeItem('erp_access_token');
                localStorage.removeItem('erp_refresh_token');
                localStorage.removeItem('erp_user');
                setUser(null);
            } finally {
                setIsLoading(false);
            }
        };
        restoreSession();
    }, []);

    useEffect(() => {
        const handleAuthExpired = () => {
            setUser(null);
            setAuthError('Session expired. Please log in again.');
        };
        window.addEventListener('auth:expired', handleAuthExpired);
        return () => window.removeEventListener('auth:expired', handleAuthExpired);
    }, []);

    const hasRole = useCallback((...allowedRoles) => {
        if (!user || !user.role) return false;
        return allowedRoles.includes(user.role);
    }, [user]);

    const canManageInventory = hasRole('ROLE_INVENTORY_USER');

    return (
        <AuthContext.Provider
            value={{
                user,
                isAuthenticated: !!user,
                isLoading,
                authError,
                login,
                logout,
                hasRole,
                canCreateProduct: canManageInventory,
                canAdjustStock: canManageInventory,
                canDeleteProduct: canManageInventory
            }}
        >
            {children}
        </AuthContext.Provider>
    );
};

export const useAuth = () => {
    const context = useContext(AuthContext);
    if (!context) {
        throw new Error('useAuth must be used within an AuthProvider');
    }
    return context;
};
