package com.example.backend.hotel_support.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class HotelInquiryDto {
    private Long reservationId;
    private String title;
    private String message;
}