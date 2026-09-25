import { useState } from 'react';
import { updateOrderStatus, cancelOrder } from '../services/salesService';

const STATUS_COLORS = {
    PENDING: 'bg-amber-100 text-amber-800 border-amber-300',
    STOCK_RESERVED: 'bg-blue-100 text-blue-800 border-blue-300',
    CONFIRMED: 'bg-emerald-100 text-emerald-800 border-emerald-300',
    REJECTED: 'bg-rose-100 text-rose-800 border-rose-300',
    CANCELLED: 'bg-gray-100 text-gray-700 border-gray-300',
};

const OrderDetailsModal = ({ order, isOpen, onClose, onOrderUpdated }) => {
    const [isUpdating, setIsUpdating] = useState(false);
    const [remarks, setRemarks] = useState('');

    if (!isOpen || !order) return null;

    const handleStatusChange = async (newStatus) => {
        setIsUpdating(true);
        try {
            const updated = await updateOrderStatus(order.id, newStatus, remarks || `Status changed to ${newStatus}`);
            if (onOrderUpdated) onOrderUpdated(updated);
            setRemarks('');
        } catch (err) {
            alert(err.response?.data?.message || err.message || 'Status update failed');
        } finally {
            setIsUpdating(false);
        }
    };

    const handleCancel = async () => {
        const reason = prompt("Enter cancellation reason:", "Order cancelled by customer request");
        if (!reason) return;

        setIsUpdating(true);
        try {
            const updated = await cancelOrder(order.id, reason);
            if (onOrderUpdated) onOrderUpdated(updated);
        } catch (err) {
            alert(err.response?.data?.message || err.message || 'Cancellation failed');
        } finally {
            setIsUpdating(false);
        }
    };

    const isFinalState = order.status === 'CONFIRMED' || order.status === 'CANCELLED';

    return (
        <div className="fixed inset-0 z-50 bg-black/50 backdrop-blur-sm flex items-center justify-center p-4">
            <div className="bg-white rounded-xl shadow-2xl border border-gray-200 max-w-2xl w-full p-6 animate-in fade-in max-h-[90vh] overflow-y-auto">
                <div className="flex justify-between items-start border-b pb-4 mb-4">
                    <div>
                        <div className="flex items-center gap-2">
                            <h3 className="text-xl font-mono font-black text-gray-900">{order.orderNumber}</h3>
                            <span className={`text-xs uppercase font-bold px-2.5 py-0.5 rounded-full border ${STATUS_COLORS[order.status] || 'bg-gray-100'}`}>
                                {order.status}
                            </span>
                        </div>
                        <p className="text-xs text-gray-500 mt-1">
                            Placed: {new Date(order.orderDate).toLocaleString()}
                        </p>
                    </div>
                    <button onClick={onClose} className="text-gray-400 hover:text-gray-600 text-xl font-bold p-1">&times;</button>
                </div>

                {/* Customer Details */}
                <div className="bg-slate-50 border rounded-lg p-3 mb-4 grid grid-cols-2 gap-3 text-xs">
                    <div>
                        <span className="text-gray-500 uppercase font-semibold block text-[10px]">Customer</span>
                        <strong className="text-gray-900 text-sm">{order.customerName}</strong>
                        <div className="text-gray-600">{order.customerEmail}</div>
                    </div>
                    <div>
                        <span className="text-gray-500 uppercase font-semibold block text-[10px]">Delivery Address</span>
                        <div className="text-gray-800">{order.shippingAddress || 'No shipping address provided'}</div>
                    </div>
                </div>

                {/* Order Items Table */}
                <div className="mb-4 border rounded-lg overflow-hidden">
                    <div className="bg-gray-100 px-3 py-2 text-xs font-bold text-gray-700 uppercase border-b">
                        Order Line Items ({order.items?.length || 0})
                    </div>
                    <table className="w-full text-left text-xs border-collapse">
                        <thead className="bg-gray-50 text-gray-600 border-b">
                            <tr>
                                <th className="p-2.5">SKU</th>
                                <th className="p-2.5">Product</th>
                                <th className="p-2.5 text-right">Unit Price</th>
                                <th className="p-2.5 text-center">Qty</th>
                                <th className="p-2.5 text-right">Subtotal</th>
                            </tr>
                        </thead>
                        <tbody className="divide-y text-gray-800">
                            {order.items?.map(item => (
                                <tr key={item.id} className="hover:bg-gray-50">
                                    <td className="p-2.5 font-mono text-blue-700 font-semibold">{item.productSku}</td>
                                    <td className="p-2.5">{item.productName}</td>
                                    <td className="p-2.5 text-right font-mono">₹{Number(item.unitPrice).toFixed(2)}</td>
                                    <td className="p-2.5 text-center font-bold">{item.quantity}</td>
                                    <td className="p-2.5 text-right font-mono font-bold">₹{Number(item.subtotal).toFixed(2)}</td>
                                </tr>
                            ))}
                        </tbody>
                        <tfoot className="bg-gray-50 font-bold border-t">
                            <tr>
                                <td colSpan="4" className="p-2.5 text-right text-gray-700">Total Order Amount:</td>
                                <td className="p-2.5 text-right text-sm font-mono text-blue-700">
                                    ₹{Number(order.totalAmount).toFixed(2)}
                                </td>
                            </tr>
                        </tfoot>
                    </table>
                </div>

                {/* Status Transitions */}
                {!isFinalState && (
                    <div className="p-3 bg-blue-50/60 border border-blue-200 rounded-lg mb-4 space-y-2">
                        <div className="flex justify-between items-center">
                            <span className="text-xs font-bold text-blue-900 uppercase">Update Status Transition</span>
                            <span className="text-[11px] text-blue-700">Current: <strong>{order.status}</strong></span>
                        </div>
                        <input
                            type="text"
                            value={remarks}
                            onChange={(e) => setRemarks(e.target.value)}
                            placeholder="Optional transition remarks / notes..."
                            className="w-full p-2 border border-blue-200 rounded text-xs bg-white focus:outline-none"
                        />
                        <div className="flex flex-wrap gap-2 pt-1">
                            {order.status === 'PENDING' && (
                                <button
                                    onClick={() => handleStatusChange('STOCK_RESERVED')}
                                    disabled={isUpdating}
                                    className="px-3 py-1.5 bg-blue-600 hover:bg-blue-700 text-white font-semibold text-xs rounded transition shadow"
                                >
                                    ✓ Reserve Stock
                                </button>
                            )}
                            {(order.status === 'PENDING' || order.status === 'STOCK_RESERVED') && (
                                <button
                                    onClick={() => handleStatusChange('CONFIRMED')}
                                    disabled={isUpdating}
                                    className="px-3 py-1.5 bg-emerald-600 hover:bg-emerald-700 text-white font-semibold text-xs rounded transition shadow"
                                >
                                    ✓ Confirm Order
                                </button>
                            )}
                            {order.status === 'PENDING' && (
                                <button
                                    onClick={() => handleStatusChange('REJECTED')}
                                    disabled={isUpdating}
                                    className="px-3 py-1.5 bg-rose-600 hover:bg-rose-700 text-white font-semibold text-xs rounded transition shadow"
                                >
                                    ✕ Reject Order
                                </button>
                            )}
                            <button
                                onClick={handleCancel}
                                disabled={isUpdating}
                                className="px-3 py-1.5 bg-gray-200 hover:bg-gray-300 text-gray-800 font-semibold text-xs rounded transition"
                            >
                                ✕ Cancel Order
                            </button>
                        </div>
                    </div>
                )}

                {/* Status History Timeline */}
                <div>
                    <h4 className="text-xs font-bold text-gray-700 uppercase mb-2">Order History & Audit Trail</h4>
                    {order.statusHistory && order.statusHistory.length > 0 ? (
                        <div className="space-y-2 border-l-2 border-blue-300 pl-3 ml-1 text-xs">
                            {order.statusHistory.map((h, i) => (
                                <div key={i} className="relative pb-1">
                                    <div className="absolute -left-[19px] top-1 w-2.5 h-2.5 rounded-full bg-blue-600"></div>
                                    <div className="font-semibold text-gray-900">
                                        {h.fromStatus ? `${h.fromStatus} → ` : ''}
                                        <span className="text-blue-700">{h.toStatus}</span>
                                        <span className="text-[10px] text-gray-400 font-normal ml-2">by {h.changedBy || 'SYSTEM'}</span>
                                    </div>
                                    <div className="text-[11px] text-gray-600">{h.remarks}</div>
                                    <div className="text-[10px] text-gray-400">{new Date(h.changedAt).toLocaleString()}</div>
                                </div>
                            ))}
                        </div>
                    ) : (
                        <div className="text-xs text-gray-400 italic">No history records available.</div>
                    )}
                </div>

                <div className="mt-5 pt-3 border-t text-right">
                    <button onClick={onClose} className="px-4 py-1.5 bg-gray-100 hover:bg-gray-200 text-gray-700 text-xs font-semibold rounded">
                        Close
                    </button>
                </div>
            </div>
        </div>
    );
};

export default OrderDetailsModal;
