import apiClient from "./apiClient.js";

export const HandoverService = {
  generateAiSummary: async ({ departmentId, fromShiftId, toShiftId }) => {
    const response = await apiClient.post(
        `/handovers/ai-summary`,
      null,
      {
        params: { departmentId, fromShiftId, toShiftId },
        withCredentials: true
      }
    );
    return { success: true, data: response.data.data };
  },

  saveHandover: async ({ departmentId, fromShiftId, toShiftId, aiSummary }) => {
    const response = await apiClient.post(
      `/handovers`,
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
    const response = await apiClient.get(
      `/handovers/department/${departmentId}`,
      { withCredentials: true }
    );
    return { success: true, data: response.data.data };
  },

  deleteHandover: async (handoverId) => {
    const response = await apiClient.delete(
      `/handovers/${handoverId}`,
      { withCredentials: true }
    );
    return { success: true, message: response.data.message };
  }
};
