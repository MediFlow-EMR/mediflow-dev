package com.mediflow.emr.controller;

import com.mediflow.emr.dto.ApiResponse;
import com.mediflow.emr.UserResponseDto;
import com.mediflow.emr.service.UsersService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 사용자 관련 API 컨트롤러
 * - 현재 인증된 사용자 정보 조회 엔드포인트 제공
 */
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UsersController {

    private final UsersService usersService;

    @GetMapping("/me")
    public ResponseEntity<?> me(@AuthenticationPrincipal String subject) {
        // subject는 OAuth2 nameAttributeKey(google: sub, kakao: id) → providerId로 사용 중
        UserResponseDto dto = usersService.findMeByProviderId(subject);
        return ResponseEntity.ok(ApiResponse.ok(dto));
    }
}
