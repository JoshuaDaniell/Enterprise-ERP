import api from '../../../api/api';

const salesApiBaseUrl = import.meta.env.VITE_SALES_API_URL || '/api/sales';

// Orders API
export const getAllOrders = async () => {
    const response = await api.get('/orders', { baseURL: salesApiBaseUrl });
    return response.data?.data || response.data || [];
};

export const getOrderById = async (id) => {
    const response = await api.get(`/orders/${id}`, { baseURL: salesApiBaseUrl });
    return response.data?.data || response.data;
};

export const createOrder = async (orderData) => {
    const response = await api.post('/orders', orderData, { baseURL: salesApiBaseUrl });
    return response.data?.data || response.data;
};

export const cancelOrder = async (id, reason = 'Cancelled by user') => {
    const response = await api.post(`/orders/${id}/cancel?reason=${encodeURIComponent(reason)}`, null, {
        baseURL: salesApiBaseUrl
    });
    return response.data?.data || response.data;
};

// Customers API
export const getAllCustomers = async () => {
    const response = await api.get('/customers', { baseURL: salesApiBaseUrl });
    return response.data?.data || response.data || [];
};

// This read-only inventory catalog endpoint is available to sales users.
// Stock reservation still happens asynchronously through Kafka.
export const getOrderableProducts = async () => {
    const inventoryApiBaseUrl = import.meta.env.VITE_INVENTORY_API_URL || '/api/inventory';
    const response = await api.get('/catalog/products', { baseURL: inventoryApiBaseUrl });
    return response.data?.data || response.data || [];
};

export const createCustomer = async (customerData) => {
    const response = await api.post('/customers', customerData, { baseURL: salesApiBaseUrl });
    return response.data?.data || response.data;
};
