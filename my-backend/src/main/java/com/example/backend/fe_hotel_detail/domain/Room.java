package com.example.backend.fe_hotel_detail.domain;

import com.example.backend.fe_hotel_detail.domain.Room;
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
    private String name;     // (ex. ?꾨??좊━/?붾툝猷???

    @Column(length=50)
    private String roomSize; // ex) "26"

    private Integer capacityMin;
    private Integer capacityMax;

    // ?꾨윴??紐⑹뾽???곗씠???ㅽ럺 ????꾨뱶(媛꾨떒 踰꾩쟾)
    @Column(length=50) private String viewName;  // ?쒗떚酉?媛?좊럭
    @Column(length=50) private String bed;       // ?붾툝踰좊뱶 1媛?
    private Integer bath;                        // ?뺤떎 ??
    private Boolean smoke;                       // ?≪뿰 ?щ?
    private Boolean sharedBath;
    private Boolean hasWindow;
    private Boolean aircon;
    private Boolean freeWater;
    private Boolean wifi;

    @Column(length=100) private String cancelPolicy; // ?좎뿰/遺遺??섎텋
    @Column(length=50)  private String payment;      // 吏湲?寃곗젣/泥댄겕????寃곗젣

    private Integer originalPrice;
    private Integer price;
}
