package com.example.backend.HotelOwner.dto;

import lombok.Data;
import java.time.LocalDate;

import com.example.backend.HotelOwner.domain.Room;

@Data
public class SalesChartRequestDto {
    private LocalDate startDate;
    private LocalDate endDate;
    private Long hotelId; // "ALL" 대신 null 사용
    private Room.RoomType roomType; // "ALL" 대신 null 사용
}