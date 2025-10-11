package com.example.backend.review.service;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.example.backend.HotelOwner.domain.Room;
import com.example.backend.HotelOwner.repository.RoomRepository;
import com.example.backend.authlogin.domain.User;
import com.example.backend.authlogin.repository.UserRepository;
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

        if (!reservation.getUserId().equals(userId)) {
            throw new SecurityException("본인 예약만 리뷰 작성 가능합니다.");
        }

        if (reviewRepository.existsByReservationId(reservationId)) {
            throw new IllegalStateException("이미 작성한 리뷰가 존재합니다.");
        }

        List<String> imagePaths = saveImages(files);

        UserReview review = UserReview.builder()
                .reservation(reservation)
                .rating(rating)
                .cleanliness(cleanliness)
                .service(service)
                .value(value)
                .location(location)
                .facilities(facilities)
                .content(content)
                .images(String.join(",", imagePaths))
                .hidden(false)
                .reported(false)
                .build();

        return toDto(reviewRepository.save(review));
    }

    /* ✅ 리뷰 수정 (세부 평점 포함) */
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

        // ✅ 평점 관련 업데이트
        review.setContent(content);
        review.setRating(rating);
        review.setCleanliness(cleanliness);
        review.setService(service);
        review.setValue(value);
        review.setLocation(location);
        review.setFacilities(facilities);

        // ✅ 이미지 처리
        List<String> currentImages = new ArrayList<>();
        if (review.getImages() != null && !review.getImages().isEmpty()) {
            currentImages.addAll(Arrays.asList(review.getImages().split(",")));
        }

        if (deleteImages != null && !deleteImages.isEmpty()) {
            for (String delImg : deleteImages) {
                File file = new File(System.getProperty("user.dir") + delImg);
                if (file.exists()) file.delete();
                currentImages.removeIf(img -> img.trim().equals(delImg.trim()));
            }
        }

        List<String> newImages = saveImages(files);
        currentImages.addAll(newImages);
        review.setImages(String.join(",", currentImages));

        UserReview saved = reviewRepository.save(review);
        UserReviewResponseDto dto = toDto(saved);

        // ✅ 통계 자동 반영
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

        if (review.getImages() != null) {
            for (String img : review.getImages().split(",")) {
                File file = new File(System.getProperty("user.dir") + img);
                if (file.exists()) file.delete();
            }
        }

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

    /* ✅ 통계 계산 */
    @Transactional(readOnly = true)
    public Map<String, Object> getHotelReviewStats(Long hotelId) {
        List<UserReview> reviews = reviewRepository.findByHotelId(hotelId);

        if (reviews.isEmpty()) {
            return Map.of("average", 0.0, "count", 0);
        }

        double avg = reviews.stream().mapToDouble(UserReview::getRating).average().orElse(0.0);
        return Map.of("average", Math.round(avg * 10.0) / 10.0, "count", reviews.size());
    }

    /* ✅ 이미지 저장 */
    private List<String> saveImages(List<MultipartFile> files) {
        List<String> imagePaths = new ArrayList<>();
        if (files == null || files.isEmpty()) return imagePaths;

        String uploadDir = System.getProperty("user.dir") + "/uploads/";
        File dir = new File(uploadDir);
        if (!dir.exists()) dir.mkdirs();

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
        if (review.getReservation() != null && review.getReservation().getRoomId() != null) {
            var roomOpt = roomRepository.findById(review.getReservation().getRoomId());
            if (roomOpt.isPresent()) {
                var room = roomOpt.get();
                if (room.getHotel() != null) {
                    hotelName = room.getHotel().getName();
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
                .hotelName(hotelName)
                .rating(review.getRating())
                .content(review.getContent())
                .images(imageList)
                .cleanliness(review.getCleanliness())
                .service(review.getService())
                .value(review.getValue())
                .location(review.getLocation())
                .facilities(review.getFacilities())
                .createdAt(review.getCreatedAt())
                .adminReply(review.getAdminReply())
                .build();
    }
}
