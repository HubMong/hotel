package com.example.backend.HotelOwner.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class OwnerDashboardDto {
    private long todaySales;
    private long weeklySales;
    private long monthlySales;
    private long todayCheckIns;
    private long todayCheckOuts;
}