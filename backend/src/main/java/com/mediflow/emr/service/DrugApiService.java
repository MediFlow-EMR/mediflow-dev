package com.mediflow.emr.service;

import com.mediflow.emr.dto.DrugApiResponse;
import com.mediflow.emr.dto.DrugSearchResult;
import com.mediflow.emr.dto.DrugDetailInfo;
import com.mediflow.emr.dto.DrugPermitApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 식약처 의약품 API 서비스
 */
@Slf4j
@Service
public class DrugApiService {

    @Value("${drug-api.base-url}")
    private String baseUrl;

    @Value("${drug-api.service-key}")
    private String serviceKey;

    private final RestClient restClient;
    private final RestClient permitRestClient;
    private final DrugPermitCache permitCache;

    public DrugApiService(
            @Value("${drug-api.base-url}") String baseUrl,
            @Value("${drug-permit-api.base-url:https://apis.data.go.kr/1471000/DrugPrdtPrmsnInfoService08}") String permitBaseUrl,
            DrugPermitCache permitCache) {
        this.permitCache = permitCache;
        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .build();
        this.permitRestClient = RestClient.builder()
                .baseUrl(permitBaseUrl)
                .build();
    }

    /**
     * 약품명으로 검색
     */
    public List<DrugSearchResult> searchDrugs(String keyword) {
        log.info("식약처 API 약품 검색 시작 - keyword: {}", keyword);

        try {
            // API 호출
            DrugApiResponse response = restClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/getDrbEasyDrugList")
                            .queryParam("serviceKey", serviceKey)
                            .queryParam("pageNo", 1)
                            .queryParam("numOfRows", 20)
                            .queryParam("type", "json")
                            .queryParam("itemName", keyword)
                            .build())
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .onStatus(status -> !status.is2xxSuccessful(), (request, responseStatus) -> {
                        log.error("식약처 API HTTP 오류 - status: {}", responseStatus.getStatusCode());
                    })
                    .body(DrugApiResponse.class);

            log.debug("API 응답: {}", response);

            // 응답 헤더 확인
            if (response != null && response.getHeader() != null) {
                String resultCode = response.getHeader().getResultCode();
                String resultMsg = response.getHeader().getResultMsg();
                log.debug("API 응답 코드: {}, 메시지: {}", resultCode, resultMsg);

                if (!"00".equals(resultCode)) {
                    log.error("식약처 API 오류 - resultCode: {}, resultMsg: {}", resultCode, resultMsg);
                    return new ArrayList<>();
                }
            }

            if (response == null || response.getBody() == null || response.getBody().getItems() == null) {
                log.warn("식약처 API 응답이 비어있습니다. response: {}", response);
                return new ArrayList<>();
            }

            // 결과 변환
            List<DrugSearchResult> results = response.getBody().getItems().stream()
                    .map(item -> DrugSearchResult.builder()
                            .itemSeq(item.getItemSeq())
                            .itemName(item.getItemName())
                            .entpName(item.getEntpName())
                            .build())
                    .collect(Collectors.toList());

            log.info("식약처 API 검색 성공: {} 건", results.size());
            return results;

        } catch (Exception e) {
            log.error("식약처 API 호출 실패 - keyword: {}, error: {}", keyword, e.getMessage(), e);
            // API 실패 시 빈 리스트 반환 (서비스 중단 방지)
            return new ArrayList<>();
        }
    }

    /**
     * 약품 상세 정보 조회 (약품명으로)
     */
    public com.mediflow.emr.dto.DrugDetailInfo getDrugDetail(String drugName) {
        log.info("식약처 API 약품 상세 조회 시작 - drugName: {}", drugName);
        String apiSearchName = normalizeDisplayDrugName(drugName);

        try {
            // 약품명으로 검색
            DrugApiResponse response = restClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/getDrbEasyDrugList")
                            .queryParam("serviceKey", serviceKey)
                            .queryParam("pageNo", 1)
                            .queryParam("numOfRows", 20)
                            .queryParam("type", "json")
                            .queryParam("itemName", apiSearchName)
                            .build())
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .onStatus(status -> !status.is2xxSuccessful(), (request, responseStatus) -> {
                        log.error("식약처 API HTTP 오류 - status: {}", responseStatus.getStatusCode());
                    })
                    .body(DrugApiResponse.class);

            log.debug("약품 상세 정보 API 응답: {}", response);

            // 응답 헤더 확인
            if (response != null && response.getHeader() != null) {
                String resultCode = response.getHeader().getResultCode();
                String resultMsg = response.getHeader().getResultMsg();
                log.debug("API 응답 코드: {}, 메시지: {}", resultCode, resultMsg);

                if (!"00".equals(resultCode)) {
                    log.error("식약처 API 오류 - resultCode: {}, resultMsg: {}", resultCode, resultMsg);
                    return getPermitDrugDetail(drugName, apiSearchName);
                }
            }

            if (response == null || response.getBody() == null || response.getBody().getItems() == null || response.getBody().getItems().isEmpty()) {
                log.warn("약품 상세 정보를 찾을 수 없습니다: {}", drugName);
                return getPermitDrugDetail(drugName, apiSearchName);
            }

            List<DrugApiResponse.DrugItem> matchingItems = response.getBody().getItems().stream()
                    .filter(candidate -> matchesRequestedProduct(candidate.getItemName(), apiSearchName))
                    .toList();
            if (matchingItems.isEmpty()) {
                return getPermitDrugDetail(drugName, apiSearchName);
            }
            if (matchingItems.size() > 1) {
                return ambiguousDetail(drugName);
            }
            DrugApiResponse.DrugItem item = matchingItems.getFirst();
            log.info("약품 상세 정보 조회 성공 - drugName: {}, itemName: {}", drugName, item.getItemName());

            return com.mediflow.emr.dto.DrugDetailInfo.builder()
                    .available(true)
                    .source("식품의약품안전처 의약품개요정보(e약은요)")
                    .status("FOUND")
                    .message("식약처 공공데이터 조회 결과")
                    .requestedName(drugName)
                    .itemSeq(item.getItemSeq())
                    .itemName(item.getItemName())
                    .entpName(item.getEntpName())
                    .efcyQesitm(item.getEfcyQesitm())
                    .useMethodQesitm(item.getUseMethodQesitm())
                    .atpnWarnQesitm(item.getAtpnWarnQesitm())
                    .atpnQesitm(item.getAtpnQesitm())
                    .intrcQesitm(item.getIntrcQesitm())
                    .seQesitm(item.getSeQesitm())
                    .depositMethodQesitm(item.getDepositMethodQesitm())
                    .build();

        } catch (Exception e) {
            log.error("약품 상세 정보 조회 실패 - drugName: {}, error: {}", drugName, e.getMessage(), e);
            return getPermitDrugDetail(drugName, apiSearchName);
        }
    }

    /**
     * e약은요에 없는 전문의약품은 전체 허가품목 서비스에서 목록을 찾고,
     * 품목기준코드로 상세정보를 다시 조회한다.
     */
    private DrugDetailInfo getPermitDrugDetail(String drugName, String apiSearchName) {
        try {
            String searchName = normalizePermitSearchName(apiSearchName);
            DrugPermitApiResponse listResponse = permitRestClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/getDrugPrdtPrmsnInq08")
                            .queryParam("serviceKey", serviceKey)
                            .queryParam("pageNo", 1)
                            .queryParam("numOfRows", 20)
                            .queryParam("type", "json")
                            .queryParam("item_name", searchName)
                            .build())
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .body(DrugPermitApiResponse.class);

            if (!isSuccessful(listResponse) || listResponse.getBody() == null
                    || listResponse.getBody().getItems() == null
                    || listResponse.getBody().getItems().isEmpty()) {
                return unavailableDetail(drugName, "NOT_FOUND",
                        "e약은요와 의약품 제품 허가정보에서 일치하는 품목을 찾지 못했습니다.");
            }

            List<DrugPermitApiResponse.Item> matchingItems = listResponse.getBody().getItems().stream()
                    .filter(item -> item.getCancelName() == null || "정상".equals(item.getCancelName()))
                    .filter(item -> matchesRequestedProduct(item.getItemName(), apiSearchName))
                    .toList();
            if (matchingItems.isEmpty()) {
                return unavailableDetail(drugName, "NOT_FOUND",
                        "처방명과 일치하는 허가품목을 확인하지 못했습니다. 정확한 제품명과 함량으로 조회해 주세요.");
            }
            if (matchingItems.size() > 1) {
                return ambiguousDetail(drugName);
            }
            DrugPermitApiResponse.Item selected = matchingItems.getFirst();
            var cached = permitCache.find(selected.getItemSeq())
                    .filter(item -> matchesRequestedProduct(item.getItemName(), apiSearchName));

            DrugPermitApiResponse detailResponse = cached.isPresent() ? null : permitRestClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/getDrugPrdtPrmsnDtlInq08")
                            .queryParam("serviceKey", serviceKey)
                            .queryParam("pageNo", 1)
                            .queryParam("numOfRows", 1)
                            .queryParam("type", "json")
                            .queryParam("item_seq", selected.getItemSeq())
                            .build())
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .body(DrugPermitApiResponse.class);

            DrugPermitApiResponse.Item detail = cached.orElse(selected);
            if (isSuccessful(detailResponse) && detailResponse.getBody() != null
                    && detailResponse.getBody().getItems() != null
                    && !detailResponse.getBody().getItems().isEmpty()) {
                detail = detailResponse.getBody().getItems().stream()
                        .filter(item -> selected.getItemSeq().equals(item.getItemSeq()))
                        .filter(item -> item.getCancelName() == null || "정상".equals(item.getCancelName()))
                        .filter(item -> matchesRequestedProduct(item.getItemName(), apiSearchName))
                        .findFirst().orElse(selected);
            }

            return DrugDetailInfo.builder()
                    .available(true)
                    .source("식품의약품안전처 의약품 제품 허가정보")
                    .status("FOUND_PERMIT")
                    .message(cached.isPresent() ? "식약처 허가정보 캐시 조회 결과" : "전문의약품을 포함한 허가 품목 조회 결과")
                    .requestedName(drugName)
                    .itemSeq(detail.getItemSeq())
                    .itemName(detail.getItemName())
                    .entpName(detail.getEntpName())
                    .efcyQesitm(stripDocumentMarkup(detail.getEfficacyDocument()))
                    .useMethodQesitm(stripDocumentMarkup(detail.getDosageDocument()))
                    .atpnQesitm(stripDocumentMarkup(detail.getCautionDocument()))
                    .depositMethodQesitm(detail.getStorageMethod())
                    .build();
        } catch (Exception exception) {
            log.error("의약품 제품 허가정보 조회 실패 - drugName: {}, error: {}",
                    drugName, exception.getMessage());
            return unavailableDetail(drugName, "TEMPORARY_ERROR",
                    "식약처 공공 API 연결이 원활하지 않습니다. 잠시 후 다시 시도해 주세요.");
        }
    }

    private boolean isSuccessful(DrugPermitApiResponse response) {
        return response != null && response.getHeader() != null
                && "00".equals(response.getHeader().getResultCode());
    }

    private String normalizePermitSearchName(String drugName) {
        if (drugName == null) return "";
        return drugName.replaceAll("(장용정|설하정|흡입액|캡슐|시럽|정|주)$", "").trim();
    }

    private String normalizeDisplayDrugName(String drugName) {
        if (drugName == null) return "";
        return drugName.replaceFirst("\\s*\\([^)]*\\)\\s*$", "").trim();
    }

    // 성분명·제형만 비슷한 검색 결과를 동일 제품으로 간주하지 않는다.
    // 괄호 안 성분 설명은 제외하고, 제품명 뒤의 함량 표기만 허용한다.
    static boolean matchesRequestedProduct(String itemName, String requestedName) {
        if (itemName == null || requestedName == null || requestedName.isBlank()) return false;
        if (itemName.contains("수출용") || itemName.contains("수출명")) return false;
        String product = itemName.split("[（(]", 2)[0].replaceAll("\\s+", "");
        String requested = requestedName.replaceAll("\\s+", "");
        if (product.equalsIgnoreCase(requested)) return true;
        if (product.length() <= requested.length()
                || !product.regionMatches(true, 0, requested, 0, requested.length())) return false;
        return !Character.isDigit(requested.charAt(requested.length() - 1))
                && Character.isDigit(product.charAt(requested.length()));
    }

    private DrugDetailInfo ambiguousDetail(String drugName) {
        return unavailableDetail(drugName, "AMBIGUOUS_MATCH",
                "여러 허가품목이 검색되어 제품을 확정할 수 없습니다. 정확한 제품명과 함량으로 조회해 주세요.");
    }

    private String stripDocumentMarkup(String document) {
        if (document == null || document.isBlank()) return null;
        return document
                .replace("<![CDATA[", "")
                .replace("]]>", "")
                // 허가문서의 장 제목을 보존해야 각 장에서 번호가 다시 시작되는 이유를 알 수 있다.
                .replaceAll("(?i)<(?:SECTION|ARTICLE)[^>]*title=\"([^\"]+)\"[^>]*>", " [[SECTION:$1]] ")
                .replaceAll("<[^>]+>", " ")
                .replace("&lt;", "<")
                .replace("&gt;", ">")
                .replace("&amp;", "&")
                .replaceAll("\\s+", " ")
                .trim();
    }

    private com.mediflow.emr.dto.DrugDetailInfo unavailableDetail(
            String drugName, String status, String message) {
        return com.mediflow.emr.dto.DrugDetailInfo.builder()
                .available(false)
                .source("식품의약품안전처 의약품개요정보(e약은요)")
                .status(status)
                .message(message)
                .requestedName(drugName)
                .build();
    }
}
