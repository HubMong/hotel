package com.example.backend.hotel_reservation.dto;

import lombok.*;
import java.time.Instant;

public class ReservationDtos {

    @Getter @Setter
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

    @Getter @Builder
    public static class HoldResponse {
        private Long reservationId;
        private Instant expiresAt;
        private String status; // PENDING
    }

    @Getter @Builder
    public static class ReservationDetail {
        private Long id;
        private String status;
        private Instant expiresAt;
        private Long hotelId; 
        private Long userId;
        private Long roomId;
        private Integer numRooms;
        private Integer adults;
        private Integer children;
        private Instant startDate;
        private Instant endDate;
    }
}
