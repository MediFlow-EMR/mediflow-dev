package com.mediflow.emr.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import java.io.IOException;
import java.util.*;

/** Explicit demo formulary, not a generic-name substitution engine for real orders. */
@Service
public class DemoDrugCatalog {
    public record Product(List<String> codes, String genericName, String english,
                          String itemSeq, String productName, String strength,
                          String route, String unresolvedReason) {
        public boolean verified() { return itemSeq != null && productName != null; }
        public String displayName() {
            return verified() ? productName.split("\\(", 2)[0] + "(" + english + ")" : genericName + "(" + english + ")";
        }
        public boolean accepts(String name) {
            return name != null && (name.equals(displayName()) || name.equals(productName)
                    || name.equals(genericName + "(" + english + ")"));
        }
    }
    private final Map<String, Product> products = new HashMap<>();
    private final boolean enabled;
    public DemoDrugCatalog(ObjectMapper mapper, Environment environment) throws IOException {
        enabled = environment.acceptsProfiles(Profiles.of("demo", "dev"));
        try (var input = new ClassPathResource("demo-drug-catalog.json").getInputStream()) {
            for (var product : mapper.readValue(input, Product[].class)) {
                for (String code : product.codes()) {
                    if (products.putIfAbsent(code, product) != null) throw new IllegalStateException("Duplicate demo drug code");
                }
            }
        }
    }
    public Optional<Product> find(String code) {
        return enabled ? Optional.ofNullable(products.get(code)) : Optional.empty();
    }
}
