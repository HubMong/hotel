// src/api/AdminApi.js
import http from '@/api/http'

export default {
  // --- Businesses ---
  getBusinesses(params = {}) {
    return http.get('/admin/businesses', { params }).then(r => r.data)
  },
  approveBusiness(id, note = '') {
    return http.put(`/admin/businesses/${id}/status`, { status: 'APPROVED', note }).then(r => r.data)
  },
  rejectBusiness(id, note = '') {
    return http.put(`/admin/businesses/${id}/status`, { status: 'REJECTED', note }).then(r => r.data)
  },

  // --- Hotels ---
  getHotels(params = {}) {
    return http.get('/admin/hotels', { params }).then(r => r.data)
  },
  getHotel(id) {
    return http.get(`/admin/hotels/${id}`).then(r => r.data)
  },
  createHotel(payload) {
    return http.post('/admin/hotels', payload).then(r => r.data)
  },
  updateHotel(id, payload) {
    return http.put(`/admin/hotels/${id}`, payload).then(r => r.data)
  },
  toggleHotelActive(id, active) {
    return http.put(`/admin/hotels/${id}`, { active }).then(r => r.data)
  },
}
