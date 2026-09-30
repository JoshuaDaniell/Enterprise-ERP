import { useAuth } from '../../auth/context/AuthContext';

const ProductTable = ({ products, onAdjustStock, onDeleteProduct }) => {
    const { canDeleteProduct, canAdjustStock } = useAuth();

    if (!products || products.length === 0) {
        return (
            <div className="bg-white p-8 rounded-lg border border-dashed border-gray-300 text-center text-gray-500 font-medium">
                No products found in the database. Add your first product using the form.
            </div>
        );
    }

    return (
        <div className="bg-white rounded-lg border border-gray-200 shadow-sm overflow-x-auto">
            <table className="w-full text-left border-collapse min-w-[600px]">
                <thead className="bg-gray-50 text-gray-700 text-xs font-semibold uppercase border-b border-gray-200">
                    <tr>
                        <th className="p-3.5">SKU</th>
                        <th className="p-3.5">Product Name</th>
                        <th className="p-3.5">Price (INR)</th>
                        <th className="p-3.5">Stock Level</th>
                        <th className="p-3.5 text-center">Concurrency Ver.</th>
                        <th className="p-3.5 text-right">Actions</th>
                    </tr>
                </thead>
                <tbody className="divide-y divide-gray-100 text-sm">
                    {products.map((product) => {
                        const isLowStock = product.quantity < 10;
                        return (
                            <tr key={product.id} className="hover:bg-blue-50/40 transition">
                                <td className="p-3.5 font-mono text-xs font-semibold text-blue-700">
                                    {product.sku || 'N/A'}
                                </td>
                                <td className="p-3.5 font-medium text-gray-900">{product.name}</td>
                                <td className="p-3.5 text-gray-700 font-mono">
                                    ₹{Number(product.price).toFixed(2)}
                                </td>
                                <td className="p-3.5">
                                    <span className={`inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-semibold ${
                                        isLowStock 
                                            ? 'bg-red-100 text-red-800' 
                                            : 'bg-emerald-100 text-emerald-800'
                                    }`}>
                                        {product.quantity} units {isLowStock && '(Low)'}
                                    </span>
                                </td>
                                <td className="p-3.5 text-center font-mono text-xs text-gray-500">
                                    v{product.version ?? 0}
                                </td>
                                <td className="p-3.5 text-right space-x-1.5 whitespace-nowrap">
                                    <button 
                                        onClick={() => onAdjustStock(product.id, 1)}
                                        disabled={!canAdjustStock}
                                        title={canAdjustStock ? "Increase stock by 1" : "Inventory access required"}
                                        className="px-2.5 py-1 bg-emerald-50 text-emerald-700 font-medium rounded border border-emerald-300 hover:bg-emerald-100 disabled:opacity-40 disabled:cursor-not-allowed transition text-xs"
                                    >
                                        +1
                                    </button>
                                    <button 
                                        onClick={() => onAdjustStock(product.id, -1)}
                                        disabled={!canAdjustStock || product.quantity <= 0}
                                        title={canAdjustStock ? "Decrease stock by 1" : "Inventory access required"}
                                        className="px-2.5 py-1 bg-amber-50 text-amber-700 font-medium rounded border border-amber-300 hover:bg-amber-100 disabled:opacity-40 disabled:cursor-not-allowed transition text-xs"
                                    >
                                        -1
                                    </button>
                                    {onDeleteProduct && (
                                        <button 
                                            onClick={() => onDeleteProduct(product.id)}
                                            disabled={!canDeleteProduct}
                                            title={canDeleteProduct ? "Delete product" : "Inventory access required"}
                                            className="px-2.5 py-1 bg-red-50 text-red-700 font-medium rounded border border-red-300 hover:bg-red-100 disabled:opacity-40 disabled:cursor-not-allowed transition text-xs"
                                        >
                                            Delete
                                        </button>
                                    )}
                                </td>
                            </tr>
                        );
                    })}
                </tbody>
            </table>
        </div>
    );
};

export default ProductTable;
