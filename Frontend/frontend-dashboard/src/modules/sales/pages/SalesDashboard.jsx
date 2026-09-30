import { useState, useEffect, useCallback } from 'react';
import { getAllOrders } from '../services/salesService';
import CreateOrderModal from '../components/CreateOrderModal';
import CustomerModal from '../components/CustomerModal';
import OrderDetailsModal from '../components/OrderDetailsModal';
import { useAuth } from '../../auth/context/AuthContext';

const STATUS_COLORS = {
    PENDING: 'bg-amber-100 text-amber-800 border-amber-300',
    STOCK_RESERVED: 'bg-blue-100 text-blue-800 border-blue-300',
    CONFIRMED: 'bg-emerald-100 text-emerald-800 border-emerald-300',
    REJECTED: 'bg-rose-100 text-rose-800 border-rose-300',
    CANCELLED: 'bg-gray-100 text-gray-700 border-gray-300',
};

const SalesDashboard = () => {
    const { hasRole } = useAuth();
    const [orders, setOrders] = useState([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState(null);
    const [filterStatus, setFilterStatus] = useState('ALL');
    const [searchQuery, setSearchQuery] = useState('');

    // Modal States
    const [isCreateOpen, setIsCreateOpen] = useState(false);
    const [isCustomerOpen, setIsCustomerOpen] = useState(false);
    const [selectedOrder, setSelectedOrder] = useState(null);

    const loadOrders = useCallback(async () => {
        setLoading(true);
        try {
            const data = await getAllOrders();
            setOrders(Array.isArray(data) ? data : []);
            setError(null);
        } catch (err) {
            console.error("Failed to load orders from sales-service:", err);
            setError("Could not connect to sales. Please try again in a moment.");
        } finally {
            setLoading(false);
        }
    }, []);

    useEffect(() => {
        // Synchronize the view with the remote sales service.
        // eslint-disable-next-line react-hooks/set-state-in-effect
        loadOrders();
    }, [loadOrders]);

    useEffect(() => {
        if (!orders.some((order) => order.status === 'PENDING')) return undefined;
        const timer = window.setInterval(() => {
            getAllOrders()
                .then((data) => setOrders(Array.isArray(data) ? data : []))
                .catch((err) => console.error('Unable to refresh pending order status:', err));
        }, 3000);
        return () => window.clearInterval(timer);
    }, [orders]);

    const handleOrderCreated = (newOrder) => {
        setOrders(prev => [newOrder, ...prev]);
        setSelectedOrder(newOrder);
    };

    const handleOrderUpdated = (updatedOrder) => {
        setOrders(prev => prev.map(o => o.id === updatedOrder.id ? updatedOrder : o));
        setSelectedOrder(updatedOrder);
    };

    const canManageSales = hasRole('ROLE_SALES_USER');

    // Filter and search
    const filteredOrders = orders.filter(order => {
        const matchesStatus = filterStatus === 'ALL' || order.status === filterStatus;
        const matchesSearch = !searchQuery || 
            order.orderNumber.toLowerCase().includes(searchQuery.toLowerCase()) ||
            order.customerName.toLowerCase().includes(searchQuery.toLowerCase()) ||
            order.customerEmail.toLowerCase().includes(searchQuery.toLowerCase());
        return matchesStatus && matchesSearch;
    });

    const totalRevenue = orders.reduce((sum, o) => sum + (Number(o.totalAmount) || 0), 0);
    const pendingCount = orders.filter(o => o.status === 'PENDING').length;
    const confirmedCount = orders.filter(o => o.status === 'CONFIRMED').length;

    return (
        <div className="dashboard-card w-full p-6 my-4">
            {/* Header & Action Toolbar */}
            <div className="border-b pb-4 mb-6 flex flex-col md:flex-row md:items-center md:justify-between gap-4">
                <div>
                    <h2 className="text-2xl font-black text-gray-900 tracking-tight">Sales & Orders</h2>
                    <p className="text-xs text-gray-500 mt-1">
                        Create orders, manage customers, and track fulfillment
                    </p>
                </div>
                <div className="flex items-center flex-wrap gap-2">
                    <button
                        onClick={() => setIsCustomerOpen(true)}
                        className="px-3.5 py-2 bg-slate-100 hover:bg-slate-200 text-slate-800 font-semibold text-xs rounded-lg border border-slate-300 transition flex items-center gap-1.5"
                    >
                        👥 Customer Profiles
                    </button>
                    <button
                        onClick={loadOrders}
                        className="px-3 py-2 bg-gray-100 hover:bg-gray-200 text-gray-700 text-xs font-semibold rounded-lg border border-gray-300 transition"
                    >
                        ↻ Refresh
                    </button>
                    <button
                        onClick={() => setIsCreateOpen(true)}
                        disabled={!canManageSales}
                        title={canManageSales ? "Create a new sales order" : "Requires Sales access"}
                        className="px-4 py-2 bg-blue-600 hover:bg-blue-700 disabled:bg-gray-300 text-white font-semibold text-xs rounded-lg shadow transition flex items-center gap-1.5"
                    >
                        ⚡ + Place Sales Order
                    </button>
                </div>
            </div>

            {error && (
                <div className="mb-5 p-3.5 bg-amber-50 border border-amber-200 text-amber-900 rounded-lg text-sm flex items-center justify-between">
                    <span>⚠️ {error}</span>
                    <button onClick={loadOrders} className="text-xs font-bold text-amber-800 underline">Retry</button>
                </div>
            )}

            {/* Metrics Row */}
            <div className="grid grid-cols-2 lg:grid-cols-4 gap-4 mb-6">
                <div className="bg-slate-50 border border-slate-200 rounded-xl p-4">
                    <span className="text-[11px] font-semibold text-slate-500 uppercase tracking-wider">Total Sales Volume</span>
                    <div className="text-xl font-black text-slate-900 mt-1 font-mono">{orders.length} Orders</div>
                </div>
                <div className="bg-blue-50 border border-blue-200 rounded-xl p-4">
                    <span className="text-[11px] font-semibold text-blue-700 uppercase tracking-wider">Gross Sales Value</span>
                    <div className="text-xl font-black text-blue-900 mt-1 font-mono">₹{totalRevenue.toFixed(2)}</div>
                </div>
                <div className="bg-amber-50 border border-amber-200 rounded-xl p-4">
                    <span className="text-[11px] font-semibold text-amber-700 uppercase tracking-wider">Pending Processing</span>
                    <div className="text-xl font-black text-amber-900 mt-1 font-mono">{pendingCount} Orders</div>
                </div>
                <div className="bg-emerald-50 border border-emerald-200 rounded-xl p-4">
                    <span className="text-[11px] font-semibold text-emerald-700 uppercase tracking-wider">Confirmed & Fulfilled</span>
                    <div className="text-xl font-black text-emerald-900 mt-1 font-mono">{confirmedCount} Orders</div>
                </div>
            </div>

            {/* Filter & Search Bar */}
            <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3 mb-4">
                <div className="flex items-center gap-1.5 flex-wrap">
                    {['ALL', 'PENDING', 'CONFIRMED', 'REJECTED', 'CANCELLED'].map(status => (
                        <button
                            key={status}
                            onClick={() => setFilterStatus(status)}
                            className={`px-3 py-1 rounded-md text-xs font-semibold transition ${
                                filterStatus === status
                                    ? 'bg-slate-900 text-white shadow'
                                    : 'bg-gray-100 text-gray-600 hover:bg-gray-200'
                            }`}
                        >
                            {status.replace('_', ' ')}
                        </button>
                    ))}
                </div>

                <div className="w-full sm:w-64">
                    <input
                        type="text"
                        value={searchQuery}
                        onChange={(e) => setSearchQuery(e.target.value)}
                        placeholder="Search order #, customer..."
                        className="w-full p-2 border border-gray-300 rounded-lg text-xs focus:ring-2 focus:ring-blue-500 focus:outline-none"
                    />
                </div>
            </div>

            {/* Orders Table */}
            <div className="border border-gray-200 rounded-lg overflow-x-auto shadow-sm">
                <table className="w-full text-left border-collapse min-w-[700px]">
                    <thead className="bg-gray-50 text-gray-700 text-xs font-semibold uppercase border-b border-gray-200">
                        <tr>
                            <th className="p-3.5">Order #</th>
                            <th className="p-3.5">Customer</th>
                            <th className="p-3.5">Date</th>
                            <th className="p-3.5 text-right">Items</th>
                            <th className="p-3.5 text-right">Total Amount</th>
                            <th className="p-3.5 text-center">Status</th>
                            <th className="p-3.5 text-right">Action</th>
                        </tr>
                    </thead>
                    <tbody className="divide-y divide-gray-100 text-xs">
                        {loading ? (
                            <tr>
                                <td colSpan="7" className="p-8 text-center text-gray-500">Loading orders from sales-service...</td>
                            </tr>
                        ) : filteredOrders.length === 0 ? (
                            <tr>
                                <td colSpan="7" className="p-8 text-center text-gray-400 italic">
                                    No sales orders found. Click "+ Place Sales Order" to create one.
                                </td>
                            </tr>
                        ) : (
                            filteredOrders.map((order) => (
                                <tr key={order.id} className="hover:bg-blue-50/40 transition">
                                    <td className="p-3.5 font-mono font-bold text-blue-700">
                                        {order.orderNumber}
                                    </td>
                                    <td className="p-3.5">
                                        <div className="font-semibold text-gray-900">{order.customerName}</div>
                                        <div className="text-[11px] text-gray-400">{order.customerEmail}</div>
                                    </td>
                                    <td className="p-3.5 text-gray-600">
                                        {new Date(order.orderDate).toLocaleDateString()}
                                    </td>
                                    <td className="p-3.5 text-right font-medium text-gray-700">
                                        {order.items?.length || 0} line(s)
                                    </td>
                                    <td className="p-3.5 text-right font-mono font-bold text-gray-900">
                                        ₹{Number(order.totalAmount).toFixed(2)}
                                    </td>
                                    <td className="p-3.5 text-center">
                                        <span className={`inline-flex items-center px-2.5 py-0.5 rounded-full text-[10px] font-bold uppercase border ${STATUS_COLORS[order.status] || 'bg-gray-100'}`}>
                                            {order.status}
                                        </span>
                                    </td>
                                    <td className="p-3.5 text-right">
                                        <button
                                            onClick={() => setSelectedOrder(order)}
                                            className="px-3 py-1 bg-blue-50 text-blue-700 font-semibold rounded border border-blue-200 hover:bg-blue-100 transition text-xs"
                                        >
                                            View & Manage &rarr;
                                        </button>
                                    </td>
                                </tr>
                            ))
                        )}
                    </tbody>
                </table>
            </div>

            {/* Modals */}
            <CreateOrderModal
                isOpen={isCreateOpen}
                onClose={() => setIsCreateOpen(false)}
                onOrderCreated={handleOrderCreated}
            />

            <CustomerModal
                isOpen={isCustomerOpen}
                onClose={() => setIsCustomerOpen(false)}
            />

            <OrderDetailsModal
                order={selectedOrder}
                isOpen={!!selectedOrder}
                onClose={() => setSelectedOrder(null)}
                onOrderUpdated={handleOrderUpdated}
            />
        </div>
    );
};

export default SalesDashboard;
