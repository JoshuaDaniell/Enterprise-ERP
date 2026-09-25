import { useState } from 'react';
import { AuthProvider } from './modules/auth/context/AuthContext';
import AuthHeader from './modules/auth/components/AuthHeader';
import InventoryDashboard from './modules/inventory/pages/InventoryDashboard';
import SalesDashboard from './modules/sales/pages/SalesDashboard';
import FinanceDashboard from './modules/finance/pages/FinanceDashboard';

function App() {
  const [activeTab, setActiveTab] = useState('inventory');

  const tabs = [
    { id: 'inventory', label: 'Inventory', icon: '📦' },
    { id: 'finance', label: 'Finance', icon: '💳' },
    { id: 'sales', label: 'Sales', icon: '💼' },
  ];

  return (
    <AuthProvider>
      <div className="min-h-screen p-3 md:p-6 font-sans">
        <div className="app-shell max-w-[1440px] mx-auto px-5 py-5 md:px-10 md:py-7">
          <AuthHeader />

          <section className="relative overflow-hidden flex flex-col lg:flex-row lg:items-end justify-between gap-8 px-2 pt-8 pb-10 md:px-8 md:pt-12">
            <div className="hero-orb absolute -right-24 -top-32 h-96 w-96 rounded-full" />
            <div className="relative max-w-2xl">
              <p className="mb-4 text-[11px] font-bold uppercase tracking-[0.28em] text-violet-700/70">Star Enterprises workspace</p>
              <h2 className="hero-title font-semibold text-slate-950">
                Make every
                <span className="hero-title-accent block">business moment</span>
                count.
              </h2>
              <p className="mt-6 max-w-xl text-sm leading-6 text-slate-600 md:text-base">
                Keep inventory, orders, customers, and billing moving together from one calm, focused workspace.
              </p>
            </div>
            <div className="relative grid grid-cols-2 gap-3 sm:flex sm:items-center">
              <div className="rounded-2xl border border-white/80 bg-white/70 px-4 py-3 shadow-sm backdrop-blur">
                <span className="block text-[10px] font-bold uppercase tracking-wider text-slate-400">Workspace</span>
                <strong className="mt-1 block text-sm text-slate-900">Live operations</strong>
              </div>
              <div className="rounded-2xl border border-white/80 bg-white/70 px-4 py-3 shadow-sm backdrop-blur">
                <span className="block text-[10px] font-bold uppercase tracking-wider text-slate-400">Coverage</span>
                <strong className="mt-1 block text-sm text-slate-900">3 business areas</strong>
              </div>
            </div>
          </section>

          {/* Module Navigation Tabs */}
          <div className="flex items-center gap-2 overflow-x-auto rounded-2xl border border-white/70 bg-white/45 p-2 mb-2 backdrop-blur">
            {tabs.map((tab) => (
              <button
                key={tab.id}
                onClick={() => setActiveTab(tab.id)}
                className={`px-6 py-2.5 rounded-t-xl font-semibold text-xs uppercase tracking-wider transition-all duration-300 border-t border-x ${
                  activeTab === tab.id
                    ? 'bg-slate-950 text-white border-slate-950 shadow-lg shadow-violet-900/15'
                    : 'bg-transparent text-gray-500 border-transparent hover:bg-white/80 hover:text-purple-600'
                }`}
              >
                <span className="mr-1.5">{tab.icon}</span>
                {tab.label}
              </button>
            ))}
          </div>

          {/* Active Module View */}
          <div className="animate-fade-in">
            {activeTab === 'inventory' ? (
              <InventoryDashboard />
            ) : activeTab === 'sales' ? (
              <SalesDashboard />
            ) : (
              <FinanceDashboard />
            )}
          </div>
        </div>
      </div>
    </AuthProvider>
  );
}

export default App;