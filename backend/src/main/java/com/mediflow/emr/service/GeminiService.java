package com.mediflow.emr.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mediflow.emr.config.GeminiConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

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
    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Gemini API에 프롬프트 전송
     *
     * @param prompt 입력 프롬프트
     * @return AI 응답 텍스트
     */
    public String generateContent(String prompt) {
        try {
            String url = String.format("%s/models/%s:generateContent?key=%s",
                    geminiConfig.getBaseUrl(),
                    geminiConfig.getModel(),
                    geminiConfig.getApiKey());

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

            log.debug("Gemini API 요청: {}", url);
            log.debug("프롬프트 길이: {} 자", prompt.length());

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
                String text = root.path("candidates")
                        .get(0)
                        .path("content")
                        .path("parts")
                        .get(0)
                        .path("text")
                        .asText();

                log.debug("Gemini API 응답 길이: {} 자", text.length());
                return text;
            } else {
                log.error("Gemini API 호출 실패: {}", response.getStatusCode());
                throw new RuntimeException("AI 요약 생성에 실패했습니다");
            }

        } catch (Exception e) {
            log.error("Gemini API 호출 중 오류 발생", e);
            throw new RuntimeException("AI 요약 생성 중 오류가 발생했습니다: " + e.getMessage());
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

