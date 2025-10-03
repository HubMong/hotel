package com.example.backend.HotelOwner.repository;

import com.example.backend.HotelOwner.domain.Amenity;
import com.example.backend.HotelOwner.domain.HotelAmenity;
import com.example.backend.HotelOwner.domain.HotelAmenityId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface HotelAmenityRepository extends JpaRepository<HotelAmenity, HotelAmenityId> {

    // 호텔에 달린 편의시설 전체 조회 (응답 DTO로 감싸도 됨)
    List<Amenity> findAmenitiesByHotel_Id(Long hotelId);

    // 호텔 기준 일괄 삭제 (교체/재저장 전에 사용)
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Transactional
    int deleteByHotel_Id(Long hotelId);

    // 개별 존재 여부 체크(옵션)
    boolean existsByHotel_IdAndAmenity_Id(Long hotelId, Long amenityId);
}
