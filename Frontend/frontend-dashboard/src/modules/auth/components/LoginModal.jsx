import { useState } from 'react';
import { useAuth } from '../context/AuthContext';

const LoginModal = ({ isOpen, onClose }) => {
    const { login, isLoading, authError } = useAuth();
    const [username, setUsername] = useState('');
    const [password, setPassword] = useState('');
    const [localError, setLocalError] = useState(null);

    if (!isOpen) return null;

    const handleSubmit = async (e) => {
        e.preventDefault();
        setLocalError(null);
        try {
            await login(username, password);
            if (onClose) onClose();
        } catch (err) {
            setLocalError(err.message || 'Authentication failed');
        }
    };

    return (
        <div className="fixed inset-0 z-50 bg-slate-950/60 backdrop-blur-sm flex items-center justify-center p-4">
            <div className="bg-white rounded-2xl shadow-2xl border border-slate-200/80 max-w-sm w-full p-6 animate-in fade-in zoom-in-95 duration-150">
                <div className="flex justify-between items-center border-b border-slate-100 pb-3 mb-5">
                    <div>
                        <h3 className="text-lg font-bold text-slate-900">Sign In</h3>
                        <p className="text-xs text-slate-500">Access your assigned ERP workspace</p>
                    </div>
                    {onClose && (
                        <button
                            type="button"
                            onClick={onClose}
                            className="text-slate-400 hover:text-slate-600 text-xl font-bold p-1 rounded-lg hover:bg-slate-100 transition"
                        >
                            &times;
                        </button>
                    )}
                </div>

                {(localError || authError) && (
                    <div className="mb-4 p-3 bg-rose-50 border border-rose-200 text-rose-700 text-xs rounded-xl flex items-center gap-2">
                        <span>⚠️</span>
                        <span>{localError || authError}</span>
                    </div>
                )}

                <form onSubmit={handleSubmit} className="space-y-4">
                    <div>
                        <label className="block text-xs font-semibold text-slate-700 uppercase tracking-wider mb-1.5">
                            Username
                        </label>
                        <input
                            type="text"
                            value={username}
                            onChange={(e) => setUsername(e.target.value)}
                            className="w-full px-3.5 py-2.5 bg-slate-50 border border-slate-300 rounded-xl text-sm text-slate-900 placeholder-slate-400 focus:bg-white focus:ring-2 focus:ring-violet-600 focus:border-violet-600 focus:outline-none transition"
                            placeholder="Enter username"
                            autoComplete="username"
                            required
                        />
                    </div>

                    <div>
                        <label className="block text-xs font-semibold text-slate-700 uppercase tracking-wider mb-1.5">
                            Password
                        </label>
                        <input
                            type="password"
                            value={password}
                            onChange={(e) => setPassword(e.target.value)}
                            className="w-full px-3.5 py-2.5 bg-slate-50 border border-slate-300 rounded-xl text-sm text-slate-900 placeholder-slate-400 focus:bg-white focus:ring-2 focus:ring-violet-600 focus:border-violet-600 focus:outline-none transition"
                            placeholder="Enter password"
                            autoComplete="current-password"
                            required
                        />
                    </div>

                    <button
                        type="submit"
                        disabled={isLoading}
                        className="w-full py-2.5 px-4 bg-slate-950 hover:bg-slate-800 disabled:bg-slate-400 text-white font-semibold rounded-xl shadow-md hover:shadow-lg transition text-sm flex items-center justify-center mt-2"
                    >
                        {isLoading ? 'Authenticating...' : 'LOGIN'}
                    </button>
                </form>
            </div>
        </div>
    );
};

export default LoginModal;
