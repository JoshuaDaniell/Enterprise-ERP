import { useState, useEffect } from 'react';
import { getAllCustomers, getOrderableProducts, createOrder } from '../services/salesService';

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
    const [referenceLoadAttempt, setReferenceLoadAttempt] = useState(0);
    const [loadedReferenceAttempt, setLoadedReferenceAttempt] = useState(-1);
    const [referenceError, setReferenceError] = useState(null);
    const [error, setError] = useState(null);
    const isLoadingReferenceData = isOpen && loadedReferenceAttempt !== referenceLoadAttempt;

    useEffect(() => {
        if (!isOpen || loadedReferenceAttempt === referenceLoadAttempt) return undefined;

        let isActive = true;

        Promise.all([getAllCustomers(), getOrderableProducts()])
            .then(([customerList, productList]) => {
                if (!isActive) return;

                if (Array.isArray(customerList) && Array.isArray(productList)) {
                    setCustomers(customerList);
                    setProducts(productList);
                    if (customerList.length > 0) {
                        setSelectedCustomerId(customerList[0].id);
                        const address = customerList[0].addresses?.[0];
                        if (address) {
                            const addressText = `${address.street}, ${address.city}, ${address.state} ${address.postalCode}, ${address.country}`;
                            setShippingAddress(addressText);
                            setBillingAddress(addressText);
                        } else {
                            setShippingAddress('');
                            setBillingAddress('');
                        }
                    } else {
                        setSelectedCustomerId('');
                        setShippingAddress('');
                        setBillingAddress('');
                    }
                } else {
                    setCustomers([]);
                    setProducts([]);
                    setSelectedCustomerId('');
                    setShippingAddress('');
                    setBillingAddress('');
                    setReferenceError('The Sales or Inventory service returned invalid order data.');
                }
                setLoadedReferenceAttempt(referenceLoadAttempt);
            })
            .catch((error) => {
                if (!isActive) return;
                setCustomers([]);
                setProducts([]);
                setSelectedCustomerId('');
                setShippingAddress('');
                setBillingAddress('');
                setReferenceError(`Could not load customers or inventory products: ${error.response?.data?.message || error.message || 'an unexpected error occurred'}.`);
                setLoadedReferenceAttempt(referenceLoadAttempt);
            });

        return () => {
            isActive = false;
        };
    }, [isOpen, referenceLoadAttempt, loadedReferenceAttempt]);

    const handleCustomerChange = (customerId) => {
        setSelectedCustomerId(customerId);
        const cust = customers.find(c => String(c.id) === String(customerId));
        if (cust && cust.addresses && cust.addresses.length > 0) {
            const addr = cust.addresses[0];
            const addrStr = `${addr.street}, ${addr.city}, ${addr.state} ${addr.postalCode}, ${addr.country}`;
            setShippingAddress(addrStr);
            setBillingAddress(addrStr);
        } else {
            setShippingAddress('');
            setBillingAddress('');
        }
    };

    const handleProductChange = (index, sku) => {
        const product = products.find(candidate => candidate.sku === sku);
        setItems(prev => {
            const next = [...prev];
            next[index] = product
                ? { ...next[index], productSku: product.sku, productName: product.name, unitPrice: Number(product.price), quantity: 1, availableQuantity: product.quantity }
                : { ...next[index], productSku: '', productName: '', unitPrice: 0, quantity: 1, availableQuantity: 0 };
            return next;
        });
    };

    const handleQuantityChange = (index, qty) => {
        setItems(prev => {
            const next = [...prev];
            const requestedQuantity = Math.max(1, parseInt(qty, 10) || 1);
            next[index].quantity = next[index].availableQuantity
                ? Math.min(requestedQuantity, next[index].availableQuantity)
                : requestedQuantity;
            return next;
        });
    };

    const addItemRow = () => {
        setItems(prev => [...prev, { productSku: '', productName: '', unitPrice: 0, quantity: 1, availableQuantity: 0 }]);
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
        if (items.some(i => !i.productSku.trim() || !i.productName.trim() || Number(i.unitPrice) <= 0)) {
            alert("Enter a SKU, product name, and positive unit price for every item");
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

                {isLoadingReferenceData && (
                    <div className="mb-4 p-3 bg-blue-50 border border-blue-200 text-blue-700 text-xs rounded-lg">
                        Loading customers and available inventory products...
                    </div>
                )}
                {referenceError && (
                    <div className="mb-4 p-3 bg-red-50 border border-red-200 text-red-700 text-xs rounded-lg flex justify-between items-center gap-3">
                        <span>{referenceError}</span>
                        <button
                            type="button"
                            onClick={() => {
                                setReferenceError(null);
                                setError(null);
                                setReferenceLoadAttempt(attempt => attempt + 1);
                            }}
                            disabled={isLoadingReferenceData}
                            className="font-semibold underline disabled:opacity-50"
                        >
                            Retry
                        </button>
                    </div>
                )}
                {!isLoadingReferenceData && !referenceError && customers.length === 0 && (
                    <div className="mb-4 p-3 bg-amber-50 border border-amber-200 text-amber-700 text-xs rounded-lg">
                        No customers are available. Add a customer before placing an order.
                    </div>
                )}
                {!isLoadingReferenceData && !referenceError && products.length === 0 && (
                    <div className="mb-4 p-3 bg-amber-50 border border-amber-200 text-amber-700 text-xs rounded-lg">
                        No products currently have available stock in Inventory.
                    </div>
                )}
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
                            disabled={isLoadingReferenceData || customers.length === 0}
                            className="w-full p-2.5 border rounded-lg text-xs bg-white focus:ring-2 focus:ring-blue-500 focus:outline-none"
                        >
                            <option value="">-- Select a customer --</option>
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
                                <div className="col-span-8">
                                    <label className="block text-[10px] text-gray-500 font-semibold mb-0.5">Product</label>
                                    <select
                                        value={item.productSku}
                                        onChange={(e) => handleProductChange(idx, e.target.value)}
                                        required
                                        disabled={products.length === 0}
                                        className="w-full p-1.5 border rounded text-xs bg-white focus:outline-none disabled:bg-gray-100"
                                    >
                                        <option value="">-- Select an available product --</option>
                                        {products.map(product => (
                                            <option key={product.id} value={product.sku}>
                                                {product.name} ({product.sku}) ? ?{Number(product.price).toFixed(2)} ? {product.quantity} available
                                            </option>
                                        ))}
                                    </select>
                                    {item.productSku && <p className="mt-1 text-[10px] text-gray-500">SKU: {item.productSku}</p>}
                                </div>
                                <div className="col-span-2">
                                    <label className="block text-[10px] text-gray-500 font-semibold mb-0.5">Unit Price</label>
                                    <div className="p-1.5 text-xs bg-gray-50 rounded font-mono">?{Number(item.unitPrice || 0).toFixed(2)}</div>
                                </div>
                                <div className="col-span-2">
                                    <label className="block text-[10px] text-gray-500 font-semibold mb-0.5">Qty {item.availableQuantity ? `(max ${item.availableQuantity})` : ''}</label>
                                    <input
                                        type="number"
                                        min="1"
                                        max={item.availableQuantity || undefined}
                                        value={item.quantity}
                                        onChange={(e) => handleQuantityChange(idx, e.target.value)}
                                        disabled={!item.productSku}
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
                            disabled={isSubmitting || isLoadingReferenceData || customers.length === 0 || products.length === 0}
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
