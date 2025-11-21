package com.mediflow.emr.dto;

import com.mediflow.emr.entity.Provider;
import com.mediflow.emr.entity.User;
import lombok.Builder;
import lombok.Getter;

/** 사용자 정보를 응답하기 위한 DTO 클래스 */
@Getter
@Builder
public class UserResponseDto {
    private Long id;
    private String email;
    private String nickname;
    private String profileImageUrl;
    private Provider provider;

    // User 엔티티를 UserResponseDto로 변환하는 정적 메서드
    public static UserResponseDto from(User user) {
        return UserResponseDto.builder()
                .id(user.getId())
                .email(user.getEmail())
                .nickname(user.getNickname())
                .profileImageUrl(user.getProfileImageUrl())
                .provider(user.getProvider())
                .build();
    }
}
