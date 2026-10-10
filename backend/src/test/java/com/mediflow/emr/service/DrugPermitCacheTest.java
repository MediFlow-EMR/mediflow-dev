package com.mediflow.emr.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import software.amazon.awssdk.services.s3.S3Client;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class DrugPermitCacheTest {
    private final Clock clock = Clock.fixed(Instant.parse("2026-10-11T03:00:00Z"), ZoneOffset.UTC);
    @SuppressWarnings("unchecked")
    private final ObjectProvider<S3Client> clients = mock(ObjectProvider.class);
    private final DrugPermitCache cache = new DrugPermitCache(clients, new ObjectMapper(), "test", 48);

    private byte[] document(String date, String rootId, String itemId, String cancel) {
        return ("""
                {"schema_version":1,"source":"MFDS_DRUG_PERMIT","item_seq":"%s",
                 "fetched_at":"%s","item":{"ITEM_SEQ":"%s","ITEM_NAME":"테스트정",
                 "CANCEL_NAME":"%s","EXTRA_UPSTREAM_FIELD":"ignored"}}
                """).formatted(rootId, date, itemId, cancel).getBytes(StandardCharsets.UTF_8);
    }

    @Test void acceptsFreshExactProductWithUnknownFields() {
        var result = cache.decode(document("2026-10-11T02:00:00Z", "202602327", "202602327", "정상"), "202602327", clock);
        assertTrue(result.isPresent());
        assertEquals("테스트정", result.get().getItemName());
    }

    @Test void rejectsExpiredAndFutureData() {
        assertTrue(cache.decode(document("2026-10-08T02:00:00Z", "202602327", "202602327", "정상"), "202602327", clock).isEmpty());
        assertTrue(cache.decode(document("2026-10-12T02:00:00Z", "202602327", "202602327", "정상"), "202602327", clock).isEmpty());
    }

    @Test void rejectsWrongEnvelopeOrProductIdentifier() {
        assertTrue(cache.decode(document("2026-10-11T02:00:00Z", "202602328", "202602327", "정상"), "202602327", clock).isEmpty());
        assertTrue(cache.decode(document("2026-10-11T02:00:00Z", "202602327", "202602328", "정상"), "202602327", clock).isEmpty());
    }

    @Test void rejectsInactiveAndInvalidJson() {
        assertTrue(cache.decode(document("2026-10-11T02:00:00Z", "202602327", "202602327", "취하"), "202602327", clock).isEmpty());
        assertTrue(cache.decode("broken".getBytes(StandardCharsets.UTF_8), "202602327", clock).isEmpty());
    }

    @Test void disabledClientAndInvalidIdentifierAreCacheMisses() {
        assertTrue(cache.find("202602327").isEmpty());
        clearInvocations(clients);
        assertTrue(cache.find("../secret").isEmpty());
        verifyNoInteractions(clients);
    }

    @Test void s3FailureIsACacheMiss() {
        S3Client client = mock(S3Client.class);
        when(clients.getIfAvailable()).thenReturn(client);
        when(client.getObject(any(software.amazon.awssdk.services.s3.model.GetObjectRequest.class)))
                .thenThrow(new IllegalStateException("access denied"));
        assertTrue(cache.find("202602327").isEmpty());
    }
}
