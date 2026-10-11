package com.mediflow.emr.repository;


import com.mediflow.emr.entity.EmailVerification;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface EmailVerificationRepository extends JpaRepository<EmailVerification, Long> {
    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("select e from EmailVerification e where e.email = :email")
    Optional<EmailVerification> findByEmailForUpdate(@org.springframework.data.repository.query.Param("email") String email);


    /**
     * 이메일로 인증 정보 조회
     */
    Optional<EmailVerification> findByEmail(String email);

    /**
     * 이메일로 인증 정보 존재 여부 확인
     */
    boolean existsByEmail(String email);

    /**
     * 이메일로 인증 정보 삭제
     */
    void deleteByEmail(String email);
}
