package com.mediflow.emr.service;

import com.mediflow.emr.dto.UserResponseDto;
import com.mediflow.emr.exception.BusinessException;
import com.mediflow.emr.exception.ErrorCode;
import com.mediflow.emr.repository.UserRepository;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 사용자 관련 비즈니스 로직 처리 서비스
 * - 현재 인증된 사용자 정보 조회 기능 제공
 */
@Service
@RequiredArgsConstructor
public class UsersService {

    private final UserRepository userRepository;

    // 조회된 providerId로 현재 사용자 정보 조회
    @Transactional(readOnly = true)
    public UserResponseDto findMeByProviderId(String providerId) {
        return userRepository.findByProviderId(providerId)
                .map(UserResponseDto::from)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
    }
}