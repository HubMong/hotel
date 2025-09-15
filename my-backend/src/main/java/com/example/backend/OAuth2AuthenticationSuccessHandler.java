package com.example.backend;

import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

@Component
@RequiredArgsConstructor
public class OAuth2AuthenticationSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final JwtUtil jwtUtil;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                      Authentication authentication) throws IOException {
        
        try {
            CustomOAuth2User oauth2User = (CustomOAuth2User) authentication.getPrincipal();
            User user = oauth2User.getUser();
            
            // 현재 로그인에 사용된 provider 정보 추출
            String currentProvider = extractProviderFromRequest(request);
            
            String token = jwtUtil.generateToken(user);
            
            // 프론트엔드로 리다이렉트하면서 토큰과 현재 로그인 provider 정보 전달
            String targetUrl = "http://localhost:5173/oauth2/redirect?token=" + token + "&provider=" + currentProvider;
            
            getRedirectStrategy().sendRedirect(request, response, targetUrl);
        } catch (Exception e) {
            System.err.println("OAuth2 성공 핸들러에서 에러 발생: " + e.getMessage());
            e.printStackTrace();
            throw e;
        }
    }
    
    private String extractProviderFromRequest(HttpServletRequest request) {
        String requestUri = request.getRequestURI();
        if (requestUri.contains("/naver")) {
            return "NAVER";
        } else if (requestUri.contains("/google")) {
            return "GOOGLE";
        } else if (requestUri.contains("/kakao")) {
            return "KAKAO";
        }
        return "LOCAL";
    }
}
