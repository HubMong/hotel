import http from './http';

// 예약 홀드/확정/취소/상세
export const hold = (payload) => http.post(`/reservations/hold`, payload).then(r => r.data);
export const get = (id) => http.get(`/reservations/${id}`).then(r => r.data);
export const confirm = (id) => http.post(`/reservations/${id}/confirm`).then(r => r.data);
export const cancel = (id) => http.post(`/reservations/${id}/cancel`).then(r => r.data);

// ★ 내 예약 조회: page/size 기본값으로 400 방지
export const getMy = (page = 0, size = 10) =>
  http.get(`/reservations/my`, { params: { page, size } }).then(r => r.data);

// 선택: 특정 사용자 ID로 조회(관리자/마이그레이션용)
export const getByUserId = (userId, page = 0, size = 10) =>
  http.get(`/reservations/user/${userId}`, { params: { page, size } }).then(r => r.data);

// 기존 default도 유지(기존 코드 호환)
export default { hold, get, confirm, cancel, getMy, getByUserId };
