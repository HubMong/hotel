// src/api/HotelApi.js
import http from './http';

// 백엔드 응답을 가공하지 않고 그대로 반환
async function getDetail(id) {
  return await http.get(`/hotels/${id}`); // http는 res.data만 돌려줌
}

export default { getDetail };
