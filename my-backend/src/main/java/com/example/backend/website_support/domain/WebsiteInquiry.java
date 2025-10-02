// src/main/java/com/example/backend/website_support/domain/WebsiteInquiry.java
package com.example.backend.website_support.domain;

import com.example.backend.authlogin.domain.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import java.time.LocalDateTime;

@Entity
@Table(name = "website_inquiry")
@Getter @Setter
@NoArgsConstructor
public class WebsiteInquiry {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String category;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String message;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(nullable = false)
    private String status = "PENDING";

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    // 👇👇👇 답변 관련 필드 2개 추가 👇👇👇
    @Column(columnDefinition = "TEXT")
    private String adminReply;

    private LocalDateTime repliedAt;
}