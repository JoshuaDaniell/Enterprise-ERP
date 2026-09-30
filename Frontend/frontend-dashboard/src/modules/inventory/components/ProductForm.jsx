import { useState } from 'react';
import { useAuth } from '../../auth/context/AuthContext';

const ProductForm = ({ onAddProduct }) => {
    const { canCreateProduct, user } = useAuth();
    const [sku, setSku] = useState('');
    const [name, setName] = useState('');
    const [price, setPrice] = useState('');
    const [quantity, setQuantity] = useState('');
    const [isSubmitting, setIsSubmitting] = useState(false);

    const handleSubmit = async (e) => {
        e.preventDefault();
        if (!sku || !name || !price || !quantity) {
            alert("Please fill out all fields first!");
            return;
        }

        setIsSubmitting(true);
        try {
            await onAddProduct({
                sku: sku.trim().toUpperCase(),
                name: name.trim(),
                price: parseFloat(price),
                quantity: parseInt(quantity, 10)
            });

            setSku('');
            setName('');
            setPrice('');
            setQuantity('');
        } finally {
            setIsSubmitting(false);
        }
    };

    return (
        <form onSubmit={handleSubmit} className="bg-white p-5 rounded-lg border border-gray-200 shadow-sm space-y-4">
            <div className="flex items-center justify-between border-b pb-2">
                <h3 className="text-base font-bold text-gray-800">Add New Product</h3>
                {!canCreateProduct && (
                    <span className="text-[11px] font-semibold bg-amber-100 text-amber-800 px-2 py-0.5 rounded border border-amber-200">
                        Read Only
                    </span>
                )}
            </div>

            {!canCreateProduct && (
                <div className="p-3 bg-amber-50 border border-amber-200 rounded-lg text-xs text-amber-800 space-y-1.5">
                    <p>Current role (<strong>{user?.role?.replace('ROLE_', '') || 'ANONYMOUS'}</strong>) cannot create products. Sign in with an inventory account to manage stock.</p>
                </div>
            )}
            
            <div>
                <label className="block text-xs font-semibold text-gray-600 uppercase mb-1">SKU / Code</label>
                <input 
                    type="text" 
                    value={sku} 
                    onChange={(e) => setSku(e.target.value)}
                    disabled={!canCreateProduct}
                    className="w-full p-2 border border-gray-300 rounded text-sm font-mono uppercase focus:ring-2 focus:ring-blue-500 focus:outline-none disabled:bg-gray-100 disabled:cursor-not-allowed"
                    placeholder="e.g., PROD-001"
                    required
                />
            </div>

            <div>
                <label className="block text-xs font-semibold text-gray-600 uppercase mb-1">Product Name</label>
                <input 
                    type="text" 
                    value={name} 
                    onChange={(e) => setName(e.target.value)}
                    disabled={!canCreateProduct}
                    className="w-full p-2 border border-gray-300 rounded text-sm focus:ring-2 focus:ring-blue-500 focus:outline-none disabled:bg-gray-100 disabled:cursor-not-allowed"
                    placeholder="e.g., Wireless Ergonomic Mouse"
                    required
                />
            </div>

            <div className="grid grid-cols-2 gap-3">
                <div>
                    <label className="block text-xs font-semibold text-gray-600 uppercase mb-1">Price (₹)</label>
                    <input 
                        type="number" 
                        step="0.01"
                        min="0.01"
                        value={price} 
                        onChange={(e) => setPrice(e.target.value)}
                        disabled={!canCreateProduct}
                        className="w-full p-2 border border-gray-300 rounded text-sm focus:ring-2 focus:ring-blue-500 focus:outline-none disabled:bg-gray-100 disabled:cursor-not-allowed"
                        placeholder="e.g., 1499.00"
                        required
                    />
                </div>

                <div>
                    <label className="block text-xs font-semibold text-gray-600 uppercase mb-1">Initial Qty</label>
                    <input 
                        type="number" 
                        min="0"
                        value={quantity} 
                        onChange={(e) => setQuantity(e.target.value)}
                        disabled={!canCreateProduct}
                        className="w-full p-2 border border-gray-300 rounded text-sm focus:ring-2 focus:ring-blue-500 focus:outline-none disabled:bg-gray-100 disabled:cursor-not-allowed"
                        placeholder="e.g., 50"
                        required
                    />
                </div>
            </div>

            <button 
                type="submit" 
                disabled={isSubmitting || !canCreateProduct}
                className="w-full py-2.5 bg-blue-600 hover:bg-blue-700 disabled:bg-gray-300 disabled:cursor-not-allowed text-white font-semibold rounded shadow transition text-sm flex items-center justify-center space-x-1"
            >
                <span>{isSubmitting ? "Adding..." : "+ Create Product"}</span>
            </button>
        </form>
    );
};

export default ProductForm;
