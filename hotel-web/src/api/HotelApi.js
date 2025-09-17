// src/api/HotelApi.js
import http from './http'

export default {
  getDetail(id) {
    return http.get(`/hotels/${id}`).then(r => r.data)
  },
}
