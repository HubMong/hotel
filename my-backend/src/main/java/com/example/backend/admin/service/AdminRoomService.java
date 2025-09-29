package com.example.backend.admin.service;

import com.example.backend.HotelOwner.domain.Room;
import com.example.backend.HotelOwner.repository.RoomRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminRoomService {
    private final RoomRepository roomRepository;

    /** 기존 호출과의 호환용 (type 없음) */
    public Page<Room> list(Long hotelId, String name, Pageable pageable) {
        return list(hotelId, name, null, pageable);
    }

    /** type 파라미터까지 받는 실제 메서드 */
    public Page<Room> list(Long hotelId, String name, String type, Pageable pageable) {
        // 레포에 페이징 쿼리가 없으므로 전체 조회 후 수동 페이징
        List<Room> all = (hotelId != null)
                ? roomRepository.findByHotelId(hotelId)   // 레포에 존재하는 메서드
                : roomRepository.findAll();

        // name: 호텔명 포함 검색 (Room 엔티티에 room.name 필드가 없기 때문)
        if (name != null && !name.isBlank()) {
            String qLower = name.trim().toLowerCase(Locale.ROOT);
            all = all.stream()
                    .filter(r -> r.getHotel() != null
                            && r.getHotel().getName() != null
                            && r.getHotel().getName().toLowerCase(Locale.ROOT).contains(qLower))
                    .collect(Collectors.toList());
        }

        // type: RoomType(enum) 필터 — 정확히 못 맞추면 부분일치로 처리
        if (type != null && !type.isBlank()) {
            String t = type.trim();
            // 1) 정확 매칭 시도 (대소문자 무시)
            Room.RoomType exact = parseRoomTypeSafe(t);
            if (exact != null) {
                Room.RoomType target = exact;
                all = all.stream()
                        .filter(r -> r.getRoomType() == target)
                        .collect(Collectors.toList());
            } else {
                // 2) 부분일치 (enum 이름 문자열에 포함되면 통과)
                String tLower = t.toLowerCase(Locale.ROOT);
                all = all.stream()
                        .filter(r -> r.getRoomType() != null
                                && r.getRoomType().name().toLowerCase(Locale.ROOT).contains(tLower))
                        .collect(Collectors.toList());
            }
        }

        int start = (int) pageable.getOffset();
        int end = Math.min(start + pageable.getPageSize(), all.size());
        List<Room> pageContent = (start >= end) ? List.of() : all.subList(start, end);

        return new PageImpl<>(pageContent, pageable, all.size());
    }

    private Room.RoomType parseRoomTypeSafe(String s) {
        if (s == null || s.isBlank()) return null;
        // enum 이름이 한글이어서 toUpperCase 의미가 없을 수 있음: 그대로 먼저 시도
        try { return Room.RoomType.valueOf(s); } catch (Exception ignored) {}
        // 대소문자 무시 매칭 시도
        try { return Room.RoomType.valueOf(s.toUpperCase(Locale.ROOT)); } catch (Exception ignored) {}
        return null;
    }

    public Room get(Long id) { return roomRepository.findById(id).orElseThrow(); }

    public void delete(Long id) { roomRepository.deleteById(id); }
}
