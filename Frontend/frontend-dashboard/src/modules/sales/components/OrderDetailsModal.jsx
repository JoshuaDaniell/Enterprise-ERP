import { useState } from 'react';
import { cancelOrder } from '../services/salesService';

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

                {order.status === 'PENDING' && (
                    <div className="mb-4 rounded-lg border border-amber-200 bg-amber-50 p-3 text-xs text-amber-900">
                        <p className="mb-2">Waiting for Inventory to reserve the requested stock.</p>
                        <button onClick={handleCancel} disabled={isUpdating}
                            className="rounded border border-gray-300 bg-white px-3 py-1.5 font-semibold text-gray-700 disabled:opacity-50">
                            Cancel Pending Order
                        </button>
                    </div>
                )}
                {order.status === 'REJECTED' && (() => {
                    const rejection = [...(order.statusHistory || [])].reverse().find(history => history.toStatus === 'REJECTED');
                    return (
                        <div className="mb-4 rounded-lg border border-rose-200 bg-rose-50 p-3 text-xs text-rose-900" role="status">
                            <p className="font-bold">Inventory could not reserve this order.</p>
                            <p className="mt-1">{rejection?.remarks || 'The requested quantity could not be fulfilled from current inventory.'}</p>
                        </div>
                    );
                })()}
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
