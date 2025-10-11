package com.example.backend.admin.service;

import com.example.backend.admin.domain.Review;
import com.example.backend.admin.repository.ReviewRepository;
import com.example.backend.admin.dto.ReviewDetailDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminReviewService {
    private final ReviewRepository reviewRepository;

    public Page<Review> list(Pageable pageable) {
        return reviewRepository.findAll(pageable);
    }

    // 상세 정보 포함 리뷰 목록 조회
    public Page<ReviewDetailDto> listWithDetails(Boolean reported, Boolean hidden,
                                                String hotelName, String userName,
                                                Pageable pageable) {
        log.info("리뷰 목록 조회 - reported: {}, hidden: {}, hotelName: {}, userName: {}",
                reported, hidden, hotelName, userName);

        Page<Object[]> results = reviewRepository.searchWithDetails(reported, hidden, hotelName, userName, pageable);

        log.info("조회된 리뷰 결과 수: {}", results.getContent().size());

        List<ReviewDetailDto> dtos = results.getContent().stream()
            .map(this::mapToReviewDetailDto)
            .toList();

        return new PageImpl<>(dtos, pageable, results.getTotalElements());
    }

    public ReviewDetailDto getDetail(Long id) {
        log.info("리뷰 상세 조회 시작 - ID: {}", id);

        if (!reviewRepository.existsById(id)) {
            log.warn("리뷰가 존재하지 않음 - ID: {}", id);
            throw new RuntimeException("리뷰를 찾을 수 없습니다: " + id);
        }

        Object[] queryResult = reviewRepository.findDetailById(id);
        if (queryResult == null || queryResult.length == 0) {
            log.warn("리뷰 상세 조회 결과 없음 - ID: {}", id);
            throw new RuntimeException("리뷰 상세 정보를 찾을 수 없습니다: " + id);
        }

        // JPA 네이티브 쿼리 결과가 중첩 배열로 반환되는 경우를 처리
        Object[] result = queryResult;
        if (result.length == 1 && result[0] instanceof Object[]) {
            result = (Object[]) result[0];
        }

        log.info("리뷰 상세 조회 성공 - ID: {}, 데이터 길이: {}", id, result.length);
        return mapToReviewDetailDto(result);
    }

    public Review get(Long id) {
        return reviewRepository.findById(id).orElseThrow();
    }

    public void delete(Long id) {
        reviewRepository.deleteById(id);
    }

    public void hide(Long id) {
        Review review = reviewRepository.findById(id).orElseThrow();
        review.setIsHidden(true);
        reviewRepository.save(review);
    }

    public void show(Long id) {
        Review review = reviewRepository.findById(id).orElseThrow();
        review.setIsHidden(false);
        reviewRepository.save(review);
    }

    // 관리자 답변 등록/수정
    public void addReply(Long reviewId, String reply) {
        if (reply == null || reply.trim().isEmpty()) {
            throw new IllegalArgumentException("답변 내용을 입력해주세요.");
        }

        Review review = reviewRepository.findById(reviewId)
            .orElseThrow(() -> new RuntimeException("리뷰를 찾을 수 없습니다: " + reviewId));

        review.setAdminReply(reply.trim());
        review.setRepliedAt(LocalDateTime.now());
        reviewRepository.save(review);

        log.info("관리자 답변 등록/수정 완료 - 리뷰 ID: {}", reviewId);
    }

    // 관리자 답변 삭제
    public void deleteReply(Long reviewId) {
        Review review = reviewRepository.findById(reviewId)
            .orElseThrow(() -> new RuntimeException("리뷰를 찾을 수 없습니다: " + reviewId));

        if (review.getAdminReply() == null || review.getAdminReply().trim().isEmpty()) {
            throw new RuntimeException("삭제할 답변이 존재하지 않습니다.");
        }

        review.setAdminReply(null);
        review.setRepliedAt(null);
        reviewRepository.save(review);

        log.info("관리자 답변 삭제 완료 - 리뷰 ID: {}", reviewId);
    }

    // 신고 처리 기능 제거됨

    private ReviewDetailDto mapToReviewDetailDto(Object[] row) {
        if (row == null || row.length < 16) {
            log.warn("리뷰 데이터 매핑 오류: 최소 16개 필드가 필요하지만 {}개만 조회됨", row != null ? row.length : 0);
            throw new IllegalArgumentException("조회된 리뷰 데이터가 불완전합니다");
        }

        return ReviewDetailDto.builder()
            .reviewId(safeLong(row[0]))
            .starRating(safeInteger(row[1]))
            .content(safeString(row[2]))
            .image(safeString(row[3]))
            .wroteOn(safeDateTime(row[4]))
            .isReported(safeBoolean(row[5]))
            .isHidden(safeBoolean(row[6]))
            .adminReply(safeString(row[7]))
            .repliedAt(safeDateTime(row[8]))
            .reservationId(safeLong(row[9]))
            .transactionId(safeString(row[10]))
            .hotelId(safeLong(row[11]))
            .hotelName(safeString(row[12]))
            .userId(safeLong(row[13]))
            .userName(safeString(row[14]))
            .userEmail(safeString(row[15]))
            .build();
    }

    private Long safeLong(Object obj) {
        if (obj == null) return null;
        try {
            if (obj instanceof Number) return ((Number) obj).longValue();
            if (obj instanceof String) return Long.parseLong((String) obj);
            return null;
        } catch (Exception e) {
            log.warn("Long 변환 실패: {}", obj, e);
            return null;
        }
    }

    private Integer safeInteger(Object obj) {
        if (obj == null) return null;
        try {
            if (obj instanceof Number) return ((Number) obj).intValue();
            if (obj instanceof String) return Integer.parseInt((String) obj);
            return null;
        } catch (Exception e) {
            log.warn("Integer 변환 실패: {}", obj, e);
            return null;
        }
    }

    private String safeString(Object obj) {
        if (obj == null) return null;
        try {
            return obj.toString().trim();
        } catch (Exception e) {
            log.warn("String 변환 실패: {}", obj, e);
            return null;
        }
    }

    private Boolean safeBoolean(Object obj) {
        if (obj == null) return false; // 기본값을 false로 설정
        try {
            if (obj instanceof Boolean) return (Boolean) obj;
            if (obj instanceof Number) return ((Number) obj).intValue() != 0;
            if (obj instanceof String) return Boolean.parseBoolean((String) obj);
            return false;
        } catch (Exception e) {
            log.warn("Boolean 변환 실패: {}", obj, e);
            return false;
        }
    }

    private LocalDateTime safeDateTime(Object obj) {
        if (obj == null) return null;
        try {
            if (obj instanceof java.sql.Timestamp) return ((java.sql.Timestamp) obj).toLocalDateTime();
            if (obj instanceof LocalDateTime) return (LocalDateTime) obj;
            if (obj instanceof java.sql.Date) return ((java.sql.Date) obj).toLocalDate().atStartOfDay();
            if (obj instanceof java.util.Date) return ((java.util.Date) obj).toInstant().atZone(java.time.ZoneId.systemDefault()).toLocalDateTime();
            return null;
        } catch (Exception e) {
            log.warn("LocalDateTime 변환 실패: {}", obj, e);
            return null;
        }
    }
}