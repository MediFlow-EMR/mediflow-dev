import { useAuthStore } from '../stores/authStore';
import { createApiClient } from './createApiClient';

const apiClient = createApiClient({
    baseURL: import.meta.env.PROD ? `${import.meta.env.VITE_API_BASE_URL}/api` : '/api',
    onAuthExpired: () => useAuthStore.getState().clear(),
});
export default apiClient;
