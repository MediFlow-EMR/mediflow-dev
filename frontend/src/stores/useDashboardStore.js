import { create } from 'zustand';
import apiClient from '../services/apiClient';
import { currentRecordDateTime } from '../utils/dateFormat';

let listController;
let summaryController;
const initial = { myPatients: [], allPatients: [], selectedPatient: null, departmentSummary: null,
    loading: false, error: null, summaryError: null, activeTab: 'my', selectedDate: null, selectedShiftType: '' };
const currentContext = () => ({ selectedDate: currentRecordDateTime().slice(0, 10), selectedShiftType: '' });

const useDashboardStore = create((set, get) => {
    const fetchPatients = async (endpoint, key) => {
        listController?.abort();
        const controller = new AbortController();
        listController = controller;
        set({ loading: true, error: null });
        try {
            const response = await apiClient.get(endpoint, { signal: controller.signal, params: key === 'myPatients' ? contextParams() : undefined });
            if (!controller.signal.aborted) set({ [key]: response.data.data || [], loading: false });
        } catch (error) {
            if (!controller.signal.aborted) set({ error: error.response?.data?.message || '환자 목록을 불러올 수 없습니다.', loading: false });
        }
    };
    const contextParams = () => ({ date: get().selectedDate, shiftType: get().selectedShiftType || undefined });
    return {
        ...initial, ...currentContext(),
        fetchDepartmentSummary: async () => {
            set({ summaryError: null });
            summaryController?.abort();
            const controller = new AbortController();
            summaryController = controller;
            try {
                const response = await apiClient.get('/dashboard/department-summary', { signal: controller.signal, params: contextParams() });
                if (!controller.signal.aborted) set({ departmentSummary: response.data.data, summaryError: null });
            } catch {
                if (!controller.signal.aborted) set({ summaryError: '부서 정보를 불러올 수 없습니다. 다시 시도해주세요.' });
            }
        },
        setContext: (selectedDate, selectedShiftType = get().selectedShiftType) => {
            listController?.abort(); summaryController?.abort();
            set({ selectedDate, selectedShiftType, departmentSummary: null, myPatients: [], allPatients: [], selectedPatient: null, loading: false, error: null, summaryError: null });
        },
        fetchMyPatients: () => fetchPatients('/dashboard/my-patients', 'myPatients'),
        fetchAllPatients: () => fetchPatients('/dashboard/all-patients', 'allPatients'),
        selectPatient: selectedPatient => set({ selectedPatient }),
        setActiveTab: activeTab => set({ activeTab }),
        reset: () => { listController?.abort(); summaryController?.abort(); set({ ...initial, ...currentContext() }); },
    };
});
export default useDashboardStore;
