package com.example.backend.review.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.backend.review.domain.UserReviewImage;

public interface UserReviewImageRepository extends JpaRepository<UserReviewImage, Long> {
}
