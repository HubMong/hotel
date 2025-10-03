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
					.body(Map.of("success", false, "message", "로그인이 필요합니다.", "data", null));
			}
			
			String userEmail = auth.getName();
			log.info("사업자 등록 신청 사용자: {}", userEmail);
			
			Optional<User> userOpt = loginRepository.findByEmail(userEmail);
			if (userOpt.isEmpty()) {
				log.warn("사용자를 찾을 수 없음: {}", userEmail);
				return ResponseEntity.status(404)
					.body(Map.of("success", false, "message", "사용자 정보를 찾을 수 없습니다.", "data", null));
			}
			
			User user = userOpt.get();
			log.info("사용자 정보 확인 완료 - ID: {}, Role: {}", user.getId(), user.getRole());
			
			// 이미 사업자인지 확인
			if (user.getRole() == User.Role.BUSINESS) {
				log.warn("이미 사업자인 사용자의 중복 신청: {}", userEmail);
				return ResponseEntity.badRequest()
					.body(Map.of("success", false, "message", "이미 사업자로 등록되어 있습니다.", "data", null));
			}
			
			// 입력값 검증
			if (request.getBusinessName() == null || request.getBusinessName().trim().isEmpty()) {
				return ResponseEntity.badRequest()
					.body(Map.of("success", false, "message", "사업자명을 입력해주세요.", "data", null));
			}
			
			if (request.getBusinessNumber() == null || request.getBusinessNumber().trim().isEmpty()) {
				return ResponseEntity.badRequest()
					.body(Map.of("success", false, "message", "사업자 등록번호를 입력해주세요.", "data", null));
			}
			
			if (request.getAddress() == null || request.getAddress().trim().isEmpty()) {
				return ResponseEntity.badRequest()
					.body(Map.of("success", false, "message", "주소를 입력해주세요.", "data", null));
			}
			
			if (request.getPhone() == null || request.getPhone().trim().isEmpty()) {
				return ResponseEntity.badRequest()
					.body(Map.of("success", false, "message", "연락처를 입력해주세요.", "data", null));
			}
			
			// 사업자 등록번호 중복 확인
			try {
				Long businessNumber = Long.parseLong(request.getBusinessNumber().replaceAll("-", ""));
				Optional<Hotel> existingHotel = hotelRepository.findByBusinessId(businessNumber);
				if (existingHotel.isPresent()) {
					log.warn("중복된 사업자 등록번호: {}", businessNumber);
					return ResponseEntity.badRequest()
						.body(Map.of("success", false, "message", "이미 등록된 사업자 등록번호입니다.", "data", null));
				}
			} catch (NumberFormatException e) {
				log.warn("잘못된 사업자 등록번호 형식: {}", request.getBusinessNumber());
				return ResponseEntity.badRequest()
					.body(Map.of("success", false, "message", "올바른 사업자 등록번호 형식을 입력해주세요.", "data", null));
			}
			
			// 사용자 역할을 BUSINESS로 변경
			user.setRole(User.Role.BUSINESS);
			
			// 연락처 업데이트 (사업자 신청 시 입력한 연락처로)
			user.setPhone(request.getPhone());
			
			loginRepository.save(user);
			log.info("사용자 역할 변경 완료 - ID: {}, 새 Role: {}", user.getId(), user.getRole());
			
			// Hotel 테이블에 사업자 정보 저장 (승인 대기 상태로)
			Hotel hotel = Hotel.builder()
				.owner(user)
				.businessId(Long.parseLong(request.getBusinessNumber().replaceAll("-", "")))
				.name(request.getBusinessName())
				.address(request.getAddress())
				.country("한국")
				.description("사업자 등록 신청")
				.approvalStatus(Hotel.ApprovalStatus.PENDING)
				.build(); // starRating은 @Builder.Default로 0이 자동 설정됨
			
			hotelRepository.save(hotel);
			log.info("Hotel 정보 저장 완료 - ID: {}, UserId: {}, BusinessId: {}", 
				hotel.getId(), hotel.getUserId(), hotel.getBusinessId());
			
			return ResponseEntity.ok(Map.of(
				"success", true, 
				"message", "사업자 등록 신청이 완료되었습니다. 관리자 승인을 기다려 주세요.", 
				"data", Map.of(
					"hotelId", hotel.getId(),
					"approvalStatus", hotel.getApprovalStatus().toString()
				)
			));
			
		} catch (Exception e) {
			log.error("사업자 등록 신청 중 오류 발생", e);
			return ResponseEntity.status(500)
				.body(Map.of("success", false, "message", "Internal server error", "data", null));
		}
	}
	
	// 요청 DTO 클래스
	@lombok.Data
	public static class BusinessApplicationRequest {
		private String businessName;
		private String businessNumber;
		private String address;
		private String phone;
	}
}