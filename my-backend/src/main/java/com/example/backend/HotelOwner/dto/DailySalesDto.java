package com.example.backend.HotelOwner.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import java.time.LocalDate;

@Data
@AllArgsConstructor
public class DailySalesDto {
    private LocalDate date;
    private Long totalSales;
}