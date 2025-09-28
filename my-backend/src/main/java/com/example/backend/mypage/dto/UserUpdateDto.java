package com.example.backend.mypage.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UserUpdateDto {
    private String email;
    // (만약 다른 필드도 수정한다면 여기에 추가)
}