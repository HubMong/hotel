package com.example.backend.admin.controller;

import com.example.backend.admin.service.AdminEventBus;
import com.example.backend.admin.repository.AdminUserRepository;
import com.example.backend.authlogin.config.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/api/admin/events")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:5173")
public class AdminEventController {
    private final AdminEventBus bus;
    private final JwtUtil jwtUtil;
    private final AdminUserRepository userRepository;

    @GetMapping("/sse")
    public ResponseEntity<SseEmitter> stream(
            @RequestHeader(value = "X-Client-Id", required = false) String id,
            @RequestParam(value = "token", required = false) String token,
            @RequestParam(value = "access_token", required = false) String accessToken,
            @RequestParam(value = "auth", required = false) String auth
    ) {
        String raw = token;
        if (raw == null || raw.isBlank()) raw = accessToken;
        if (raw == null || raw.isBlank()) raw = auth;
        if (raw == null || raw.isBlank()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        try {
            String email = jwtUtil.extractEmail(raw);
            if (!jwtUtil.validateToken(raw, email)) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }
            var user = userRepository.findByEmail(email).orElse(null);
            if (user == null || user.getRole() != com.example.backend.authlogin.domain.User.Role.ADMIN) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        String clientId = (id == null || id.isBlank()) ? java.util.UUID.randomUUID().toString() : id;
        return ResponseEntity.ok(bus.subscribe(clientId));
    }
}
