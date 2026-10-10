package com.mediflow.emr.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mediflow.emr.config.GeminiConfig;
import com.mediflow.emr.exception.BusinessException;
import com.mediflow.emr.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.client.RestClientException;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Gemini AI API 서비스
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GeminiService {

    private final GeminiConfig geminiConfig;
    private final RestTemplate restTemplate = createRestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    private static RestTemplate createRestTemplate() {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(5_000);
        requestFactory.setReadTimeout(30_000);
        return new RestTemplate(requestFactory);
    }

    /**
     * Gemini API에 프롬프트 전송
     *
     * @param prompt 입력 프롬프트
     * @return AI 응답 텍스트
     */
    public String generateContent(String prompt) {
        try {
            if (prompt == null || prompt.isBlank()) {
                throw new IllegalArgumentException("AI 요청 내용이 비어 있습니다.");
            }

            String url = UriComponentsBuilder
                    .fromUriString(geminiConfig.getBaseUrl())
                    .pathSegment("models", geminiConfig.getModel() + ":generateContent")
                    .queryParam("key", geminiConfig.getApiKey())
                    .build()
                    .toUriString();

            // 요청 바디 생성
            Map<String, Object> requestBody = new HashMap<>();
            Map<String, Object> contents = new HashMap<>();
            Map<String, Object> parts = new HashMap<>();
            parts.put("text", prompt);
            contents.put("parts", List.of(parts));
            requestBody.put("contents", List.of(contents));

            // 헤더 설정
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

            // API 키가 query string에 있으므로 전체 URL은 절대 로그에 남기지 않는다.
            log.info("Gemini 요청 시작 - model: {}, promptLength: {}",
                    geminiConfig.getModel(), prompt.length());

            // API 호출
            ResponseEntity<String> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    entity,
                    String.class
            );

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                // 응답 파싱
                JsonNode root = objectMapper.readTree(response.getBody());
                JsonNode candidates = root.path("candidates");
                if (!candidates.isArray() || candidates.isEmpty()) {
                    String blockReason = root.path("promptFeedback").path("blockReason").asText("UNKNOWN");
                    log.warn("Gemini 응답에 후보가 없습니다 - blockReason: {}", blockReason);
                    throw new IllegalStateException("AI가 안전 정책 또는 응답 오류로 결과를 생성하지 못했습니다.");
                }

                JsonNode responseParts = candidates.get(0).path("content").path("parts");
                if (!responseParts.isArray() || responseParts.isEmpty()) {
                    throw new IllegalStateException("AI 응답 본문이 비어 있습니다.");
                }

                String text = responseParts.get(0).path("text").asText();
                if (text.isBlank()) {
                    throw new IllegalStateException("AI 응답 본문이 비어 있습니다.");
                }

                log.debug("Gemini API 응답 길이: {} 자", text.length());
                return text;
            } else {
                log.error("Gemini API 호출 실패: {}", response.getStatusCode());
                throw new RuntimeException("AI 요약 생성에 실패했습니다");
            }

        } catch (HttpClientErrorException e) {
            String responseBody = e.getResponseBodyAsString();
            String message;

            if (responseBody.contains("API_KEY_INVALID")) {
                message = "Gemini API 키가 유효하지 않습니다. 서버 환경 설정의 키를 확인해 주세요.";
            } else if (e.getStatusCode() == HttpStatus.NOT_FOUND) {
                message = "설정된 Gemini 모델을 사용할 수 없습니다. 모델 설정을 확인해 주세요.";
            } else if (e.getStatusCode() == HttpStatus.TOO_MANY_REQUESTS) {
                message = "Gemini API 사용량 한도를 초과했습니다. 잠시 후 다시 시도해 주세요.";
            } else if (e.getStatusCode() == HttpStatus.FORBIDDEN) {
                message = "Gemini API 사용 권한이 없습니다. API 키와 프로젝트 설정을 확인해 주세요.";
            } else {
                message = "Gemini API 요청에 실패했습니다. 서버의 AI 설정을 확인해 주세요.";
            }

            // API 응답 전문이나 요청 URL에는 인증 정보가 포함될 수 있으므로 로그에 남기지 않는다.
            log.warn("Gemini API 요청 거절 - model: {}, status: {}",
                    geminiConfig.getModel(), e.getStatusCode());
            throw new BusinessException(ErrorCode.BAD_REQUEST, message);
        } catch (RestClientException e) {
            log.error("Gemini API 통신 실패 - model: {}", geminiConfig.getModel(), e);
            throw new BusinessException(
                    ErrorCode.INTERNAL_ERROR,
                    "AI 서비스에 연결할 수 없습니다. 잠시 후 다시 시도해 주세요."
            );
        } catch (IllegalArgumentException | IllegalStateException e) {
            throw e;
        } catch (Exception e) {
            log.error("Gemini API 응답 처리 실패 - model: {}", geminiConfig.getModel(), e);
            throw new IllegalStateException("AI 응답을 처리하지 못했습니다.");
        }
    }

    /**
     * 인수인계 요약 생성
     *
     * @param departmentName 부서명
     * @param shiftType      근무조 타입
     * @param patientsData   환자별 데이터 텍스트
     * @return AI 요약 텍스트
     */
    public String generateHandoverSummary(String departmentName, String shiftType, String patientsData) {
        String prompt = buildHandoverPrompt(departmentName, shiftType, patientsData);
        return generateContent(prompt);
    }

    /**
     * 인수인계 프롬프트 생성
     */
    private String buildHandoverPrompt(String departmentName, String shiftType, String patientsData) {
        return String.format("""
                다음은 %s %s 근무조의 인수인계 정보입니다.
                
                %s
                
                각 환자별로 다음 형식으로 인수인계문을 작성해주세요:
                
                [환자명 (병실, 나이세/성별)]
                - 주요 변화: 특이사항 및 상태 변화
                - 수행한 처치: 투약, 검사 등
                - 지속 관찰 사항: 다음 근무조에서 주의할 점
                
                작성 지침:
                1. 환자당 3-5문장으로 간결하게 작성
                2. 중요한 환자(바이탈 이상, 검사 이상)는 더 자세히 작성
                3. 특이사항이 없는 환자는 "안정적으로 경과 관찰 중"으로 간략히 작성
                4. 의학적으로 중요한 정보 우선
                5. 구체적인 수치와 시간 포함
                6. 다음 근무조가 즉시 확인해야 할 사항 강조
                
                인수인계문을 작성해주세요:
                """,
                departmentName, shiftType, patientsData);
    }
}
