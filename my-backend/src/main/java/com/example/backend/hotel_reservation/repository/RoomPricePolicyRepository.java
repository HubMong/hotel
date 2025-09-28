package com.example.backend.hotel_reservation.repository;

import com.example.backend.hotel_reservation.domain.RoomPricePolicy;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Optional;

public interface RoomPricePolicyRepository extends JpaRepository<RoomPricePolicy, Long> {

    /**
     * 특정 객실 ID와 날짜를 기준으로 적용 가능한 가격 정책의 가격을 조회합니다.
     * @param roomId 객실 ID
     * @param date 조회할 날짜
     * @return 해당 날짜에 적용되는 가격 (Optional)
     */
    @Query("SELECT p.price FROM RoomPricePolicy p " +
           "WHERE p.roomId = :roomId AND :date BETWEEN p.startDate AND p.endDate")
    Optional<Integer> findApplicablePrice(@Param("roomId") Long roomId, @Param("date") LocalDate date);
}