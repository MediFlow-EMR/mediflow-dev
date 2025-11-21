package com.mediflow.emr.service;

import com.mediflow.emr.entity.Provider;
import com.mediflow.emr.entity.User;
import com.mediflow.emr.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;

/**
 * OAuth2 공급자(Google, Kakao)로부터 사용자 정보를 조회하고,
 * 우리 서비스의 User 엔티티와 동기화하는 커스텀 OAuth2UserService 구현체.
 * - DefaultOAuth2UserService를 상속하여 공급자 사용자 정보 조회 기능 재사용
 * - 공급자별 응답 스키마에 맞게 주요 필드 추출
 * - 기존 사용자 프로필 업데이트 또는 신규 사용자 저장
 * - 동기화된 사용자 정보를 담은 OAuth2User 반환
 *
 *
 */
@Service
@RequiredArgsConstructor
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private final UserRepository userRepository;

    /**
     * OAuth2 공급자로부터 사용자 정보를 조회하고 동기화
     *
     * @param userRequest OAuth2UserRequest 객체
     * @return 동기화된 사용자 정보를 담은 OAuth2User 객체
     * @throws OAuth2AuthenticationException 인증 실패 시 예외 발생
     */
    @Override
    @Transactional
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oAuth2User = super.loadUser(userRequest); // 공급자 사용자 정보 조회 재사용

        String registrationId = userRequest.getClientRegistration().getRegistrationId(); // google, kakao
        Provider provider = resolveProvider(registrationId); // 도메인 Provider enum 변환
        Map<String, Object> attributes = oAuth2User.getAttributes(); // 공급자 응답 속성 맵

        String providerId;
        String email;
        String nickname;
        String profileImageUrl;

        // Google 응답 스키마: https://openid.net/specs/openid-connect-core-1_0.html#StandardClaims
        if (provider == Provider.GOOGLE) {
            providerId = String.valueOf(attributes.get("sub"));
            email = safeStr(attributes.get("email"));
            nickname = coalesce(safeStr(attributes.get("name")), email != null ? email.split("@")[0] : null);
            profileImageUrl = safeStr(attributes.get("picture"));
            // Kakao 응답 스키마: https://developers.kakao.com/docs/latest/ko/kakaologin/rest-api#req-user-info
        } else if (provider == Provider.KAKAO) {
            providerId = String.valueOf(attributes.get("id"));
            Map<String, Object> kakaoAccount = (Map<String, Object>) attributes.getOrDefault("kakao_account", Collections.emptyMap());
            email = safeStr(kakaoAccount.get("email"));
            Map<String, Object> profile = (Map<String, Object>) kakaoAccount.getOrDefault("profile", Collections.emptyMap());
            nickname = coalesce(safeStr(profile.get("nickname")), email != null ? email.split("@")[0] : null);
            profileImageUrl = safeStr(profile.get("profile_image_url"));
        } else {
            throw new OAuth2AuthenticationException("Unsupported provider: " + registrationId);
        }

        // Kakao의 경우 이메일이 없을 수 있으므로 placeholder 이메일 처리
        final String effectiveEmail = (provider == Provider.KAKAO && (email == null || email.isBlank()))
                ? (providerId + "@kakao.local")
                : email;

        Optional<User> existing = userRepository.findByProviderAndProviderId(provider, providerId);
        User user = existing.map(u -> {
            u.updateProfile(nickname, profileImageUrl);
            // 기존 사용자가 placeholder 이메일 상태이고, 이후 실제 이메일을 가져오게 된 경우 교체
            u.updateEmailIfPlaceholder(effectiveEmail, "@kakao.local");
            return u;
        }).orElseGet(() -> User.builder()
                .email(effectiveEmail)
                .nickname(nickname)
                .profileImageUrl(profileImageUrl)
                .provider(provider)
                .providerId(providerId)
                .build());

        userRepository.save(user);

        return new DefaultOAuth2User(
                Collections.singleton(new SimpleGrantedAuthority("ROLE_USER")),
                attributes,
                resolveNameAttributeKey(registrationId)
        );
    }

    /**
     * registrationId(google|kakao)를 도메인 Provider enum으로 변환합니다.
     */
    private Provider resolveProvider(String registrationId) {
        String id = registrationId == null ? "" : registrationId.toLowerCase();
        return switch (id) {
            case "google" -> Provider.GOOGLE;
            case "kakao" -> Provider.KAKAO;
            default -> throw new IllegalArgumentException("Unknown provider: " + registrationId);
        };
    }

    /**
     * Spring Security가 principal의 name으로 사용할 attribute key를 공급자별로 반환합니다.
     * - Google: sub (OpenID Connect 표준 subject)
     * - Kakao: id
     */
    private String resolveNameAttributeKey(String registrationId) {
        String id = registrationId == null ? "" : registrationId.toLowerCase();
        return switch (id) {
            case "google" -> "sub";
            case "kakao" -> "id";
            default -> "id";
        };
    }

    /**
     * 객체를 null-safe하게 문자열로 변환합니다. null이면 null을 반환합니다.
     */
    private String safeStr(Object v) {
        return v == null ? null : String.valueOf(v);
    }

    /**
     * 첫 번째 인자가 비어있지 않으면 그대로, 아니면 두 번째 인자를 반환합니다.
     */
    private String coalesce(String a, String b) {
        return a != null && !a.isBlank() ? a : b;
    }
}
