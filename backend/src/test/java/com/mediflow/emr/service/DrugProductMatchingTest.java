package com.mediflow.emr.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DrugProductMatchingTest {
    @Test
    void rejectsCombinationProductFoundThroughIngredientSearch() {
        assertFalse(DrugApiService.matchesRequestedProduct(
                "브레즈트리에어로스피어흡입제160/14.4/5.0마이크로그램(부데소니드,글리코피로니움,포르모테롤)",
                "부데소니드흡입액"));
    }

    @Test
    void acceptsProductNameWithStrengthAndIngredientDescription() {
        assertTrue(DrugApiService.matchesRequestedProduct("타이레놀정500밀리그람(아세트아미노펜)", "타이레놀정"));
        assertTrue(DrugApiService.matchesRequestedProduct("타이레놀정500밀리그람(아세트아미노펜)", "타이레놀정500밀리그람"));
    }

    @Test
    void rejectsDifferentFormulationAndStrength() {
        assertFalse(DrugApiService.matchesRequestedProduct("타이레놀정500밀리그람", "타이레놀정650밀리그람"));
        assertFalse(DrugApiService.matchesRequestedProduct("타이레놀정500밀리그람", "타이레놀정50"));
        assertFalse(DrugApiService.matchesRequestedProduct("부데소니드흡입제", "부데소니드흡입액"));
        assertFalse(DrugApiService.matchesRequestedProduct("타이레놀정", "타이레놀"));
    }

    @Test
    void rejectsExportProductsAndMissingNames() {
        assertFalse(DrugApiService.matchesRequestedProduct("타이레놀정500밀리그람(수출용)", "타이레놀정"));
        assertFalse(DrugApiService.matchesRequestedProduct(null, "타이레놀정"));
        assertFalse(DrugApiService.matchesRequestedProduct("타이레놀정", ""));
    }

    @Test
    void ignoresFormattingWhitespace() {
        assertTrue(DrugApiService.matchesRequestedProduct("타이레놀정 500밀리그람", "타이레놀정 500밀리그람"));
    }
}
