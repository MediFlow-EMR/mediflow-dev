import { useCallback, useEffect, useRef, useState } from 'react';
import apiClient from '../services/apiClient';

export default function useRecordList(endpoint, errorMessage) {
    const [items, setItems] = useState([]);
    const [loading, setLoading] = useState(true);
    const [loadError, setLoadError] = useState(null);
    const request = useRef(null);
    const refresh = useCallback(async () => {
        request.current?.abort();
        const controller = new AbortController();
        request.current = controller;
        setLoading(true); setLoadError(null);
        try {
            const response = await apiClient.get(endpoint, { signal: controller.signal });
            if (!controller.signal.aborted) setItems(response.data.data || []);
        } catch (error) {
            if (!controller.signal.aborted) setLoadError(error.response?.data?.message || errorMessage);
        } finally {
            if (!controller.signal.aborted) setLoading(false);
        }
    }, [endpoint, errorMessage]);
    useEffect(() => {
        setItems([]);
        refresh();
        return () => request.current?.abort();
    }, [refresh]);
    return { items, setItems, loading, loadError, refresh };
}
