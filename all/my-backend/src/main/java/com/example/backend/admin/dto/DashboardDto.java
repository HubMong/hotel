package com.example.backend.admin.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@AllArgsConstructor
@Builder
public class DashboardDto {
    private long totalUsers;
    private long totalBusinesses;
    private long totalHotels;
    private long totalReservations;
    private long totalPayments;
    private long totalReviews;
    private long totalCoupons;
    private long pendingInquiries;
    private long recentRevenue;
    private long totalRevenue;
    private long todayReservations;
}
