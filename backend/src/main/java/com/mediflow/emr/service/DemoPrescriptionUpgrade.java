package com.mediflow.emr.service;

import com.mediflow.emr.entity.MedicalOrder;
import com.mediflow.emr.entity.Medication;
import com.mediflow.emr.entity.enums.MedicationRoute;
import java.util.Set;

/** Versioned synthetic seed changes only; never a clinical substitution rule. */
public final class DemoPrescriptionUpgrade {
    private DemoPrescriptionUpgrade() {}
    private record Replacement(String code, String name, String dose, String frequency,
                               String instructions, String oldDose, String oldFrequency) {}
    private static Replacement replacement(String code) {
        if ("CARD-NTG".equals(code)) return new Replacement("CARD-CLOP",
                "니트로글리세린설하정(Nitroglycerin)", "75mg", "1일 1회", "데모 처방 · 출혈 징후 확인", "0.4mg", "필요시");
        if (Set.of("GI-SUC", "GI-SUCR").contains(code == null ? "" : code)) return new Replacement("GI-ALM",
                "수크랄페이트정(Sucralfate)", "1g", "1일 3회", "데모 처방 · 식후 30분~1시간에 씹어서 복용", "1g", "1일 3회");
        return null;
    }
    private static boolean matches(Replacement r, String name, String dose, String route, String frequency) {
        return r != null && r.name.equals(name) && r.oldDose.equals(dose)
                && "PO".equals(route) && r.oldFrequency.equals(frequency);
    }
    public static boolean upgrade(MedicalOrder order, DemoDrugCatalog catalog) {
        var r = replacement(order.getOrderCode());
        if (!"MEDICATION".equals(order.getOrderType()) || !matches(r, order.getOrderName(), order.getDose(),
                order.getRoute(), order.getFrequency()) || catalog.find(r.code).isEmpty()) return false;
        String oldInstructions = "CARD-NTG".equals(order.getOrderCode())
                ? "흉통 시 혈압 확인 후 투여" : "식전 투여, 타 약물과 간격 확인";
        if (!oldInstructions.equals(order.getInstructions())) return false;
        order.setOrderCode(r.code);
        order.update(catalog.find(r.code).orElseThrow().displayName(), r.dose, "PO", r.frequency, r.instructions);
        return true;
    }
    public static boolean upgrade(Medication medication, DemoDrugCatalog catalog) {
        // Actual performed records linked to an order must retain what was administered.
        if (medication.getMedicalOrder() != null || medication.getRoute() == null) return false;
        var r = replacement(medication.getDrugCode());
        if (!matches(r, medication.getDrugName(), medication.getDose(), medication.getRoute().name(),
                medication.getFrequency()) || catalog.find(r.code).isEmpty()) return false;
        medication.update(catalog.find(r.code).orElseThrow().displayName(), r.code, r.dose,
                MedicationRoute.PO, r.frequency, medication.getAdministeredAt(), medication.getOrderDoctor());
        return true;
    }
}
