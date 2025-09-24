package com.example.backend.authlogin.domain;

import java.time.LocalDateTime;

import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "users")
@Getter
@NoArgsConstructor
public class User {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;
    
    @Column(nullable = false, unique = true)
    private String email;

    private String phone;
    private String password;
    private String address;
    private String profileImageUrl;
    
    private String providerId;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Provider provider;
    
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
    
    public void setPassword(String password) { this.password = password; }
    public void setName(String name) { this.name = name; }
    public void setEmail(String email) { this.email = email; }
    public void setPhone(String phone) { this.phone = phone; }
    public void setAddress(String address) { this.address = address; }
    public void setProvider(Provider provider) { this.provider = provider; }
    public void setRole(Role role) { this.role = role; }
}
