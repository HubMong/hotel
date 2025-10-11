package com.example.backend.admin.event;

import java.time.LocalDateTime;

public record UserLoginEvent(Long userId, String email, String name, LocalDateTime loginAt) {}
