import http from './http'
export default {
  list(){ return http.get('/amenities') } // 권장: 마스터 조회
}
