package com.mediflow.emr.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 식약처 API 약품 상세 정보
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DrugDetailInfo {
    private boolean available;
    private String source;
    private String status;
    private String message;
    private String requestedName;
    private String itemSeq;        // 품목기준코드
    private String itemName;       // 약품명
    private String entpName;       // 제조사
    private String efcyQesitm;     // 효능효과
    private String useMethodQesitm; // 사용법
    private String atpnWarnQesitm; // 주의사항 경고
    private String atpnQesitm;     // 주의사항
    private String intrcQesitm;    // 상호작용
    private String seQesitm;       // 부작용
    private String depositMethodQesitm; // 보관방법
}
