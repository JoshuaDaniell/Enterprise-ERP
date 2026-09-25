import api from '../../../api/api';

export const getAllProducts = async () => {
    try {
        const response = await api.get("/products");
        const products = response.data?.data || response.data || [];
        localStorage.setItem('erp_cache_products', JSON.stringify(products));
        return products;
    } catch (error) {
        const cached = localStorage.getItem('erp_cache_products');
        if (cached) return JSON.parse(cached);
        throw error;
    }
};

export const searchProducts = async (query = '', page = 0, size = 10) => {
    const response = await api.get(`/products/search?query=${encodeURIComponent(query)}&page=${page}&size=${size}`);
    return response.data?.data || response.data;
};

export const getProductById = async (id) => {
    const response = await api.get(`/products/${id}`);
    return response.data?.data || response.data;
};

export const createProduct = async (productData) => {
    const response = await api.post("/products", productData);
    return response.data?.data || response.data;
};

export const updateProduct = async (id, productData) => {
    const response = await api.put(`/products/${id}`, productData);
    return response.data?.data || response.data;
};

export const adjustProductStock = async (id, amount, reason = "Dashboard manual adjustment") => {
    const response = await api.patch(`/products/${id}/stock`, { amount, reason });
    return response.data?.data || response.data;
};

export const deleteProduct = async (id) => {
    const response = await api.delete(`/products/${id}`);
    return response.data?.data || response.data;
};

export const getProductMovements = async (id) => {
    const response = await api.get(`/products/${id}/movements`);
    return response.data?.data || response.data || [];
};