package com.example.backend.authlogin.config;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;

import com.example.backend.authlogin.domain.User;

@Slf4j
@Getter
public class OAuth2Attributes {
    private Map<String, Object> attributes;
    private String nameAttributeKey;
    private String name;
    private String email;
    private String profileImageUrl;
    private String providerId;
    private User.Provider provider;

    private OAuth2Attributes(Map<String, Object> attributes, String nameAttributeKey, String name, String email, String profileImageUrl, String providerId, User.Provider provider) {
        this.attributes = attributes;
        this.nameAttributeKey = nameAttributeKey;
        this.name = name;
        this.email = email;
        this.profileImageUrl = profileImageUrl;
        this.providerId = providerId;
        this.provider = provider;
    }

    public static OAuth2Attributes of(String registrationId, String userNameAttributeName, Map<String, Object> attributes) {
        switch (registrationId) {
            case "google":
                return ofGoogle(userNameAttributeName, attributes);
            case "naver":
                return ofNaver(userNameAttributeName, attributes);
            case "kakao":
                return ofKakao(userNameAttributeName, attributes);
            default:
                throw new IllegalArgumentException("Unexpected value: " + registrationId);
        }
    }

    private static OAuth2Attributes ofGoogle(String userNameAttributeName, Map<String, Object> attributes) {
        return OAuth2Attributes.builder()
            .name((String) attributes.get("name"))
            .email((String) attributes.get("email"))
            .profileImageUrl((String) attributes.get("picture"))
            .providerId((String) attributes.get("sub"))
            .provider(User.Provider.GOOGLE)
            .attributes(attributes)
            .nameAttributeKey(userNameAttributeName)
            .build();
    }

    private static OAuth2Attributes ofNaver(String userNameAttributeName, Map<String, Object> attributes) {
        @SuppressWarnings("unchecked")
        Map<String, Object> response = (Map<String, Object>) attributes.get("response");
        
        return OAuth2Attributes.builder()
            .name((String) response.get("name"))
            .email((String) response.get("email"))
            .profileImageUrl((String) response.get("profile_image"))
            .providerId((String) response.get("id"))
            .provider(User.Provider.NAVER)
            .attributes(attributes)
            .nameAttributeKey(userNameAttributeName)
            .build();
    }

    private static OAuth2Attributes ofKakao(String userNameAttributeName, Map<String, Object> attributes) {
        @SuppressWarnings("unchecked")
        Map<String, Object> account = (Map<String, Object>) attributes.get("kakao_account");
        @SuppressWarnings("unchecked")
        Map<String, Object> profile = (Map<String, Object>) account.get("profile");
        
        String profileImageUrl = null;
        if (profile != null) {
            profileImageUrl = (String) profile.get("profile_image_url");
        }
        
        // 디버깅을 위한 로그
        log.info("Kakao account: {}", account);
        log.info("Kakao profile: {}", profile);
        
        return OAuth2Attributes.builder()
            .name((String) profile.get("nickname"))
            .email((String) account.get("email"))
            .profileImageUrl(profileImageUrl)
            .providerId(attributes.get("id").toString())
            .provider(User.Provider.KAKAO)
            .attributes(attributes)
            .nameAttributeKey(userNameAttributeName)
            .build();
    }
    
    private static OAuth2AttributesBuilder builder() {
        return new OAuth2AttributesBuilder();
    }
    
    private static class OAuth2AttributesBuilder {
        private Map<String, Object> attributes;
        private String nameAttributeKey;
        private String name;
        private String email;
        private String profileImageUrl;
        private String providerId;
        private User.Provider provider;

        OAuth2AttributesBuilder attributes(Map<String, Object> attributes) {
            this.attributes = attributes;
            return this;
        }

        OAuth2AttributesBuilder nameAttributeKey(String nameAttributeKey) {
            this.nameAttributeKey = nameAttributeKey;
            return this;
        }

        OAuth2AttributesBuilder name(String name) {
            this.name = name;
            return this;
        }

        OAuth2AttributesBuilder email(String email) {
            this.email = email;
            return this;
        }

        OAuth2AttributesBuilder profileImageUrl(String profileImageUrl) {
            this.profileImageUrl = profileImageUrl;
            return this;
        }

        OAuth2AttributesBuilder providerId(String providerId) {
            this.providerId = providerId;
            return this;
        }

        OAuth2AttributesBuilder provider(User.Provider provider) {
            this.provider = provider;
            return this;
        }

        OAuth2Attributes build() {
            return new OAuth2Attributes(attributes, nameAttributeKey, name, email, profileImageUrl, providerId, provider);
        }
    }
}