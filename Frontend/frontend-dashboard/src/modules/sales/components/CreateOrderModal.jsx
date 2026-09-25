import { useState, useEffect } from 'react';
import { getAllCustomers, createOrder } from '../services/salesService';
import { getAllProducts } from '../../inventory/services/inventoryservice';

const CreateOrderModal = ({ isOpen, onClose, onOrderCreated }) => {
    const [customers, setCustomers] = useState([]);
    const [products, setProducts] = useState([]);
    const [selectedCustomerId, setSelectedCustomerId] = useState('');
    const [shippingAddress, setShippingAddress] = useState('');
    const [billingAddress, setBillingAddress] = useState('');
    const [notes, setNotes] = useState('');
    const [items, setItems] = useState([
        { productSku: '', productName: '', unitPrice: 0, quantity: 1 }
    ]);
    const [isSubmitting, setIsSubmitting] = useState(false);
    const [error, setError] = useState(null);

    useEffect(() => {
        if (isOpen) {
            Promise.all([getAllCustomers(), getAllProducts()])
                .then(([custList, prodList]) => {
                    setCustomers(Array.isArray(custList) ? custList : []);
                    setProducts(Array.isArray(prodList) ? prodList : []);
                    if (custList && custList.length > 0) {
                        setSelectedCustomerId(custList[0].id);
                        if (custList[0].addresses && custList[0].addresses.length > 0) {
                            const addr = custList[0].addresses[0];
                            const addrStr = `${addr.street}, ${addr.city}, ${addr.state} ${addr.postalCode}, ${addr.country}`;
                            setShippingAddress(addrStr);
                            setBillingAddress(addrStr);
                        }
                    }
                })
                .catch(err => console.error("Failed to load reference data for order:", err));
        }
    }, [isOpen]);

    const handleCustomerChange = (customerId) => {
        setSelectedCustomerId(customerId);
        const cust = customers.find(c => String(c.id) === String(customerId));
        if (cust && cust.addresses && cust.addresses.length > 0) {
            const addr = cust.addresses[0];
            const addrStr = `${addr.street}, ${addr.city}, ${addr.state} ${addr.postalCode}, ${addr.country}`;
            setShippingAddress(addrStr);
            setBillingAddress(addrStr);
        }
    };

    const handleProductSelect = (index, sku) => {
        const prod = products.find(p => p.sku === sku);
        setItems(prev => {
            const next = [...prev];
            if (prod) {
                next[index] = {
                    ...next[index],
                    productSku: prod.sku,
                    productName: prod.name,
                    unitPrice: prod.price
                };
            } else {
                next[index] = { ...next[index], productSku: sku, productName: '', unitPrice: 0 };
            }
            return next;
        });
    };

    const handleQuantityChange = (index, qty) => {
        setItems(prev => {
            const next = [...prev];
            next[index].quantity = Math.max(1, parseInt(qty, 10) || 1);
            return next;
        });
    };

    const addItemRow = () => {
        setItems(prev => [...prev, { productSku: '', productName: '', unitPrice: 0, quantity: 1 }]);
    };

    const removeItemRow = (index) => {
        if (items.length <= 1) return;
        setItems(prev => prev.filter((_, i) => i !== index));
    };

    const totalAmount = items.reduce((sum, item) => sum + (Number(item.unitPrice || 0) * Number(item.quantity || 1)), 0);

    const handleSubmit = async (e) => {
        e.preventDefault();
        if (!selectedCustomerId) {
            alert("Please select a customer");
            return;
        }
        if (items.some(i => !i.productSku || !i.unitPrice)) {
            alert("Please select a valid product for every item row");
            return;
        }

        setIsSubmitting(true);
        setError(null);
        try {
            const orderPayload = {
                customerId: Number(selectedCustomerId),
                shippingAddress,
                billingAddress,
                notes,
                items: items.map(i => ({
                    productSku: i.productSku,
                    productName: i.productName,
                    unitPrice: Number(i.unitPrice),
                    quantity: Number(i.quantity)
                }))
            };

            const createdOrder = await createOrder(orderPayload);
            if (onOrderCreated) {
                onOrderCreated(createdOrder);
            }
            onClose();
        } catch (err) {
            setError(err.response?.data?.message || err.message || 'Failed to create order');
        } finally {
            setIsSubmitting(false);
        }
    };

    if (!isOpen) return null;

    return (
        <div className="fixed inset-0 z-50 bg-black/50 backdrop-blur-sm flex items-center justify-center p-4">
            <div className="bg-white rounded-xl shadow-2xl border border-gray-200 max-w-2xl w-full p-6 animate-in fade-in max-h-[90vh] overflow-y-auto">
                <div className="flex justify-between items-center border-b pb-3 mb-4">
                    <div>
                        <h3 className="text-lg font-bold text-gray-900">Create Sales Order</h3>
                        <p className="text-xs text-gray-500">Place a new order and reserve inventory automatically</p>
                    </div>
                    <button onClick={onClose} className="text-gray-400 hover:text-gray-600 text-xl font-bold p-1">&times;</button>
                </div>

                {error && (
                    <div className="mb-4 p-3 bg-red-50 border border-red-200 text-red-700 text-xs rounded-lg">
                        ⚠️ {error}
                    </div>
                )}

                <form onSubmit={handleSubmit} className="space-y-4">
                    <div>
                        <label className="block text-xs font-semibold text-gray-600 uppercase mb-1">Customer Account</label>
                        <select
                            value={selectedCustomerId}
                            onChange={(e) => handleCustomerChange(e.target.value)}
                            required
                            className="w-full p-2.5 border rounded-lg text-xs bg-white focus:ring-2 focus:ring-blue-500 focus:outline-none"
                        >
                            {customers.map(c => (
                                <option key={c.id} value={c.id}>
                                    {c.fullName} ({c.companyName ? `${c.companyName}, ` : ''}{c.email})
                                </option>
                            ))}
                        </select>
                    </div>

                    <div className="border rounded-lg p-3 bg-gray-50 space-y-3">
                        <div className="flex justify-between items-center border-b pb-2">
                            <span className="text-xs font-bold text-gray-700 uppercase">Order Line Items</span>
                            <button
                                type="button"
                                onClick={addItemRow}
                                className="text-xs font-semibold text-blue-600 hover:text-blue-700"
                            >
                                + Add Another Item
                            </button>
                        </div>

                        {items.map((item, idx) => (
                            <div key={idx} className="grid grid-cols-12 gap-2 items-center bg-white p-2 rounded border">
                                <div className="col-span-5">
                                    <label className="block text-[10px] text-gray-500 font-semibold mb-0.5">Product / SKU</label>
                                    <select
                                        value={item.productSku}
                                        onChange={(e) => handleProductSelect(idx, e.target.value)}
                                        required
                                        className="w-full p-1.5 border rounded text-xs bg-white focus:outline-none"
                                    >
                                        <option value="">-- Select Product --</option>
                                        {products.map(p => (
                                            <option key={p.sku} value={p.sku}>
                                                {p.sku} - {p.name} (₹{p.price})
                                            </option>
                                        ))}
                                    </select>
                                </div>
                                <div className="col-span-2">
                                    <label className="block text-[10px] text-gray-500 font-semibold mb-0.5">Price (₹)</label>
                                    <input
                                        type="number"
                                        readOnly
                                        value={item.unitPrice}
                                        className="w-full p-1.5 border rounded text-xs bg-gray-100 text-gray-600 font-mono"
                                    />
                                </div>
                                <div className="col-span-2">
                                    <label className="block text-[10px] text-gray-500 font-semibold mb-0.5">Qty</label>
                                    <input
                                        type="number"
                                        min="1"
                                        value={item.quantity}
                                        onChange={(e) => handleQuantityChange(idx, e.target.value)}
                                        className="w-full p-1.5 border rounded text-xs focus:ring-1 focus:ring-blue-500"
                                    />
                                </div>
                                <div className="col-span-2 text-right">
                                    <label className="block text-[10px] text-gray-500 font-semibold mb-0.5">Subtotal</label>
                                    <div className="text-xs font-mono font-bold text-gray-800 pt-1.5">
                                        ₹{(Number(item.unitPrice || 0) * Number(item.quantity || 1)).toFixed(2)}
                                    </div>
                                </div>
                                <div className="col-span-1 text-right pt-4">
                                    {items.length > 1 && (
                                        <button
                                            type="button"
                                            onClick={() => removeItemRow(idx)}
                                            className="text-red-500 hover:text-red-700 text-sm font-bold"
                                        >
                                            &times;
                                        </button>
                                    )}
                                </div>
                            </div>
                        ))}

                        <div className="flex justify-between items-center pt-2 font-bold text-sm text-gray-900 border-t">
                            <span>Estimated Total:</span>
                            <span className="text-base text-blue-700 font-mono">₹{totalAmount.toFixed(2)}</span>
                        </div>
                    </div>

                    <div className="grid grid-cols-2 gap-3">
                        <div>
                            <label className="block text-xs font-semibold text-gray-600 mb-1">Shipping Address</label>
                            <textarea
                                value={shippingAddress}
                                onChange={(e) => setShippingAddress(e.target.value)}
                                rows="2"
                                className="w-full p-2 border rounded-lg text-xs focus:ring-2 focus:ring-blue-500 focus:outline-none"
                            />
                        </div>
                        <div>
                            <label className="block text-xs font-semibold text-gray-600 mb-1">Order Notes</label>
                            <textarea
                                value={notes}
                                onChange={(e) => setNotes(e.target.value)}
                                rows="2"
                                placeholder="Special delivery instructions..."
                                className="w-full p-2 border rounded-lg text-xs focus:ring-2 focus:ring-blue-500 focus:outline-none"
                            />
                        </div>
                    </div>

                    <div className="flex justify-end gap-2 pt-3 border-t">
                        <button
                            type="button"
                            onClick={onClose}
                            className="px-4 py-2 border rounded-lg text-xs font-semibold text-gray-600 hover:bg-gray-50"
                        >
                            Cancel
                        </button>
                        <button
                            type="submit"
                            disabled={isSubmitting}
                            className="px-5 py-2 bg-blue-600 hover:bg-blue-700 disabled:bg-blue-300 text-white font-semibold text-xs rounded-lg shadow transition"
                        >
                            {isSubmitting ? 'Placing Order...' : '⚡ Confirm & Place Order'}
                        </button>
                    </div>
                </form>
            </div>
        </div>
    );
};

export default CreateOrderModal;
