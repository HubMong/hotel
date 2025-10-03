package com.example.backend.HotelOwner.service;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.backend.HotelOwner.domain.Hotel;
import com.example.backend.HotelOwner.domain.Room;
import com.example.backend.HotelOwner.domain.RoomImage;
import com.example.backend.HotelOwner.dto.RoomDto;
import com.example.backend.HotelOwner.repository.HotelRepository;
import com.example.backend.HotelOwner.repository.RoomRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class RoomService {
    
    private final RoomRepository roomRepository;
    private final HotelRepository hotelRepository;

    @Transactional
    public RoomDto createRoom(Long hotelId, RoomDto roomDto, List<String> imageUrls, String userEmail) {
        log.info("3. [Service-객실 생성] createRoom 호출됨. Hotel ID: {}, 요청자: {}", hotelId, userEmail);

        // ★ 변경: existsByIdAndOwnerEmail → existsByIdAndOwner_Email
        if (!hotelRepository.existsByIdAndOwner_Email(hotelId, userEmail)) {
            log.error("   [Service-객실 생성] 권한 오류 또는 호텔 없음. Hotel ID: {}, 요청자: {}", hotelId, userEmail);
            throw new SecurityException("객실을 추가할 권한이 없거나 호텔이 존재하지 않습니다.");
        }

        Hotel hotel = hotelRepository.findById(hotelId)
                .orElseThrow(() -> new IllegalArgumentException("호텔을 찾을 수 없습니다. id=" + hotelId));

        Room room = Room.builder()
                .hotel(hotel)
                .roomType(Room.RoomType.valueOf(roomDto.getRoomType()))
                .roomSize(roomDto.getRoomSize())
                .capacityMin(roomDto.getCapacityMin())
                .capacityMax(roomDto.getCapacityMax())
                .price(roomDto.getPrice())
                .roomCount(roomDto.getRoomCount())
                .checkInTime(roomDto.getCheckInTime())
                .checkOutTime(roomDto.getCheckOutTime())
                .build();

        if (imageUrls != null && !imageUrls.isEmpty()) {
            List<RoomImage> roomImages = imageUrls.stream()
                    .map(url -> RoomImage.builder().room(room).url(url).build())
                    .collect(Collectors.toList());
            room.setImages(roomImages);
        }

        Room savedRoom = roomRepository.save(room);
        log.info("   [Service-객실 생성] 객실 저장 완료");
        return RoomDto.fromEntity(savedRoom);
    }

    @Transactional(readOnly = true)
    public List<RoomDto> findByHotelId(Long hotelId) {
        log.info("2. [Service-객실 조회] findByHotelId 호출됨. Hotel ID: {}", hotelId);
        List<Room> rooms = roomRepository.findByHotelIdWithImages(hotelId);
        log.info("   [Service-객실 조회] DB에서 {}개의 객실(이미지 포함)을 찾음", rooms.size());
        return rooms.stream().map(RoomDto::fromEntity).collect(Collectors.toList());
    }

    @Transactional
    public Room updateRoom(Long roomId, RoomDto roomDto, List<String> imageUrls, String userEmail) {
        log.info("3. [Service-객실 수정] updateRoom 호출됨. Room ID: {}, 요청자: {}", roomId, userEmail);
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new IllegalArgumentException("객실을 찾을 수 없습니다. id=" + roomId));

        if (!room.getHotel().getOwner().getEmail().equals(userEmail)) {
            log.error("   [Service-객실 수정] 권한 오류. 객실 소유주: {}, 요청자: {}", room.getHotel().getOwner().getEmail(), userEmail);
            throw new SecurityException("객실을 수정할 권한이 없습니다.");
        }

        room.setRoomType(Room.RoomType.valueOf(roomDto.getRoomType()));
        room.setRoomSize(roomDto.getRoomSize());
        room.setCapacityMin(roomDto.getCapacityMin());
        room.setCapacityMax(roomDto.getCapacityMax());
        room.setRoomCount(roomDto.getRoomCount());
        room.setCheckInTime(roomDto.getCheckInTime());
        room.setCheckOutTime(roomDto.getCheckOutTime());
        room.setPrice(roomDto.getPrice());

        room.getImages().clear();
        if (imageUrls != null && !imageUrls.isEmpty()) {
            List<RoomImage> newImages = imageUrls.stream()
                    .map(url -> RoomImage.builder().room(room).url(url).build())
                    .collect(Collectors.toList());
            room.getImages().addAll(newImages);
        }
        log.info("   [Service-객실 수정] 객실 업데이트 완료");
        return roomRepository.save(room);
    }

    @Transactional
    public void deleteRoom(Long roomId, String userEmail) {
        log.info("3. [Service-객실 삭제] deleteRoom 호출됨. Room ID: {}, 요청자: {}", roomId, userEmail);
        Room room = roomRepository.findById(roomId)
            .orElseThrow(() -> new RuntimeException("Room not found"));
        
        if (!room.getHotel().getOwner().getEmail().equals(userEmail)) {
            log.error("   [Service-객실 삭제] 권한 오류. 객실 소유주: {}, 요청자: {}", room.getHotel().getOwner().getEmail(), userEmail);
            throw new RuntimeException("User not authorized to delete this room");
        }
        
        log.info("   [Service-객실 삭제] DB에서 객실 삭제 실행");
        roomRepository.delete(room);    
    }
}
