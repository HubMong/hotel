package com.example.backend.HotelOwner.service;

import com.example.backend.HotelOwner.domain.Hotel;
import com.example.backend.HotelOwner.repository.OwnerHotelRepository;
import com.example.backend.authlogin.domain.User;
import com.example.backend.authlogin.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Owner 도메인의 호텔 조회 전용 파사드.
 * - OwnerInquiryService 등에서 오너 소유 호텔 범위 검증/조회에 사용.
 */
@Service("ownerHotelService")
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class HotelService {

    private final OwnerHotelRepository ownerHotelRepository;
    private final UserRepository userRepository;

    /**
     * 오너가 소유한 호텔 목록을 반환.
     * 프로젝트 정책상 1호텔만 운영이면 0~1개 리스트가 반환된다.
     */
    public List<Hotel> getHotelsByOwner(Long ownerId) {
        User owner = userRepository.findById(ownerId)
                .orElseThrow(() -> new IllegalArgumentException("소유주를 찾을 수 없습니다. id=" + ownerId));

        // OwnerHotelRepository 시그니처에 따라 분기:
        // case A) Optional<Hotel> findByOwner(User owner)  (예: OwnerReservationService에서 사용)
        return ownerHotelRepository.findByOwner(owner)
                .map(List::of)
                .orElse(List.of());

        // case B) 만약 프로젝트가 List<Hotel> findAllByOwner(User owner) 라면 위를 아래로 교체:
        // return ownerHotelRepository.findAllByOwner(owner);
    }

    /**
     * 특정 호텔 단건 조회 (권한검증은 호출자에서 수행)
     */
    public Hotel getHotel(Long hotelId) {
        return ownerHotelRepository.findById(hotelId)
                .orElseThrow(() -> new IllegalArgumentException("호텔을 찾을 수 없습니다. id=" + hotelId));
    }

    //추가 고객 문의 목록
    public List<Hotel> getHotelsByIds(List<Long> ids) {
        return ownerHotelRepository.findAllByIdIn(ids);
    }
}
