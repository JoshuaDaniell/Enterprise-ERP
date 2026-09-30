import { AuthProvider, useAuth } from './modules/auth/context/AuthContext';
import AuthHeader from './modules/auth/components/AuthHeader';
import InventoryDashboard from './modules/inventory/pages/InventoryDashboard';
import SalesDashboard from './modules/sales/pages/SalesDashboard';
import FinanceDashboard from './modules/finance/pages/FinanceDashboard';

function AppContent() {
  const { user, isAuthenticated } = useAuth();

  const renderModule = () => {
    if (!isAuthenticated || !user) {
      return (
        <div className="py-20 text-center">
          <div className="max-w-md mx-auto p-8 rounded-2xl border border-white/80 bg-white/70 shadow-lg backdrop-blur">
            <div className="text-4xl mb-4">🔐</div>
            <h3 className="text-lg font-bold text-slate-900 mb-2">Authentication Required</h3>
            <p className="text-sm text-slate-600 mb-6">
              Please sign in with your enterprise credentials to access your assigned module.
            </p>
          </div>
        </div>
      );
    }

    switch (user.role) {
      case 'ROLE_INVENTORY_USER':
        return <InventoryDashboard />;
      case 'ROLE_SALES_USER':
        return <SalesDashboard />;
      case 'ROLE_FINANCE_USER':
        return <FinanceDashboard />;
      default:
        return (
          <div className="py-16 text-center">
            <div className="max-w-md mx-auto p-6 rounded-2xl border border-rose-200 bg-rose-50 text-rose-800">
              <h3 className="font-bold text-base mb-1">Access Restricted</h3>
              <p className="text-sm">You do not have access to any enterprise module. Please contact system administrator.</p>
            </div>
          </div>
        );
    }
  };

  const getRoleTitle = () => {
    if (!user?.role) return 'Enterprise Workspace';
    switch (user.role) {
      case 'ROLE_INVENTORY_USER':
        return 'Inventory & Warehouse Operations';
      case 'ROLE_SALES_USER':
        return 'Sales & Order Management';
      case 'ROLE_FINANCE_USER':
        return 'Financial Invoicing & Ledger';
      default:
        return 'Enterprise Workspace';
    }
  };

  return (
    <div className="min-h-screen p-3 md:p-6 font-sans">
      <div className="app-shell max-w-[1440px] mx-auto px-5 py-5 md:px-10 md:py-7">
        <AuthHeader />

        {isAuthenticated && (
          <section className="relative overflow-hidden flex flex-col lg:flex-row lg:items-end justify-between gap-6 px-2 pt-6 pb-6 md:px-6">
            <div className="relative max-w-2xl">
              <p className="mb-2 text-[11px] font-bold uppercase tracking-[0.25em] text-violet-700/80">
                {user?.role?.replace('ROLE_', '')} WORKSPACE
              </p>
              <h2 className="hero-title text-2xl md:text-3xl font-semibold text-slate-950">
                {getRoleTitle()}
              </h2>
            </div>
          </section>
        )}

        {/* Dashboard Isolated View */}
        <div className="animate-fade-in mt-2">
          {renderModule()}
        </div>
      </div>
    </div>
  );
}

function App() {
  return (
    <AuthProvider>
      <AppContent />
    </AuthProvider>
  );
}

export default App;