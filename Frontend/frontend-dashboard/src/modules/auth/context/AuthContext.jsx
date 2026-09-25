/* eslint-disable react-refresh/only-export-components */
import { createContext, useContext, useState, useEffect, useCallback } from 'react';
import { loginUser, registerUser, logoutUser } from '../services/authService';

const AuthContext = createContext(null);

export const DEMO_ACCOUNTS = [
    { username: 'admin', role: 'ROLE_ADMIN', label: 'Admin (Full Access)', desc: 'Create, Read, Update, Delete' },
    { username: 'manager', role: 'ROLE_WAREHOUSE_MANAGER', label: 'Warehouse Manager', desc: 'Create, Read, Update Stock' },
    { username: 'sales', role: 'ROLE_SALES_USER', label: 'Sales Rep', desc: 'Read Products' },
    { username: 'finance', role: 'ROLE_FINANCE_USER', label: 'Finance User', desc: 'Invoices and payments' },
    { username: 'viewer', role: 'ROLE_VIEWER', label: 'Auditor / Viewer', desc: 'Read Only' },
];

export const AuthProvider = ({ children }) => {
    const [user, setUser] = useState(() => {
        try {
            const saved = localStorage.getItem('erp_user');
            return saved ? JSON.parse(saved) : null;
        } catch {
            return null;
        }
    });

    const [isLoading, setIsLoading] = useState(false);
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
            const msg = err.response?.data?.message || err.message || 'Login failed';
            setAuthError(msg);
            throw new Error(msg, { cause: err });
        } finally {
            setIsLoading(false);
        }
    };

    const register = async (userData) => {
        setIsLoading(true);
        setAuthError(null);
        try {
            const data = await registerUser(userData);
            saveAuthData(data);
            return data;
        } catch (err) {
            const msg = err.response?.data?.message || err.message || 'Registration failed';
            setAuthError(msg);
            throw new Error(msg, { cause: err });
        } finally {
            setIsLoading(false);
        }
    };

    const logout = async () => {
        const storedRefreshToken = localStorage.getItem('erp_refresh_token');
        if (storedRefreshToken) {
            await logoutUser(storedRefreshToken);
        }
        localStorage.removeItem('erp_access_token');
        localStorage.removeItem('erp_refresh_token');
        localStorage.removeItem('erp_user');
        setUser(null);
    };

    const quickLoginAs = async (roleName) => {
        const account = DEMO_ACCOUNTS.find(a => a.role === roleName) || DEMO_ACCOUNTS[0];
        return await login(account.username, `${account.username}123`);
    };

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

    const canCreateProduct = hasRole('ROLE_ADMIN', 'ROLE_WAREHOUSE_MANAGER');
    const canDeleteProduct = hasRole('ROLE_ADMIN');
    const canAdjustStock = hasRole('ROLE_ADMIN', 'ROLE_WAREHOUSE_MANAGER');

    return (
        <AuthContext.Provider
            value={{
                user,
                isAuthenticated: !!user,
                isLoading,
                authError,
                login,
                register,
                logout,
                quickLoginAs,
                hasRole,
                canCreateProduct,
                canDeleteProduct,
                canAdjustStock
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
