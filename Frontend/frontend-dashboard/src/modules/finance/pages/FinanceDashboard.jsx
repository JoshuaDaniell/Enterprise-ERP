import { useCallback, useEffect, useState } from 'react';
import { downloadInvoice, getFinanceDashboard, getInvoices, updatePaymentStatus } from '../services/financeService';
import { useAuth } from '../../auth/context/AuthContext';

const FinanceDashboard = () => {
    const { hasRole } = useAuth();
    const [invoices, setInvoices] = useState([]);
    const [metrics, setMetrics] = useState({});
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState(null);

    const loadFinance = useCallback(async () => {
        setLoading(true);
        try {
            const [invoiceData, dashboardData] = await Promise.all([getInvoices(), getFinanceDashboard()]);
            setInvoices(Array.isArray(invoiceData) ? invoiceData : []);
            setMetrics(dashboardData || {});
            setError(null);
        } catch (err) {
            setError(err.response?.data?.message || 'Could not connect to finance. Please try again in a moment.');
        } finally {
            setLoading(false);
        }
    }, []);

    useEffect(() => {
        // Synchronize the view with the remote finance service.
        // eslint-disable-next-line react-hooks/set-state-in-effect
        loadFinance();
    }, [loadFinance]);

    const markPaid = async (invoiceId) => {
        try {
            const updated = await updatePaymentStatus(invoiceId);
            setInvoices((current) => current.map((invoice) => invoice.id === updated.id ? updated : invoice));
        } catch (err) {
            setError(err.response?.data?.message || 'Unable to update payment status.');
        }
    };

    const downloadPdf = async (invoiceId, invoiceNumber) => {
        const response = await downloadInvoice(invoiceId);
        const url = URL.createObjectURL(response.data);
        const link = document.createElement('a');
        link.href = url;
        link.download = `${invoiceNumber || 'invoice'}.pdf`;
        link.click();
        URL.revokeObjectURL(url);
    };

    const canManagePayments = hasRole('ROLE_FINANCE_USER');
    const revenue = Number(metrics.totalRevenue ?? metrics.revenue ?? 0);
    const outstanding = Number(metrics.outstandingAmount ?? metrics.outstanding ?? 0);

    return (
        <div className="dashboard-card w-full p-6 my-4">
            <div className="border-b pb-4 mb-6 flex items-center justify-between gap-4">
                <div>
                    <h2 className="text-2xl font-black text-gray-900">Finance & Billing</h2>
                    <p className="text-xs text-gray-500 mt-1">Invoices, tax, revenue ledger and payment tracking</p>
                </div>
                <button onClick={loadFinance} className="px-3 py-2 bg-gray-100 hover:bg-gray-200 text-xs font-semibold rounded-lg border border-gray-300">↻ Refresh</button>
            </div>
            {error && <div className="mb-5 p-3.5 bg-amber-50 border border-amber-200 text-amber-900 rounded-lg text-sm">{error}</div>}
            <div className="grid grid-cols-1 sm:grid-cols-3 gap-4 mb-6">
                <div className="bg-emerald-50 border border-emerald-200 rounded-xl p-4"><span className="text-[11px] font-semibold text-emerald-700 uppercase">Revenue</span><div className="text-xl font-black text-emerald-900 mt-1">₹{revenue.toFixed(2)}</div></div>
                <div className="bg-amber-50 border border-amber-200 rounded-xl p-4"><span className="text-[11px] font-semibold text-amber-700 uppercase">Outstanding</span><div className="text-xl font-black text-amber-900 mt-1">₹{outstanding.toFixed(2)}</div></div>
                <div className="bg-blue-50 border border-blue-200 rounded-xl p-4"><span className="text-[11px] font-semibold text-blue-700 uppercase">Invoices</span><div className="text-xl font-black text-blue-900 mt-1">{invoices.length}</div></div>
            </div>
            <div className="border border-gray-200 rounded-lg overflow-x-auto">
                <table className="w-full text-left min-w-[720px]">
                    <thead className="bg-gray-50 text-gray-700 text-xs font-semibold uppercase border-b"><tr><th className="p-3">Invoice</th><th className="p-3">Order</th><th className="p-3">Customer</th><th className="p-3 text-right">Total</th><th className="p-3 text-center">Payment</th><th className="p-3 text-right">Actions</th></tr></thead>
                    <tbody className="divide-y divide-gray-100 text-xs">
                        {loading ? <tr><td colSpan="6" className="p-8 text-center text-gray-500">Loading finance records...</td></tr> :
                            invoices.length === 0 ? <tr><td colSpan="6" className="p-8 text-center text-gray-400">No invoices have been generated yet.</td></tr> :
                                invoices.map((invoice) => <tr key={invoice.id} className="hover:bg-blue-50/40">
                                    <td className="p-3 font-mono font-bold text-blue-700">{invoice.invoiceNumber}</td>
                                    <td className="p-3">{invoice.orderNumber || invoice.orderId}</td>
                                    <td className="p-3">{invoice.customerName || invoice.customerEmail || '—'}</td>
                                    <td className="p-3 text-right font-mono font-bold">₹{Number(invoice.totalAmount ?? invoice.grandTotal ?? 0).toFixed(2)}</td>
                                    <td className="p-3 text-center"><span className="px-2 py-1 rounded-full bg-slate-100 border text-[10px] font-bold">{invoice.paymentStatus || invoice.status}</span></td>
                                    <td className="p-3 text-right space-x-2"><button onClick={() => downloadPdf(invoice.invoiceNumber, invoice.invoiceNumber)} className="px-2 py-1 text-blue-700 bg-blue-50 rounded border border-blue-200">PDF</button>{canManagePayments && invoice.paymentStatus !== 'PAID' && <button onClick={() => markPaid(invoice.invoiceNumber)} className="px-2 py-1 text-emerald-700 bg-emerald-50 rounded border border-emerald-200">Mark paid</button>}</td>
                                </tr>)}
                    </tbody>
                </table>
            </div>
        </div>
    );
};

export default FinanceDashboard;
