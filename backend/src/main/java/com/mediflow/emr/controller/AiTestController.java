package com.mediflow.emr.controller;

import com.mediflow.emr.dto.ApiResponse;
import com.mediflow.emr.dto.ai.AiAnswerResponse;
import com.mediflow.emr.dto.ai.AiQuestionRequest;
import com.mediflow.emr.service.GeminiService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * AI 테스트 컨트롤러
 */
@Slf4j
@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
public class AiTestController {

    private final GeminiService geminiService;

    /**
     * AI에게 질문하기 (테스트용)
     */
    @PostMapping("/ask")
    public ResponseEntity<ApiResponse<AiAnswerResponse>> askQuestion(
            @RequestBody AiQuestionRequest request) {

        log.info("AI 질문 요청: {}", request.getQuestion());

        try {
            String answer = geminiService.generateContent(request.getQuestion());

            AiAnswerResponse response = AiAnswerResponse.builder()
                    .question(request.getQuestion())
                    .answer(answer)
                    .build();

            return ResponseEntity.ok(ApiResponse.ok(response, "AI 응답이 생성되었습니다"));
        } catch (Exception e) {
            log.error("AI 질문 처리 중 오류 발생", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("AI 응답 생성 중 오류가 발생했습니다: " + e.getMessage()));
        }
    }
}

