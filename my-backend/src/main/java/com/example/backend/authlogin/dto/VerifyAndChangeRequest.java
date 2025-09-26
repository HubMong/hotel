package com.example.backend.authlogin.dto;

import lombok.Data;

@Data
public class VerifyAndChangeRequest {
    private String email;
    private String verificationCode;
    private String newPassword;
}
