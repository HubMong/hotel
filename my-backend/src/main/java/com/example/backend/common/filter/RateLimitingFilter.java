package com.example.backend.common.filter;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

/**
 * 간단한 IP 기반 요청 레이트 리미터.
 * 새로고침/짧은 시간 내 반복 호출이 일정 횟수를 초과하면 429를 반환한다.
 * 1분에 10개 요청 제한
 * 
 * Spring Security보다 먼저 실행되도록 HIGHEST_PRECEDENCE 설정
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RateLimitingFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(RateLimitingFilter.class);
    private static final Logger suspiciousLog = LoggerFactory.getLogger("SUSPICIOUS");

    // 1분에 100개 요청으로 제한 (기존 10개에서 상향)
    private static final Bandwidth LIMIT = Bandwidth.builder()
        .capacity(100)
        .refillGreedy(100, Duration.ofMinutes(1))
        .build();

    private final Cache<String, Bucket> bucketCache = Caffeine.newBuilder()
        .maximumSize(10_000)
        .expireAfterAccess(Duration.ofMinutes(10))
        .build();

    @Override
    protected boolean shouldNotFilter(@NonNull HttpServletRequest request) {
        String uri = request.getRequestURI();
        
        // OAuth2, 업로드, 정적 리소스는 필터 건너뛰기
        if (uri.startsWith("/api/oauth2") ||
            uri.startsWith("/oauth2") ||
            uri.startsWith("/login/oauth2") ||
            uri.startsWith("/uploads") ||
            uri.startsWith("/assets") ||
            uri.startsWith("/actuator") || 
            uri.startsWith("/favicon.ico") ||
            uri.endsWith(".js") ||
            uri.endsWith(".css") ||
            uri.endsWith(".png") ||
            uri.endsWith(".jpg") ||
            uri.endsWith(".ico")) {
            log.debug("Skipping rate limit for: {}", uri);
            return true;
        }
        // 사전 요청(OPTIONS)은 제한하지 않는다.
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            log.debug("Skipping rate limit for OPTIONS request");
            return true;
        }
        return false;
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response, @NonNull FilterChain filterChain)
        throws ServletException, IOException {

        String clientIp = resolveClientIp(request);
        String uri = request.getRequestURI();
        String method = request.getMethod();
        String bucketKey = buildBucketKey(clientIp, method, uri);

        log.info("=== Rate Limit Check === IP: {}, Method: {}, URI: {}", clientIp, method, uri);

        Bucket bucket = bucketCache.get(bucketKey, key -> {
            log.info("Creating new rate limit bucket for key: {}", key);
            return Bucket.builder().addLimit(LIMIT).build();
        });

        long availableTokens = bucket.getAvailableTokens();
        if (bucket.tryConsume(1)) {
            long remaining = availableTokens - 1;
            log.info("✅ Request ALLOWED - key: {}, Remaining: {}/10", bucketKey, remaining);
            response.setHeader("X-RateLimit-Remaining", String.valueOf(remaining));
            response.setHeader("X-RateLimit-Limit", "100");
            filterChain.doFilter(request, response);
            return;
        }

        log.warn("❌ Rate limit EXCEEDED - key: {}", bucketKey);
        suspiciousLog.warn("Rate limit exceeded key={}", bucketKey);

        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setHeader(HttpHeaders.RETRY_AFTER, "60");
        response.setHeader("X-RateLimit-Remaining", "0");
        response.setHeader("X-RateLimit-Limit", "100");
        response.getWriter().write("{\"status\":\"error\",\"message\":\"요청이 너무 많습니다. 1분 후 다시 시도해주세요.\"}");
    }

    private String resolveClientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            int commaIndex = forwarded.indexOf(',');
            return commaIndex > 0 ? forwarded.substring(0, commaIndex).trim() : forwarded.trim();
        }
        return request.getRemoteAddr();
    }

    private String buildBucketKey(String clientIp, String method, String uri) {
        return clientIp + '|' + method + '|' + uri;
    }
}
