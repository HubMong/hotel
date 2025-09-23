// src/api/http.js
import axios from 'axios'

const http = axios.create({
  baseURL: import.meta.env.VITE_API_BASE || '/api', // vite.config.js proxy or .env
  timeout: 8000,
})

export default http
