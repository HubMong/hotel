package com.example.backend.hotel_support.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

/**
 * 오너가 문의에 답변을 등록할 때 사용하는 DTO
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class ReplyRequest {
    private String replyContent;
}
