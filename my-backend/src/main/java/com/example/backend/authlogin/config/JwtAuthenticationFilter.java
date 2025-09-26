package com.example.backend.authlogin.config;

import java.io.IOException;
import java.util.List;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j // SLF4J 로거 사용
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {

        final String requestTokenHeader = request.getHeader("Authorization");

        String email = null;
        String jwtToken = null;

        // 1) Bearer 토큰 추출
        if (requestTokenHeader != null && requestTokenHeader.startsWith("Bearer ")) {
            jwtToken = requestTokenHeader.substring(7);
            try {
                email = jwtUtil.extractEmail(jwtToken);
            } catch (Exception e) {
                log.warn("Unable to parse JWT token", e);
            }
        } else if (requestTokenHeader == null) {
            log.trace("No Authorization header");
        } else {
            log.trace("Authorization header present but not Bearer");
        }

        // 2) SecurityContext 비어 있고, 이메일이 추출되었으면 검증/인증
        if (email != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            if (jwtUtil.validateToken(jwtToken, email)) {

                // 2-1) 토큰 roles 클레임을 읽어 권한 복원 (없으면 폴백)
                List<String> roleStrings;
                try {
                    roleStrings = jwtUtil.extractRoles(jwtToken); // JwtUtil에 구현 필요
                } catch (Exception e) {
                    log.debug("No roles claim or invalid format in JWT", e);
                    roleStrings = List.of();
                }

                List<SimpleGrantedAuthority> authorities = roleStrings == null || roleStrings.isEmpty()
                        ? List.of(new SimpleGrantedAuthority("ROLE_USER")) // 정책상 폴백 유지(필요 없으면 제거)
                        : roleStrings.stream()
                                     .map(r -> r.startsWith("ROLE_") ? r : "ROLE_" + r)
                                     .map(SimpleGrantedAuthority::new)
                                     .toList();

                // 2-2) 인증 토큰 생성 후 SecurityContext에 설정
                UsernamePasswordAuthenticationToken authToken =
                        new UsernamePasswordAuthenticationToken(email, null, authorities);
                authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authToken);

                log.debug("JWT authenticated: email={}, authorities={}", email, authorities);
            } else {
                log.debug("JWT invalid/expired for email={}", email);
            }
        }

        // 3) 다음 필터로 진행
        chain.doFilter(request, response);
    }
}
