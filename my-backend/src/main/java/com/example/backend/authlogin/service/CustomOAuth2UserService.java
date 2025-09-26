package com.example.backend.authlogin.service;

import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

import com.example.backend.authlogin.config.OAuth2Attributes;
import com.example.backend.authlogin.domain.User;
import com.example.backend.authlogin.repository.LoginRepository;

@Slf4j
@Service
@RequiredArgsConstructor
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private final LoginRepository loginRepository;

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        try {
            OAuth2User oauth2User = super.loadUser(userRequest);
            
            String registrationId = userRequest.getClientRegistration().getRegistrationId();
            String userNameAttributeName = userRequest.getClientRegistration()
                    .getProviderDetails().getUserInfoEndpoint().getUserNameAttributeName();

            OAuth2Attributes attributes = OAuth2Attributes.of(registrationId, userNameAttributeName, oauth2User.getAttributes());
            User user = saveOrUpdate(attributes);
            
            log.info("OAuth2 사용자 로드 완료: {} ({})", user.getEmail(), user.getProvider());
            return new CustomOAuth2User(user, oauth2User.getAttributes(), userNameAttributeName);
            
        } catch (Exception e) {
            log.error("OAuth2 사용자 로드 중 오류 발생: {}", e.getMessage(), e);
            throw new OAuth2AuthenticationException("사용자 정보 로드 실패: " + e.getMessage());
        }
    }

    private User saveOrUpdate(OAuth2Attributes attributes) {
        List<User> existingUsers = loginRepository.findAllByEmail(attributes.getEmail());
        
        if (!existingUsers.isEmpty()) {
            User mainUser = existingUsers.get(0);
            
            // 중복 계정 통합 및 소셜 계정 추가
            for (User duplicateUser : existingUsers) {
                if (!duplicateUser.getId().equals(mainUser.getId())) {
                    mainUser.mergeSocialProvider(duplicateUser.getProvider(), duplicateUser.getProviderId());
                    loginRepository.delete(duplicateUser);
                    log.info("중복 계정 통합: {} 계정을 메인 계정에 병합", duplicateUser.getProvider());
                }
            }
            
            // 새로운 소셜 계정 추가
            if (!mainUser.hasSocialProvider(attributes.getProvider())) {
                mainUser.mergeSocialProvider(attributes.getProvider(), attributes.getProviderId());
                log.info("새 소셜 계정 추가: {} -> {}", attributes.getProvider(), mainUser.getEmail());
            }
            
            // 사용자 정보 업데이트
            mainUser.update(attributes.getName(), attributes.getProfileImageUrl());
            return loginRepository.save(mainUser);
            
        } else {
            // 새 사용자 생성
            User newUser = User.builder()
                    .name(attributes.getName())
                    .email(attributes.getEmail())
                    .providerId(attributes.getProviderId())
                    .provider(attributes.getProvider())
                    .profileImageUrl(attributes.getProfileImageUrl())
                    .build();
                    
            User savedUser = loginRepository.save(newUser);
            log.info("새 사용자 생성: {} ({})", savedUser.getEmail(), savedUser.getProvider());
            return savedUser;
        }
    }
}