import http from './http'

// (옵션) 룸 기준 요약. 백엔드에 있으면 사용, 없으면 호출하지 않음.
async function getRoomSummary(roomId) {
  const { data } = await http.get(`/rooms/${roomId}/summary`)
  return data
}

// 기존에 이미 있는 getDetail(hotelId)는 그대로 두고 사용
export default {
  getDetail(hotelId){
    return http.get(`/hotels/${hotelId}`).then(r => r.data)
  },
  getRoomSummary
}
