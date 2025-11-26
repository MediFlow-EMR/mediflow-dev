package com.mediflow.emr.dto.ai;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * AI 질문 요청 DTO
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class AiQuestionRequest {
    private String question;
}

