import http from './http'
export default {
  list(params={}) { return http.get('/hotels', { params }) },     // (권장) 목록 API
  get(id){ return http.get(`/hotels/${id}`) },
  updateBasic(id, payload){ return http.put(`/hotels/${id}`, payload) }, // (권장) 기본정보 수정
  setStatus(id, status){ return http.put(`/hotels/${id}/status`, { status }) }, // (권장) PUBLISHED/PAUSED
  // 이미지
  listImages(hotelId){ return http.get(`/hotels/${hotelId}/images`) },        // (권장)
  addImage(hotelId, payload){ return http.post(`/hotels/${hotelId}/images`, payload) },
  updateImage(imageId, payload){ return http.put(`/hotel-images/${imageId}`, payload) },
  removeImage(imageId){ return http.delete(`/hotel-images/${imageId}`) },
  // 편의시설
  getAmenities(hotelId){ return http.get(`/hotels/${hotelId}/amenities`) },
  saveAmenities(hotelId, amenityIds){ return http.put(`/hotels/${hotelId}/amenities`, { amenityIds }) },
}
