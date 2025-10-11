// src/main/java/com/example/backend/website_support/service/InquiryService.java
package com.example.backend.website_support.service;

import com.example.backend.admin.dto.AdminInquiryResponse;
import com.example.backend.admin.repository.InquiryUserRepository;
import com.example.backend.authlogin.domain.User;
import com.example.backend.website_support.domain.WebsiteInquiry;
import com.example.backend.website_support.dto.CreateInquiryRequest;
import com.example.backend.website_support.dto.InquiryResponse;
import com.example.backend.website_support.repository.InquirySpecification;
import com.example.backend.website_support.repository.WebsiteInquiryRepository;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true) // 클래스 레벨에 공통으로 적용 (읽기 전용이 기본)
public class InquiryService {

    private final WebsiteInquiryRepository websiteInquiryRepository;
    private final InquiryUserRepository userRepository;

    public InquiryService(WebsiteInquiryRepository websiteInquiryRepository, InquiryUserRepository userRepository) {
        this.websiteInquiryRepository = websiteInquiryRepository;
        this.userRepository = userRepository;
    }

    @Transactional // 쓰기 작업이므로 readOnly=false (기본값) 적용
    public void createInquiry(CreateInquiryRequest dto, Long userId) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new RuntimeException("User not found with id: " + userId));
        WebsiteInquiry inquiry = dto.toEntity();
        inquiry.setUser(user);
        websiteInquiryRepository.save(inquiry);
    }

    // getMyInquiries는 클래스 레벨의 @Transactional(readOnly = true)가 적용됨
    public List<InquiryResponse> getMyInquiries(Long userId) {
        List<WebsiteInquiry> inquiries = websiteInquiryRepository.findByUser_IdOrderByCreatedAtDesc(userId);
        return inquiries.stream()
                .map(InquiryResponse::new)
                .collect(Collectors.toList());
    }
    
    // getAllInquiries는 클래스 레벨의 @Transactional(readOnly = true)가 적용됨
    public List<AdminInquiryResponse> getAllInquiries(String status, String userName) {
        // Specification<WebsiteInquiry> spec = Specification.where(null);

        Specification<WebsiteInquiry> spec = Specification.where(InquirySpecification.fetchUser());

        if (status != null && !status.isEmpty()) {
            spec = spec.and(InquirySpecification.hasStatus(status));
        }
        if (userName != null && !userName.isEmpty()) {
            spec = spec.and(InquirySpecification.containsUserName(userName));
        }

        List<WebsiteInquiry> inquiries = websiteInquiryRepository.findAll(spec, Sort.by(Sort.Direction.DESC, "createdAt"));
        
        return inquiries.stream()
                .map(AdminInquiryResponse::new)
                .collect(Collectors.toList());
    }

    @Transactional // 쓰기 작업이므로 readOnly=false (기본값) 적용
    public void addReply(Long inquiryId, String replyContent) {
        WebsiteInquiry inquiry = websiteInquiryRepository.findById(inquiryId)
                .orElseThrow(() -> new RuntimeException("Inquiry not found with id: " + inquiryId));
        
        inquiry.setAdminReply(replyContent);
        inquiry.setRepliedAt(LocalDateTime.now());
        inquiry.setStatus("ANSWERED");
        
        websiteInquiryRepository.save(inquiry);
    }
    
    @Transactional(readOnly=true)
    public WebsiteInquiry getInquiryByIdForUser(Long inquiryId, Long userId) {
        // return websiteInquiryRepository.findByIdAndUser_Id(inquiryId, userId)
        return websiteInquiryRepository.findByIdAndUser_IdWithUserFetch(inquiryId, userId)
                .orElseThrow(() -> new RuntimeException("Inquiry not found or user does not have permission"));
    }

    // 🔹 (추가) 미답변(PENDING) 문의 개수 반환 → 관리자 알림용
    public long countUnansweredInquiries() {
    return websiteInquiryRepository.countUnansweredInquiries();
    }
}