import http from './http'
export default {
  list(params={}) {
    // 권장: 백엔드에 status=PENDING 필터 추가
    return http.get('/businesses', { params })
  },
  get(id){ return http.get(`/businesses/${id}`) },
  setStatus(id, status, reason){
    return http.put(`/businesses/${id}/status`, { status, reason })
  }
}
