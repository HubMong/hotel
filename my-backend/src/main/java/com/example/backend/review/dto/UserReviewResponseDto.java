package com.example.backend.review.dto;

import lombok.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UserReviewResponseDto {
    private Long id;
    private Long userId;
    private String userName;
    private Long hotelId;
    private String hotelName;
    private Long reservationId;
    private double rating;
    private String content;
    private List<String> images;

    private double cleanliness;
    private double service;
    private double value;
    private double location;
    private double facilities;

    private LocalDateTime createdAt;
    private String adminReply;

    // ✅ 평균 평점 및 세부 항목 통계 포함
    private Map<String, Object> hotelStats;

    public List<String> getImages() {
        return images == null ? null : new ArrayList<>(images);
    }

    public void setImages(List<String> images) {
        this.images = images == null ? null : new ArrayList<>(images);
    }

    public Map<String, Object> getHotelStats() {
        return hotelStats == null ? null : new HashMap<>(hotelStats);
    }

    public void setHotelStats(Map<String, Object> hotelStats) {
        this.hotelStats = hotelStats == null ? null : new HashMap<>(hotelStats);
    }
}
