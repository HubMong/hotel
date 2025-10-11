// src/api/InquiryApi.js (수정 완료 코드)

import http from './http';

/**
 * 💡 http.js에 인터셉터가 있으므로,
 * getConfig 함수 및 해당 함수의 모든 호출을 제거했습니다.
 */

const InquiryApi = {
  // --- 사용자용 API ---
  getMyInquiries: (params) => {
    // [수정] getConfig() 제거 및 params 추가 (필요하다면)
    return http.get('/hotel-inquiries/my', { params });
  },
  
  submitUserInquiry: (inquiryData) => {
    // [수정] getConfig() 제거
    return http.post('/hotel-inquiries', inquiryData);
  },

  // --- 점주용 API ---
  getOwnerInquiries: (params) => {
    // [수정] getConfig() 제거 및 params를 직접 설정
    return http.get('/owner/inquiries', { params });
  },

  submitOwnerReply: (inquiryId, replyData) => {
    // [수정] getConfig() 제거
    return http.post(`/owner/inquiries/${inquiryId}/reply`, replyData);
  }
};

export default InquiryApi;