// src/main/java/com/example/backend/HotelOwner/dto/OwnerRoomDto.java
package com.example.backend.HotelOwner.dto;

import com.example.backend.HotelOwner.domain.Room;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.*;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class OwnerRoomDto {

    /* ===== 공통 서브 DTO ===== */
    @Getter @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Facilities {
        private Boolean aircon;
        private Boolean wifi;
        private Boolean freeWater;
        private Boolean hasWindow;
        private Boolean sharedBath;
        private Boolean smoke;
        private Integer bath;

        // 서비스 호환 boolean 헬퍼 (NPE 방지)
        public boolean isAircon()    { return Boolean.TRUE.equals(aircon); }
        public boolean isWifi()      { return Boolean.TRUE.equals(wifi); }
        public boolean isFreeWater() { return Boolean.TRUE.equals(freeWater); }
        public boolean isHasWindow() { return Boolean.TRUE.equals(hasWindow); }
        public boolean isSharedBath(){ return Boolean.TRUE.equals(sharedBath); }
        public boolean isSmoke()     { return Boolean.TRUE.equals(smoke); }
    }

    /* ===== 등록/수정 공통 필드 ===== */
    @Getter @Setter
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class BaseUpsert {
        private String  name;
        private String  roomType;        // Room.RoomType.name() 사용 (예: "디럭스룸")
        private Integer price;
        private Integer originalPrice;

        // 크기: 프론트가 roomSize(string) 또는 size(int)로 보낼 수 있음 → 둘 다 수용
        private String  roomSize;        // "23" 또는 "23m²" 등
        private Integer size;            // 숫자만 오는 레거시 케이스

        private Integer roomCount;
        private Integer capacityMin;
        private Integer capacityMax;

        // 요청은 LocalTime으로 받아서 자동 파싱
        @JsonFormat(pattern = "HH:mm")
        @DateTimeFormat(pattern = "HH:mm")
        private LocalTime checkInTime;

        @JsonFormat(pattern = "HH:mm")
        @DateTimeFormat(pattern = "HH:mm")
        private LocalTime checkOutTime;

        private String  bed;
        private Integer bath;            // Facilities.bath와 중복 가능 → 서비스에서 facilities 우선 사용
        private String  cancelPolicy;
        private String  payment;
        private String  viewName;

        // 검색 노출 위해 기본 ACTIVE 권장(프론트 기본값도 ACTIVE)
        private String  status;

        // 프론트가 톱레벨 boolean들로 보낼 수도 있어 허용(선택)
        private Boolean aircon;
        private Boolean wifi;
        private Boolean freeWater;
        private Boolean hasWindow;
        private Boolean sharedBath;
        private Boolean smoke;

        private Facilities facilities;       // null 가능
        private List<String> deletedImages;  // 수정 시 삭제할 이미지 URL 목록

        /** 서비스 호환: roomSize 의사 게터 */
        public String getRoomSize() {
            if (roomSize != null && !roomSize.isBlank()) return roomSize;
            if (size != null) return String.valueOf(size);
            return null;
        }

        /** 서비스 호환: facilities 의사 게터(톱레벨 값으로 백업 구성) */
        public Facilities getFacilities() {
            if (facilities != null) return facilities;
            return Facilities.builder()
                    .aircon(aircon)
                    .wifi(wifi)
                    .freeWater(freeWater)
                    .hasWindow(hasWindow)
                    .sharedBath(sharedBath)
                    .smoke(smoke)
                    .bath(bath)
                    .build();
        }
    }

    /* ===== 등록 요청 ===== */
    @Getter @Setter
    @NoArgsConstructor
    public static class RegisterRequest extends BaseUpsert { }

    /* ===== 수정 요청 ===== */
    @Getter @Setter
    @NoArgsConstructor
    public static class UpdateRequest extends BaseUpsert { }

    /* ===== 목록 응답 ===== */
    @Getter @Builder
    public static class ListResponse {
        private Long    id;
        private String  name;
        private String  roomType;
        private Integer roomCount;
        private String  capacity; // "min~max명"
        private Integer price;

        public static ListResponse fromEntity(Room r) {
            String cap;
            if (r.getCapacityMin() == null && r.getCapacityMax() == null) cap = "-";
            else if (r.getCapacityMin() != null && r.getCapacityMax() != null)
                cap = r.getCapacityMin() + "~" + r.getCapacityMax() + "명";
            else cap = (r.getCapacityMin() != null ? r.getCapacityMin() : r.getCapacityMax()) + "명";

            return ListResponse.builder()
                    .id(r.getId())
                    .name(r.getName())
                    .roomType(r.getRoomType() != null ? r.getRoomType().name() : null)
                    .roomCount(r.getRoomCount())
                    .capacity(cap)
                    .price(r.getPrice())
                    .build();
        }
    }

    /* ===== 상세 응답 =====
       프론트 <input type="time">와 호환되도록 시간은 "HH:mm" 문자열로 내려줌 */
    @Getter @Builder
    public static class DetailResponse {
        private Long     id;
        private String   name;
        private String   roomType;
        private Integer  price;
        private Integer  originalPrice;
        private String   roomSize;
        private Integer  roomCount;
        private Integer  capacityMin;
        private Integer  capacityMax;
        private String   checkInTime;    // "HH:mm"
        private String   checkOutTime;   // "HH:mm"
        private String   bed;
        private Integer  bath;
        private String   cancelPolicy;
        private String   payment;
        private String   viewName;
        private String   status;

        private Boolean  aircon;
        private Boolean  wifi;
        private Boolean  freeWater;
        private Boolean  hasWindow;
        private Boolean  sharedBath;
        private Boolean  smoke;

        private List<String> imageUrls;

        public static DetailResponse fromEntity(Room r) {
            DateTimeFormatter F = DateTimeFormatter.ofPattern("HH:mm");
            return DetailResponse.builder()
                    .id(r.getId())
                    .name(r.getName())
                    .roomType(r.getRoomType() != null ? r.getRoomType().name() : null)
                    .price(r.getPrice())
                    .originalPrice(r.getOriginalPrice())
                    .roomSize(r.getRoomSize())
                    .roomCount(r.getRoomCount())
                    .capacityMin(r.getCapacityMin())
                    .capacityMax(r.getCapacityMax())
                    .checkInTime(r.getCheckInTime()  != null ? r.getCheckInTime().format(F)  : null)
                    .checkOutTime(r.getCheckOutTime() != null ? r.getCheckOutTime().format(F) : null)
                    .bed(r.getBed())
                    .bath(r.getBath())
                    .cancelPolicy(r.getCancelPolicy())
                    .payment(r.getPayment())
                    .viewName(r.getViewName())
                    .status(r.getStatus())
                    .aircon(bool(r.getAircon()))
                    .wifi(bool(r.getWifi()))
                    .freeWater(bool(r.getFreeWater()))
                    .hasWindow(bool(r.getHasWindow()))
                    .sharedBath(bool(r.getSharedBath()))
                    .smoke(bool(r.getSmoke()))
                    .imageUrls(r.getImages() == null ? List.of()
                            : r.getImages().stream().map(img -> img.getUrl()).toList())
                    .build();
        }
        private static Boolean bool(Boolean v) { return v != null && v; }
    }
}
