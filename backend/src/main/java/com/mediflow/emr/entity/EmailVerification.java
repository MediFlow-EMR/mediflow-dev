package com.mediflow.emr.entity;


import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@ToString(exclude = "user")
@EqualsAndHashCode(exclude = "user")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder

@Entity
@Table(name = "email_verification")
public class EmailVerification extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "verification_id")
    private Long id;

    @Column(nullable = false, unique = true)
    private String email; // 인증할 이메일

    @Column(nullable = false)
    private String verificationCode; // 인증코드 (4자리)

    @Column(nullable = false)
    private LocalDateTime expiryDate; // 인증 만료시간

    @Column(nullable = false)
    @Builder.Default
    private Boolean isVerified = false; // 인증 완료 여부

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user; // 인증을 진행하는 사용자

    /**
     * 인증 코드가 유효한지 확인
     * @param code 입력된 인증 코드
     * @return 유효하면 true
     */
    public boolean isValid(String code) {
        return !isVerified
                && verificationCode.equals(code)
                && LocalDateTime.now().isBefore(expiryDate);
    }

    /**
     * 인증 완료 처리
     */
    public void verify() {
        this.isVerified = true;
    }

    /**
     * 인증 코드 만료 여부 확인
     * @return 만료되었으면 true
     */
    public boolean isExpired() {
        return LocalDateTime.now().isAfter(expiryDate);
    }

    /**
     * 인증 코드 재발급 (새 코드와 만료시간 갱신)
     * @param newCode 새로운 인증 코드
     */
    public void updateNewCode(String newCode) {
        this.verificationCode = newCode;
        this.expiryDate = LocalDateTime.now().plusMinutes(5);
        this.isVerified = false;
    }
}