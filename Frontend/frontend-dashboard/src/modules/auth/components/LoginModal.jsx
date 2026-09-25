import { useState } from 'react';
import { useAuth, DEMO_ACCOUNTS } from '../context/AuthContext';

const LoginModal = ({ isOpen, onClose }) => {
    const { login, register, isLoading, authError } = useAuth();
    const [isRegister, setIsRegister] = useState(false);
    const [username, setUsername] = useState('');
    const [password, setPassword] = useState('');
    const [email, setEmail] = useState('');
    const [role, setRole] = useState('ROLE_WAREHOUSE_MANAGER');
    const [localError, setLocalError] = useState(null);

    if (!isOpen) return null;

    const handleSubmit = async (e) => {
        e.preventDefault();
        setLocalError(null);
        try {
            if (isRegister) {
                await register({ username, email, password, role });
            } else {
                await login(username, password);
            }
            onClose();
        } catch (err) {
            setLocalError(err.message);
        }
    };

    const handleDemoClick = async (demoUsername) => {
        setLocalError(null);
        try {
            await login(demoUsername, `${demoUsername}123`);
            onClose();
        } catch (err) {
            setLocalError(err.message);
        }
    };

    return (
        <div className="fixed inset-0 z-50 bg-black/50 backdrop-blur-sm flex items-center justify-center p-4">
            <div className="bg-white rounded-xl shadow-2xl border border-gray-200 max-w-md w-full p-6 animate-in fade-in zoom-in-95 duration-150">
                <div className="flex justify-between items-center border-b pb-3 mb-4">
                    <h3 className="text-lg font-bold text-gray-900">
                        {isRegister ? 'Create Account' : 'Sign In'}
                    </h3>
                    <button 
                        onClick={onClose}
                        className="text-gray-400 hover:text-gray-600 text-xl font-bold p-1"
                    >
                        &times;
                    </button>
                </div>

                {(localError || authError) && (
                    <div className="mb-4 p-3 bg-red-50 border border-red-200 text-red-700 text-xs rounded-lg">
                        ⚠️ {localError || authError}
                    </div>
                )}

                <form onSubmit={handleSubmit} className="space-y-3.5">
                    <div>
                        <label className="block text-xs font-semibold text-gray-600 uppercase mb-1">Username</label>
                        <input
                            type="text"
                            value={username}
                            onChange={(e) => setUsername(e.target.value)}
                            className="w-full p-2.5 border border-gray-300 rounded-lg text-sm focus:ring-2 focus:ring-blue-500 focus:outline-none"
                            placeholder="e.g. admin or your username"
                            required
                        />
                    </div>

                    {isRegister && (
                        <div>
                            <label className="block text-xs font-semibold text-gray-600 uppercase mb-1">Email Address</label>
                            <input
                                type="email"
                                value={email}
                                onChange={(e) => setEmail(e.target.value)}
                                className="w-full p-2.5 border border-gray-300 rounded-lg text-sm focus:ring-2 focus:ring-blue-500 focus:outline-none"
                                placeholder="name@company.com"
                                required
                            />
                        </div>
                    )}

                    <div>
                        <label className="block text-xs font-semibold text-gray-600 uppercase mb-1">Password</label>
                        <input
                            type="password"
                            value={password}
                            onChange={(e) => setPassword(e.target.value)}
                            className="w-full p-2.5 border border-gray-300 rounded-lg text-sm focus:ring-2 focus:ring-blue-500 focus:outline-none"
                            placeholder="••••••••"
                            required
                        />
                    </div>

                    {isRegister && (
                        <div>
                            <label className="block text-xs font-semibold text-gray-600 uppercase mb-1">Assigned Role</label>
                            <select
                                value={role}
                                onChange={(e) => setRole(e.target.value)}
                                className="w-full p-2.5 border border-gray-300 rounded-lg text-sm bg-white focus:ring-2 focus:ring-blue-500 focus:outline-none"
                            >
                                <option value="ROLE_ADMIN">ROLE_ADMIN (Full Admin Access)</option>
                                <option value="ROLE_WAREHOUSE_MANAGER">ROLE_WAREHOUSE_MANAGER (Stock & Product Mgmt)</option>
                                <option value="ROLE_SALES_USER">ROLE_SALES_USER (Sales View)</option>
                                <option value="ROLE_FINANCE_USER">ROLE_FINANCE_USER (Finance)</option>
                                <option value="ROLE_VIEWER">ROLE_VIEWER (Read Only)</option>
                            </select>
                        </div>
                    )}

                    <button
                        type="submit"
                        disabled={isLoading}
                        className="w-full py-2.5 bg-blue-600 hover:bg-blue-700 disabled:bg-blue-300 text-white font-semibold rounded-lg shadow transition text-sm flex items-center justify-center mt-2"
                    >
                        {isLoading ? 'Processing...' : (isRegister ? 'Complete Registration' : 'Sign In')}
                    </button>
                </form>

                <div className="mt-4 pt-3 border-t text-center text-xs text-gray-500">
                    {isRegister ? (
                        <span>Already have an account? <button type="button" onClick={() => setIsRegister(false)} className="text-blue-600 font-semibold hover:underline">Sign in</button></span>
                    ) : (
                        <span>Need a new user? <button type="button" onClick={() => setIsRegister(true)} className="text-blue-600 font-semibold hover:underline">Register here</button></span>
                    )}
                </div>

                {!isRegister && (
                    <div className="mt-4 pt-3 border-t">
                        <p className="text-xs font-bold text-gray-500 uppercase tracking-wider mb-2">⚡ Quick 1-Click Demo Logins:</p>
                        <div className="grid grid-cols-2 gap-2">
                            {DEMO_ACCOUNTS.map(acc => (
                                <button
                                    key={acc.username}
                                    type="button"
                                    onClick={() => handleDemoClick(acc.username)}
                                    className="px-2.5 py-1.5 bg-gray-50 hover:bg-gray-100 border border-gray-200 rounded text-left text-xs transition"
                                >
                                    <div className="font-semibold text-gray-800">{acc.username}</div>
                                    <div className="text-[10px] text-gray-500 truncate">{acc.role.replace('ROLE_', '')}</div>
                                </button>
                            ))}
                        </div>
                    </div>
                )}
            </div>
        </div>
    );
};

export default LoginModal;
