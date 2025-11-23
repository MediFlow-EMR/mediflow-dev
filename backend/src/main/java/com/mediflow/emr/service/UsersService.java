package com.mediflow.emr.service;

import com.mediflow.emr.dto.UserResponseDto;
import com.mediflow.emr.entity.EmailVerification;
import com.mediflow.emr.entity.Provider;
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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * 사용자 관련 비즈니스 로직 처리 서비스
 * - 현재 인증된 사용자 정보 조회 기능 제공
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class UsersService {

    private final UserRepository userRepository;
    private final EmailVerificationRepository emailVerificationRepository;

    // 조회된 providerId로 현재 사용자 정보 조회
    @Transactional(readOnly = true)
    public UserResponseDto findMeByProviderId(String providerId) {
        return userRepository.findByProviderId(providerId)
                .map(UserResponseDto::from)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
    }

    // 메일 발송인의 정보
    @Value("${spring.mail.username}")
    private String mailHost;

    // 이메일 발송을 위한 의존객체
    private final JavaMailSender mailSender;

    // 이메일 중복확인 처리
    @Transactional(readOnly = true)
    public boolean checkEmailDuplicate(String email) {

        // 중복확인
        boolean flag = userRepository.existsByEmail(email);

        // 사용가능한 이메일인 경우 인증메일 발송
        if (!flag) processSignup(email);

        return flag;
    }

    // 인증 코드를 발송할 때 사용할 임시 회원가입 로직
    // 인증코드를 데이터베이스에 저장하려면 회원정보가 필요
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

    // 이메일 인증코드 발송 로직
    private String sendVerificationEmail(String email) {

        // 인증코드 생성
        String code = generateCode();

        // 메일 전송 로직
        MimeMessage mimeMessage = mailSender.createMimeMessage();

        try {
            MimeMessageHelper messageHelper
                    = new MimeMessageHelper(mimeMessage, false, "UTF-8");

            // 누구에게 이메일을 보낼지
            messageHelper.setTo(email);

            // 누가 보내는 건지
            messageHelper.setFrom(mailHost);

            // 이메일 제목 설정
            messageHelper.setSubject("[인증메일] 가입 인증 메일입니다.");
            // 이메일 내용 설정
            messageHelper.setText(
                    "인증 코드: <b style=\"font-weight: 700; letter-spacing: 5px; font-size: 30px;\">" + code + "</b>"
                    , true
            );

            // 메일 보내기
            mailSender.send(mimeMessage);

            log.info("{} 님에게 이메일이 발송되었습니다.", email);
            return code;

        } catch (Exception e) {
            log.error("메일 발송 실패: {}", email, e);
            throw new RuntimeException("메일 발송에 실패했습니다.", e);
        }

    }

    // 무작위로 1000~9999 사이의 랜덤 숫자를 생성
    private String generateCode() {
        return String.valueOf((int) (Math.random() * 9000) + 1000);
    }
}