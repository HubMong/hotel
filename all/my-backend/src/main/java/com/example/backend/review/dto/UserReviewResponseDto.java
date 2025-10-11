package com.example.backend.review.dto;

import lombok.*;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UserReviewResponseDto {
    private Long id;
    private Long userId;
    private String userName;
    private String hotelName;
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
}
