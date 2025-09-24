package com.example.backend.fe_hotel_detail.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RoomSummaryDto {

    private Long roomId;
    private String roomName;
    private Integer price;
    private Integer originalPrice;
    private Integer capacity; // 최대 수용 인원

}