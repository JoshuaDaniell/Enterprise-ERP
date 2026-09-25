import { useState, useEffect, useCallback } from 'react';
import ProductForm from '../components/ProductForm';
import ProductTable from '../components/ProductTable';
import { getAllProducts, createProduct, adjustProductStock, deleteProduct } from '../services/inventoryservice';

const InventoryDashboard = () => {
    const [products, setProducts] = useState([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState(null);
    const [successMessage, setSuccessMessage] = useState(null);

    const loadProducts = useCallback(async () => {
        setLoading(true);
        try {
            const data = await getAllProducts();
            setProducts(Array.isArray(data) ? data : []);
            setError(null);
        } catch (err) {
            console.error("Failed to fetch inventory:", err);
            setError("Could not connect to the inventory backend. Showing local dashboard shell.");
        } finally {
            setLoading(false);
        }
    }, []);

    useEffect(() => {
        // eslint-disable-next-line react-hooks/set-state-in-effect
        loadProducts();
    }, [loadProducts]);

    const handleCreateProduct = async (formData) => {
        try {
            const savedProduct = await createProduct(formData);
            setProducts((prev) => [...prev, savedProduct]);
            setSuccessMessage(`Product ${savedProduct.sku} created successfully!`);
            setTimeout(() => setSuccessMessage(null), 4000);
        } catch (err) {
            const msg = err.response?.data?.message || err.message || "Failed to save product";
            console.error("Save product error:", msg);
            alert(`Error: ${msg}`);
        }
    };

    const handleAdjustStock = async (id, amount) => {
        try {
            const updatedProduct = await adjustProductStock(id, amount);
            setProducts((prev) =>
                prev.map((p) => (p.id === id ? updatedProduct : p))
            );
        } catch (err) {
            const msg = err.response?.data?.message || err.message || "Failed to adjust stock";
            console.error("Stock adjustment error:", msg);
            alert(`Adjustment failed: ${msg}`);
            // Reload on optimistic concurrency conflict
            loadProducts();
        }
    };

    const handleDeleteProduct = async (id) => {
        if (!window.confirm("Are you sure you want to delete this product?")) return;
        try {
            await deleteProduct(id);
            setProducts((prev) => prev.filter((p) => p.id !== id));
            setSuccessMessage("Product deleted successfully");
            setTimeout(() => setSuccessMessage(null), 3000);
        } catch (err) {
            const msg = err.response?.data?.message || err.message || "Failed to delete product";
            alert(`Delete failed: ${msg}`);
        }
    };

    return (
        <div className="dashboard-card w-full p-6 my-4">
            <div className="border-b pb-4 mb-6 flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
                <div>
                    <h2 className="text-2xl font-black text-gray-900 tracking-tight">Inventory Management</h2>
                    <p className="text-xs text-gray-500 mt-1">Track stock levels, manage products, and monitor warehouse activity</p>
                </div>
                <button 
                    onClick={loadProducts}
                    className="px-3 py-1.5 bg-gray-100 hover:bg-gray-200 text-gray-700 text-xs font-semibold rounded border border-gray-300 transition flex items-center gap-1 self-start sm:self-auto"
                >
                    ↻ Refresh
                </button>
            </div>

            {error && (
                <div className="mb-5 p-3.5 bg-amber-50 border border-amber-200 text-amber-900 rounded-lg text-sm flex items-center justify-between">
                    <span>⚠️ {error}</span>
                </div>
            )}

            {successMessage && (
                <div className="mb-5 p-3.5 bg-emerald-50 border border-emerald-200 text-emerald-900 rounded-lg text-sm font-medium">
                    ✓ {successMessage}
                </div>
            )}

            <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
                <div className="lg:col-span-1">
                    <ProductForm onAddProduct={handleCreateProduct} />
                </div>

                <div className="lg:col-span-2 space-y-3">
                    <div className="flex items-center justify-between">
                        <h3 className="font-bold text-gray-800 text-base">Warehouse Stock Table</h3>
                        <span className="text-xs font-semibold text-gray-500 bg-gray-100 px-2.5 py-1 rounded">
                            Total SKUs: {products.length}
                        </span>
                    </div>

                    {loading ? (
                        <div className="p-12 text-center text-gray-500 text-sm">
                            Loading inventory records...
                        </div>
                    ) : (
                        <ProductTable 
                            products={products} 
                            onAdjustStock={handleAdjustStock} 
                            onDeleteProduct={handleDeleteProduct}
                        />
                    )}
                </div>
            </div>
        </div>
    );
};

export default InventoryDashboard;