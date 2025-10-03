// src/main/java/com/example/backend/website_support/repository/InquirySpecification.java
package com.example.backend.website_support.repository;

import com.example.backend.website_support.domain.WebsiteInquiry;
import org.springframework.data.jpa.domain.Specification;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;

public class InquirySpecification {

    public static Specification<WebsiteInquiry> fetchUser() {
        return (root, query, criteriaBuilder) -> {
            // COUNT 쿼리가 아닐 때만 fetch join을 적용하여 중복 데이터 문제를 방지
            if (query.getResultType() != Long.class && query.getResultType() != long.class) {
                root.fetch("user", JoinType.LEFT);
                query.distinct(true);
            }
            return criteriaBuilder.conjunction();
        };
    }

    public static Specification<WebsiteInquiry> hasStatus(String status) {
        return (root, query, criteriaBuilder) -> {
            if (status == null || status.isEmpty()) {
                return criteriaBuilder.conjunction(); // 조건이 없으면 아무것도 하지 않음
            }
            return criteriaBuilder.equal(root.get("status"), status);
        };
    }

    public static Specification<WebsiteInquiry> containsUserName(String userName) {
        return (root, query, criteriaBuilder) -> {
            if (userName == null || userName.isEmpty()) {
                return criteriaBuilder.conjunction();
            }
            // User 객체를 조인하고, 그 안의 name 필드를 검색
            return criteriaBuilder.like(root.join("user").get("name"), "%" + userName + "%");
        };
    }
}