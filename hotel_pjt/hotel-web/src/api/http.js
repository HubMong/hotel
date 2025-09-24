// src/api/http.js
import axios from 'axios';

// Vite 프록시(/api) 사용
const http = axios.create({
  baseURL: '/api',
  baseURL: 'http://localhost:8080/api', // <- 여기 변경
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