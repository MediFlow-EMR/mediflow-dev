package com.mediflow.emr.service;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mediflow.emr.dto.DrugPermitApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

@Slf4j
@Service
public class DrugPermitCache {
    private static final int MAX_BYTES = 2_000_000;
    private final ObjectProvider<S3Client> clients;
    private final ObjectMapper mapper;
    private final String bucket;
    private final Duration maxAge;

    public DrugPermitCache(ObjectProvider<S3Client> clients, ObjectMapper mapper,
                           @Value("${drug-cache.bucket:}") String bucket,
                           @Value("${drug-cache.max-age-hours:48}") long maxAgeHours) {
        this.clients = clients;
        this.mapper = mapper;
        this.bucket = bucket;
        if (maxAgeHours <= 0) throw new IllegalArgumentException("Cache max age must be positive");
        this.maxAge = Duration.ofHours(maxAgeHours);
    }

    public Optional<DrugPermitApiResponse.Item> find(String itemSeq) {
        if (itemSeq == null || !itemSeq.matches("[0-9]{9}") || bucket.isBlank()) {
            return Optional.empty();
        }
        var client = clients.getIfAvailable();
        if (client == null) return Optional.empty();
        try (var stream = client.getObject(GetObjectRequest.builder()
                .bucket(bucket).key("drug-cache/" + itemSeq + ".json").build())) {
            byte[] bytes = stream.readNBytes(MAX_BYTES + 1);
            if (bytes.length > MAX_BYTES) return Optional.empty();
            var result = decode(bytes, itemSeq, Clock.systemUTC());
            if (result.isPresent()) log.info("Drug permit cache hit - itemSeq: {}", itemSeq);
            return result;
        } catch (Exception error) {
            // Missing/denied/corrupt cache is never a reason to fail the live API lookup.
            log.debug("Drug permit cache miss - itemSeq: {}, type: {}",
                    itemSeq, error.getClass().getSimpleName());
            return Optional.empty();
        }
    }

    Optional<DrugPermitApiResponse.Item> decode(byte[] bytes, String itemSeq, Clock clock) {
        try {
            var root = mapper.readTree(bytes);
            if (root.path("schema_version").asInt() != 1
                    || !"MFDS_DRUG_PERMIT".equals(root.path("source").asText())
                    || !itemSeq.equals(root.path("item_seq").asText())) return Optional.empty();
            Instant fetchedAt = Instant.parse(root.path("fetched_at").asText());
            Instant now = clock.instant();
            if (fetchedAt.isAfter(now) || fetchedAt.isBefore(now.minus(maxAge))) return Optional.empty();
            var item = mapper.readerFor(DrugPermitApiResponse.Item.class)
                    .without(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                    .readValue(root.path("item"));
            DrugPermitApiResponse.Item product = (DrugPermitApiResponse.Item) item;
            if (!itemSeq.equals(product.getItemSeq())
                    || product.getItemName() == null || product.getItemName().isBlank()
                    || (product.getCancelName() != null && !product.getCancelName().isBlank()
                    && !"정상".equals(product.getCancelName()))) return Optional.empty();
            return Optional.of(product);
        } catch (Exception error) {
            return Optional.empty();
        }
    }
}
