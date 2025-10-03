package com.example.backend.authlogin.config;

import java.io.IOException;

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
import org.springframework.lang.NonNull;

import com.example.backend.admin.repository.AdminUserRepository;
import com.example.backend.authlogin.domain.User;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final AdminUserRepository userRepository;

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response, @NonNull FilterChain chain)
            throws ServletException, IOException {

        final String requestTokenHeader = request.getHeader("Authorization");

        String email = null;
        String jwtToken = null;

        // 1. Authorization 헤더에서 토큰 추출
        if (requestTokenHeader != null && requestTokenHeader.startsWith("Bearer ")) {
            jwtToken = requestTokenHeader.substring(7);
            try {
                email = jwtUtil.extractEmail(jwtToken);
            } catch (Exception e) {
                logger.warn("Unable to get JWT Token from Authorization header");
            }
        }
        
        // 2. 쿼리 파라미터에서 토큰 추출 (SSE를 위해)
        if (jwtToken == null) {
            String tokenParam = request.getParameter("token");
            if (tokenParam == null) tokenParam = request.getParameter("access_token");
            if (tokenParam == null) tokenParam = request.getParameter("auth");
            
            if (tokenParam != null && !tokenParam.isBlank()) {
                jwtToken = tokenParam;
                try {
                    email = jwtUtil.extractEmail(jwtToken);
                } catch (Exception e) {
                    logger.warn("Unable to get JWT Token from query parameter");
                }
            }
        }

        if (email != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            if (jwtUtil.validateToken(jwtToken, email)) {
                User user = userRepository.findByEmail(email).orElse(null);
                if (user != null && user.getRole() != null) {
                    var authorities = java.util.List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name()));
                    UsernamePasswordAuthenticationToken auth =
                            new UsernamePasswordAuthenticationToken(email, null, authorities);
                    auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(auth);
                }
            }
        }
        chain.doFilter(request, response);
    }
}