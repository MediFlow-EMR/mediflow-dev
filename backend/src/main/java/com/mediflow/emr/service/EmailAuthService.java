package com.mediflow.emr.service;

import com.mediflow.emr.dto.EmailLoginRequest;
import com.mediflow.emr.dto.EmailSignupRequest;
import com.mediflow.emr.entity.EmailVerification;
import com.mediflow.emr.entity.enums.Provider;
import com.mediflow.emr.entity.User;
import com.mediflow.emr.exception.BusinessException;
import com.mediflow.emr.exception.ErrorCode;
import com.mediflow.emr.repository.EmailVerificationRepository;
import com.mediflow.emr.repository.UserRepository;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * 이메일 기반 회원가입 및 인증 처리 서비스
 * - 이메일 중복 확인
 * - 인증 코드 발송
 * - 인증 코드 검증
 * - 회원가입 완료 처리
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class EmailAuthService {

    private final UserRepository userRepository;
    private final EmailVerificationRepository emailVerificationRepository;
    private final JavaMailSender mailSender;
    private final PasswordEncoder passwordEncoder;
    private final DummyDataService dummyDataService;

    @Value("${spring.mail.username}")
    private String mailHost;

    @Transactional(readOnly = true)
    public boolean checkEmailDuplicate(String email) {
        return userRepository.findByEmail(email.trim()).map(user -> !isIncompleteLocalUser(user)).orElse(false);
    }

    private boolean isIncompleteLocalUser(User user) {
        return user.getProvider() == Provider.LOCAL && user.getPassword() == null;
    }

    public void sendCode(String email) {
        email = email.trim();
        User user = userRepository.findByEmailForUpdate(email).orElse(null);
        if (user != null && !isIncompleteLocalUser(user)) {
            throw new BusinessException(ErrorCode.CONFLICT, "이미 가입된 이메일입니다. 로그인을 이용해주세요.");
        }
        EmailVerification verification = emailVerificationRepository.findByEmailForUpdate(email).orElse(null);
        if (verification != null && verification.getExpiryDate().minusMinutes(4).isAfter(LocalDateTime.now())) {
            throw new BusinessException(ErrorCode.TOO_MANY_REQUESTS);
        }
        if (user == null) {
            user = userRepository.save(User.builder().email(email).nickname("임시회원")
                    .provider(Provider.LOCAL).providerId(email).build());
        }
        String code = sendVerificationEmail(email);
        if (verification == null) {
            emailVerificationRepository.save(EmailVerification.builder().email(email).verificationCode(code)
                    .expiryDate(LocalDateTime.now().plusMinutes(5)).user(user).build());
        } else {
            verification.updateNewCode(code);
        }
    }

    private String sendVerificationEmail(String email) {
        // 인증 코드 생성
        String code = generateCode();

        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
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
            log.warn("인증 메일 발송 실패: {}", e.getClass().getSimpleName());
            throw new BusinessException(ErrorCode.MAIL_UNAVAILABLE);
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
        EmailVerification verification = emailVerificationRepository.findByEmailForUpdate(email.trim()).orElse(null);
        if (verification == null || verification.getIsVerified() || verification.isExpired()
                || verification.getFailedAttempts() >= 5) return false;
        if (!verification.isValid(code)) {
            verification.recordFailedAttempt();
            return false;
        }
        verification.verify();
        return true;
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

        if (verification == null || verification.getIsVerified() || verification.getFailedAttempts() >= 5) {
            log.info("인증 정보 없음 또는 이미 인증 완료. email={}", email);
            return 0;
        }

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime expiryDate = verification.getExpiryDate();

        // 만료 시간이 지난 경우
        if (now.isAfter(expiryDate)) {
            log.info("인증 코드 만료됨. email={}", email);
            return 0;
        }

        // 남은 시간을 초 단위로 계산
        long remainingSeconds = java.time.Duration.between(now, expiryDate).getSeconds();
        log.info("남은 시간 조회 성공. email={}, remainingSeconds={}", email, remainingSeconds);
        return remainingSeconds;
    }

    /**
     * 무작위로 1000~9999 사이의 랜덤 숫자를 생성
     *
     * @return 4자리 인증 코드
     */
    private String generateCode() {
        return String.valueOf(new java.security.SecureRandom().nextInt(9000) + 1000);
    }

    /**
     * 회원가입 완료 처리
     * - 이메일 인증이 완료된 사용자의 비밀번호를 설정하고 회원가입을 완료
     *
     * @param dto 이메일과 비밀번호
     */
    public void completeSignup(EmailSignupRequest dto) {
        String email = dto.email().trim();
        User user = userRepository.findByEmailForUpdate(email)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
        EmailVerification verification = emailVerificationRepository.findByEmailForUpdate(email)
                .orElseThrow(() -> new BusinessException(ErrorCode.EMAIL_NOT_VERIFIED));
        if (!isIncompleteLocalUser(user) || !verification.getIsVerified() || verification.isExpired()) {
            throw new BusinessException(ErrorCode.EMAIL_NOT_VERIFIED);
        }
        user.setPassword(passwordEncoder.encode(dto.password()));
        user.completeVerifying();
        userRepository.save(user);
        emailVerificationRepository.delete(verification);
        dummyDataService.applyDummyDataToNewUser(user);
    }

    /**
     * 이메일 로그인 처리
     * - 이메일과 비밀번호를 검증하고 사용자 정보 반환
     *
     * @param dto 이메일과 비밀번호
     * @return 로그인한 사용자 정보
     */
    @Transactional(readOnly = true)
    public User loginWithEmail(EmailLoginRequest dto) {
        String email = dto.email().trim();
        String password = dto.password();

        log.info("이메일 로그인 시도. email={}", email);

        // 1. 이메일로 사용자 조회
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> {
                    log.warn("존재하지 않는 이메일. email={}", email);
                    return new BusinessException(ErrorCode.USER_NOT_FOUND);
                });

        // 2. 회원가입을 중단한 회원에 대해서 체크
        if (!user.isEmailVerified() || user.getPassword() == null) {
            log.warn("회원가입이 완료되지 않은 회원. email={}", email);
            throw new BusinessException(ErrorCode.EMAIL_NOT_VERIFIED);
        }

        // 3. 비밀번호 검증
        if (!passwordEncoder.matches(password, user.getPassword())) {
            log.warn("비밀번호 불일치. email={}", email);
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }


        // 4. 계정 활성화 상태 확인
        if (!user.getIsActive()) {
            log.warn("비활성화된 계정. email={}", email);
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }

        // 5. 계정 잠금 상태 확인
        if (user.getIsLocked()) {
            log.warn("잠긴 계정. email={}", email);
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }

        log.info("이메일 로그인 성공. email={}, userId={}", email, user.getId());
        return user;
    }
}
