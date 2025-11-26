import axios from 'axios';

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:9005';

export const HandoverService = {
  generateAiSummary: async ({ departmentId, fromShiftId }) => {
    const response = await axios.post(
      `${API_BASE_URL}/api/handovers/ai-summary`,
      null,
      {
        params: { departmentId, fromShiftId },
        withCredentials: true
      }
    );
    return { success: true, data: response.data.data };
  },

  saveHandover: async ({ departmentId, fromShiftId, toShiftId, aiSummary }) => {
    const response = await axios.post(
      `${API_BASE_URL}/api/handovers`,
      aiSummary,
      {
        params: { departmentId, fromShiftId, toShiftId },
        headers: { 'Content-Type': 'text/plain' },
        withCredentials: true
      }
    );
    return { success: true, message: response.data.message };
  },

  getHandoversByDepartment: async (departmentId) => {
    const response = await axios.get(
      `${API_BASE_URL}/api/handovers/department/${departmentId}`,
      { withCredentials: true }
    );
    return { success: true, data: response.data.data };
  },

  deleteHandover: async (handoverId) => {
    const response = await axios.delete(
      `${API_BASE_URL}/api/handovers/${handoverId}`,
      { withCredentials: true }
    );
    return { success: true, message: response.data.message };
  }
};
