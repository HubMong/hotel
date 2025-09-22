
import http from './http'

export default {
 hold(payload){ return http.post(`/reservations/hold`, payload).then(r=>r.data) },
  get(id){ return http.get(`/reservations/${id}`).then(r=>r.data) },
 confirm(id){ return http.post(`/reservations/${id}/confirm`).then(r=>r.data) },
  cancel(id){ return http.post(`/reservations/${id}/cancel`).then(r=>r.data) },
}
