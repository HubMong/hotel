package com.example.backend.HotelOwner.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "hotel_image")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class HotelImage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 호텔 연관관계
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "hotel_id", nullable = false)
    @JsonIgnore
    private Hotel hotel;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String url;

    @Builder.Default
    @Column(name = "sort_no", nullable = false)
    private int sortNo = 0;

    @Builder.Default
    @Column(name = "is_cover", nullable = false)
    private boolean isCover = false;

    @Column(length = 255)
    private String caption;

    @Column(name = "alt_text", length = 255)
    private String altText;
}
