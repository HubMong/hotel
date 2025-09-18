import axios from 'axios';

// Axios instance configured to hit Vite proxy at /api
// In production, this should point to the real backend base URL if proxy is not used.
const http = axios.create({
  baseURL: '/api',
  withCredentials: false,
  timeout: 10000,
});

// Inject JWT token if present
http.interceptors.request.use((config) => {
  const token = localStorage.getItem('token');
  if (token) {
    config.headers = config.headers || {};
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

export default http;
