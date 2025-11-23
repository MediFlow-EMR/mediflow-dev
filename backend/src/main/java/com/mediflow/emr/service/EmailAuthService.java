package com.mediflow.emr.service;

import com.mediflow.emr.entity.EmailVerification;
import com.mediflow.emr.entity.Provider;
import com.mediflow.emr.entity.User;
import com.mediflow.emr.repository.EmailVerificationRepository;
import com.mediflow.emr.repository.UserRepository;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * 이메일 기반 회원가입 및 인증 처리 서비스
 * - 이메일 중복 확인
 * - 인증 코드 발송
 * - 인증 코드 검증
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class EmailAuthService {

    private final UserRepository userRepository;
    private final EmailVerificationRepository emailVerificationRepository;
    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String mailHost;

    /**
     * 이메일 중복 확인 및 인증 코드 발송
     *
     * @param email 검증할 이메일
     * @return true: 중복됨, false: 사용 가능
     */
    @Transactional(readOnly = true)
    public boolean checkEmailDuplicate(String email) {
        // 중복 확인
        boolean isDuplicate = userRepository.existsByEmail(email);

        // 사용 가능한 이메일인 경우 인증 메일 발송
        if (!isDuplicate) {
            processSignup(email);
        }

        return isDuplicate;
    }

    /**
     * 임시 회원가입 및 인증 코드 발송
     *
     * @param email 사용자 이메일
     */
    private void processSignup(String email) {
        // 1. 임시 회원가입 (필수 필드를 모두 채워서 저장)
        User tempUser = User.builder()
                .email(email)
                .nickname("임시회원") // 임시 닉네임
                .provider(Provider.LOCAL) // 로컬 가입
                .providerId(email) // 이메일을 providerId로 사용
                .build();

        User savedUser = userRepository.save(tempUser);

        // 2. 인증 메일 발송
        String code = sendVerificationEmail(email);

        // 3. 인증 코드와 만료시간을 DB에 저장
        EmailVerification verification = EmailVerification.builder()
                .email(email)
                .verificationCode(code)
                .expiryDate(LocalDateTime.now().plusMinutes(5)) // 만료시간 5분 설정
                .user(savedUser) // FK 설정
                .build();
        emailVerificationRepository.save(verification);
    }

    /**
     * 이메일 인증 코드 발송
     *
     * @param email 수신자 이메일
     * @return 생성된 인증 코드
     */
    private String sendVerificationEmail(String email) {
        // 인증 코드 생성
        String code = generateCode();

        // 메일 전송 로직
        MimeMessage mimeMessage = mailSender.createMimeMessage();

        try {
            MimeMessageHelper messageHelper
                    = new MimeMessageHelper(mimeMessage, false, "UTF-8");

            // 수신자 설정
            messageHelper.setTo(email);

            // 발신자 설정
            messageHelper.setFrom(mailHost);

            // 이메일 제목 설정
            messageHelper.setSubject("[MediFlow] 이메일 인증 코드");

            // 이메일 내용 설정
            messageHelper.setText(
                    "<div style='text-align: center; padding: 20px;'>" +
                            "<h2>MediFlow 이메일 인증</h2>" +
                            "<p>아래 인증 코드를 입력해주세요.</p>" +
                            "<p>인증 코드: <b style='font-weight: 700; letter-spacing: 5px; font-size: 30px; color: #0ea5e9;'>"
                            + code + "</b></p>" +
                            "<p style='color: #6b7280; font-size: 14px;'>이 코드는 5분간 유효합니다.</p>" +
                            "</div>",
                    true
            );

            // 메일 발송
            mailSender.send(mimeMessage);

            log.info("인증 코드 이메일 발송 완료: {}", email);
            return code;

        } catch (Exception e) {
            log.error("메일 발송 실패: {}", email, e);
            throw new RuntimeException("메일 발송에 실패했습니다.", e);
        }
    }

    /**
     * 클라이언트가 전송한 인증코드를 검증하는 처리
     *
     * @param email 사용자 이메일
     * @param code  입력한 인증 코드
     * @return true: 인증 성공, false: 인증 실패
     */
    public boolean isMatchCode(String email, String code) {
        // 이메일로 인증 정보 조회
        EmailVerification verification = emailVerificationRepository.findByEmail(email)
                .orElse(null);

        if (verification == null) {
            log.warn("인증 정보를 찾을 수 없습니다. email={}", email);
            return false;
        }

        // 코드가 일치하고 만료시간이 지나지 않았는지 체크
        if (code.equals(verification.getVerificationCode())
                && verification.getExpiryDate().isAfter(LocalDateTime.now())) {

            // 인증 완료 처리
            verification.verify();
            log.info("인증 코드 검증 성공. email={}", email);
            return true;
        }

        log.warn("인증 코드 검증 실패. email={}, inputCode={}", email, code);
        return false;
    }

    /**
     * 인증 코드의 남은 시간을 조회 (초 단위)
     *
     * @param email 사용자 이메일
     * @return 남은 시간(초), 인증 정보가 없거나 만료된 경우 0
     */
    @Transactional(readOnly = true)
    public long getRemainingTime(String email) {
        EmailVerification verification = emailVerificationRepository.findByEmail(email)
                .orElse(null);

        if (verification == null || verification.getIsVerified()) {
            return 0;
        }

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime expiryDate = verification.getExpiryDate();

        // 만료 시간이 지난 경우
        if (now.isAfter(expiryDate)) {
            return 0;
        }

        // 남은 시간을 초 단위로 계산
        return java.time.Duration.between(now, expiryDate).getSeconds();
    }

    /**
     * 무작위로 1000~9999 사이의 랜덤 숫자를 생성
     *
     * @return 4자리 인증 코드
     */
    private String generateCode() {
        return String.valueOf((int) (Math.random() * 9000) + 1000);
    }
}

