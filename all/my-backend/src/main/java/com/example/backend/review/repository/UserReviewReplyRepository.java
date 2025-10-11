package com.example.backend.review.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.backend.review.domain.UserReviewReply;

public interface UserReviewReplyRepository extends JpaRepository<UserReviewReply, Long> {
}
