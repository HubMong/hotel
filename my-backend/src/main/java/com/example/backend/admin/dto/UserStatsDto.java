package com.example.backend.admin.dto;

import java.util.List;
import java.util.Map;

// 두 용도의 DTO를 구분: SummaryForUser, OverallStats
public class UserStatsDto {
    // Overall stats fields
    private Long total;
    private Long admins;
    private Long business;
    private Long users;

    // User summary fields
    private Long totalReservations;
    private Long totalPayment;
    private Long totalCoupons;
    private List<Map<String, Object>> recentReservations;
    private List<Map<String, Object>> coupons;

    // Constructors for overall stats
    public UserStatsDto(long total, long admins, long business, long users) {
        this.total = total;
        this.admins = admins;
        this.business = business;
        this.users = users;
    }

    // Constructor for user summary
    public UserStatsDto(long totalReservations, long totalPayment, long totalCoupons, 
                       List<Map<String, Object>> recentReservations, 
                       List<Map<String, Object>> coupons) {
        this.totalReservations = totalReservations;
        this.totalPayment = totalPayment;
        this.totalCoupons = totalCoupons;
        this.recentReservations = recentReservations;
        this.coupons = coupons;
    }

    public Long getTotal() { return total; }
    public Long getAdmins() { return admins; }
    public Long getBusiness() { return business; }
    public Long getUsers() { return users; }

    public Long getTotalReservations() { return totalReservations; }
    public Long getTotalPayment() { return totalPayment; }
    public Long getTotalCoupons() { return totalCoupons; }
    public List<Map<String, Object>> getRecentReservations() { return recentReservations; }
    public List<Map<String, Object>> getCoupons() { return coupons; }
}
