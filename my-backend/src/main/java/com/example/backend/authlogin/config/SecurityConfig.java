// src/main/java/com/example/backend/authlogin/config/SecurityConfig.java
package com.example.backend.authlogin.config;

import com.example.backend.authlogin.service.CustomOAuth2UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import jakarta.servlet.http.HttpServletResponse;

@Configuration
@RequiredArgsConstructor
public class SecurityConfig {

    private final CustomOAuth2UserService customOAuth2UserService;
    private final OAuth2AuthenticationSuccessHandler oAuth2AuthenticationSuccessHandler;
    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .cors(cors -> {
                }) // CORS는 WebConfig에서 허용
                .csrf(csrf -> csrf.disable())
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .formLogin(f -> f.disable())
                .httpBasic(b -> b.disable())

                // ❗ API는 리다이렉트 말고 JSON 401/403
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint((req, res, e) -> {
                            res.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                            res.setContentType(MediaType.APPLICATION_JSON_VALUE);
                            res.getWriter().write("{\"error\":\"UNAUTHORIZED\"}");
                        })
                        .accessDeniedHandler((req, res, e) -> {
                            res.setStatus(HttpServletResponse.SC_FORBIDDEN);
                            res.setContentType(MediaType.APPLICATION_JSON_VALUE);
                            res.getWriter().write("{\"error\":\"FORBIDDEN\"}");
                        }))

                .authorizeHttpRequests(auth -> auth
    .requestMatchers(
        "/", "/index.html", "/favicon.ico", "/error",
        "/css/**", "/js/**", "/images/**", "/webjars/**",
        "/api/test/**",
        "/api/users/register",
        "/api/users/login",
        "/api/password/**",
        "/api/user/info",
        "/api/hotels/**",
        "/api/rooms/**",
        "/api/payments/**",
        "/oauth2/**",
        "/login/oauth2/code/**")
    .permitAll()
    // CORS preflight 허용
    .requestMatchers(org.springframework.http.HttpMethod.OPTIONS, "/**").permitAll()

    // ▼ 추가: 홀드 API는 인증 필요(또는 역할 필요)로 '명시'
    .requestMatchers(org.springframework.http.HttpMethod.POST, "/api/reservations/hold")
        .hasAnyRole("USER","ADMIN") // ← 단순 authenticated()도 가능, 역할 요구시 이렇게

                        // ▼▼▼ [추가] 마이페이지 수정 API는 인증된 사용자만 접근 가능하도록 명시 ▼▼▼
                        .requestMatchers(HttpMethod.PATCH, "/api/users/**").authenticated()

    .anyRequest().authenticated())
                .oauth2Login(oauth2 -> oauth2
                        .userInfoEndpoint(u -> u.userService(customOAuth2UserService))
                        .successHandler(oAuth2AuthenticationSuccessHandler))

                // 🔐 JWT 필터를 UsernamePasswordAuthenticationFilter 앞에
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
