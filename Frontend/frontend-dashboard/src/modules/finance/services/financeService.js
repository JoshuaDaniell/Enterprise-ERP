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

export const getInvoices = async () => unwrap(await financeApi.get('/invoices'));
export const getFinanceDashboard = async () => unwrap(await financeApi.get('/invoices/dashboard'));
export const updatePaymentStatus = async (invoiceNumber, reference = 'dashboard-payment') =>
    unwrap(await financeApi.post(`/invoices/${invoiceNumber}/payment`, { paymentReference: reference }));
export const downloadInvoice = (invoiceNumber) =>
    financeApi.get(`/invoices/${invoiceNumber}/pdf`, { responseType: 'blob' });
