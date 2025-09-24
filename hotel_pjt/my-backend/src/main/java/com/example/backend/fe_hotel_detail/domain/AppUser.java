// AppUser.java (최소)
package com.example.backend.fe_hotel_detail.domain;

import com.example.backend.fe_hotel_detail.domain.AppUser;
import jakarta.persistence.*;
import lombok.Getter;

@Entity @Getter
public class AppUser {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable=false,length=50) private String name;
    @Column(nullable=false,length=100) private String email;
    @Enumerated(EnumType.STRING) @Column(nullable=false)
    private Role role;
    public enum Role { USER, ADMIN, BUSINESS }
}
