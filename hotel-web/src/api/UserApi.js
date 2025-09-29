import http from './http';

export default {
  // JWT 토큰 헤더 포함 사용자 정보 조회
  getInfo: async () => {
    const token = localStorage.getItem('token');
    const res = await http.get('/user/info', {
      headers: { Authorization: `Bearer ${token}` }
    });
    return res.data;
  },

  // 특정 사용자 ID로 조회
  getById: async (userId) => {
    const res = await http.get(`/users/${userId}`);
    return res.data;
  },

  
  // 💡 [추가] 사용자 정보(이메일 등) 수정
  updateInfo: async (userId, payload) => {
    // PATCH /users/{userId}
    const res = await http.patch(`/users/${userId}`, payload);
    return res.data;
  },
  // 💡 [추가] 비밀번호 변경
  updatePassword: async (userId, payload) => {
    // PATCH /users/{userId}/password
    const res = await http.patch(`/users/${userId}/password`, payload);
    return res.data;
  },
  // 🏆 [추가] 동시 수정 API: 이메일과 비밀번호를 단일 요청으로 처리
  updateProfileAndPass: async (userId, payload) => {
    // 백엔드에 새로 구현될 단일 엔드포인트: PATCH /users/{userId}/profile 가정
    const res = await http.patch(`/users/${userId}/profile`, payload);
    return res.data;
  },
};
