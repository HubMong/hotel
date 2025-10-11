package com.example.backend.HotelOwner.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class OwnerHotelDto {
    private Long id;
    private String name;
    private String address;
    private int starRating;
    private String approvalStatus;
}