import axios from 'axios';

const financeApi = axios.create({
    baseURL: import.meta.env.VITE_FINANCE_API_URL || '/api/finance',
    headers: { 'Content-Type': 'application/json' }
});

financeApi.interceptors.request.use((config) => {
    const token = localStorage.getItem('erp_access_token');
    if (token) config.headers.Authorization = `Bearer ${token}`;
    return config;
});

const unwrap = (response) => response.data?.data ?? response.data;

export const getInvoices = async () => {
    try {
        const invoices = unwrap(await financeApi.get('/invoices'));
        localStorage.setItem('erp_cache_invoices', JSON.stringify(invoices));
        return invoices;
    } catch (error) {
        const cached = localStorage.getItem('erp_cache_invoices');
        if (cached) return JSON.parse(cached);
        throw error;
    }
};
export const getFinanceDashboard = async () => {
    try {
        const dashboard = unwrap(await financeApi.get('/invoices/dashboard'));
        localStorage.setItem('erp_cache_finance_dashboard', JSON.stringify(dashboard));
        return dashboard;
    } catch (error) {
        const cached = localStorage.getItem('erp_cache_finance_dashboard');
        if (cached) return JSON.parse(cached);
        throw error;
    }
};
export const updatePaymentStatus = async (invoiceNumber, reference = 'dashboard-payment') =>
    unwrap(await financeApi.post(`/invoices/${invoiceNumber}/payment`, { paymentReference: reference }));
export const downloadInvoice = (invoiceNumber) =>
    financeApi.get(`/invoices/${invoiceNumber}/pdf`, { responseType: 'blob' });
