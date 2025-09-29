package com.example.backend.admin.dto;

import com.example.backend.authlogin.domain.User;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RoleUpdateRequest {
    private User.Role role;
}
