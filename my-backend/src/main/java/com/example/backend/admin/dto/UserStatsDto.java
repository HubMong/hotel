package com.example.backend.admin.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class UserStatsDto {
    private long total;
    private long admins;
    private long business;
    private long users;
}
