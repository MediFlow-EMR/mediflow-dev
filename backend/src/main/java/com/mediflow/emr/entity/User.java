package com.mediflow.emr.entity;


import com.mediflow.emr.entity.enums.Provider;
import com.mediflow.emr.entity.enums.Role;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Entity
@Table(
        name = "users",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_user_email", columnNames = {"email"}),
                @UniqueConstraint(name = "uk_provider_provider_id", columnNames = {"provider", "provider_id"})
        }
)
public class User extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id")
    private Long id;

    @Column(nullable = false, length = 255)
    private String email;

    @Column(length = 500)
    private String password;

    @Column(nullable = false, length = 100)
    private String nickname;

    /** 실명 (EMR 사용자용) */
    @Column(length = 100)
    private String name;

    @Column(name = "profile_image_url", length = 500)
    private String profileImageUrl;

    /** 연락처 (EMR 사용자용) */
    @Column(length = 20)
    private String phone;

    /** 입사일 (EMR 사용자용) */
    @Column(name = "hire_date")
    private java.time.LocalDate hireDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private Provider provider;

    @Column(name = "provider_id", nullable = false, length = 255)
    private String providerId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private Role role = Role.USER;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "department_id")
    private DepartmentEntity department;

    @Column(nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    @Column(nullable = false)
    @Builder.Default
    private Boolean isLocked = false;

    @Column(name = "last_login_at")
    private java.time.LocalDateTime lastLoginAt;

    /**
     * 사용자 프로필 정보를 업데이트합니다.
     *
     * - 닉네임: null이 아닌 경우에만 변경됩니다.
     * - 프로필 이미지 URL: 전달된 값으로 그대로 설정됩니다(Null 허용).
     *
     * @param nickname          변경할 닉네임(Null이면 기존 값 유지)
     * @param profileImageUrl   변경할 프로필 이미지 URL(Null 가능)
     */
    public void updateProfile(String nickname, String profileImageUrl) {
        if (nickname != null) {
            this.nickname = nickname;
        }
        this.profileImageUrl = profileImageUrl;
    }
    /**
     * 현재 이메일이 공급자 placeholder 도메인으로 저장된 경우, 실제 이메일로 교체합니다.
     * 예: "@kakao.local"로 끝나는 임시 이메일을 실제 이메일로 교체
     *
     * @param realEmail         실제 이메일 주소
     * @param placeholderSuffix 공급자별 placeholder 도메인 접미사 (예: "@kakao.local")
     */
    public void updateEmailIfPlaceholder(String realEmail, String placeholderSuffix) {
        if (realEmail == null || realEmail.isBlank()) {
            return;
        }
        if (this.email != null && placeholderSuffix != null && this.email.endsWith(placeholderSuffix)) {
            this.email = realEmail;
        }
    }

    /**
     * 사용자의 역할을 변경합니다.
     *
     * @param role 새로운 역할
     */
    public void updateRole(Role role) {
        this.role = role;
    }

    /**
     * 사용자의 부서를 변경합니다.
     *
     * @param department 새로운 부서
     */
    public void updateDepartment(DepartmentEntity department) {
        this.department = department;
    }

    /**
     * 마지막 로그인 시간을 업데이트합니다.
     */
    public void updateLastLogin() {
        this.lastLoginAt = java.time.LocalDateTime.now();
    }

    /**
     * 계정을 비활성화합니다.
     */
    public void deactivate() {
        this.isActive = false;
    }

    /**
     * 계정을 활성화합니다.
     */
    public void activate() {
        this.isActive = true;
    }

    /**
     * 계정을 잠급니다.
     */
    public void lock() {
        this.isLocked = true;
    }

    /**
     * 계정 잠금을 해제합니다.
     */
    public void unlock() {
        this.isLocked = false;
    }

    /**
     * 이메일 인증을 완료합니다.
     */
    public void completeVerifying() {
        this.emailVerified = true;
    }

    /**
     * 비밀번호를 설정합니다.
     *
     * @param encodedPassword 암호화된 비밀번호
     */
    public void setPassword(String encodedPassword) {
        this.password = encodedPassword;
    }

    /**
     * 이메일 인증 완료 여부를 업데이트합니다.
     */
    @Column(nullable = false)
    @Builder.Default
    private boolean emailVerified = false;
}
