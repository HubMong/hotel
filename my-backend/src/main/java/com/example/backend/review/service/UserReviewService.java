package com.example.backend.review.service;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.example.backend.HotelOwner.domain.Room;
import com.example.backend.HotelOwner.repository.RoomRepository;
import com.example.backend.authlogin.domain.User;
import com.example.backend.authlogin.repository.UserRepository;
import com.example.backend.common.util.HtmlSanitizer;
import com.example.backend.hotel_reservation.domain.Reservation;
import com.example.backend.hotel_reservation.repository.ReservationRepository;
import com.example.backend.review.domain.UserReview;
import com.example.backend.review.dto.UserReviewResponseDto;
import com.example.backend.review.repository.UserReviewRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserReviewService {

    private final UserReviewRepository reviewRepository;
    private final ReservationRepository reservationRepository;
    private final RoomRepository roomRepository;
    private final UserRepository userRepository;

    /* ✅ 특정 호텔 리뷰 조회 */
    @Transactional(readOnly = true)
    public List<UserReviewResponseDto> getHotelReviews(Long hotelId) {
        return reviewRepository.findByHotelId(hotelId)
                .stream()
                .filter(r -> !r.isHidden())
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    /* ✅ 특정 유저의 리뷰 조회 */
    @Transactional(readOnly = true)
    public List<UserReviewResponseDto> getUserReviews(Long userId) {
        return reviewRepository.findByReservation_UserId(userId)
                .stream()
                .filter(r -> !r.isHidden())
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    /* ✅ 리뷰 등록 */
@Transactional
public UserReviewResponseDto createReview(
        Long reservationId,
        Long userId,
        String content,
        double rating,
        double cleanliness,
        double service,
        double value,
        double location,
        double facilities,
        List<MultipartFile> files
) {
    Reservation reservation = reservationRepository.findById(reservationId)
            .orElseThrow(() -> new IllegalArgumentException("❌ 예약 정보를 찾을 수 없습니다."));

    if (reservation.getUserId() == null || !reservation.getUserId().equals(userId)) {
        throw new SecurityException("본인 예약만 리뷰 작성 가능합니다.");
    }

    if (reviewRepository.existsByReservationIdAndReservation_UserId(reservationId, userId)) {
        throw new IllegalStateException("이미 작성한 리뷰가 존재합니다.");
    }

    List<String> imagePaths = saveImages(files);

    // ✅ 평균 평점 자동 계산 및 반올림 (소수점 1자리)
    double avg = (cleanliness + service + value + location + facilities) / 5.0;
    avg = Math.round(avg * 10) / 10.0; // ✅ 2.666 → 2.7

    String safeContent = HtmlSanitizer.sanitize(content);

    UserReview review = UserReview.builder()
            .reservation(reservation)
            .rating(avg) // ✅ 평균 반올림값으로 통일
            .cleanliness(Math.round(cleanliness * 10) / 10.0)
            .service(Math.round(service * 10) / 10.0)
            .value(Math.round(value * 10) / 10.0)
            .location(Math.round(location * 10) / 10.0)
            .facilities(Math.round(facilities * 10) / 10.0)
        .content(safeContent)
            .images(String.join(",", imagePaths))
            .hidden(false)
            .reported(false)
            .build();

    UserReview saved = reviewRepository.save(review);
    UserReviewResponseDto dto = toDto(saved);

    Long hotelId = getHotelIdFromReview(saved);
    if (hotelId != null) {
        dto.setHotelStats(getHotelReviewStats(hotelId));
    }

    return dto;
}

/* ✅ 리뷰 수정 */
@Transactional
public UserReviewResponseDto updateReview(
        Long reviewId,
        Long userId,
        String content,
        double rating,
        double cleanliness,
        double service,
        double value,
        double location,
        double facilities,
        List<MultipartFile> files,
        List<String> deleteImages
) {
    UserReview review = reviewRepository.findById(reviewId)
            .orElseThrow(() -> new IllegalArgumentException("리뷰를 찾을 수 없습니다."));

    if (!review.getReservation().getUserId().equals(userId)) {
        throw new SecurityException("본인 리뷰만 수정 가능합니다.");
    }

    // ✅ 평균 평점 다시 계산 후 반올림
    double avg = (cleanliness + service + value + location + facilities) / 5.0;
    avg = Math.round(avg * 10) / 10.0;

    review.setContent(HtmlSanitizer.sanitize(content));
    review.setRating(avg);
    review.setCleanliness(Math.round(cleanliness * 10) / 10.0);
    review.setService(Math.round(service * 10) / 10.0);
    review.setValue(Math.round(value * 10) / 10.0);
    review.setLocation(Math.round(location * 10) / 10.0);
    review.setFacilities(Math.round(facilities * 10) / 10.0);

    // ✅ 이미지 처리 로직 (기존 그대로)
    List<String> currentImages = new ArrayList<>();
    if (review.getImages() != null && !review.getImages().isEmpty()) {
        currentImages.addAll(Arrays.asList(review.getImages().split(",")));
    }

    if (deleteImages != null && !deleteImages.isEmpty()) {
        for (String delImg : deleteImages) {
            File file = new File(System.getProperty("user.dir") + delImg);
            if (file.exists() && !file.delete()) {
                log.warn("리뷰 이미지 삭제 실패 - path={}", file.getAbsolutePath());
            }
            currentImages.removeIf(img -> img.trim().equals(delImg.trim()));
        }
    }

    List<String> newImages = saveImages(files);
    currentImages.addAll(newImages);
    review.setImages(String.join(",", currentImages));

    UserReview saved = reviewRepository.save(review);
    UserReviewResponseDto dto = toDto(saved);

    Long hotelId = getHotelIdFromReview(saved);
    if (hotelId != null) {
        dto.setHotelStats(getHotelReviewStats(hotelId));
    }

    return dto;
}

    /* ✅ 리뷰 삭제 */
    @Transactional
    public void deleteReview(Long reviewId, Long userId) {
        UserReview review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new IllegalArgumentException("리뷰를 찾을 수 없습니다."));

        if (!review.getReservation().getUserId().equals(userId)) {
            throw new SecurityException("본인 리뷰만 삭제 가능합니다.");
        }

        // ✅ 업로드 이미지 삭제
        if (review.getImages() != null && !review.getImages().isEmpty()) {
            for (String img : review.getImages().split(",")) {
                File file = new File(System.getProperty("user.dir") + img);
                if (file.exists() && !file.delete()) {
                    log.warn("리뷰 이미지 삭제 실패 - path={}", file.getAbsolutePath());
                }
            }
        }

        // ✅ 리뷰 삭제
        reviewRepository.delete(review);
    }

    /* ✅ 호텔 ID 추출 */
    private Long getHotelIdFromReview(UserReview review) {
        try {
            Long roomId = review.getReservation().getRoomId();
            if (roomId == null) return null;
            Room room = roomRepository.findById(roomId).orElse(null);
            return (room != null && room.getHotel() != null) ? room.getHotel().getId() : null;
        } catch (Exception e) {
            log.warn("호텔 ID 추출 실패: {}", e.getMessage());
            return null;
        }
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getHotelReviewStats(Long hotelId) {
        List<UserReview> list = reviewRepository.findByHotelId(hotelId)
            .stream()
            .filter(r -> !r.isHidden())
            .toList();

        if (list.isEmpty()) {
            return Map.of(
                "average", 0.0,
                "count", 0,
                "details", defaultDetails()
            );
        }

        double avgCleanliness = averageScore(list, UserReview::getCleanliness);
        double avgService     = averageScore(list, UserReview::getService);
        double avgValue       = averageScore(list, UserReview::getValue);
        double avgLocation    = averageScore(list, UserReview::getLocation);
        double avgFacilities  = averageScore(list, UserReview::getFacilities);

        double avg = (avgCleanliness + avgService + avgValue + avgLocation + avgFacilities) / 5.0;

        // ✅ 반올림 고정 (소수점 1자리)
        avg            = Math.round(avg * 10) / 10.0;
        avgCleanliness = Math.round(avgCleanliness * 10) / 10.0;
        avgService     = Math.round(avgService * 10) / 10.0;
        avgValue       = Math.round(avgValue * 10) / 10.0;
        avgLocation    = Math.round(avgLocation * 10) / 10.0;
        avgFacilities  = Math.round(avgFacilities * 10) / 10.0;

        Map<String, Object> details = new LinkedHashMap<>();
        details.put("숙소 청결 상태", avgCleanliness);
        details.put("서비스", avgService);
        details.put("가격 대비 만족도", avgValue);
        details.put("위치", avgLocation);
        details.put("부대시설", avgFacilities);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("average", avg);
        result.put("count", list.size());
        result.put("details", details);

        return result;
    }

    private Map<String, Object> defaultDetails() {
        Map<String, Object> details = new LinkedHashMap<>();
        details.put("숙소 청결 상태", 0.0);
        details.put("서비스", 0.0);
        details.put("가격 대비 만족도", 0.0);
        details.put("위치", 0.0);
        details.put("부대시설", 0.0);
        return details;
    }
    
    /* ✅ 이미지 저장 */
    private List<String> saveImages(List<MultipartFile> files) {
        List<String> imagePaths = new ArrayList<>();
        if (files == null || files.isEmpty()) return imagePaths;

        String uploadDir = System.getProperty("user.dir") + "/uploads/";
        File dir = new File(uploadDir);
        if (!dir.exists() && !dir.mkdirs()) {
            log.warn("리뷰 이미지 업로드 디렉터리 생성 실패 - path={}", dir.getAbsolutePath());
        }

        for (MultipartFile file : files) {
            try {
                String fileName = System.currentTimeMillis() + "_" + file.getOriginalFilename();
                String filePath = uploadDir + fileName;
                file.transferTo(new File(filePath));
                imagePaths.add("/uploads/" + fileName);
            } catch (IOException e) {
                throw new RuntimeException("이미지 저장 실패", e);
            }
        }
        return imagePaths;
    }

    /* ✅ DTO 변환 */
    private UserReviewResponseDto toDto(UserReview review) {
    String userName = "익명 사용자";
    Long userId = review.getReservation().getUserId();

    if (userId != null) {
        userName = userRepository.findById(userId)
                .map(User::getName)
                .orElse("탈퇴한 사용자");
    }

    String hotelName = "알 수 없는 숙소";
    Long hotelId = null;

    if (review.getReservation() != null && review.getReservation().getRoomId() != null) {
        var roomOpt = roomRepository.findById(review.getReservation().getRoomId());
        if (roomOpt.isPresent()) {
            var room = roomOpt.get();
            if (room.getHotel() != null) {
                hotelName = room.getHotel().getName();
                hotelId = room.getHotel().getId(); // ✅ 추가됨!
            } else {
                hotelName = room.getName();
            }
        }
    }

    List<String> imageList = new ArrayList<>();
    if (review.getImages() != null && !review.getImages().isEmpty()) {
        imageList = List.of(review.getImages().split(","));
    }

    return UserReviewResponseDto.builder()
            .id(review.getId())
            .userId(userId)
            .userName(userName)
            .hotelId(hotelId)              // ✅ 추가됨
            .hotelName(hotelName)
            .reservationId(review.getReservation() != null ? review.getReservation().getId() : null)
            .rating(review.getRating())
            .content(review.getContent())
            .images(imageList)
            .cleanliness(safeScore(review.getCleanliness()))
            .service(safeScore(review.getService()))
            .value(safeScore(review.getValue()))
            .location(safeScore(review.getLocation()))
            .facilities(safeScore(review.getFacilities()))
            .createdAt(review.getCreatedAt())
            .adminReply(review.getAdminReply())
            .build();
}

    private double averageScore(List<UserReview> reviews, Function<UserReview, Double> extractor) {
        return reviews.stream()
                .map(extractor)
                .filter(Objects::nonNull)
                .mapToDouble(Double::doubleValue)
                .average()
                .orElse(0.0);
    }

    private double safeScore(Double value) {
        return value == null ? 0.0 : value;
    }
}
