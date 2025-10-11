package com.example.backend.admin.dto;

import com.example.backend.HotelOwner.domain.Hotel;
import com.example.backend.HotelOwner.domain.Room;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

public record HotelAdminDto(
    Long id,
    String name,
    String address,
    String description,
    String country,
    Integer starRating,
    String status,
    LocalDateTime approvalDate,
    Long approvedBy,
    String rejectionReason,
    LocalDateTime createdAt,
    // 사업자 정보
    String businessName,
    String businessEmail,
    String businessPhone,
    String businessNumber, // [수정] 사업자 번호 필드 추가
    // 통계 정보
    Integer roomCount,
    Integer reservationCount,
    Double averageRating,
    Long totalRevenue,
    // 상세 정보
    List<Room> rooms
) {

    public static HotelAdminDto from(Hotel hotel, String businessName, String businessEmail, String businessPhone, String businessNumber, int roomCount, int reservationCount, double averageRating, long totalRevenue) {
        return new HotelAdminDto(
                hotel.getId(), hotel.getName(), hotel.getAddress(), hotel.getDescription(), hotel.getCountry(),
                hotel.getStarRating(), hotel.getApprovalStatus() != null ? hotel.getApprovalStatus().name() : null,
                hotel.getApprovalDate(), hotel.getApprovedBy(), hotel.getRejectionReason(), hotel.getCreatedAt(),
                businessName, businessEmail, businessPhone, businessNumber, // [수정] 사업자 번호 추가
                roomCount, reservationCount, averageRating, totalRevenue,
                Collections.emptyList() // 간단 조회 시에는 객실 목록 비움
        );
    }

    public static HotelAdminDto from(Hotel hotel, String businessName, String businessEmail, String businessPhone, String businessNumber, int roomCount, int reservationCount, double averageRating, long totalRevenue, List<Room> rooms) {
        return new HotelAdminDto(
                hotel.getId(), hotel.getName(), hotel.getAddress(), hotel.getDescription(), hotel.getCountry(),
                hotel.getStarRating(), hotel.getApprovalStatus() != null ? hotel.getApprovalStatus().name() : null,
                hotel.getApprovalDate(), hotel.getApprovedBy(), hotel.getRejectionReason(), hotel.getCreatedAt(),
                businessName, businessEmail, businessPhone, businessNumber, // [수정] 사업자 번호 추가
                roomCount, reservationCount, averageRating, totalRevenue,
                rooms
        );
    }
}