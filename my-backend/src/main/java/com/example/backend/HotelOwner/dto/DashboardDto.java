package com.example.backend.HotelOwner.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class DashboardDto {

    // 매출액 정보
    private Long todaySales;
    private Long thisWeekSales;
    private Long thisMonthSales;

    // 전일, 전주, 전월 대비 변화율
    private double salesChangeVsYesterday;
    private double salesChangeVsLastWeek;
    private double salesChangeVsLastMonth;
}