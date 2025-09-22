// src/main/java/com/example/backend/authlogin/config/SecurityConfig.java
package com.example.backend.authlogin.config;

import com.example.backend.authlogin.service.CustomOAuth2UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
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
            .cors(cors -> {})                          // CORS는 WebConfig에서 허용
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
                })
            )

.authorizeHttpRequests(auth -> auth
    .requestMatchers(
        // 정적/시스템 경로
        "/", "/index.html", "/favicon.ico", "/error",
        "/css/**", "/js/**", "/images/**", "/webjars/**",

        // 공개 API
        "/api/test/**",
        "/api/users/register",
        "/api/users/login",
        "/api/password/**",
        "/api/user/info",
        "/api/hotels/**",
        "/api/rooms/**",

        // OAuth2 콜백
        "/oauth2/**",
        "/login/oauth2/code/**"
    ).permitAll()
    // CORS preflight
    .requestMatchers(org.springframework.http.HttpMethod.OPTIONS, "/**").permitAll()
    .anyRequest().authenticated()
)

            .oauth2Login(oauth2 -> oauth2
                .userInfoEndpoint(u -> u.userService(customOAuth2UserService))
                .successHandler(oAuth2AuthenticationSuccessHandler)
            )

            // 🔐 JWT 필터를 UsernamePasswordAuthenticationFilter 앞에
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
