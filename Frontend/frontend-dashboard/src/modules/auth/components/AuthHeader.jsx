import { useState } from 'react';
import { useAuth, DEMO_ACCOUNTS } from '../context/AuthContext';
import LoginModal from './LoginModal';

const ROLE_BADGE_COLORS = {
    ROLE_ADMIN: 'bg-purple-100 text-purple-800 border-purple-300',
    ROLE_WAREHOUSE_MANAGER: 'bg-blue-100 text-blue-800 border-blue-300',
    ROLE_SALES_USER: 'bg-emerald-100 text-emerald-800 border-emerald-300',
    ROLE_VIEWER: 'bg-amber-100 text-amber-800 border-amber-300',
};

const AuthHeader = () => {
    const { user, isAuthenticated, logout, quickLoginAs, isLoading } = useAuth();
    const [isModalOpen, setIsModalOpen] = useState(false);

    return (
        <>
            <header className="relative z-10 flex flex-col md:flex-row items-center justify-between gap-4 border-b border-slate-900/10 px-2 pb-5 md:px-3">
                <div className="flex items-center space-x-3">
                    <div className="w-10 h-10 rounded-xl bg-slate-950 text-white flex items-center justify-center font-black text-lg shadow-lg">
                        S
                    </div>
                    <div>
                        <h1 className="font-bold text-base md:text-lg text-slate-900 leading-tight">
                            Star Enterprises
                        </h1>
                        <p className="text-xs text-slate-500">
                            Unified business workspace
                        </p>
                    </div>
                </div>

                <div className="flex items-center flex-wrap gap-3">
                    {isAuthenticated ? (
                        <>
                            <div className="flex items-center gap-2 rounded-2xl border border-white/80 bg-white/70 px-3 py-2 shadow-sm backdrop-blur">
                                <div className="h-2 w-2 animate-pulse rounded-full bg-neon-cyan"></div>
                                <div className="text-xs">
                                    <span className="font-normal text-slate-500">Active User: </span>
                                    <strong className="font-semibold text-slate-900">{user.username}</strong>
                                </div>
                                <span className={`rounded-full border px-2 py-0.5 text-[10px] font-bold uppercase ${ROLE_BADGE_COLORS[user.role] || 'bg-gray-100 text-gray-800'}`}>
                                    {user.role?.replace('ROLE_', '')}
                                </span>
                            </div>

                            {/* Quick Role Switcher for Testing */}
                            <div className="flex items-center gap-1 overflow-x-auto rounded-2xl border border-white/80 bg-white/55 p-1.5 shadow-sm backdrop-blur">
                                <span className="hidden whitespace-nowrap px-2 text-[10px] font-bold uppercase tracking-wider text-slate-400 sm:inline">Switch view</span>
                                {DEMO_ACCOUNTS.map(acc => {
                                    const isActive = user.role === acc.role;
                                    return (
                                        <button
                                            key={acc.role}
                                            onClick={() => quickLoginAs(acc.role)}
                                            disabled={isLoading || isActive}
                                            title={acc.desc}
                                            className={`rounded-xl px-3 py-1.5 text-[11px] font-semibold uppercase tracking-wide transition-all duration-300 ${
                                                isActive
                                                    ? 'bg-slate-950 text-white shadow-lg shadow-violet-900/15'
                                                    : 'text-slate-500 hover:bg-white/90 hover:text-violet-700'
                                            }`}
                                        >
                                            {acc.username}
                                        </button>
                                    );
                                })}
                            </div>

                            <button
                                onClick={() => setIsModalOpen(true)}
                                className="rounded-xl border border-white/80 bg-white/70 px-3.5 py-2 text-xs font-semibold text-slate-700 shadow-sm transition hover:bg-white hover:text-violet-700"
                            >
                                Account
                            </button>

                            <button
                                onClick={logout}
                                className="rounded-xl border border-rose-200/80 bg-rose-50/80 px-3.5 py-2 text-xs font-semibold text-rose-700 transition hover:bg-rose-100"
                            >
                                Sign Out
                            </button>
                        </>
                    ) : (
                        <div className="flex items-center gap-2">
                            <span className="text-xs font-medium text-amber-700">⚠️ Sign in to unlock protected actions</span>
                            <button
                                onClick={() => setIsModalOpen(true)}
                                className="rounded-xl bg-slate-950 px-4 py-2 text-xs font-semibold text-white shadow-lg shadow-violet-900/15 transition hover:bg-violet-700"
                            >
                                Sign In / Switch Role
                            </button>
                        </div>
                    )}
                </div>
            </header>

            <LoginModal isOpen={isModalOpen} onClose={() => setIsModalOpen(false)} />
        </>
    );
};

export default AuthHeader;
