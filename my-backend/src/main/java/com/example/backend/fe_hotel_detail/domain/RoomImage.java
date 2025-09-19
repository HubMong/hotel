package com.example.backend.fe_hotel_detail.domain;

import com.example.backend.fe_hotel_detail.domain.RoomImage;
import jakarta.persistence.*;
import lombok.*;

@Entity @Table(name = "room_image")
@Getter @Setter @NoArgsConstructor
public class RoomImage {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable=false)
    private Long roomId;

    @Lob @Column(nullable=false)
    private String url;

    @Column(nullable=false)
    private Integer sortNo = 0;

    @Column(nullable=false)
    private Boolean isCover = false;

    @Column(length=255) private String caption;
    @Column(length=255) private String altText;
}
