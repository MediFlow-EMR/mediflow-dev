package com.mediflow.emr.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mediflow.emr.entity.MedicalOrder;
import com.mediflow.emr.entity.Medication;
import com.mediflow.emr.entity.enums.MedicationRoute;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;
import static org.junit.jupiter.api.Assertions.*;

class DemoPrescriptionUpgradeTest {
    private DemoDrugCatalog catalog(String profile) throws Exception {
        var env = new MockEnvironment();
        env.setActiveProfiles(profile);
        return new DemoDrugCatalog(new ObjectMapper(), env);
    }
    private MedicalOrder legacyOrder() {
        return MedicalOrder.builder().orderType("MEDICATION").orderCode("CARD-NTG")
                .orderName("니트로글리세린설하정(Nitroglycerin)").dose("0.4mg")
                .route("PO").frequency("필요시").instructions("흉통 시 혈압 확인 후 투여").build();
    }
    @Test void upgradeUpdatesEntireSyntheticPrescriptionAndIsIdempotent() throws Exception {
        var order = legacyOrder();
        var catalog = catalog("demo");
        assertTrue(DemoPrescriptionUpgrade.upgrade(order, catalog));
        assertEquals("CARD-CLOP", order.getOrderCode());
        assertEquals("플라빅스정75밀리그램(Clopidogrel)", order.getOrderName());
        assertEquals("75mg", order.getDose());
        assertEquals("1일 1회", order.getFrequency());
        assertFalse(DemoPrescriptionUpgrade.upgrade(order, catalog));
    }
    @Test void manualChangesAndNonDemoProfilesArePreserved() throws Exception {
        assertFalse(DemoPrescriptionUpgrade.upgrade(legacyOrder(), catalog("prod")));
        var order = legacyOrder();
        order.setDose("0.2mg");
        assertFalse(DemoPrescriptionUpgrade.upgrade(order, catalog("demo")));
        assertEquals("CARD-NTG", order.getOrderCode());
        var edited = legacyOrder();
        edited.setInstructions("직접 수정한 지시");
        assertFalse(DemoPrescriptionUpgrade.upgrade(edited, catalog("demo")));
    }
    @Test void syntheticMedicationUpgradePreservesTimeButNotLinkedPerformedRecords() throws Exception {
        var time = java.time.LocalDateTime.of(2026, 10, 10, 12, 0);
        var medication = Medication.builder().drugName("수크랄페이트정(Sucralfate)")
                .drugCode("GI-SUCR").dose("1g").route(MedicationRoute.PO).frequency("1일 3회")
                .administeredAt(time).build();
        var catalog = catalog("demo");
        assertTrue(DemoPrescriptionUpgrade.upgrade(medication, catalog));
        assertEquals("GI-ALM", medication.getDrugCode());
        assertEquals("알마겔정(Almagate)", medication.getDrugName());
        assertEquals(time, medication.getAdministeredAt());
        assertFalse(DemoPrescriptionUpgrade.upgrade(medication, catalog));
        var performed = Medication.builder().drugName("수크랄페이트정(Sucralfate)")
                .drugCode("GI-SUCR").dose("1g").route(MedicationRoute.PO).frequency("1일 3회")
                .medicalOrder(legacyOrder()).build();
        assertFalse(DemoPrescriptionUpgrade.upgrade(performed, catalog));
    }
}
