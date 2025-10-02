// src/api/SupportApi.js (최종 수정)
import http from './http';
import ReservationApi from './ReservationApi'; // getByUserId를 가져오기 위해 필요

// 공통 응답 처리 함수 toList는 ReservationApi.js에 정의되어 있거나 여기에 복사되어 있어야 합니다.

// 🔥🔥 [수정] /api/support/bookings -> /support/bookings 로 변경 (중복 제거)
export const fetchReservationsWithHotelInfo = async (userId) => {
    // 백엔드에서 이미 2단계 조인을 처리했기 때문에, 프론트는 단일 호출만 수행
    // userId는 쿼리 파라미터로 전달
    const res = await http.get(`/support/bookings`, { params: { userId } }); 
    
    // toList 함수는 이 파일 또는 ReservationApi에서 가져와야 합니다.
    // 여기서는 응답 데이터를 바로 반환한다고 가정합니다.
    return res.data; 
};

// 1:1 문의 전송 API
export const submitInquiry = async (payload) => {
    // 404 오류 방지를 위해 /api를 제거합니다.
    const res = await http.post('/inquiries', payload); 
    return res.data;
};

export default { fetchReservationsWithHotelInfo, submitInquiry };