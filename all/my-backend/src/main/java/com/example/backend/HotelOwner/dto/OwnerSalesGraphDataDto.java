package com.example.backend.HotelOwner.dto;

import lombok.Builder;
import lombok.Value;

import java.util.List;

@Value
@Builder
public class OwnerSalesGraphDataDto {
    List<DataPoint> dataPoints;

    @Value
    @Builder
    public static class DataPoint {
        String label; // 날짜 또는 월
        Long value; // 매출액
    }
}