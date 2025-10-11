package com.example.backend.admin.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentExportRequest {

    public enum Scope {
        PAGE,
        ALL,
        SELECTION
    }

    @NotBlank
    private String format; // csv, excel, pdf

    @NotNull
    private Scope scope;

    private List<Long> selectedIds;

    private List<String> columns;

    @Valid
    private PaymentFilterDto filters;

    private boolean async;
}
