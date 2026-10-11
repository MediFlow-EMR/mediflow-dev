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

    /**
     * 이메일 중복 확인 및 인증 코드 발송
     *
     * @param email 검증할 이메일
     * @return true: 중복됨, false: 사용 가능
     */
    public boolean checkEmailDuplicate(String email) {
        log.info("[checkEmailDuplicate] 시작. email={}", email);

        // 중복 확인
        boolean isDuplicate = userRepository.existsByEmail(email);
        log.info("[checkEmailDuplicate] 중복 확인 결과. email={}, isDuplicate={}", email, isDuplicate);

        // 이메일이 중복되었지만 회원가입이 마무리되지 않은 회원은
        // 중복을 무시하고 인증코드를 재발송
        if (isDuplicate && isSignupNotFinished(email)) {
            log.info("[checkEmailDuplicate] 회원가입 미완료 회원 - false 반환. email={}", email);
            return false;
        }

        // 사용 가능한 이메일인 경우 인증 메일 발송
        if (!isDuplicate) {
            log.info("[checkEmailDuplicate] 사용 가능한 이메일 - 임시 회원가입 및 인증 메일 발송. email={}", email);
            processSignup(email);
        }

        log.info("[checkEmailDuplicate] 최종 반환 값. email={}, isDuplicate={}", email, isDuplicate);
        return isDuplicate;
    }

    /**
     * 회원가입이 완료되지 않은 회원인지 확인하고 인증코드 재발송
     *
     * @param email 사용자 이메일
     * @return true: 회원가입 미완료 회원, false: 회원가입 완료 회원
     */
    private boolean isSignupNotFinished(String email) {
        // 회원가입이 중단된 회원정보를 조회
        User foundUser = userRepository.findByEmail(email).orElse(null);

        if (foundUser == null) {
            return false;
        }

        // 실제로 중단된 회원인지 재확인
        // 이메일 인증이 완료되지 않았거나 비밀번호가 설정되지 않은 경우
        if (!foundUser.isEmailVerified() || foundUser.getPassword() == null) {
            log.info("회원가입 미완료 회원 발견. email={}, emailVerified={}, hasPassword={}",
                email, foundUser.isEmailVerified(), foundUser.getPassword() != null);

            // 인증코드 재생성하고 이메일을 발송
            EmailVerification existingVerification = emailVerificationRepository.findByEmail(email)
                    .orElse(null);

            // 1. 인증코드를 과거에 받은 경우 - UPDATE
            if (existingVerification != null) {
                log.info("기존 인증 정보 발견. 인증코드 재발급. email={}", email);
                updateVerificationCode(email, existingVerification);
            } else {
                // 2. 받지 않은 경우 - 인증을 끝내면 인증코드가 삭제됨 - INSERT
                log.info("기존 인증 정보 없음. 새 인증코드 생성. email={}", email);
                generateAndSendCode(email, foundUser);
            }
            return true;
        }

        return false;
    }

    /**
     * 새로운 인증코드 생성 및 발송
     *
     * @param email 사용자 이메일
     * @param user 사용자 정보
     */
    private void generateAndSendCode(String email, User user) {
        // 인증 메일 발송
        String code = sendVerificationEmail(email);

        // 인증 코드와 만료시간을 DB에 저장
        EmailVerification verification = EmailVerification.builder()
                .email(email)
                .verificationCode(code)
                .expiryDate(LocalDateTime.now().plusMinutes(5)) // 만료시간 5분 설정
                .user(user) // FK 설정
                .build();
        emailVerificationRepository.save(verification);

        log.info("새 인증코드 생성 및 저장 완료. email={}", email);
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

        // 2. 인증 메일 발송 및 인증 정보 저장
        generateAndSendCode(email, savedUser);
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

            // User 엔티티의 이메일 인증 완료 처리
            User user = verification.getUser();
            if (user != null) {
                user.completeVerifying();
                log.info("사용자 이메일 인증 완료. userId={}, email={}", user.getId(), email);
            }

            log.info("인증 코드 검증 성공. email={}", email);
            return true;
        }

        // 인증코드가 틀렸거나 만료된 경우 자동으로 인증코드를 재발송
        log.warn("인증 코드 검증 실패 - 새 인증 코드 발송");
        updateVerificationCode(email, verification);
        return false;
    }

    /**
     * 인증코드 재발급 처리
     *
     * @param email 사용자 이메일
     * @param verification 기존 인증 정보
     */
    private void updateVerificationCode(String email, EmailVerification verification) {
        // 1. 새 인증코드를 생성하고 메일을 재발송
        String newCode = sendVerificationEmail(email);

        // 2. 데이터베이스에 인증코드와 만료시간을 갱신
        verification.updateNewCode(newCode);

        log.info("인증 코드 재발급 완료. email={}", email);
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
        return String.valueOf((int) (Math.random() * 9000) + 1000);
    }

    /**
     * 회원가입 완료 처리
     * - 이메일 인증이 완료된 사용자의 비밀번호를 설정하고 회원가입을 완료
     *
     * @param dto 이메일과 비밀번호
     */
    public void completeSignup(EmailSignupRequest dto) {
        String email = dto.email();
        String password = dto.password();

        log.info("회원가입 완료 처리 시작. email={}", email);

        // 1. 이메일 인증 확인
        EmailVerification verification = emailVerificationRepository.findByEmail(email)
                .orElseThrow(() -> {
                    log.error("인증 정보를 찾을 수 없음. email={}", email);
                    return new BusinessException(ErrorCode.EMAIL_NOT_VERIFIED);
                });

        if (!verification.getIsVerified()) {
            log.warn("이메일 인증이 완료되지 않음. email={}", email);
            throw new BusinessException(ErrorCode.EMAIL_NOT_VERIFIED);
        }

        log.info("이메일 인증 확인 완료. email={}", email);

        // 2. 사용자 정보 조회
        User user = verification.getUser();
        if (user == null) {
            log.error("인증 정보는 있지만 사용자 정보가 없음. email={}", email);
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }

        log.info("사용자 정보 조회 완료. userId={}, email={}", user.getId(), email);

        // 3. 비밀번호 암호화 및 설정
        String encodedPassword = passwordEncoder.encode(password);
        user.setPassword(encodedPassword);
        log.info("비밀번호 암호화 및 설정 완료. email={}", email);

        // 4. 이메일 인증 완료 처리
        user.completeVerifying();
        log.info("이메일 인증 완료 처리. email={}", email);

        // 5. 사용자 정보 저장 (명시적으로 저장)
        userRepository.save(user);
        log.info("사용자 정보 저장 완료. userId={}, email={}", user.getId(), email);

        // 6. 인증 정보 삭제 (더 이상 필요 없음)
        emailVerificationRepository.delete(verification);
        log.info("인증 정보 삭제 완료. email={}", email);

        // 7. 신규 사용자에게 더미 데이터 적용 (부서 배정, 환자 배정 등)
        dummyDataService.applyDummyDataToNewUser(user);

        log.info("회원가입 완료. email={}, userId={}", email, user.getId());
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
        String email = dto.email();
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
