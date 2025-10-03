package com.example.backend.HotelOwner.domain;

import com.example.backend.authlogin.domain.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Getter @Setter
@Table(name="hotel")
@Builder
@AllArgsConstructor @NoArgsConstructor
@ToString(exclude={"owner","images","hotelAmenities"})
public class Hotel {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 🔸 FE/관리 코드가 기대하는 user_id를 @ManyToOne으로 매핑
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User owner;

    @Column(name = "business_id")
    private Long businessId;

    @Column(nullable = false, length = 50)
    private String name;

    @Column(nullable = false, length = 255)
    private String address;

    @Column(name = "star_rating", nullable = false)
    @Builder.Default
    private Integer starRating = 0;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false, length = 50)
    private String country;

    // 🔸 FE/관리 코드가 쓰는 승인 상태 (SUSPENDED까지 포함)
    public enum ApprovalStatus { PENDING, APPROVED, REJECTED, SUSPENDED }

    @Enumerated(EnumType.STRING)
    @Column(name = "approval_status", nullable = false, length = 20)
    @Builder.Default
    private ApprovalStatus approvalStatus = ApprovalStatus.PENDING;

    @Column(name = "approval_date")
    private LocalDateTime approvalDate;

    @Column(name = "approved_by")
    private Long approvedBy;

    @Lob
    @Column(name = "rejection_reason")
    private String rejectionReason;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @OneToMany(mappedBy = "hotel", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<HotelAmenity> hotelAmenities = new ArrayList<>();

    @OneToMany(mappedBy = "hotel", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<HotelImage> images = new ArrayList<>();

    // 🔸 관리 코드 호환용 헬퍼 (예: hotel.getUserId())
    @Transient
    public Long getUserId() { return owner != null ? owner.getId() : null; }
}
