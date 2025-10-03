package com.example.backend.admin.dto;

import com.example.backend.authlogin.domain.User;
import lombok.Builder;
import lombok.Value;

import java.time.LocalDateTime;

@Value
@Builder
public class UserAdminDto {
    Long id;
    String name;
    String email;
    String phone;
    String role;              // USER / ADMIN / BUSINESS
    String provider;          // LOCAL / NAVER / GOOGLE / KAKAO
    Boolean active;           // raw active flag
    String status;            // Derived: ACTIVE / INACTIVE
    LocalDateTime createdOn;  // registration date
    LocalDateTime lastLoginAt;

    public static UserAdminDto from(User u) {
        return UserAdminDto.builder()
                .id(u.getId())
                .name(u.getName())
                .email(u.getEmail())
                .phone(u.getPhone())
                .role(u.getRole() != null ? u.getRole().name() : null)
                .provider(u.getProvider() != null ? u.getProvider().name() : null)
                .active(u.getActive())
                .status(Boolean.TRUE.equals(u.getActive()) ? "ACTIVE" : "INACTIVE")
                .createdOn(u.getCreatedOn())
                .lastLoginAt(u.getLastLoginAt())
                .build();
    }
}
