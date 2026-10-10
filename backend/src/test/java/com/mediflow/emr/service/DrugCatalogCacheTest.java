package com.mediflow.emr.service;

import com.mediflow.emr.dto.DrugPermitApiResponse;
import org.junit.jupiter.api.Test;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class DrugCatalogCacheTest {
    @Test void exactIdentifierReturnsCacheWithoutExternalApi() {
        var cache = mock(DrugPermitCache.class);
        var item = new DrugPermitApiResponse.Item();
        item.setItemSeq("202602327");
        item.setItemName("브레즈트리에어로스피어흡입제160/14.4/5.0마이크로그램");
        item.setEfficacyDocument("<ARTICLE title=\"효능\">허가 원문</ARTICLE>");
        when(cache.find("202602327")).thenReturn(Optional.of(item));
        // Unreachable endpoints: returning a cache hit must not require an API call.
        var service = new DrugApiService("http://127.0.0.1:1", "http://127.0.0.1:1", cache);
        var detail = service.getDrugDetailByItemSeq("202602327");
        assertTrue(detail.isAvailable());
        assertEquals("FOUND_PERMIT_CACHE", detail.getStatus());
        assertEquals(item.getItemName(), detail.getItemName());
        assertEquals("202602327", detail.getItemSeq());
        assertTrue(detail.getEfcyQesitm().contains("허가 원문"));
    }

    @Test void invalidIdentifierNeverReadsCache() {
        var cache = mock(DrugPermitCache.class);
        var service = new DrugApiService("http://127.0.0.1:1", "http://127.0.0.1:1", cache);
        assertFalse(service.getDrugDetailByItemSeq("../secret").isAvailable());
        verifyNoInteractions(cache);
    }
}
