package com.example.backend.review.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class UserReviewStatsDto {
    private double averageRating;
    private long reviewCount;
}
