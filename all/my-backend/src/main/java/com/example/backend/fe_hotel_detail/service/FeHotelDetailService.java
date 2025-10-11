// src/main/java/com/example/backend/fe_hotel_detail/service/FeHotelDetailService.java
package com.example.backend.fe_hotel_detail.service;

import com.example.backend.HotelOwner.domain.Amenity;
import com.example.backend.HotelOwner.domain.Hotel;
import com.example.backend.HotelOwner.domain.HotelImage;
import com.example.backend.HotelOwner.domain.Room;
import com.example.backend.HotelOwner.domain.RoomImage;
import com.example.backend.HotelOwner.repository.HotelAmenityRepository;
import com.example.backend.HotelOwner.repository.HotelImageRepository;
import com.example.backend.HotelOwner.repository.HotelRepository;
import com.example.backend.HotelOwner.repository.RoomRepository;
import com.example.backend.fe_hotel_detail.dto.HotelDetailDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;

@Service("feHotelDetailService")
@RequiredArgsConstructor
public class FeHotelDetailService {

    private final HotelRepository hotelRepository;
    private final HotelImageRepository hotelImageRepository;
    private final RoomRepository roomRepository;
    private final HotelAmenityRepository hotelAmenityRepository;

    public HotelDetailDto getHotelDetail(Long id) {
        Hotel h = hotelRepository.findById(id)
            .orElseThrow(() -> new NoSuchElementException("hotel not found"));

        // 호텔 이미지
        List<String> hotelImages = hotelImageRepository.findByHotel_IdOrderBySortNoAsc(id)
            .stream().map(HotelImage::getUrl).toList();

        // 객실: 이미지까지 fetch-join 메서드가 있으면 사용
        List<Room> roomEntities = roomRepository.findByHotelIdWithImages(id);
        // ↑ 만약 프로젝트에 없다면 아래 라인으로 교체하세요.
        // List<Room> roomEntities = roomRepository.findByHotelId(id);

        // 편의시설: 이름 → 좌/우 분배
        List<Amenity> amenList = hotelAmenityRepository.findAmenitiesByHotel_Id(id);
        List<String> amenNames = amenList.stream().map(Amenity::getName).toList();
        List<String> left = new ArrayList<>(), right = new ArrayList<>();
        for (int i = 0; i < amenNames.size(); i++) {
            (i % 2 == 0 ? left : right).add(amenNames.get(i));
        }

        // (하이라이트 키 매핑/lat/lng는 사용 안 함)

        // 호텔 DTO
        HotelDetailDto.HotelDto hotelDto = HotelDetailDto.HotelDto.builder()
            .id(h.getId())
            .name(h.getName())
            .address(h.getAddress())
            .description(h.getDescription())
            .images(hotelImages)
            .badges(List.of())
            .rating(new HotelDetailDto.Rating(0.0, Map.of()))
            .amenities(new HotelDetailDto.Amenities(left, right))
            .notice(null)
            .build();

        // 객실 DTO
        List<HotelDetailDto.RoomDto> roomDtos = new ArrayList<>();
        for (Room r : roomEntities) {
            List<String> photos = (r.getImages() == null) ? List.of()
                : r.getImages().stream().map(RoomImage::getUrl).toList();

            roomDtos.add(HotelDetailDto.RoomDto.builder()
                .id(r.getId())
                .name(safeRoomName(r))
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
                .lastBookedHours(3) // 데모 값
                .photos(photos)
                .promos(List.of())
                .qty(r.getRoomCount())
                .capacityMin(r.getCapacityMin())
                .capacityMax(r.getCapacityMax())
                .build());
        }

        HotelDetailDto dto = new HotelDetailDto();
        dto.setHotel(hotelDto);
        dto.setRooms(roomDtos);
        return dto;
    }

    // "75㎡", "약 75 m²", "75m2" → 75
    private Integer parseIntSafe(String s) {
        if (s == null) return null;
        String digits = s.replaceAll("[^0-9]", "");
        if (digits.isEmpty()) return null;
        try { return Integer.parseInt(digits); }
        catch (NumberFormatException e) { return null; }
    }

    private String nullToDash(String s) {
        return (s == null || s.isBlank()) ? "-" : s;
    }

    private String safeRoomName(Room r) {
        if (r.getName() != null && !r.getName().isBlank()) return r.getName();
        return (r.getRoomType() != null) ? r.getRoomType().name() : "객실";
    }
}
