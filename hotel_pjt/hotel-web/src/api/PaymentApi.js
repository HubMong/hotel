// src/api/PaymentApi.js
import http from './http'

export default {
  getMyPayments() {
    return http.get('/payments/my').then(r => r.data)
  }
}
