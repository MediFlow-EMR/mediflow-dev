import apiClient from "./apiClient.js";

export const HandoverService = {
  generateAiSummary: async ({ departmentId, fromShiftId, toShiftId }) => {
    const response = await apiClient.post(
        `/handovers/ai-summary`,
      null,
      {
        params: { departmentId, fromShiftId, toShiftId },
        timeout: 120000,
        withCredentials: true
      }
    );
    return { success: true, data: response.data.data };
  },

  regeneratePatientAiSummary: async ({ patientId, fromShiftId }) => {
    const response = await apiClient.post(
      `/handovers/ai-summary/patient/${patientId}`,
      null,
      {
        params: { fromShiftId },
        timeout: 120000,
        withCredentials: true
      }
    );
    return { success: true, data: response.data.data };
  },

  saveHandover: async (payload, id = null) => {
    const response = id
      ? await apiClient.put(`/handovers/${id}`, payload)
      : await apiClient.post('/handovers', payload);
    return { success: true, data: response.data.data };
  },

  getRevisions: async (id) => {
    const response = await apiClient.get(`/handovers/${id}/revisions`);
    return { success: true, data: response.data.data };
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
