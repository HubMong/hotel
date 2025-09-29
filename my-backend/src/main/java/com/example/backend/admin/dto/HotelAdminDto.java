package com.example.backend.admin.dto;

import com.example.backend.HotelOwner.domain.Hotel;
import com.example.backend.HotelOwner.domain.Room;
import lombok.Builder;
import lombok.Value;

import java.time.LocalDateTime;
import java.util.List;

@Value
@Builder
public class HotelAdminDto {
    Long id;
    String name;
    String address;
    String description;
    String country;
    Integer starRating;
    String status; // approval_status 문자열
    LocalDateTime approvalDate;
    Long approvedBy;
    String rejectionReason;
    LocalDateTime createdAt;

    // 사업자 정보
    String businessName;
    String businessEmail;
    String businessPhone;

    // 통계 정보
    Integer roomCount;
    Integer reservationCount;
    Double averageRating;
    Long totalRevenue;

    // 추가 정보
    String city;
    List<Room> rooms;

    /** rooms 없이 생성 (기존 시그니처 유지) */
    public static HotelAdminDto from(Hotel hotel, String businessName, String businessEmail, String businessPhone,
                                     Integer roomCount, Integer reservationCount, Double averageRating, Long totalRevenue) {
        return from(hotel, businessName, businessEmail, businessPhone, roomCount, reservationCount, averageRating, totalRevenue, null);
    }

    /** rooms 포함 생성 */
    public static HotelAdminDto from(Hotel hotel, String businessName, String businessEmail, String businessPhone,
                                     Integer roomCount, Integer reservationCount, Double averageRating, Long totalRevenue, List<Room> rooms) {
        return HotelAdminDto.builder()
                .id(hotel.getId())
                .name(hotel.getName())
                .address(hotel.getAddress())
                .description(hotel.getDescription())
                .country(hotel.getCountry())
                .starRating(hotel.getStarRating())
                .status(hotel.getApprovalStatus() != null ? hotel.getApprovalStatus().name() : "PENDING")
                .approvalDate(hotel.getApprovalDate())
                .approvedBy(hotel.getApprovedBy())
                .rejectionReason(hotel.getRejectionReason())
                .createdAt(hotel.getCreatedAt())
                .businessName(businessName != null ? businessName : "N/A")
                .businessEmail(businessEmail != null ? businessEmail : "N/A")
                .businessPhone(businessPhone != null ? businessPhone : "N/A")
                .city(extractCity(hotel.getAddress()))
                .roomCount(roomCount != null ? roomCount : 0)
                .reservationCount(reservationCount != null ? reservationCount : 0)
                .averageRating(averageRating != null ? averageRating : 0.0)
                .totalRevenue(totalRevenue != null ? totalRevenue : 0L)
                .rooms(rooms)
                .build();
    }

    private static String extractCity(String address) {
        if (address == null) return "N/A";
        // 간단한 도시 추출 로직 (예: "서울 강남구 언주로 640" -> "서울")
        String[] parts = address.split(" ");
        return parts.length > 0 ? parts[0] : "N/A";
    }
}
