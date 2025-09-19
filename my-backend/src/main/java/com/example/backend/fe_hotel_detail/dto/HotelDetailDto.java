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
    public static class Highlight {
        private String ic;
        private String title;
        private String sub;
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
        private List<String> images;   // ?명뀛 ?대?吏 URL
        private List<String> badges;   // 媛꾨떒 諛곗?
        private Rating rating;         // ?됱젏 ?붿빟
        private List<Highlight> highlights;
        private Amenities amenities;
        private String notice;         // 怨듭?/?뚮┝
    }

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class RoomDto {
        private Long id;
        private String name;
        private Integer size;      // m2 ?レ옄
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
        private Integer lastBookedHours;  // ?곕え??
        private List<String> photos;
        private List<Map<String, String>> promos;
        private Integer qty;              // ?꾨윴??諛붿씤?⑹슜
    }

    private HotelDto hotel;
    private List<RoomDto> rooms;
}
