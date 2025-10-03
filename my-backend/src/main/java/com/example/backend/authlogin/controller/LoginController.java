package com.example.backend.authlogin.controller;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import com.example.backend.authlogin.config.JwtUtil;
import com.example.backend.authlogin.domain.User;
import com.example.backend.authlogin.repository.LoginRepository;
import com.example.backend.authlogin.service.EmailService;
import com.example.backend.authlogin.service.LoginService;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
@CrossOrigin(origins = "http://localhost:5173")
public class LoginController {
    
    private final LoginService loginService;
    private final JwtUtil jwtUtil;
    private final LoginRepository loginRepository;
    private final EmailService emailService;
    
    // 회원가입
    @PostMapping("/users/register")
    public ResponseEntity<?> register(@RequestBody User user) {
        log.info("User registration attempt for email: {}", user.getEmail());
        log.info("User registration data - Name: {}, Email: {}, Phone: {}, Address: {}", 
                user.getName(), user.getEmail(), user.getPhone(), user.getAddress());
        
        // 필수 필드 검증
        if (user.getName() == null || user.getName().trim().isEmpty()) {
            log.warn("Registration failed - Name is required");
            return ResponseEntity.badRequest().body("이름을 입력해주세요.");
        }
        
        if (user.getEmail() == null || user.getEmail().trim().isEmpty()) {
            log.warn("Registration failed - Email is required");
            return ResponseEntity.badRequest().body("이메일을 입력해주세요.");
        }
        
        if (user.getPassword() == null || user.getPassword().trim().isEmpty()) {
            log.warn("Registration failed - Password is required");
            return ResponseEntity.badRequest().body("비밀번호를 입력해주세요.");
        }
        
        // 이메일 형식 검증
        if (!isValidEmail(user.getEmail())) {
            log.warn("Registration failed - Invalid email format: {}", user.getEmail());
            return ResponseEntity.badRequest().body("올바른 이메일 형식을 입력해주세요.");
        }
        
        // 비밀번호 최소 길이 검증
        if (user.getPassword().length() < 6) {
            log.warn("Registration failed - Password too short");
            return ResponseEntity.badRequest().body("비밀번호는 최소 6자 이상이어야 합니다.");
        }
        
        try {
            User savedUser = loginService.register(user);
            log.info("User registration successful - ID: {}, Email: {}", savedUser.getId(), savedUser.getEmail());
            return ResponseEntity.ok(savedUser);
        } catch (IllegalArgumentException e) {
            log.error("User registration failed for email: {} - {}", user.getEmail(), e.getMessage());
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            log.error("User registration failed for email: {} - Error: {}", user.getEmail(), e.getMessage(), e);
            return ResponseEntity.status(500).body("회원가입 중 오류가 발생했습니다.");
        }
    }

    // 로그인
    @PostMapping("/users/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request) {
        String email = request != null ? request.getEmail() : null;
        String password = request != null ? request.getPassword() : null;
        log.info("Login attempt for email: {}", email);

        if (email == null || email.trim().isEmpty()) {
            return ResponseEntity.badRequest().body("이메일을 입력해주세요.");
        }

        if (password == null || password.trim().isEmpty()) {
            return ResponseEntity.badRequest().body("비밀번호를 입력해주세요.");
        }

        try {
            Optional<User> user = loginService.login(email.trim(), password);

            if (user.isPresent()) {
                String token = jwtUtil.generateToken(user.get());
                Map<String, Object> response = new HashMap<>();
                response.put("token", token);
                response.put("user", getUserInfo(user.get()));
                log.info("Login successful for email: {}", email);
                return ResponseEntity.ok(response);
            } else {
                log.warn("Login failed for email: {} - Invalid credentials", email);
                return ResponseEntity.status(401).body("로그인 실패: 이메일 또는 비밀번호가 일치하지 않습니다.");
            }
        } catch (Exception e) {
            log.error("Login error for email: {} - {}", email, e.getMessage());
            return ResponseEntity.status(500).body("로그인 중 오류가 발생했습니다.");
        }
    }

    public static class LoginRequest {
        private String email;
        private String password;

        public String getEmail() {
            return email;
        }

        public void setEmail(String email) {
            this.email = email;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }
    }

    // 사용자 정보 조회 (JWT 토큰 기반)
    @GetMapping("/user/info")
    public ResponseEntity<?> getUserInfo(@RequestHeader("Authorization") String authHeader) {
        try {
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return ResponseEntity.badRequest().body("Invalid authorization header");
            }

            String token = authHeader.substring(7);
            String email = jwtUtil.extractEmail(token);
            
            if (!jwtUtil.validateToken(token, email)) {
                return ResponseEntity.status(401).body("Token validation failed");
            }

            User user = loginRepository.findByEmail(email)
                    .orElse(null);

            if (user == null) {
                return ResponseEntity.notFound().build();
            }

            return ResponseEntity.ok(getUserInfo(user));
            
        } catch (Exception e) {
            log.error("Error getting user info: {}", e.getMessage());
            return ResponseEntity.status(500).body("Internal server error");
        }
    }

    // 사용자 ID로 조회
    @GetMapping("/users/{id}")
    public ResponseEntity<User> getUser(@PathVariable Long id) {
        return loginService.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    private Map<String, Object> getUserInfo(User user) {
        Map<String, Object> userInfo = new HashMap<>();
        userInfo.put("id", user.getId());
        userInfo.put("name", user.getName());
        userInfo.put("email", user.getEmail());
        userInfo.put("provider", user.getProvider().toString());
        userInfo.put("profileImageUrl", user.getProfileImageUrl());
        userInfo.put("createdOn", user.getCreatedOn());
        if (user.getRole() != null) {
            userInfo.put("role", user.getRole().name());
        }
        return userInfo;
    }

    // 비밀번호 재설정 - 이메일 인증코드 발송
    @PostMapping("/password/reset/send-code")
    public ResponseEntity<?> sendPasswordResetCode(@RequestParam String email) {
        log.info("Password reset code request for email: {}", email);
        
        try {
            // 이메일 형식 검증
            if (email == null || email.trim().isEmpty() || !email.contains("@")) {
                return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "올바른 이메일 주소를 입력해주세요."
                ));
            }

            // 사용자 존재 여부 확인
            log.info("사용자 검색 시작: {}", email.trim());
            Optional<User> userOpt = loginRepository.findByEmail(email.trim());
            log.info("사용자 검색 결과: {}", userOpt.isPresent());
            
            if (userOpt.isEmpty()) {
                log.warn("존재하지 않는 사용자 이메일: {}", email.trim());
                // 보안상 실제로는 존재하지 않는 이메일이어도 성공 메시지 반환
                return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "인증코드가 발송되었습니다. 이메일을 확인해주세요."
                ));
            }

            // 소셜 로그인 사용자는 비밀번호 재설정 불가
            User user = userOpt.get();
            log.info("사용자 정보 확인 - Provider: {}, Email: {}", user.getProvider(), user.getEmail());
            if (user.getProvider() != User.Provider.LOCAL) {
                return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "소셜 로그인 계정은 비밀번호 재설정이 불가능합니다."
                ));
            }

            // 인증코드 발송
            log.info("EmailService.sendVerificationCode 호출 직전: {}", email.trim());
            String result = emailService.sendVerificationCode(email.trim());
            log.info("EmailService.sendVerificationCode 호출 결과: {}", result);
            
            if (result.contains("발송되었습니다")) {
                return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "인증코드가 발송되었습니다. 이메일을 확인해주세요."
                ));
            } else {
                return ResponseEntity.internalServerError().body(Map.of(
                    "success", false,
                    "message", "이메일 발송에 실패했습니다. 잠시 후 다시 시도해주세요."
                ));
            }

        } catch (Exception e) {
            log.error("Password reset code sending failed for email: {} - {}", email, e.getMessage());
            return ResponseEntity.internalServerError().body(Map.of(
                "success", false,
                "message", "서버 오류가 발생했습니다. 잠시 후 다시 시도해주세요."
            ));
        }
    }

    // 비밀번호 재설정 - 인증코드 검증 및 비밀번호 변경
    @PostMapping("/password/reset/verify-and-change")
    public ResponseEntity<?> verifyCodeAndChangePassword(
            @RequestParam String email,
            @RequestParam String verificationCode,
            @RequestParam String newPassword) {
        
        log.info("Password reset verification for email: {}", email);
        
        try {
            // 입력값 검증
            if (email == null || email.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "이메일을 입력해주세요."
                ));
            }

            if (verificationCode == null || verificationCode.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "인증코드를 입력해주세요."
                ));
            }

            if (newPassword == null || newPassword.length() < 6) {
                return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "새 비밀번호는 6자 이상이어야 합니다."
                ));
            }

            // 인증코드 검증 및 비밀번호 재설정
            String resetResult = emailService.verifyCodeAndResetPassword(email.trim(), verificationCode.trim(), newPassword);
            if (resetResult.contains("성공적으로")) {
                log.info("Password successfully changed for user: {}", email);
                return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "비밀번호가 성공적으로 변경되었습니다."
                ));
            } else {
                return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", resetResult
                ));
            }

        } catch (Exception e) {
            log.error("Password reset failed for email: {} - {}", email, e.getMessage());
            return ResponseEntity.internalServerError().body(Map.of(
                "success", false,
                "message", "서버 오류가 발생했습니다. 잠시 후 다시 시도해주세요."
            ));
        }
    }
    
    // 이메일 형식 검증 메소드
    private boolean isValidEmail(String email) {
        if (email == null || email.trim().isEmpty()) {
            return false;
        }
        
        String emailRegex = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$";
        return email.matches(emailRegex);
    }
}
