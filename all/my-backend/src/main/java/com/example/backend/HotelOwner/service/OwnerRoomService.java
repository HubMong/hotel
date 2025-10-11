// src/main/java/com/example/backend/HotelOwner/service/OwnerRoomService.java
package com.example.backend.HotelOwner.service;

import com.example.backend.HotelOwner.domain.Hotel;
import com.example.backend.HotelOwner.domain.Room;
import com.example.backend.HotelOwner.domain.RoomImage;
import com.example.backend.HotelOwner.dto.OwnerRoomDto;
import com.example.backend.HotelOwner.dto.OwnerRoomDto.UpdateRequest;
import com.example.backend.HotelOwner.file.FileStorageService; // ← 인터페이스 주입(파일 패키지)
import com.example.backend.HotelOwner.repository.HotelRepository;
import com.example.backend.HotelOwner.repository.RoomImageRepository;
import com.example.backend.HotelOwner.repository.RoomRepository;
import com.example.backend.authlogin.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.nio.file.AccessDeniedException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Service
@RequiredArgsConstructor
public class OwnerRoomService {

    private final RoomRepository roomRepository;
    private final RoomImageRepository roomImageRepository;
    private final HotelRepository hotelRepository;
    private final UserRepository userRepository;
    private final FileStorageService fileStorageService; // ← 구현체는 file 패키지에 @Service로 등록

    /** 공통: 소유 호텔 가져오기(없으면 404, 소유 아니면 404로 통일) */
    private Hotel getMyHotelOr404(Long ownerId, Long hotelId) throws AccessDeniedException {
        return hotelRepository.findByIdAndOwnerId(hotelId, ownerId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "호텔을 찾을 수 없습니다."));
    }

    /** 안전 불리언 */
    private static boolean b(Boolean v) { return v != null && v; }

    // =========================
    // 객실 등록
    // =========================
    @Transactional
    public Long registerRoom(Long ownerId, Long hotelId,
                             OwnerRoomDto.RegisterRequest request,
                             List<MultipartFile> images) throws AccessDeniedException {

        // 소유주, 호텔 검증
        userRepository.findById(ownerId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "소유주 없음"));
        Hotel hotel = getMyHotelOr404(ownerId, hotelId);

        var f = request.getFacilities(); // null 가능
        Room room = Room.builder()
                .hotel(hotel)
                .name(request.getName())
                .roomType(Room.RoomType.valueOf(request.getRoomType()))
                .price(request.getPrice())
                .originalPrice(request.getOriginalPrice())
                .roomSize(request.getRoomSize() != null ? request.getRoomSize() : null) // 프런트가 숫자만 보낼 수도 있어 문자열 보존
                .roomCount(request.getRoomCount() == null ? 1 : request.getRoomCount())
                .capacityMin(request.getCapacityMin())
                .capacityMax(request.getCapacityMax())
                .checkInTime(request.getCheckInTime())
                .checkOutTime(request.getCheckOutTime())
                .bed(request.getBed())
                .bath(f == null ? null : f.getBath())
                .viewName(request.getViewName())
                .payment(request.getPayment())
                .cancelPolicy(request.getCancelPolicy())
                .smoke(f != null && b(f.isSmoke()))
                .aircon(f != null && b(f.isAircon()))
                .wifi(f != null && b(f.isWifi()))
                .freeWater(f != null && b(f.isFreeWater()))
                .hasWindow(f != null && b(f.isHasWindow()))
                // status/재고 기본값 폴백(검색 집계 포함되도록)
                .status( (request.getStatus() == null || request.getStatus().isBlank()) ? "ACTIVE" : request.getStatus() )
                .build();

        Room saved = roomRepository.save(room);

        // 이미지 저장
        if (images != null && !images.isEmpty()) {
            List<RoomImage> toSave = new ArrayList<>();
            for (int i = 0; i < images.size(); i++) {
                String url = fileStorageService.storeFile(images.get(i));
                RoomImage img = RoomImage.builder()
                        .room(saved)
                        .url(url)
                        .sortNo(i)
                        .isCover(i == 0) // 첫 번째 이미지를 커버로
                        .build();
                toSave.add(img);
            }
            roomImageRepository.saveAll(toSave);
        }

        return saved.getId();
    }

    // =========================
    // 객실 목록(특정 호텔)
    // =========================
    @Transactional(readOnly = true)
    public List<OwnerRoomDto.ListResponse> getRoomsForOwner(Long ownerId, Long hotelId) throws AccessDeniedException {
        if (hotelId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "hotelId가 필요합니다.");
        }
        Hotel hotel = getMyHotelOr404(ownerId, hotelId);
        List<Room> rooms = roomRepository.findByHotelId(hotel.getId());
        return rooms.stream()
                .map(OwnerRoomDto.ListResponse::fromEntity)
                .collect(Collectors.toList());
    }

    // =========================
    // 객실 상세
    // =========================
    @Transactional(readOnly = true)
    public OwnerRoomDto.DetailResponse getRoomDetails(Long ownerId, Long roomId) throws AccessDeniedException {
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "객실을 찾을 수 없습니다."));
        if (!Objects.equals(room.getHotel().getOwner().getId(), ownerId)) {
            throw new AccessDeniedException("이 객실을 조회할 권한이 없습니다.");
        }
        return OwnerRoomDto.DetailResponse.fromEntity(room);
    }

    // =========================
    // 객실 수정
    // =========================
    @Transactional
    public void updateRoom(Long ownerId, Long roomId, UpdateRequest request, List<MultipartFile> newImages) throws AccessDeniedException {
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "객실을 찾을 수 없습니다."));
        if (!Objects.equals(room.getHotel().getOwner().getId(), ownerId)) {
            throw new AccessDeniedException("이 객실을 수정할 권한이 없습니다.");
        }

        // 기본 필드
        room.setName(request.getName());
        room.setRoomType(Room.RoomType.valueOf(request.getRoomType()));
        room.setPrice(request.getPrice());
        room.setOriginalPrice(request.getOriginalPrice());
        room.setRoomSize(request.getRoomSize() != null ? request.getRoomSize() : null);
        room.setRoomCount(request.getRoomCount());
        room.setCapacityMin(request.getCapacityMin());
        room.setCapacityMax(request.getCapacityMax());
        room.setCheckInTime(request.getCheckInTime());
        room.setCheckOutTime(request.getCheckOutTime());
        room.setBed(request.getBed());
        room.setViewName(request.getViewName());
        room.setPayment(request.getPayment());
        room.setCancelPolicy(request.getCancelPolicy());
        // status는 필요 시 DTO에 따라 세팅(없으면 유지)
        if (request.getStatus() != null && !request.getStatus().isBlank()) {
            room.setStatus(request.getStatus());
        }

        // 편의시설 NPE 방어
        var f = request.getFacilities(); // null 가능
        room.setSmoke(f != null && b(f.isSmoke()));
        room.setBath(f == null ? null : f.getBath());
        room.setAircon(f != null && b(f.isAircon()));
        room.setWifi(f != null && b(f.isWifi()));
        room.setFreeWater(f != null && b(f.isFreeWater()));
        room.setHasWindow(f != null && b(f.isHasWindow()));

        // ===== 기존 이미지 삭제 처리 =====
        if (request.getDeletedImages() != null && !request.getDeletedImages().isEmpty()) {
            List<RoomImage> toDelete = room.getImages().stream()
                    .filter(img -> request.getDeletedImages().contains(img.getUrl()))
                    .collect(Collectors.toList());

            // 실제 파일도 삭제 시도(실패해도 서비스 계속)
            toDelete.forEach(img -> {
                try { fileStorageService.deleteFile(img.getUrl()); } catch (Exception ignore) {}
            });

            room.getImages().removeAll(toDelete);
            roomImageRepository.deleteAll(toDelete);
        }

        // ===== 새 이미지 추가 =====
        if (newImages != null && !newImages.isEmpty()) {
            int maxSortNo = room.getImages().stream().mapToInt(RoomImage::getSortNo).max().orElse(-1);
            boolean hasCover = room.getImages().stream().anyMatch(RoomImage::isCover);

            List<RoomImage> toAdd = new ArrayList<>();
            for (int i = 0; i < newImages.size(); i++) {
                String url = fileStorageService.storeFile(newImages.get(i));
                RoomImage img = RoomImage.builder()
                        .room(room)
                        .url(url)
                        .sortNo(maxSortNo + 1 + i)
                        .isCover(!hasCover && i == 0) // 기존에 커버 없으면 첫 신규를 커버로
                        .build();
                toAdd.add(img);
            }
            roomImageRepository.saveAll(toAdd);
            room.getImages().addAll(toAdd);
        }

        // ===== 커버 보정(최소 1개) =====
        if (!room.getImages().isEmpty() && room.getImages().stream().noneMatch(RoomImage::isCover)) {
            room.getImages().stream()
                    .min(Comparator.comparingInt(RoomImage::getSortNo))
                    .ifPresent(first -> first.setCover(true));
        }

        roomRepository.save(room);
    }

    // =========================
    // 객실 삭제
    // =========================
    @Transactional
    public void deleteRoom(Long ownerId, Long roomId) throws AccessDeniedException {
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "객실을 찾을 수 없습니다."));
        if (!Objects.equals(room.getHotel().getOwner().getId(), ownerId)) {
            throw new AccessDeniedException("이 객실을 삭제할 권한이 없습니다.");
        }

        // 이미지 파일 먼저 정리(실패 무시)
        List<RoomImage> imgs = new ArrayList<>(room.getImages());
        imgs.forEach(img -> {
            try { fileStorageService.deleteFile(img.getUrl()); } catch (Exception ignore) {}
        });

        roomRepository.delete(room);
    }
}
