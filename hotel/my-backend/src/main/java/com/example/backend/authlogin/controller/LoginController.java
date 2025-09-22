// src/main/java/com/example/backend/authlogin/controller/LoginController.java
package com.example.backend.authlogin.controller;

import com.example.backend.authlogin.config.JwtUtil;
import com.example.backend.authlogin.domain.User;
import com.example.backend.authlogin.dto.AuthResponse;
import com.example.backend.authlogin.dto.LoginRequest;
import com.example.backend.authlogin.dto.RegisterRequest;
import com.example.backend.authlogin.dto.SendCodeRequest;
import com.example.backend.authlogin.dto.VerifyAndChangeRequest;
import com.example.backend.authlogin.repository.LoginRepository;
import com.example.backend.authlogin.service.EmailService;
import com.example.backend.authlogin.service.LoginService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

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

    // 회원가입 → 토큰 즉시 발급
    @PostMapping("/users/register")
    public ResponseEntity<?> register(@RequestBody RegisterRequest req) {
        if (req.getName() == null || req.getName().isBlank())
            return ResponseEntity.badRequest().body("이름을 입력해주세요.");
        if (req.getEmail() == null || req.getEmail().isBlank())
            return ResponseEntity.badRequest().body("이메일을 입력해주세요.");
        if (req.getPassword() == null || req.getPassword().isBlank())
            return ResponseEntity.badRequest().body("비밀번호를 입력해주세요.");
        if (req.getPassword().length() < 6)
            return ResponseEntity.badRequest().body("비밀번호는 최소 6자 이상이어야 합니다.");

        try {
            User u = new User();
            u.setName(req.getName());
            u.setEmail(req.getEmail());
            u.setPassword(req.getPassword()); // 암호화는 Service에서 처리
            User saved = loginService.register(u);

            String token = jwtUtil.generateToken(saved);
            return ResponseEntity.ok(new AuthResponse(token, toUserInfo(saved)));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            log.error("register error", e);
            return ResponseEntity.internalServerError().body("회원가입 중 오류가 발생했습니다.");
        }
    }

    // 로그인(JSON)
    @PostMapping("/users/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest req) {
        if (req.getEmail() == null || req.getEmail().isBlank())
            return ResponseEntity.badRequest().body("이메일을 입력해주세요.");
        if (req.getPassword() == null || req.getPassword().isBlank())
            return ResponseEntity.badRequest().body("비밀번호를 입력해주세요.");

        var opt = loginService.login(req.getEmail().trim(), req.getPassword());
        if (opt.isPresent()) {
            var u = opt.get();
            String token = jwtUtil.generateToken(u);
            return ResponseEntity.ok(new AuthResponse(token, toUserInfo(u)));
        } else {
            return ResponseEntity.status(401).body("로그인 실패: 이메일 또는 비밀번호가 일치하지 않습니다.");
        }
    }

    // 토큰으로 유저 정보
    @GetMapping("/user/info")
    public ResponseEntity<?> getUserInfo(@RequestHeader("Authorization") String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer "))
            return ResponseEntity.badRequest().body("Invalid authorization header");

        String token = authHeader.substring(7);
        String email = jwtUtil.extractEmail(token);
        if (!jwtUtil.validateToken(token, email))
            return ResponseEntity.status(401).body("Token validation failed");

        return loginRepository.findByEmail(email)
                .<ResponseEntity<?>>map(u -> ResponseEntity.ok(toUserInfo(u)))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    // 비밀번호 재설정 - 인증코드 발송(JSON)
    @PostMapping("/password/reset/send-code")
    public ResponseEntity<?> sendPasswordResetCode(@RequestBody SendCodeRequest req) {
        String email = req.getEmail();
        if (email == null || email.trim().isEmpty() || !email.contains("@")) {
            return ResponseEntity.badRequest()
                    .body(Map.of("success", false, "message", "올바른 이메일 주소를 입력해주세요."));
        }
        String result = emailService.sendVerificationCode(email.trim());
        boolean ok = result.contains("발송");
        return ok
                ? ResponseEntity.ok(Map.of("success", true, "message", "인증코드가 발송되었습니다. 이메일을 확인해주세요."))
                : ResponseEntity.internalServerError().body(Map.of("success", false, "message", "이메일 발송 실패"));
    }

    // 비밀번호 재설정 - 코드 검증 및 변경(JSON)
    @PostMapping("/password/reset/verify-and-change")
    public ResponseEntity<?> verifyCodeAndChangePassword(@RequestBody VerifyAndChangeRequest req) {
        if (req.getEmail() == null || req.getEmail().isBlank())
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "이메일을 입력해주세요."));
        if (req.getVerificationCode() == null || req.getVerificationCode().isBlank())
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "인증코드를 입력해주세요."));
        if (req.getNewPassword() == null || req.getNewPassword().length() < 6)
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "새 비밀번호는 6자 이상이어야 합니다."));

        String msg = emailService.verifyCodeAndResetPassword(
                req.getEmail().trim(), req.getVerificationCode().trim(), req.getNewPassword());
        boolean ok = msg.contains("성공");
        return ok
                ? ResponseEntity.ok(Map.of("success", true, "message", "비밀번호가 성공적으로 변경되었습니다."))
                : ResponseEntity.badRequest().body(Map.of("success", false, "message", msg));
    }

    private Map<String, Object> toUserInfo(User u) {
        Map<String, Object> m = new HashMap<>();
        m.put("id", u.getId());
        m.put("name", u.getName());
        m.put("email", u.getEmail());
        m.put("provider", u.getProvider().toString());
        m.put("profileImageUrl", u.getProfileImageUrl());
        m.put("createdOn", u.getCreatedOn());
        return m;
    }
}
