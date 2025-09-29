// src/api/ReservationApi.js
import http from './http';

// 공통: 어떤 응답이 와도 배열로 통일
function toList(data) {
  if (!data) return [];
  if (Array.isArray(data)) return data;

  // 흔한 형태들
  if (Array.isArray(data.content)) return data.content; // Spring Page
  if (Array.isArray(data.items)) return data.items;
  if (Array.isArray(data.rows)) return data.rows;
  if (Array.isArray(data.list)) return data.list;
  if (Array.isArray(data.reservations)) return data.reservations;

  // 혹시 한 번 더 래핑된 경우
  if (data.data) return toList(data.data);

  return [];
}

// ===== 예약 홀드/확정/취소/상세 =====
export const hold    = (payload) => http.post(`/reservations/hold`, payload).then(r => r.data);
export const get     = (id)      => http.get(`/reservations/${id}`).then(r => r.data);
export const confirm = (id)      => http.post(`/reservations/${id}/confirm`).then(r => r.data);
export const cancel  = (id)      => http.post(`/reservations/${id}/cancel`).then(r => r.data);
export const expire  = (id)      => http.post(`/reservations/${id}/expire`).then(r => r.data);

// ★ 내 예약 조회: 항상 "배열"로 반환
export const getMy = async (page = 0, size = 100) => {
  const res = await http.get(`/reservations/my`, { params: { page, size } });
  return toList(res.data);
};

// (옵션) 특정 사용자 ID로 조회: 항상 "배열"로 반환
export const getByUserId = async (userId, page = 0, size = 20) => {
  const res = await http.get(`/reservations/user/${userId}`, { params: { page, size } });
  return toList(res.data);
};


export default { hold, get, confirm, cancel, expire, getMy, getByUserId };
