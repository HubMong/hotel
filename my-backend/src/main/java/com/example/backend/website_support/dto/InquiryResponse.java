// src/main/java/com/example/backend/website_support/dto/InquiryResponse.java
package com.example.backend.website_support.dto;

import com.example.backend.website_support.domain.WebsiteInquiry;
import lombok.Getter;
import java.time.format.DateTimeFormatter;

@Getter
public class InquiryResponse {
    private final Long id;
    private final String title;
    private final String status;
    private final String date;
    private final String category;

    public InquiryResponse(WebsiteInquiry inquiry) {
        this.id = inquiry.getId();
        this.title = inquiry.getTitle();
        this.status = inquiry.getStatus();
        this.date = inquiry.getCreatedAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        this.category = inquiry.getCategory();
    }
}