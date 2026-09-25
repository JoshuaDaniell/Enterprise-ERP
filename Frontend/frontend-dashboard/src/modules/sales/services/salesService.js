import axios from 'axios';

const salesApi = axios.create({
    baseURL: import.meta.env.VITE_SALES_API_URL || '/api/sales',
    headers: {
        'Content-Type': 'application/json'
    }
});

salesApi.interceptors.request.use((config) => {
    const token = localStorage.getItem('erp_access_token');
    if (token) {
        config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
});

// Orders API
export const getAllOrders = async () => {
    try {
        const response = await salesApi.get('/orders');
        const orders = response.data?.data || response.data || [];
        localStorage.setItem('erp_cache_orders', JSON.stringify(orders));
        return orders;
    } catch (error) {
        const cached = localStorage.getItem('erp_cache_orders');
        if (cached) return JSON.parse(cached);
        throw error;
    }
};

export const getOrderById = async (id) => {
    const response = await salesApi.get(`/orders/${id}`);
    return response.data?.data || response.data;
};

export const createOrder = async (orderData) => {
    const response = await salesApi.post('/orders', orderData);
    return response.data?.data || response.data;
};

export const updateOrderStatus = async (id, status, remarks = '') => {
    const response = await salesApi.patch(`/orders/${id}/status`, { status, remarks });
    return response.data?.data || response.data;
};

export const cancelOrder = async (id, reason = 'Cancelled by user') => {
    const response = await salesApi.post(`/orders/${id}/cancel?reason=${encodeURIComponent(reason)}`);
    return response.data?.data || response.data;
};

// Customers API
export const getAllCustomers = async () => {
    try {
        const response = await salesApi.get('/customers');
        const customers = response.data?.data || response.data || [];
        localStorage.setItem('erp_cache_customers', JSON.stringify(customers));
        return customers;
    } catch (error) {
        const cached = localStorage.getItem('erp_cache_customers');
        if (cached) return JSON.parse(cached);
        throw error;
    }
};

export const createCustomer = async (customerData) => {
    const response = await salesApi.post('/customers', customerData);
    return response.data?.data || response.data;
};
