package com.mediflow.emr.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

/** 식약처 의약품 제품 허가정보 API(08) 응답 */
@Data
public class DrugPermitApiResponse {
    private Header header;
    private Body body;

    @Data
    public static class Header {
        private String resultCode;
        private String resultMsg;
    }

    @Data
    public static class Body {
        private Integer totalCount;
        private List<Item> items;
    }

    @Data
    public static class Item {
        @JsonProperty("ITEM_SEQ")
        private String itemSeq;
        @JsonProperty("ITEM_NAME")
        private String itemName;
        @JsonProperty("ENTP_NAME")
        private String entpName;
        @JsonProperty("SPCLTY_PBLC")
        private String specialtyPublic;
        @JsonProperty("ITEM_INGR_NAME")
        private String ingredientName;
        @JsonProperty("CANCEL_NAME")
        private String cancelName;
        @JsonProperty("EDI_CODE")
        private String ediCode;
        @JsonProperty("EE_DOC_DATA")
        private String efficacyDocument;
        @JsonProperty("UD_DOC_DATA")
        private String dosageDocument;
        @JsonProperty("NB_DOC_DATA")
        private String cautionDocument;
        @JsonProperty("STORAGE_METHOD")
        private String storageMethod;
    }
}
