package com.example.backend.admin.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CustomerNotificationRequest {
    @NotBlank
    private String subject;

    @NotBlank
    private String body;

    private boolean includeReceipt;

    @Email
    private String overrideEmail;

    private boolean ccManager;
}
