package com.example.backend.admin.service;

import com.example.backend.admin.dto.RoomCreateRequest;
import com.example.backend.admin.dto.RoomResponse;
import com.example.backend.hotel_reservation.repository.ReservationRepository;
import com.example.backend.HotelOwner.domain.Hotel;
import com.example.backend.HotelOwner.domain.Room;
import com.example.backend.HotelOwner.domain.RoomImage;
import com.example.backend.HotelOwner.repository.HotelRepository;
import com.example.backend.HotelOwner.repository.RoomRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AdminRoomService {
    private final RoomRepository roomRepository;
    private final ReservationRepository reservationRepository;
    private final HotelRepository hotelRepository;

    @Transactional(readOnly = true)
    public Page<RoomResponse> list(Long hotelId, String name, Pageable pageable) {
        Page<Room> rooms;
        if (hotelId != null && name != null) {
            rooms = roomRepository.findByHotel_IdAndNameContaining(hotelId, name, pageable);
        } else if (hotelId != null) {
            rooms = roomRepository.findByHotel_Id(hotelId, pageable);
        } else if (name != null) {
            rooms = roomRepository.findByNameContaining(name, pageable);
        } else {
            rooms = roomRepository.findAll(pageable);
        }
        // 트랜잭션 내에서 images를 Eager 로딩
        return rooms.map(room -> {
            room.getImages().size(); // Lazy loading 강제 초기화
            return RoomResponse.from(room);
        });
    }

    @Transactional(readOnly = true)
    public RoomResponse get(Long id) { 
        Room room = roomRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("객실을 찾을 수 없습니다: " + id));
        return RoomResponse.from(room);
    }
    
    public void delete(Long id) { 
        if (!roomRepository.existsById(id)) {
            throw new IllegalArgumentException("삭제할 객실을 찾을 수 없습니다: " + id);
        }
        
        // 예약이 있는지 확인
        long reservationCount = reservationRepository.countByRoomId(id);
        if (reservationCount > 0) {
            throw new IllegalArgumentException("예약이 있는 객실은 삭제할 수 없습니다. 먼저 예약을 취소해주세요.");
        }
        
        roomRepository.deleteById(id); 
    }

    @Transactional
    public RoomResponse create(RoomCreateRequest request) {
    // create room
        
        // 필수 필드 검증
        if (request.getHotelId() == null) {
            throw new IllegalArgumentException("호텔 정보가 필요합니다.");
        }
        
        if (request.getName() == null || request.getName().isBlank()) {
            throw new IllegalArgumentException("객실 번호가 필요합니다.");
        }
        
        // 호텔 조회
        Hotel hotel = hotelRepository.findById(request.getHotelId())
                .orElseThrow(() -> new IllegalArgumentException("호텔을 찾을 수 없습니다: " + request.getHotelId()));
        
        // roomType을 Enum으로 변환
        Room.RoomType roomType = Room.RoomType.스탠다드룸; // 기본값
        if (request.getRoomType() != null && !request.getRoomType().isBlank() && !request.getRoomType().equals("-")) {
            try {
                roomType = Room.RoomType.valueOf(request.getRoomType().trim());
            } catch (IllegalArgumentException e) {
                // 유효하지 않은 값이면 기본값 유지
                System.err.println("유효하지 않은 roomType: " + request.getRoomType() + ", 기본값(스탠다드룸) 사용");
            }
        }
        
        // Room 엔티티 생성
    Room room = Room.builder()
                .hotel(hotel)
                .name(request.getName())
                .roomType(roomType)
                .roomSize(request.getRoomSize() != null && !request.getRoomSize().isBlank() 
                        ? request.getRoomSize() : "기본크기")
                .capacityMin(request.getCapacityMin() != null ? request.getCapacityMin() : 
                        (request.getCapacityMax() != null ? request.getCapacityMax() : 1))
                .capacityMax(request.getCapacityMax() != null ? request.getCapacityMax() : 
                        (request.getCapacityMin() != null ? request.getCapacityMin() : 1))
                .checkInTime(java.time.LocalTime.of(15, 0))
                .checkOutTime(java.time.LocalTime.of(11, 0))
                .roomCount(request.getRoomCount() != null ? request.getRoomCount() : 1)
                .price(request.getPrice() != null ? request.getPrice() : 0)
                .originalPrice(request.getOriginalPrice())
                .status(normalizeStatus(request.getStatus() != null ? request.getStatus() : "available"))
                .wifi(request.getWifi() != null ? request.getWifi() : false)
                .aircon(request.getAircon() != null ? request.getAircon() : false)
                .freeWater(request.getFreeWater() != null ? request.getFreeWater() : false)
                .hasWindow(request.getHasWindow() != null ? request.getHasWindow() : false)
                .sharedBath(request.getSharedBath() != null ? request.getSharedBath() : false)
                .smoke(request.getSmoke() != null ? request.getSmoke() : false)
                .bed(request.getBed())
                .bath(request.getBath())
                .viewName(request.getViewName())
                .cancelPolicy(request.getCancelPolicy())
                .payment(request.getPayment())
                .build();

        // 1단계: Room을 먼저 저장 (ID 생성)
        Room savedRoom = roomRepository.save(room);
        
        // 2단계: 이제 ID가 있으니 이미지 추가
        if (request.getImageUrl() != null && !request.getImageUrl().trim().isEmpty()) {
            applyRoomImages(savedRoom, request.getImageUrl());
            
            // 3단계: 이미지가 추가된 Room을 다시 저장
            savedRoom = roomRepository.save(savedRoom);
        }
        
        // 4단계: Lazy loading 강제 초기화
        savedRoom.getImages().size();
        
        return RoomResponse.from(savedRoom);
    }

    @Transactional
    public RoomResponse update(Long id, RoomCreateRequest request) {
        Room existing = roomRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("객실을 찾을 수 없습니다: " + id));

        if (request.getName() != null && !request.getName().isBlank()) {
            existing.setName(request.getName());
        }

        if (request.getRoomType() != null && !request.getRoomType().isBlank() && !request.getRoomType().equals("-")) {
            try {
                Room.RoomType roomType = Room.RoomType.valueOf(request.getRoomType().trim());
                existing.setRoomType(roomType);
            } catch (IllegalArgumentException e) {
                System.err.println("유효하지 않은 roomType(업데이트): " + request.getRoomType() + ", 기존 값을 유지합니다.");
            }
        }

        if (request.getRoomSize() != null) {
            existing.setRoomSize(!request.getRoomSize().isBlank() ? request.getRoomSize() : "기본크기");
        }

        Integer newCapacityMin = request.getCapacityMin();
        Integer newCapacityMax = request.getCapacityMax();
        if (newCapacityMin == null && newCapacityMax != null) {
            newCapacityMin = newCapacityMax;
        }
        if (newCapacityMax == null && newCapacityMin != null) {
            newCapacityMax = newCapacityMin;
        }
        existing.setCapacityMin(newCapacityMin != null ? newCapacityMin : existing.getCapacityMin());
        existing.setCapacityMax(newCapacityMax != null ? newCapacityMax : existing.getCapacityMax());

        if (request.getPrice() != null) {
            existing.setPrice(request.getPrice());
        }
        if (request.getOriginalPrice() != null) {
            existing.setOriginalPrice(request.getOriginalPrice());
        }

        if (request.getRoomCount() != null) {
            existing.setRoomCount(request.getRoomCount());
        }

        if (request.getStatus() != null && !request.getStatus().isBlank()) {
            existing.setStatus(normalizeStatus(request.getStatus()));
        }

        if (request.getWifi() != null) existing.setWifi(request.getWifi());
        if (request.getAircon() != null) existing.setAircon(request.getAircon());
        if (request.getFreeWater() != null) existing.setFreeWater(request.getFreeWater());
        if (request.getHasWindow() != null) existing.setHasWindow(request.getHasWindow());
        if (request.getSharedBath() != null) existing.setSharedBath(request.getSharedBath());
        if (request.getSmoke() != null) existing.setSmoke(request.getSmoke());

        if (request.getBed() != null) existing.setBed(request.getBed());
        if (request.getBath() != null) existing.setBath(request.getBath());
        if (request.getViewName() != null) existing.setViewName(request.getViewName());
        if (request.getCancelPolicy() != null) existing.setCancelPolicy(request.getCancelPolicy());
        if (request.getPayment() != null) existing.setPayment(request.getPayment());

        // 이미지 업데이트가 필요한 경우 (저장 전에 처리)
        if (request.getImageUrl() != null && !request.getImageUrl().trim().isEmpty()) {
            applyRoomImages(existing, request.getImageUrl());
        }
        
        Room savedRoom = roomRepository.save(existing);
        
        // Lazy loading 강제 초기화
        savedRoom.getImages().size();
        
        return RoomResponse.from(savedRoom);
    }

    @Transactional
    public RoomResponse updateStatus(Long id, String status) {
        Room room = roomRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("객실을 찾을 수 없습니다: " + id));
        room.setStatus(normalizeStatus(status));
        Room savedRoom = roomRepository.save(room);
        return RoomResponse.from(savedRoom);
    }

    private String normalizeStatus(String raw) {
        if (raw == null) return null;
        // 허용되는 상태: available, occupied, maintenance, cleaning
        String s = raw.trim();
        String lower = s.toLowerCase();
        switch (lower) {
            case "available":
            case "가능":
                return "available";
            case "occupied":
            case "예약중":
            case "예약 중":
                return "occupied";
            case "maintenance":
            case "점검":
            case "점검중":
            case "점검 중":
                return "maintenance";
            case "cleaning":
            case "청소":
            case "청소중":
            case "청소 중":
                return "cleaning";
            default:
                // 알 수 없는 값은 그대로 저장하지 않고 기본값 처리
                return "available";
        }
    }

    private void applyRoomImages(Room room, String imageUrl) {
        if (imageUrl == null || imageUrl.trim().isEmpty()) {
            return;
        }

        String trimmed = imageUrl.trim();
        
        // 기존 이미지 목록 가져오기
        List<RoomImage> images = room.getImages();
        if (images == null) {
            images = new ArrayList<>();
            room.setImages(images);
        }
        
        // 이미 같은 URL이 있는지 확인 (중복 방지)
        boolean exists = images.stream()
                .anyMatch(img -> trimmed.equals(img.getUrl()));
        
        if (exists) { return; }
        
        // 기존 이미지 제거 (orphanRemoval이 처리)
        images.clear();
        
        // 새 이미지 추가
        RoomImage newImage = RoomImage.builder()
                .room(room)
                .url(trimmed)
                .sortNo(0)
                .isCover(true)
                .build();
        images.add(newImage);
        
    }
}
