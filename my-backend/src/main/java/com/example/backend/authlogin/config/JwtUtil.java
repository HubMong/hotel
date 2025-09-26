package com.example.backend.authlogin.config;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import com.example.backend.authlogin.domain.User;

import javax.crypto.SecretKey;
import java.util.*;
import org.springframework.security.core.GrantedAuthority; // ★ 추가

@Slf4j
@Component
public class JwtUtil {

    @Value("${jwt.secret}")
    private String secret;
    
    @Value("${jwt.expiration}")
    private Long expiration;

    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(secret.getBytes());
    }

    // (1) 기존 generateToken(User) 유지: 하위 호환용
    public String generateToken(User user) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", user.getId());
        claims.put("email", user.getEmail());
        claims.put("name", user.getName());
        claims.put("provider", user.getProvider().toString());
        // ★ 권한이 없는 토큰이므로, 가급적 아래 (2) 오버로드로 대체 사용 권장
        log.debug("Generating JWT (no roles) for user: {}", user.getEmail());
        return createToken(claims, user.getEmail());
    }

    // (2) ★ 새 오버로드: email + 권한을 받아 roles 클레임에 넣는다
    public String generateToken(String email, Collection<? extends GrantedAuthority> authorities) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("email", email);
        // ★ "roles" 클레임 추가 (예: ["ROLE_USER","ROLE_ADMIN"])
        List<String> roles = authorities == null ? List.of()
                : authorities.stream().map(GrantedAuthority::getAuthority).toList();
        claims.put("roles", roles);
        log.debug("Generating JWT with roles for {}: {}", email, roles);
        return createToken(claims, email);
    }

    private String createToken(Map<String, Object> claims, String subject) {
        return Jwts.builder()
                .setClaims(claims)
                .setSubject(subject)
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis() + expiration))
                .signWith(getSigningKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    public Boolean validateToken(String token, String email) {
        try {
            final String extractedEmail = extractEmail(token);
            boolean isValid = extractedEmail.equals(email) && !isTokenExpired(token);
            log.debug("Token validation for {}: {}", email, isValid);
            return isValid;
        } catch (Exception e) {
            log.warn("Invalid token for email {}: {}", email, e.getMessage());
            return false;
        }
    }

    public String extractEmail(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    public Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    public <T> T extractClaim(String token, java.util.function.Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    private Boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    // (3) ★ 추가: roles 클레임 파서
    @SuppressWarnings("unchecked")
    public List<String> extractRoles(String token) {
        Claims claims = extractAllClaims(token);
        Object raw = claims.get("roles");
        if (raw instanceof Collection<?> c) {
            return c.stream().map(Object::toString).toList();
        }
        return List.of();
    }
}
