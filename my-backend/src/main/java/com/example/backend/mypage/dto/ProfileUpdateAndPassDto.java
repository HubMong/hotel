package com.example.backend.mypage.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
// 이메일, 현재 비밀번호, 새 비밀번호를 모두 받기 위한 DTO
public class ProfileUpdateAndPassDto {
    private String email;
    private String currentPassword;
    private String newPassword;
}