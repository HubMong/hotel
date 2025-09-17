import http from '@/api/http'

export default {
  // 내 호텔
  myHotels(params={}) { return http.get('/owner/hotels', { params }).then(r=>r.data) },
  getHotel(id)       { return http.get(`/owner/hotels/${id}`).then(r=>r.data) },
  createHotel(p)     { return http.post('/owner/hotels', p).then(r=>r.data) },
  updateHotel(id,p)  { return http.put(`/owner/hotels/${id}`, p).then(r=>r.data) },

  // 이미지
  addHotelImage(id, payload){ return http.post(`/owner/hotels/${id}/images`, payload).then(r=>r.data) },
  deleteHotelImage(imageId){ return http.delete(`/owner/hotel-images/${imageId}`).then(r=>r.data) },

  // 편의시설
  setHotelAmenities(id, amenityIds){ return http.put(`/owner/hotels/${id}/amenities`, { amenityIds }).then(r=>r.data) },

  // 객실
  addRoom(hotelId,p){ return http.post(`/owner/hotels/${hotelId}/rooms`, p).then(r=>r.data) },
  updateRoom(roomId,p){ return http.put(`/owner/rooms/${roomId}`, p).then(r=>r.data) },
  addRoomImage(roomId,p){ return http.post(`/owner/rooms/${roomId}/images`, p).then(r=>r.data) },
  deleteRoomImage(imageId){ return http.delete(`/owner/room-images/${imageId}`).then(r=>r.data) },
}
