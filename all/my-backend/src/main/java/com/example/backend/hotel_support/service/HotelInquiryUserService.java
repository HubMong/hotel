package com.example.backend.hotel_support.service;

import com.example.backend.authlogin.domain.User;
import com.example.backend.authlogin.repository.UserRepository;
import com.example.backend.hotel_reservation.domain.Reservation;
import com.example.backend.hotel_reservation.repository.ReservationRepository;
import com.example.backend.HotelOwner.domain.Room;
import com.example.backend.HotelOwner.repository.RoomRepository;
import com.example.backend.hotel_support.domain.HotelInquiry;
import com.example.backend.hotel_support.domain.InquiryStatus;
import com.example.backend.hotel_support.dto.HotelInquiryRequest;
import com.example.backend.hotel_support.dto.HotelInquiryResponse;
import com.example.backend.hotel_support.repository.HotelInquiryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class HotelInquiryUserService {

    private final HotelInquiryRepository inquiryRepository;
    private final ReservationRepository reservationRepository;
    private final RoomRepository roomRepository; 
    private final UserRepository userRepository;

    @Transactional
    public void createHotelInquiry(String username, HotelInquiryRequest request) {
        
        User user = userRepository.findByEmail(username)
                .orElseThrow(() -> new NoSuchElementException("사용자를 찾을 수 없습니다: " + username));
        Long userId = user.getId();

        Reservation reservation = reservationRepository.findById(request.getReservationId())
                .orElseThrow(() -> new NoSuchElementException("예약 정보를 찾을 수 없습니다."));

        if (!reservation.getUserId().equals(userId)) {
            throw new SecurityException("자신의 예약에 대해서만 문의할 수 있습니다.");
        }

        Room room = roomRepository.findById(reservation.getRoomId())
                .orElseThrow(() -> new NoSuchElementException("객실 정보를 찾을 수 없습니다."));
        
        if (room.getHotel() == null) {
            throw new NoSuchElementException("객실에 연결된 호텔 정보를 찾을 수 없습니다.");
        }
        
        Long hotelId = room.getHotel().getId();
        
        
        HotelInquiry inquiry = HotelInquiry.builder()
                .userId(userId)
                .hotelId(hotelId)
                .room(room) 
                .reservation(reservation)
                .title(request.getTitle())
                .message(request.getMessage())
                .status(InquiryStatus.PENDING.name())
                .createdAt(LocalDateTime.now())
                .build();
        
        inquiryRepository.save(inquiry);
    }

    public List<HotelInquiryResponse> getInquiriesByUserId(String username) {
        User user = userRepository.findByEmail(username)
                .orElseThrow(() -> new NoSuchElementException("사용자를 찾을 수 없습니다: " + username));
        Long userId = user.getId();

        // findByUserIdWithDetails가 DB 관계를 EAGER하게 가져온다고 가정
        List<HotelInquiry> inquiries = inquiryRepository.findByUserIdWithDetails(userId);
        
        return inquiries.stream()
                // 🔑 [수정] map 대신 flatMap을 사용할 필요가 없는 단순 변환이므로 map을 유지합니다.
                .map(inquiry -> {
                    // 호텔 이름 추출
                    String hotelName = (inquiry.getRoom() != null && inquiry.getRoom().getHotel() != null)
                                            ? inquiry.getRoom().getHotel().getName()
                                            : "호텔 정보 없음";
                                            
                    // 🔑 [추가] 객실 이름 추출
                    String roomName = (inquiry.getRoom() != null) 
                                          ? inquiry.getRoom().getName() // Room 엔티티에서 이름을 가져옴
                                          : "객실 정보 없음";

                    // 🔑 [수정] DTO 생성자에 4번째 인자(roomName) 추가
                    return new HotelInquiryResponse(
                        inquiry,
                        user.getName(), 
                        hotelName,
                        roomName // ⬅️ 객실 이름 전달
                    );
                })
                // 🔑 [수정] Stream 타입 추론 오류 방지를 위해 명시적 타입 지정 없이 toList() 사용
                .collect(Collectors.toList()); 
    }
}