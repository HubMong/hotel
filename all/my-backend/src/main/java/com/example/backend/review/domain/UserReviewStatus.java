package com.example.backend.review.domain;

public enum UserReviewStatus {
    VISIBLE,   // 노출 중
    REPORTED,  // 신고됨 (관리자 확인 대기)
    HIDDEN     // 관리자에 의해 숨김 처리됨
}
    