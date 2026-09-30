import { useState } from 'react';
import { useAuth } from '../context/AuthContext';
import LoginModal from './LoginModal';

const ROLE_BADGE_COLORS = {
    ROLE_INVENTORY_USER: 'bg-amber-100 text-amber-800 border-amber-300',
    ROLE_SALES_USER: 'bg-emerald-100 text-emerald-800 border-emerald-300',
    ROLE_FINANCE_USER: 'bg-indigo-100 text-indigo-800 border-indigo-300',
};

const AuthHeader = () => {
    const { user, isAuthenticated, logout } = useAuth();
    const [isModalOpen, setIsModalOpen] = useState(false);

    return (
        <>
            <header className="relative z-10 flex flex-col md:flex-row items-center justify-between gap-4 border-b border-slate-900/10 px-2 pb-5 md:px-3">
                <div className="flex items-center space-x-3">
                    <div className="w-10 h-10 rounded-xl bg-slate-950 text-white flex items-center justify-center font-black text-lg shadow-lg">
                        E
                    </div>
                    <div>
                        <h1 className="font-bold text-base md:text-lg text-slate-900 leading-tight">
                            Enterprise ERP
                        </h1>
                        <p className="text-xs text-slate-500">
                            Distributed Event-Driven Operations
                        </p>
                    </div>
                </div>

                <div className="flex items-center flex-wrap gap-3">
                    {isAuthenticated ? (
                        <>
                            <div className="flex items-center gap-2 rounded-2xl border border-white/80 bg-white/70 px-3.5 py-2 shadow-sm backdrop-blur">
                                <div className="h-2 w-2 rounded-full bg-emerald-500 animate-pulse"></div>
                                <div className="text-xs">
                                    <span className="font-normal text-slate-500">User: </span>
                                    <strong className="font-semibold text-slate-900">{user.username}</strong>
                                </div>
                                <span className={`rounded-full border px-2.5 py-0.5 text-[10px] font-bold uppercase tracking-wide ${ROLE_BADGE_COLORS[user.role] || 'bg-slate-100 text-slate-800 border-slate-200'}`}>
                                    {user.role ? user.role.replace('ROLE_', '') : 'USER'}
                                </span>
                            </div>

                            <button
                                onClick={logout}
                                className="rounded-xl border border-slate-300/80 bg-white/80 px-3.5 py-2 text-xs font-semibold text-slate-700 hover:bg-rose-50 hover:text-rose-700 hover:border-rose-300 transition shadow-sm"
                            >
                                Sign Out
                            </button>
                        </>
                    ) : (
                        <button
                            onClick={() => setIsModalOpen(true)}
                            className="rounded-xl bg-slate-950 px-5 py-2 text-xs font-semibold text-white hover:bg-slate-800 transition shadow-md"
                        >
                            Sign In
                        </button>
                    )}
                </div>
            </header>

            <LoginModal
                isOpen={isModalOpen || !isAuthenticated}
                onClose={() => {
                    if (isAuthenticated) setIsModalOpen(false);
                }}
            />
        </>
    );
};

export default AuthHeader;
