package com.example.backend;

import java.time.LocalDateTime;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "users")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;
    
    @Column(nullable = false, unique = true)
    private String email;

    private String phone;
    private String password; // 소셜 로그인 사용자는 null
    private String address;
    private String profileImageUrl;
    
    // 소셜 로그인 관련 필드
    private String providerId; // 소셜 플랫폼에서 제공하는 고유 ID
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Provider provider;
    
    // 연결된 소셜 계정들 (JSON 형태)
    @Column(columnDefinition = "TEXT")
    private String socialProviders;
    
    @Column(nullable = false)
    private LocalDateTime createdOn;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    public enum Role {
        USER, ADMIN
    }

    public enum Provider {
        LOCAL, NAVER, GOOGLE, KAKAO
    }

    @Builder
    public User(String name, String email, String password, String providerId, Provider provider, String profileImageUrl) {
        this.name = name;
        this.email = email;
        this.password = password;
        this.providerId = providerId;
        this.provider = provider;
        this.profileImageUrl = profileImageUrl;
        this.role = Role.USER;
        this.createdOn = LocalDateTime.now();
        this.socialProviders = "{}";
    }

    @PrePersist
    protected void onCreate() {
        if (createdOn == null) createdOn = LocalDateTime.now();
        if (role == null) role = Role.USER;
        if (provider == null) provider = Provider.LOCAL;
        if (socialProviders == null) socialProviders = "{}";
    }
    
    public User update(String name, String profileImageUrl) {
        this.name = name;
        if (profileImageUrl != null) {
            this.profileImageUrl = profileImageUrl;
        }
        return this;
    }
    
    // 소셜 계정 통합 메서드
    public void mergeSocialProvider(Provider provider, String providerId) {
        if (this.socialProviders.equals("{}")) {
            this.socialProviders = "{\"" + provider.name() + "\":\"" + providerId + "\"}";
        } else {
            String newEntry = ",\"" + provider.name() + "\":\"" + providerId + "\"";
            this.socialProviders = this.socialProviders.substring(0, this.socialProviders.length() - 1) + newEntry + "}";
        }
    }
    
    public boolean hasSocialProvider(Provider provider) {
        return this.socialProviders != null && 
               this.socialProviders.contains("\"" + provider.name() + "\":");
    }
    
    // 패스워드 설정 메서드 (회원가입용)
    public void setPassword(String password) {
        this.password = password;
    }
}
