package com.example.backend.admin.dto;

import lombok.Builder;
import lombok.Value;

import java.util.List;

@Value
@Builder
public class PaymentAnalyticsDto {
    List<TimeBucket> byPeriod;
    List<CategoryBucket> byHotel;
    List<CategoryBucket> byMethod;

    @Value
    @Builder
    public static class TimeBucket {
        String period;
        long amount;
        long count;
    }

    @Value
    @Builder
    public static class CategoryBucket {
        String label;
        long amount;
        long count;
    }


}

