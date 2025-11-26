import { create } from 'zustand';
import apiClient from '../services/apiClient';

/**
 * 대시보드 전역 상태 관리
 */
const useDashboardStore = create((set) => ({
  // 상태
  myPatients: [],
  allPatients: [],
  selectedPatient: null,
  loading: false,
  error: null,
  activeTab: 'my', // 현재 활성화된 탭 ('my', 'all', 'handover')

  // 내 담당 환자 목록 조회
  fetchMyPatients: async () => {
    set({ loading: true, error: null });
    try {
      const response = await apiClient.get('/dashboard/my-patients');
      set({ 
        myPatients: response.data.data,
        loading: false 
      });
    } catch (error) {
      console.error('담당 환자 목록 조회 실패:', error);
      set({ 
        error: error.response?.data?.message || '환자 목록을 불러올 수 없습니다.',
        loading: false 
      });
    }
  },

  // 전체 환자 목록 조회
  fetchAllPatients: async () => {
    set({ loading: true, error: null });
    try {
      const response = await apiClient.get('/dashboard/all-patients');
      set({
        allPatients: response.data.data,
        loading: false
      });
    } catch (error) {
      console.error('전체 환자 목록 조회 실패:', error);
      set({
        error: error.response?.data?.message || '환자 목록을 불러올 수 없습니다.',
        loading: false
      });
    }
  },


  // 환자 선택
  selectPatient: (patient) => {
    set({ selectedPatient: patient });
  },

  // 탭 변경
  setActiveTab: (tab) => {
    set({ activeTab: tab });
  },

  // 상태 초기화
  reset: () => {
    set({
      myPatients: [],
      allPatients: [],
      selectedPatient: null,
      loading: false,
      error: null,
      activeTab: 'my',
    });
  },
}));

export default useDashboardStore;
