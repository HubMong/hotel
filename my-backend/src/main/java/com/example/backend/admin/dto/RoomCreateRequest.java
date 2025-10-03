package com.example.backend.admin.dto;

import lombok.Data;

@Data
public class RoomCreateRequest {
    private Long hotelId;
    private String name;
    private String roomType;  // String으로 받아서 Enum으로 변환
    private String roomSize;
    private String status;
    private Integer capacityMin;
    private Integer capacityMax;
    private Integer price;
    private Integer originalPrice;
    private Integer roomCount;
    private Boolean wifi;
    private Boolean aircon;
    private Boolean freeWater;
    private Boolean hasWindow;
    private Boolean sharedBath;
    private Boolean smoke;
    private String bed;
    private Integer bath;
    private String imageUrl;
    private String viewName;
    private String cancelPolicy;
    private String payment;
}
