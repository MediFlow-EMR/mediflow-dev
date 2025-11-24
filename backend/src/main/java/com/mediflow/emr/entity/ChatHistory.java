package com.mediflow.emr.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * AI 챗봇 대화 기록 엔티티
 * 간호사와 AI 챗봇 간의 대화 내역
 */
@Getter
@Entity
@Table(name = "chat_history")
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatHistory extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "chat_history_id")
    private Long id;

    /** 간호사 */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "nurse_id", nullable = false)
    private User nurse;

    /** 사용자 메시지 */
    @Column(nullable = false, columnDefinition = "TEXT")
    private String message;

    /** AI 응답 */
    @Column(nullable = false, columnDefinition = "TEXT")
    private String response;

    /** 액션 유형 (기록/조회/투약 등) */
    @Column(name = "action_type", length = 50)
    private String actionType;
}
