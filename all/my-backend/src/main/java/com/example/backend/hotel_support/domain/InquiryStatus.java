// src/main/java/com/example/backend/hotel_support/domain/InquiryStatus.java

package com.example.backend.hotel_support.domain;

// 문의 상태를 정의하는 ENUM (수정됨)
public enum InquiryStatus {
    PENDING("답변 대기"), 
    ANSWERED("답변 완료"); 
    
    private final String displayValue;

    InquiryStatus(String displayValue) {
        this.displayValue = displayValue;
    }

    public String getDisplayValue() {
        return displayValue;
    }
}