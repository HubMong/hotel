package com.example.backend.HotelOwner.dto;

import java.time.LocalTime;
import java.util.List;
import java.util.stream.Collectors;

import com.example.backend.HotelOwner.domain.Room;
import com.example.backend.HotelOwner.domain.RoomImage;
import com.fasterxml.jackson.annotation.JsonFormat;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Data
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class RoomDto {
     private Long id;
    private String hotelName;
    private String roomType;
    private String roomSize;
    private Integer capacityMin;
    private Integer capacityMax;
    private int price;
    private int roomCount;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "HH:mm")
    private LocalTime checkInTime;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "HH:mm")
    private LocalTime checkOutTime;

    // Room 엔티티를 RoomDto로 변환하는 정적 메서드
    private List<String> imageUrls;

    public static RoomDto fromEntity(Room room) {
        return RoomDto.builder()
                .id(room.getId())
                .hotelName(room.getHotel().getName())
                .roomType(room.getRoomType().name())
                .roomSize(room.getRoomSize())
                .capacityMin(room.getCapacityMin())
                .capacityMax(room.getCapacityMax())
                .price(room.getPrice())
                .roomCount(room.getRoomCount())
                .checkInTime(room.getCheckInTime())
                .checkOutTime(room.getCheckOutTime())
                // ✨ [수정] RoomImage 엔티티 리스트에서 URL 문자열 리스트로 변환
                .imageUrls(room.getImages().stream()
                        .map(RoomImage::getUrl)
                        .collect(Collectors.toList()))
                .build();

    }
}

