    package com.example.backend;

    import java.security.Key;
    import java.util.Date;

    import org.springframework.stereotype.Component;

    import io.jsonwebtoken.Jwts;
    import io.jsonwebtoken.SignatureAlgorithm;
    import io.jsonwebtoken.security.Keys;

    @Component
    public class JwtUtil {
        
        private final Key key = Keys.secretKeyFor(SignatureAlgorithm.HS256);
        private final long EXPIRATION_TIME = 1000 * 60 * 60; // 1Hour

        public String generateToken(String username) {
            return Jwts.builder()
                    .setSubject(username)
                    .setExpiration(new Date(System.currentTimeMillis() + EXPIRATION_TIME))
                    .signWith(key)
                    .compact();
        }

        public String getUsername(String token) {
            return Jwts.parserBuilder()
                    .setSigningKey(key)
                    .build()
                    .parseClaimsJws(token)
                    .getBody()
                    .getSubject();
        }

        public boolean validateToken(String token) {
            try {
                Jwts.parserBuilder()
                    .setSigningKey(key)
                    .build()
                    .parseClaimsJws(token); // 토큰을 파싱하면서 검증 (서명 + 만료일 확인)
                return true;
            } catch (io.jsonwebtoken.ExpiredJwtException e) {
                System.out.println("Token expired: " + e.getMessage());
            } catch (io.jsonwebtoken.MalformedJwtException e) {
                System.out.println("Invalid JWT token: " + e.getMessage());
            } catch (IllegalArgumentException e) {
                System.out.println("Token is null or empty: " + e.getMessage());
            }
            return false;
        }
    }
