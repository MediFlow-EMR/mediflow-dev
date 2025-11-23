package com.mediflow.emr.controller;

import com.mediflow.emr.service.EmailAuthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 이메일 인증 관련 API 컨트롤러
 * - 이메일 중복 확인 및 인증 코드 발송
 * - 인증 코드 검증
 */
@RestController
@RequestMapping("/api/auth/email")
@RequiredArgsConstructor
@Slf4j
public class EmailAuthController {

    private final EmailAuthService emailAuthService;

    /**
     * 이메일 중복 검사 및 인증 코드 발송 API
     *
     * @param email 검사할 이메일 주소
     * @return 중복 여부 및 메시지
     */
    @GetMapping("/check")
    public ResponseEntity<?> checkEmail(@RequestParam String email) {
        log.info("이메일 중복 확인 요청: {}", email);

        boolean isDuplicate = emailAuthService.checkEmailDuplicate(email);
        String message = isDuplicate ? "이메일이 중복되었습니다." : "사용 가능한 이메일입니다.";

        log.info("이메일 중복 확인 결과: {}, message={}", isDuplicate, message);

        return ResponseEntity.ok().body(Map.of(
                "isDuplicate", isDuplicate,
                "message", message
        ));
    }

    /**
     * 인증 코드 검증 API
     *
     * @param email 사용자 이메일
     * @param code  입력한 인증 코드
     * @return 인증 코드 일치 여부
     */
    @GetMapping("/verify-code")
    public ResponseEntity<?> verifyCode(@RequestParam String email, @RequestParam String code) {
        log.info("인증 코드 검증 요청: email={}, code={}", email, code);

        boolean isMatch = emailAuthService.isMatchCode(email, code);

        log.info("인증 코드 검증 결과: {}", isMatch);

        return ResponseEntity.ok().body(Map.of(
                "isMatch", isMatch
        ));
    }

    /**
     * 인증 코드 남은 시간 조회 API
     *
     * @param email 사용자 이메일
     * @return 남은 시간(초)
     */
    @GetMapping("/remaining-time")
    public ResponseEntity<?> getRemainingTime(@RequestParam String email) {
        log.info("인증 코드 남은 시간 조회: email={}", email);

        long remainingSeconds = emailAuthService.getRemainingTime(email);

        log.info("남은 시간: {}초", remainingSeconds);

        return ResponseEntity.ok().body(Map.of(
                "remainingSeconds", remainingSeconds
        ));
    }
}

