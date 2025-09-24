import http from './http'

async function getRoomSummary(roomId) {
  const { data } = await http.get(`/rooms/${roomId}/summary`)
  return data
}

export default {
  getDetail(hotelId){
    return http.get(`/hotels/${hotelId}`).then(r => r.data)
  },
  getRoomSummary
}
