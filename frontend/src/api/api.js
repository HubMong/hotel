import axios from "axios";

const API_BASE = "http://localhost:8888"; // 백엔드 주소

const api = axios.create({
    baseURL: API_BASE,
});

// JWT 토큰 자동 추가
api.interceptors.request.use((config) => {
    const token = localStorage.getItem("token");
    if (token) {
        config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
});

export default api;
