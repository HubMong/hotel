package com.example.backend.fe_hotel_detail.dto;

import lombok.*;
import java.util.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class HotelDetailDto {

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class Rating {
        private double score;
        private Map<String, Double> subs;
    }

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class Amenities {
        private List<String> left;
        private List<String> right;
    }

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class HotelDto {
        private Long id;
        private String name;
        private String address;
        private String description;

        // 좌표/하이라이트 미사용 → 제거
        private List<String> images;
        private List<String> badges;
        private Rating rating;
        private Amenities amenities;
        private String notice;
    }

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class RoomDto {
        private Long id;
        private String name;
        private Integer size;          // m² 단위의 숫자만 (예: "75㎡" → 75)
        private String view;
        private String bed;
        private Integer bath;
        private Boolean smoke;
        private Boolean sharedBath;
        private Boolean window;
        private Boolean aircon;
        private Boolean water;
        private Boolean wifi;
        private String cancelPolicy;
        private String payment;
        private Integer originalPrice;
        private Integer price;
        private Integer lastBookedHours;  // 최근 예약 시점(시간) 데모용
        private List<String> photos;
        private List<Map<String, String>> promos;
        private Integer qty;              // 재고 수량
        private Integer capacityMin;
        private Integer capacityMax;
    }

    private HotelDto hotel;
    private List<RoomDto> rooms;
}
