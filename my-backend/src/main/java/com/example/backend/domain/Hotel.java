package com.example.backend.domain;

import jakarta.persistence.*;
import lombok.*;

@Entity @Table(name = "Hotel")
@Getter @Setter @NoArgsConstructor
public class Hotel {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable=false, length=100)
    private String name;

    @Column(nullable=false, length=255)
    private String address;

    private Integer starRating;

    @Lob
    private String description;

    @Column(length=50)
    private String country;

    // 소유주/비즈니스 등은 여기선 생략(필요시 컬럼 추가)
}
