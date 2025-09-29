package com.example.backend.authlogin.config;

import com.example.backend.authlogin.service.CustomOAuth2UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@RequiredArgsConstructor
public class SecurityConfig {

    private final CustomOAuth2UserService customOAuth2UserService;
    private final OAuth2AuthenticationSuccessHandler oAuth2AuthenticationSuccessHandler;
    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    // 공통 CORS
    @Bean
    CorsConfigurationSource corsSource() {
        CorsConfiguration c = new CorsConfiguration();
        c.setAllowedOrigins(List.of("http://localhost:5173"));
        c.setAllowedMethods(List.of("GET","POST","PUT","DELETE","OPTIONS","PATCH"));
        c.setAllowedHeaders(List.of("*"));
        c.setAllowCredentials(true);
        UrlBasedCorsConfigurationSource s = new UrlBasedCorsConfigurationSource();
        s.registerCorsConfiguration("/**", c);
        return s;
    }

    // ---------- (1) API 전용 체인 ----------
    @Bean @Order(1)
    public SecurityFilterChain apiChain(HttpSecurity http) throws Exception {
        http
            // 이 체인은 오직 /api/** 만 매칭
            .securityMatcher("/api/**")
            .cors(c -> c.configurationSource(corsSource()))
            .csrf(csrf -> csrf.disable())
            .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(HttpMethod.OPTIONS, "/api/**").permitAll()
                .requestMatchers("/api/admin/health/**").permitAll() // 헬스체크는 public
                .requestMatchers("/api/admin/test").permitAll() // 테스트는 public
                .requestMatchers("/api/admin/**").hasRole("ADMIN")
                // Public auth endpoints for API
                .requestMatchers(HttpMethod.POST, "/api/users/register", "/api/users/login").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/admins/register").permitAll()
                .requestMatchers("/api/password/**", "/api/test/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/hotels", "/api/hotels/**").permitAll() // ← 공개 조회
                
                // 💡 [추가] PATCH /api/users/** 경로를 명시적으로 authenticated() 처리합니다.
                // 이는 .anyRequest().authenticated()보다 먼저 실행되어 Spring Security가 올바른 권한 검사를 수행하게 합니다.
                .requestMatchers(HttpMethod.PATCH, "/api/users/**").authenticated() 

                .anyRequest().authenticated()
            )
            // ★ 로그인 페이지로 리다이렉트 금지 → 401 반환
            .exceptionHandling(ex -> ex.authenticationEntryPoint(
                new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)))
            // ★ API 체인에서는 모든 로그인 메커니즘 비활성화
            .formLogin(form -> form.disable())
            .httpBasic(basic -> basic.disable())
            .oauth2Login(oauth2 -> oauth2.disable())
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    // ---------- (2) 웹(화면) 체인 ----------
    @Bean @Order(2)
    public SecurityFilterChain webChain(HttpSecurity http) throws Exception {
        http
            .cors(c -> c.configurationSource(corsSource()))
            .csrf(csrf -> csrf.disable())
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(
                    "/", "/login",
                    "/oauth2/**", "/login/oauth2/code/**",
                    "/css/**", "/js/**", "/images/**", "/public/**",
                    // (필요 시) 회원가입/비번 재설정 같은 공개 API
                    "/api/users/register", "/api/users/login",
                    "/api/user/info", "/api/password/**", "/api/test/**"
                ).permitAll()
                .anyRequest().authenticated()
            )
            .formLogin(form -> form.disable())
            .httpBasic(basic -> basic.disable())
            .oauth2Login(oauth2 -> oauth2
                .userInfoEndpoint(u -> u.userService(customOAuth2UserService))
                .successHandler(oAuth2AuthenticationSuccessHandler)
            );
        return http.build();
    }
}
