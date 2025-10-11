// src/main/java/com/example/backend/admin/dto/AdminInquiryResponse.java

package com.example.backend.admin.dto;

import com.example.backend.website_support.domain.WebsiteInquiry;
import lombok.Getter;
import java.time.format.DateTimeFormatter;

@Getter
public class AdminInquiryResponse {

    private final Long id;
    private final String category;
    private final String title;
    private final String message;
    private final String status;
    private final String userName; // 문의 작성자 이름
    private final String userEmail; // 문의 작성자 이메일
    private final String date;

    public AdminInquiryResponse(WebsiteInquiry inquiry) {
        this.id = inquiry.getId();
        this.category = inquiry.getCategory();
        this.title = inquiry.getTitle();
        this.message = inquiry.getMessage();
        this.status = inquiry.getStatus();
        this.date = inquiry.getCreatedAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));

        // User 정보가 있는 경우, 이름과 이메일을 가져옵니다.
        if (inquiry.getUser() != null) {
            this.userName = inquiry.getUser().getName();
            this.userEmail = inquiry.getUser().getEmail();
        } else {
            this.userName = "비회원";
            this.userEmail = "";
        }
    }
}