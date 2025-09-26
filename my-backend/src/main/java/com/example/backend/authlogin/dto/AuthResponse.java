package com.example.backend.authlogin.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import java.util.Map;

@Getter @Setter
@AllArgsConstructor
public class AuthResponse {
    private String token;
    private Map<String, Object> user;
}
