package com.example.backend.hotel_reservation.dto;

import lombok.*;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.List;

import com.example.backend.HotelOwner.domain.Room;
import com.example.backend.authlogin.domain.User;
import com.example.backend.hotel_reservation.domain.Reservation;
import com.example.backend.hotel_reservation.domain.ReservationStatus;

public class ReservationDtos {

    // 요청 DTO (역직렬화용)
    @Getter @Setter
    @NoArgsConstructor @AllArgsConstructor
    public static class HoldRequest {
        private Long userId;
        private Long roomId;
        private Integer qty;          // 예약 객실 수
        private String checkIn;       // 'YYYY-MM-DD'
        private String checkOut;      // 'YYYY-MM-DD'
        private Integer adults;       // optional
        private Integer children;     // optional
        private Integer holdSeconds;  // optional, default 30
    }

    // 응답 DTO
    @Getter @Builder
    @NoArgsConstructor @AllArgsConstructor
    public static class HoldResponse {
        private Long reservationId;
        private Instant expiresAt;
        private String status; // PENDING
    }

    @Getter @Builder
    @NoArgsConstructor @AllArgsConstructor
    public static class ReservationDetail {
        private Long id;
        private String status;     // "PENDING"/"COMPLETED"/"CANCELLED"
        private Instant expiresAt;
        private Long hotelId;
        private Long userId;
        private Long roomId;
        private Integer numRooms;
        private Integer adults;
        private Integer children;
        private Instant startDate; // ISO
        private Instant endDate;   // ISO
    }

    @Getter @Builder
    @NoArgsConstructor @AllArgsConstructor
    public static class ReservationSummary {
        private Long id;
        private String status;     // "PENDING"/"COMPLETED"/"CANCELLED"
        private Long userId;
        private Long roomId;
        private Long hotelId;
        private Integer numRooms;
        private Integer adults;
        private Integer children;
        private Instant startDate; // ISO
        private Instant endDate;   // ISO
    }

    @Getter
    @NoArgsConstructor
    public static class OwnerReservationResponse {
        private Long id;
        private String guestName;
        private String guestPhone;
        private String hotelName;
        private String roomType;
        private LocalDate checkInDate;
        private LocalDate checkOutDate;
        private int adults;
        private int children;
        private ReservationStatus status;
        private long nights;
        private String statusLabel;
        private Instant createdAt;

        public OwnerReservationResponse(Reservation reservation, User user, Room room) {
            this.id = reservation.getId();
            this.guestName = user.getName();
            this.guestPhone = user.getPhone();
            this.hotelName = room.getHotel().getName();
            this.roomType = room.getRoomType().toString();
            this.checkInDate = reservation.getStartDate().atZone(ZoneId.systemDefault()).toLocalDate();
            this.checkOutDate = reservation.getEndDate().atZone(ZoneId.systemDefault()).toLocalDate();
            this.adults = reservation.getNumAdult();
            this.children = reservation.getNumKid();
            this.status = ReservationStatus.valueOf(reservation.getStatus().name());
            this.nights = ChronoUnit.DAYS.between(this.checkInDate, this.checkOutDate);

            this.statusLabel = ReservationStatus.valueOf(reservation.getStatus().name()) == ReservationStatus.COMPLETED ? "예약 완료" : "예약 취소";
            if (reservation.getCreatedAt() != null) {
                this.createdAt = reservation.getCreatedAt().atZone(ZoneId.systemDefault()).toInstant();
            }
        }

        public static OwnerReservationResponse fromEntity(Reservation reservation, User user, Room room) {
            return new OwnerReservationResponse(reservation, user, room);
        }
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class DashboardActivityResponse {
        private List<OwnerReservationResponse> checkIns;
        private List<OwnerReservationResponse> checkOuts;
        private List<OwnerReservationResponse> recentReservations;
    }
}
