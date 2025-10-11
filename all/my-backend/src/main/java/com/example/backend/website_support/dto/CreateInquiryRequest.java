// src/main/java/com/example/backend/website_support/dto/CreateInquiryRequest.java
package com.example.backend.website_support.dto;

import com.example.backend.website_support.domain.WebsiteInquiry;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class CreateInquiryRequest {
    private String category;
    private String title;
    private String message;

    public WebsiteInquiry toEntity() {
        WebsiteInquiry inquiry = new WebsiteInquiry();
        inquiry.setCategory(this.category);
        inquiry.setTitle(this.title);
        inquiry.setMessage(this.message);
        return inquiry;
    }
}