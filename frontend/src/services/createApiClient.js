import axios from 'axios';

export function createApiClient({ baseURL, onAuthExpired, timeout = 20000 }) {
    const client = axios.create({ baseURL, timeout, withCredentials: true });
    let refreshPromise = null;
    client.interceptors.response.use(response => response, async error => {
        const request = error.config;
        if (error.response?.status !== 401 || !request || request.skipAuthRefresh || request.url?.startsWith('/auth/')) throw error;
        if (request._retry) { onAuthExpired(); throw error; }
        request._retry = true;
        if (!refreshPromise) {
            // Keep one refresh in flight; each waiting request may retry only once.
            refreshPromise = client.post('/auth/refresh', {}, { skipAuthRefresh: true })
                .catch(refreshError => { onAuthExpired(); throw refreshError; })
                .finally(() => { refreshPromise = null; });
        }
        await refreshPromise;
        return client(request);
    });
    return client;
}
