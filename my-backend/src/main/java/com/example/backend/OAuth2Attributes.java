package com.example.backend;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;

@Slf4j
@Getter
public class OAuth2Attributes {
    
    private final String name;
    private final String email;
    private final String providerId;
    private final User.Provider provider;
    private final String profileImageUrl;

    private OAuth2Attributes(String name, String email, String providerId, 
                           User.Provider provider, String profileImageUrl) {
        this.name = name;
        this.email = email;
        this.providerId = providerId;
        this.provider = provider;
        this.profileImageUrl = profileImageUrl;
    }

    public static OAuth2Attributes of(String registrationId, String userNameAttributeName, 
                                    Map<String, Object> attributes) {
        return switch (registrationId.toLowerCase()) {
            case "naver" -> ofNaver(attributes);
            case "google" -> ofGoogle(attributes);
            case "kakao" -> ofKakao(attributes);
            default -> throw new IllegalArgumentException("지원하지 않는 OAuth2 제공자: " + registrationId);
        };
    }

    @SuppressWarnings("unchecked")
    private static OAuth2Attributes ofNaver(Map<String, Object> attributes) {
        Map<String, Object> response = (Map<String, Object>) attributes.get("response");
        
        return new OAuth2Attributes(
                (String) response.get("name"),
                (String) response.get("email"),
                (String) response.get("id"),
                User.Provider.NAVER,
                (String) response.get("profile_image")
        );
    }

    private static OAuth2Attributes ofGoogle(Map<String, Object> attributes) {
        return new OAuth2Attributes(
                (String) attributes.get("name"),
                (String) attributes.get("email"),
                (String) attributes.get("sub"),
                User.Provider.GOOGLE,
                (String) attributes.get("picture")
        );
    }

    @SuppressWarnings("unchecked")
    private static OAuth2Attributes ofKakao(Map<String, Object> attributes) {
        try {
            Map<String, Object> account = (Map<String, Object>) attributes.get("kakao_account");
            if (account == null) {
                throw new RuntimeException("카카오 계정 정보를 가져올 수 없습니다.");
            }
            
            Map<String, Object> profile = (Map<String, Object>) account.get("profile");
            if (profile == null) {
                throw new RuntimeException("카카오 프로필 정보를 가져올 수 없습니다.");
            }
            
            String email = (String) account.get("email");
            String nickname = (String) profile.get("nickname");
            String id = String.valueOf(attributes.get("id"));
            String profileImageUrl = (String) profile.get("profile_image_url");
            
            // 이메일이 없는 경우 카카오 ID를 이용해 임시 이메일 생성
            if (email == null || email.trim().isEmpty()) {
                email = "kakao_" + id + "@kakao.temp";
                log.warn("카카오 계정에서 이메일을 가져올 수 없어 임시 이메일 생성: {}", email);
            }
            
            return new OAuth2Attributes(
                    nickname,
                    email,
                    id,
                    User.Provider.KAKAO,
                    profileImageUrl
            );
            
        } catch (Exception e) {
            log.error("카카오 사용자 정보 처리 중 오류 발생: {}", e.getMessage());
            log.debug("카카오 attributes: {}", attributes);
            throw new RuntimeException("카카오 로그인 처리 중 오류가 발생했습니다.", e);
        }
    }
}