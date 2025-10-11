package com.example.backend.HotelOwner.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;

public class OwnerReservationDto {

    /**
     * FullCalendar에 이벤트를 표시하기 위한 DTO
     * title, start, end 필드명은 FullCalendar의 기본 속성에 맞춘 것입니다.
     */
    // 캘린더 이벤트 DTO
    @Getter
    @Builder
    public static class CalendarEvent {
        private Long id;
        private String title;
        private LocalDate start;
        private LocalDate end;
        private String color;
        private String status;
        private ExtendedProps extendedProps; // 프론트엔드 필터링을 위해 추가
    }

    @Getter
    @Builder
    public static class ExtendedProps {
        private String guestName;
        private String roomName;
        private String resStatus;
        private String roomType;
    }
    

    @Getter
    @Builder
    public static class DetailResponse {
        private Long id;
        private String hotelName;
        private String guestName;
        private String guestPhone;
        private LocalDate checkInDate;
        private LocalDate checkOutDate;
        private String roomType;
        private Integer numAdult;
        private Integer numKid;
        private String reservationStatus;
        private String resStatus;
    }

    public static LocalDate toLocalDate(Instant instant) {
        if (instant == null) return null;
        return instant.atZone(ZoneId.systemDefault()).toLocalDate();
    }
}