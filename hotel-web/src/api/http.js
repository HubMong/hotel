// src/api/http.js
import axios from 'axios';

// Vite 프록시(/api) 사용
const http = axios.create({
  baseURL: '/api',
  withCredentials: false,
  timeout: 10000,
});

// JWT 헤더 주입
http.interceptors.request.use((config) => {
  const token = localStorage.getItem('token');
  if (token) {
    config.headers = config.headers || {};
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

export default http;
