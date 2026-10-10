package com.mediflow.emr.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mediflow.emr.dto.DrugDetailInfo;
import com.mediflow.emr.entity.MedicalOrder;
import com.mediflow.emr.repository.*;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.mock.env.MockEnvironment;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class DemoDrugCatalogTest {
    private DemoDrugCatalog catalog(boolean demo) throws Exception {
        var env = new MockEnvironment();
        env.setActiveProfiles(demo ? "demo" : "prod");
        return new DemoDrugCatalog(new ObjectMapper(), env);
    }
    private MedicalOrderService service(MedicalOrderRepository repository, DrugApiService api, DemoDrugCatalog catalog) {
        return new MedicalOrderService(repository, mock(PatientRepository.class), api, catalog,
                mock(PatientAccessService.class), mock(UserRepository.class));
    }
    @Test void allFifteenVerifiedProductsUseExactIdentifierAndPreserveDose() throws Exception {
        var catalog = catalog(true);
        var repo = mock(MedicalOrderRepository.class);
        var api = mock(DrugApiService.class);
        var service = service(repo, api, catalog);
        DemoDrugCatalog.Product[] products;
        try (var input = new ClassPathResource("demo-drug-catalog.json").getInputStream()) {
            products = new ObjectMapper().readValue(input, DemoDrugCatalog.Product[].class);
        }
        int count = 0;
        for (var product : products) {
            if (!product.verified()) continue;
            count++;
            assertTrue(product.itemSeq().matches("[0-9]{9}"));
            for (String code : product.codes()) {
                assertEquals(product.itemSeq(), catalog.find(code).orElseThrow().itemSeq());
                var order = MedicalOrder.builder().orderType("MEDICATION").orderCode(code)
                        .orderName(product.genericName() + "(" + product.english() + ")")
                        .route(product.route()).dose("unchanged-demo-dose").build();
                when(repo.findById(1L)).thenReturn(Optional.of(order));
                when(api.getDrugDetailByItemSeq(product.itemSeq())).thenReturn(DrugDetailInfo.builder()
                        .available(true).itemSeq(product.itemSeq()).itemName(product.productName()).status("FOUND_PERMIT").build());
                var result = service.getOrderDrugDetail(1L);
                assertTrue(result.isAvailable(), code);
                assertEquals(product.strength(), result.getProductStrength());
                assertEquals("unchanged-demo-dose", order.getDose());
                assertEquals("식품의약품안전처 의약품 제품 허가정보", result.getSource());
            }
        }
        assertEquals(15, count);
        verify(api, never()).getDrugDetail(anyString());
    }
    @Test void unresolvedProductsNeverUseFuzzySearch() throws Exception {
        var catalog = catalog(true);
        var repo = mock(MedicalOrderRepository.class);
        var api = mock(DrugApiService.class);
        for (String code : new String[]{"CARD-NTG", "GI-SUCR"}) {
            var product = catalog.find(code).orElseThrow();
            when(repo.findById(1L)).thenReturn(Optional.of(MedicalOrder.builder()
                    .orderType("MEDICATION").orderCode(code).orderName(product.displayName()).route("PO").build()));
            assertEquals("DEMO_MAPPING_REQUIRED", service(repo, api, catalog).getOrderDrugDetail(1L).getStatus());
        }
        verifyNoInteractions(api);
    }
    @Test void nonDemoProfileDoesNotSubstituteAndChangedNameIsNotAccepted() throws Exception {
        assertTrue(catalog(false).find("ENDO-GLA").isEmpty());
        assertFalse(catalog(true).find("ENDO-GLA").orElseThrow().accepts("다른 약"));
    }
    @Test void wrongRouteNeverConnectsToProduct() throws Exception {
        var catalog = catalog(true);
        var repo = mock(MedicalOrderRepository.class);
        var api = mock(DrugApiService.class);
        when(repo.findById(1L)).thenReturn(Optional.of(MedicalOrder.builder().orderType("MEDICATION")
                .orderCode("ENDO-GLA").orderName(catalog.find("ENDO-GLA").orElseThrow().displayName()).route("IV").build()));
        assertFalse(service(repo, api, catalog).getOrderDrugDetail(1L).isAvailable());
        verifyNoInteractions(api);
    }
}
