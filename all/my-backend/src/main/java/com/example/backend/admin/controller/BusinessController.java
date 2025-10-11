package com.example.backend.admin.controller;

import com.example.backend.authlogin.domain.User;
import com.example.backend.authlogin.repository.LoginRepository;
import com.example.backend.HotelOwner.domain.Hotel;
import com.example.backend.HotelOwner.repository.HotelRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Slf4j
@RestController
@RequestMapping("/api/businesses")
@RequiredArgsConstructor
public class BusinessController {
	
	private final LoginRepository loginRepository;
	private final HotelRepository hotelRepository;
	
	@GetMapping("/ping")
	public ResponseEntity<String> ping() {
		return ResponseEntity.ok("business-api-ok");
	}
	
	@PostMapping("/apply")
public ResponseEntity<Map<String, Object>> applyBusiness(@RequestBody BusinessApplicationRequest request) {
    try {
        log.info("사업자 등록 신청 시작");

        // 현재 로그인한 사용자 정보 가져오기
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            log.warn("인증되지 않은 사용자의 사업자 등록 신청");
            return ResponseEntity.status(401)
                .body(buildResponse(false, "로그인이 필요합니다.", null));
        }

        String userEmail = auth.getName();
        log.info("사업자 등록 신청 사용자: {}", userEmail);

        Optional<User> userOpt = loginRepository.findByEmail(userEmail);
        if (userOpt.isEmpty()) {
            log.warn("사용자를 찾을 수 없음: {}", userEmail);
            return ResponseEntity.status(404)
                .body(buildResponse(false, "사용자 정보를 찾을 수 없습니다.", null));
        }

        User user = userOpt.get();
        log.info("사용자 정보 확인 완료 - ID: {}, Role: {}", user.getId(), user.getRole());

        // 이미 사업자인지 확인
        if (user.getRole() == User.Role.BUSINESS) {
            log.warn("이미 사업자인 사용자의 중복 신청: {}", userEmail);
            return ResponseEntity.badRequest()
                .body(buildResponse(false, "이미 사업자로 등록되어 있습니다.", null));
        }

        // 입력값 검증
        if (request.getBusinessName() == null || request.getBusinessName().trim().isEmpty()) {
            return ResponseEntity.badRequest()
                .body(buildResponse(false, "사업자명을 입력해주세요.", null));
        }

        if (request.getHotelName() == null || request.getHotelName().trim().isEmpty()) {
            return ResponseEntity.badRequest()
                .body(buildResponse(false, "호텔명을 입력해주세요.", null));
        }

        if (request.getBusinessNumber() == null || request.getBusinessNumber().trim().isEmpty()) {
            return ResponseEntity.badRequest()
                .body(buildResponse(false, "사업자 등록번호를 입력해주세요.", null));
        }

        // 사업자 등록번호 형식 검증 (000-00-00000)
        String businessNumberInput = request.getBusinessNumber().trim();
        if (!businessNumberInput.matches("\\d{3}-\\d{2}-\\d{5}")) {
            log.warn("잘못된 사업자 등록번호 형식: {}", businessNumberInput);
            return ResponseEntity.badRequest()
                .body(buildResponse(false, "사업자 등록번호는 000-00-00000 형식으로 입력해주세요.", null));
        }

        if (request.getAddress() == null || request.getAddress().trim().isEmpty()) {
            return ResponseEntity.badRequest()
                .body(buildResponse(false, "주소를 입력해주세요.", null));
        }

        if (request.getPhone() == null || request.getPhone().trim().isEmpty()) {
            return ResponseEntity.badRequest()
                .body(buildResponse(false, "연락처를 입력해주세요.", null));
        }

        String businessName = request.getBusinessName().trim();
        String hotelName = request.getHotelName().trim();
        String address = request.getAddress().trim();
        String phone = request.getPhone().trim();
        String businessNumberRaw = businessNumberInput.replaceAll("-", "");
        Long businessNumber;

        // 사업자 등록번호 중복 확인
        try {
            businessNumber = Long.parseLong(businessNumberRaw);
            Optional<Hotel> existingHotel = hotelRepository.findByBusinessId(businessNumber);
            if (existingHotel.isPresent()) {
                log.warn("중복된 사업자 등록번호: {}", businessNumber);
                return ResponseEntity.badRequest()
                    .body(buildResponse(false, "이미 등록된 사업자 등록번호입니다.", null));
            }
        } catch (NumberFormatException e) {
            log.warn("사업자 등록번호 파싱 실패: {}", businessNumberInput);
            return ResponseEntity.badRequest()
                .body(buildResponse(false, "사업자 등록번호 처리 중 오류가 발생했습니다.", null));
        }

        // 사용자 역할을 BUSINESS로 변경
        user.setRole(User.Role.BUSINESS);
        user.setPhone(phone);
        loginRepository.save(user);
        log.info("사용자 역할 변경 완료 - ID: {}, 새 Role: {}", user.getId(), user.getRole());

        // Hotel 테이블에 사업자 정보 저장 (승인 대기 상태로)
        Hotel hotel = Hotel.builder()
            .owner(user)
            .businessId(businessNumber)
            .businessName(businessName)
            .name(hotelName)
            .address(address)
            .country("한국")
            .description("사업자 등록 신청 - " + businessName)
            .approvalStatus(Hotel.ApprovalStatus.PENDING)
            .build();

        hotelRepository.save(hotel);
        log.info("Hotel 정보 저장 완료 - ID: {}, UserId: {}, BusinessId: {}",
            hotel.getId(), hotel.getUserId(), hotel.getBusinessId());

        // 성공 응답
        Map<String, Object> data = new HashMap<>();
        data.put("hotelId", hotel.getId());
        data.put("approvalStatus", hotel.getApprovalStatus().toString());

        return ResponseEntity.ok(buildResponse(true,
            "사업자 등록 신청이 완료되었습니다. 관리자 승인을 기다려 주세요.", data));

    } catch (Exception e) {
        log.error("사업자 등록 신청 중 오류 발생", e);
        return ResponseEntity.status(500)
            .body(buildResponse(false, "Internal server error", null));
    }
}

	/** ✅ 공통 응답 생성 메서드 */
	private Map<String, Object> buildResponse(boolean success, String message, Object data) {
		Map<String, Object> map = new HashMap<>();
		map.put("success", success);
		map.put("message", message);
		map.put("data", data);
		return map;
	}

	
	// 요청 DTO 클래스
	@lombok.Data
	public static class BusinessApplicationRequest {
		private String businessName;
		private String hotelName;
		private String businessNumber;
		private String address;
		private String phone;
	}
}