package com.example.backend.fe_hotel_detail.service;

import com.example.backend.fe_hotel_detail.dto.RoomSummaryDto;
import com.example.backend.hotel_reservation.domain.Room;
import com.example.backend.hotel_reservation.repository.RoomRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.NoSuchElementException;

@Service
@RequiredArgsConstructor
public class RoomService {

    private final RoomRepository roomRepository;

    public RoomSummaryDto getRoomSummary(Long roomId) {
        // 1. ID를 기반으로 데이터베이스에서 Room 엔티티를 찾습니다.
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new NoSuchElementException("해당 ID의 객실을 찾을 수 없습니다: " + roomId));

        // 2. 찾은 Room 엔티티의 정보를 RoomSummaryDto 형식으로 변환(매핑)합니다.
        return RoomSummaryDto.builder()
                .roomId(room.getId())
                .roomName(room.getName())
                .price(room.getPrice())
                .originalPrice(room.getOriginalPrice())
                // .capacity(room.getCapacity()) // Room 엔티티에 capacity 필드가 있다고 가정
                .build();
    }
}