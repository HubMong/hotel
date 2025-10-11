package com.example.backend.admin.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CustomerNotificationResponse {
    private String recipient;
    private String subject;
    private LocalDateTime sentAt;
    private boolean success;
    private String messageId;
}
