package com.example.backend.domain;

import jakarta.persistence.*;
import lombok.*;

@Entity @Table(name = "Room")
@Getter @Setter @NoArgsConstructor
public class Room {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable=false)
    private Long hotelId;

    @Column(nullable=false, length=100)
    private String name;     // (ex. 도미토리/더블룸 등)

    @Column(length=50)
    private String roomSize; // ex) "26"

    private Integer capacityMin;
    private Integer capacityMax;

    // 프런트 목업에 쓰이는 스펙 대응 필드(간단 버전)
    @Column(length=50) private String viewName;  // 시티뷰/가든뷰
    @Column(length=50) private String bed;       // 더블베드 1개
    private Integer bath;                        // 욕실 수
    private Boolean smoke;                       // 흡연 여부
    private Boolean sharedBath;
    private Boolean hasWindow;
    private Boolean aircon;
    private Boolean freeWater;
    private Boolean wifi;

    @Column(length=100) private String cancelPolicy; // 유연/부분 환불
    @Column(length=50)  private String payment;      // 지금 결제/체크인 시 결제

    private Integer originalPrice;
    private Integer price;
}
