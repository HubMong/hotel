package com.example.backend.admin.dto;

import com.example.backend.HotelOwner.domain.Room;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalTime;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Room 엔티티의 응답 DTO
 * LazyInitializationException 방지를 위해 필요한 데이터만 포함
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RoomResponse {
    private Long id;
    private Long hotelId;
    private String hotelName;
    private String name;
    private String roomType;
    private String roomSize;
    private String status;
    private Integer capacityMin;
    private Integer capacityMax;
    private LocalTime checkInTime;
    private LocalTime checkOutTime;
    private Integer price;
    private Integer originalPrice;
    private Integer roomCount;
    private Boolean wifi;
    private Boolean aircon;
    private Boolean freeWater;
    private Boolean hasWindow;
    private Boolean sharedBath;
    private Boolean smoke;
    private String bed;
    private Integer bath;
    private String viewName;
    private String cancelPolicy;
    private String payment;
    private String primaryImageUrl;
    private List<String> imageUrls;

    /**
     * Room 엔티티를 RoomResponse DTO로 변환
     */
    public static RoomResponse from(Room room) {
        if (room == null) {
            return null;
        }
        
        // LazyInitializationException 방지를 위해 안전하게 처리
        List<String> imageUrls = List.of();
        try {
            if (room.getImages() != null && !room.getImages().isEmpty()) {
                imageUrls = room.getImages().stream()
                        .sorted(Comparator.comparingInt(image -> image.getSortNo()))
                        .map(com.example.backend.HotelOwner.domain.RoomImage::getUrl)
                        .filter(url -> url != null && !url.isBlank())
                        .collect(Collectors.toList());
            } else {
                // no images
            }
        } catch (Exception e) {
            // ignore transformation exception to keep API resilient
        }

        String primaryImageUrl = imageUrls.isEmpty() ? null : imageUrls.get(0);
        
        return RoomResponse.builder()
                .id(room.getId())
                .hotelId(room.getHotel() != null ? room.getHotel().getId() : null)
                .hotelName(room.getHotel() != null ? room.getHotel().getName() : null)
                .name(room.getName())
                .roomType(room.getRoomType() != null ? room.getRoomType().name() : null)
                .roomSize(room.getRoomSize())
                .status(room.getStatus())
                .capacityMin(room.getCapacityMin())
                .capacityMax(room.getCapacityMax())
                .checkInTime(room.getCheckInTime())
                .checkOutTime(room.getCheckOutTime())
                .price(room.getPrice())
                .originalPrice(room.getOriginalPrice())
                .roomCount(room.getRoomCount())
                .wifi(room.getWifi())
                .aircon(room.getAircon())
                .freeWater(room.getFreeWater())
                .hasWindow(room.getHasWindow())
                .sharedBath(room.getSharedBath())
                .smoke(room.getSmoke())
                .bed(room.getBed())
                .bath(room.getBath())
                .viewName(room.getViewName())
                .cancelPolicy(room.getCancelPolicy())
                .payment(room.getPayment())
                .primaryImageUrl(primaryImageUrl)
                .imageUrls(imageUrls)
                .build();
    }
}
