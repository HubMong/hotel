package com.example.backend.authlogin.controller;

import com.example.backend.admin.dto.ApiResponse;
import com.example.backend.admin.repository.AdminUserRepository;
import com.example.backend.authlogin.domain.User;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/admins")
@RequiredArgsConstructor
public class AdminRegistrationController {
    private final AdminUserRepository userRepository;
    private final BCryptPasswordEncoder passwordEncoder;

    public static record AdminRegisterRequest(
        String name,
        String email,
        String password,
        String phone,
        String address,
        @JsonFormat(pattern = "yyyy-MM-dd") LocalDate dateOfBirth
    ) {}

    public static record AdminUserResponse(
        Long id,
        String name,
        String email,
        String phone,
        String address,
        LocalDate dateOfBirth,
        User.Role role
    ) {
    static AdminUserResponse from(User u) {
        return new AdminUserResponse(
            u.getId(),
            u.getName(),
            u.getEmail(),
            u.getPhone(),
            u.getAddress(),
            u.getDateOfBirth(),
            u.getRole()
        );
    }
    }

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<AdminUserResponse>> register(@RequestBody AdminRegisterRequest req) {
        // 기본 유효성 검사
        if (req.name() == null || req.name().isBlank()) {
            return ResponseEntity.badRequest().body(ApiResponse.fail("Name is required"));
        }
        if (req.email() == null || req.email().isBlank()) {
            return ResponseEntity.badRequest().body(ApiResponse.fail("Email is required"));
        }
        if (req.password() == null || req.password().length() < 6) {
            return ResponseEntity.badRequest().body(ApiResponse.fail("Password must be at least 6 characters"));
        }
        if (userRepository.findByEmail(req.email()).isPresent()) {
            return ResponseEntity.badRequest().body(ApiResponse.fail("Email already in use"));
        }

        User u = User.builder()
                .name(req.name())
                .email(req.email())
                .password(passwordEncoder.encode(req.password()))
                .provider(User.Provider.LOCAL)
                .profileImageUrl(null)
                .build();
        u.setPhone(req.phone());
        u.setAddress(req.address());
        u.setDateOfBirth(req.dateOfBirth());
        u.setRole(User.Role.ADMIN);
        u.setActive(true);
        User saved = userRepository.save(u);
        return ResponseEntity.ok(ApiResponse.ok(AdminUserResponse.from(saved)));
    }
}
