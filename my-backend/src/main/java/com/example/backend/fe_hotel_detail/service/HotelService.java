package com.example.backend.fe_hotel_detail.service;

import com.example.backend.HotelOwner.repository.RoomImageRepository;
import com.example.backend.HotelOwner.repository.RoomRepository;
import com.example.backend.fe_hotel_detail.dto.HotelDetailDto;
import com.example.backend.HotelOwner.domain.*;
import com.example.backend.HotelOwner.repository.HotelImageRepository;
import com.example.backend.HotelOwner.repository.HotelRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class HotelService {

    private final HotelRepository hotelRepository;
    private final HotelImageRepository hotelImageRepository;
    private final RoomRepository roomRepository;
    private final RoomImageRepository roomImageRepository;

    public HotelDetailDto getHotelDetail(Long id) {
        Hotel h = hotelRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("hotel not found"));//호텔정보불러오기

        // 호텔 이미지 정렬
        List<String> hotelImages = hotelImageRepository.findByHotelIdOrderBySortNoAsc(id)
                .stream().map(HotelImage::getUrl).toList();

        // 객실 + 이미지 일괄
        List<com.example.backend.HotelOwner.domain.Room> roomEntities = roomRepository.findByHotelId(id);
        List<Long> roomIds = roomEntities.stream().map(com.example.backend.HotelOwner.domain.Room::getId).toList();

        Map<Long, List<String>> roomImagesMap = Collections.emptyMap();
        if (!roomIds.isEmpty()) {
            roomImagesMap = roomImageRepository.findByRoom_IdInOrderBySortNoAsc(roomIds)
                    .stream()
                    .collect(Collectors.groupingBy(img -> img.getRoom().getId(),
                            Collectors.mapping(RoomImage::getUrl, Collectors.toList())));
        }

        // 호텔 DTO
        HotelDetailDto.HotelDto hotelDto = HotelDetailDto.HotelDto.builder()
                .id(h.getId())
                .name(h.getName())
                .address(h.getAddress())
                .description(h.getDescription())
                .images(hotelImages)
                // 아래 배지/평점/하이라이트/편의시설은 데모 값 (테이블 붙이면 거기로 변경)
                .badges(List.of("International Deals", "베스트셀러", "PREFERRED"))
                .rating(new HotelDetailDto.Rating(6.4,
                        Map.of("위치",7.2, "서비스",6.7, "가성비",6.7, "부대시설",5.7)))
                .highlights(List.of(
                        new HotelDetailDto.Highlight("📍","방콕의 중심지에 위치","도심 근접"),
                        new HotelDetailDto.Highlight("⭐","다양한 액티비티","투어/이벤트"),
                        new HotelDetailDto.Highlight("🚌","공항 이동 교통편","셔틀/픽업"),
                        new HotelDetailDto.Highlight("🕒","24시간 상시 체크인","야간 도착 OK"),
                        new HotelDetailDto.Highlight("🏞️","카오산 로드(670m)","도보 거리")
                ))
                .amenities(new HotelDetailDto.Amenities(
                        List.of("무료 Wi-Fi","조식","24시간 프런트 데스크","투어"),
                        List.of("공항 이동 서비스","세탁","여행 가방 보관","택시 서비스")
                ))
                .notice("오늘 17명의 여행객이 이 숙소 예약함")
                .build();

        // 객실 DTO
        List<HotelDetailDto.RoomDto> rooms = new ArrayList<>();
        for (com.example.backend.HotelOwner.domain.Room r : roomEntities) {
            rooms.add(HotelDetailDto.RoomDto.builder()
                    .id(r.getId())
                    .name(r.getName())
                    .size(parseIntSafe(r.getRoomSize()))
                    .view(nullToDash(r.getViewName()))
                    .bed(nullToDash(r.getBed()))
                    .bath(r.getBath())
                    .smoke(r.getSmoke())
                    .sharedBath(r.getSharedBath())
                    .window(r.getHasWindow())
                    .aircon(r.getAircon())
                    .water(r.getFreeWater())
                    .wifi(r.getWifi())
                    .cancelPolicy(r.getCancelPolicy())
                    .payment(r.getPayment())
                    .originalPrice(r.getOriginalPrice())
                    .price(r.getPrice())
                    .lastBookedHours(3) // 데모
                    .photos(roomImagesMap.getOrDefault(r.getId(), List.of()))
                    .promos(List.of(
                            Map.of("type","deal","title","특별 할인 쿠폰 적용됨","desc","코드 WEEKENDSALE")
                    ))
                    .qty(1)
                    .build()
            );
        }

        HotelDetailDto dto = new HotelDetailDto();
        dto.setHotel(hotelDto);
        dto.setRooms(rooms);
        return dto;
    }

    private Integer parseIntSafe(String s) {
        try { return s==null? null : Integer.parseInt(s); }
        catch (Exception e) { return null; }
    }
    private String nullToDash(String s){ return (s==null||s.isBlank())?"-":s; }
}
