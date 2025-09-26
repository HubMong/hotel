package com.example.backend.authlogin.dto;

import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class RegisterRequest {
    private String name;
    private String email;
    private String password;
}
