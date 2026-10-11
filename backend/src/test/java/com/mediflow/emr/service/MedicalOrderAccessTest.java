package com.mediflow.emr.service;

import com.mediflow.emr.entity.MedicalOrder;
import com.mediflow.emr.entity.Patient;
import com.mediflow.emr.exception.BusinessException;
import com.mediflow.emr.exception.ErrorCode;
import com.mediflow.emr.repository.*;
import org.junit.jupiter.api.Test;
import java.util.Optional;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class MedicalOrderAccessTest {
    private final MedicalOrderRepository orders = mock(MedicalOrderRepository.class);
    private final PatientAccessService access = mock(PatientAccessService.class);
    private final DrugApiService drugs = mock(DrugApiService.class);
    private final MedicalOrderService service = new MedicalOrderService(orders, mock(PatientRepository.class), drugs,
            mock(DemoDrugCatalog.class), access, mock(UserRepository.class));
    private void denyAccess() {
        var order = MedicalOrder.builder().id(1L).patient(Patient.builder().id(100L).build()).orderType("MEDICATION").orderName("가상약").build();
        when(orders.findById(1L)).thenReturn(Optional.of(order));
        when(access.requireAccessiblePatient(10L,100L)).thenThrow(new BusinessException(ErrorCode.FORBIDDEN));
    }
    @Test void inaccessibleOrderNeverFetchesDrugInformation() {
        denyAccess();
        assertThatThrownBy(() -> service.getOrderDrugDetail(1L,10L)).isInstanceOf(BusinessException.class);
        verifyNoInteractions(drugs);
    }
    @Test void inaccessibleOrderCannotBeDeleted() {
        denyAccess();
        assertThatThrownBy(() -> service.deleteOrder(1L,10L)).isInstanceOf(BusinessException.class);
        verify(orders,never()).delete(any());
    }
}
