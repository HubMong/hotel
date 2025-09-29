package com.example.backend.fe_hotel_detail.service;

import com.example.backend.HotelOwner.domain.Hotel;
import com.example.backend.HotelOwner.domain.Room;
import com.example.backend.HotelOwner.domain.RoomImage;
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

    public HotelDetailDto getHotelDetail(Long id) {
        Hotel h = hotelRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("hotel not found"));

        // 호텔 이미지
        List<String> hotelImages = hotelImageRepository.findByHotelIdOrderBySortNoAsc(id)
                .stream().map(img -> img.getUrl()).toList();

        // 객실 + 이미지 로딩 (이미지까지 fetch)
        List<Room> rooms = roomRepository.findByHotelIdWithImages(id);

        List<HotelDetailDto.RoomDto> roomDtos = new ArrayList<>();
        for (Room r : rooms) {
            List<String> photos = (r.getImages() == null) ? List.of()
                    : r.getImages().stream().map(RoomImage::getUrl).toList();

            roomDtos.add(HotelDetailDto.RoomDto.builder()
                    .id(r.getId())
                    .name(safeRoomName(r))                  // 오너 엔티티엔 name 없음 → roomType으로 대체
                    .size(parseIntSafe(r.getRoomSize()))    // "30" → 30, 파싱 실패/null이면 null
                    .view("-")                              // 오너 엔티티에 없음 → 기본값
                    .bed("-")                               // 오너 엔티티에 없음 → 기본값
                    .bath(null)                             // 오너 엔티티에 없음
                    .smoke(null)                            // 오너 엔티티에 없음
                    .sharedBath(null)                       // 오너 엔티티에 없음
                    .window(null)                           // 오너 엔티티에 없음
                    .aircon(null)                           // 오너 엔티티에 없음
                    .water(null)                            // 오너 엔티티에 없음
                    .wifi(null)                             // 오너 엔티티에 없음
                    .cancelPolicy(null)                     // 오너 엔티티에 없음
                    .payment(null)                          // 오너 엔티티에 없음
                    .originalPrice(null)                    // 오너 엔티티에 없음
                    .price(r.getPrice())
                    .lastBookedHours(3)                     // 데모 값
                    .photos(photos)
                    .promos(List.of(
                            Map.of("type","deal","title","특별 할인 쿠폰 적용됨","desc","코드 WEEKENDSALE")
                    ))
                    .qty(r.getRoomCount())
                    .build());
        }

        HotelDetailDto.HotelDto hotelDto = HotelDetailDto.HotelDto.builder()
                .id(h.getId())
                .name(h.getName())
                .address(h.getAddress())
                .description(h.getDescription())
                .images(hotelImages)
                // 데모 데이터 (실사용 시 테이블/엔티티 연결로 교체)
                .badges(List.of("International Deals", "베스트셀러", "PREFERRED"))
                .rating(new HotelDetailDto.Rating(6.4, Map.of(
                        "위치",7.2, "서비스",6.7, "가성비",6.7, "부대시설",5.7
                )))
                .highlights(List.of(
                        new HotelDetailDto.Highlight("📍","도심 근접","중심가 위치"),
                        new HotelDetailDto.Highlight("⭐","다양한 액티비티","투어/이벤트"),
                        new HotelDetailDto.Highlight("🚌","공항 이동 교통편","셔틀/픽업"),
                        new HotelDetailDto.Highlight("🕒","24시간 체크인","야간 도착 OK")
                ))
                .amenities(new HotelDetailDto.Amenities(
                        List.of("무료 Wi-Fi","조식","24시간 프런트 데스크","투어"),
                        List.of("공항 이동 서비스","세탁","여행 가방 보관","택시 서비스")
                ))
                .notice("오늘 17명의 여행객이 이 숙소 예약함")
                .build();

        HotelDetailDto dto = new HotelDetailDto();
        dto.setHotel(hotelDto);
        dto.setRooms(roomDtos);
        return dto;
    }

    private Integer parseIntSafe(String s) {
        try { return (s == null || s.isBlank()) ? null : Integer.parseInt(s); }
        catch (Exception e) { return null; }
    }

    private String safeRoomName(Room r) {
        return (r.getRoomType() != null) ? r.getRoomType().name() : "객실";
    }
}
