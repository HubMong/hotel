// src/api/http.js
import axios from "axios";

// Allow Vite env (VITE_API_BASE_URL) to override, otherwise follow current page origin.
const envBase = import.meta.env?.VITE_API_BASE_URL;
const runtimeOrigin =
  typeof window !== "undefined" && window.location?.origin
    ? window.location.origin
    : "https://hwiyeong.shop";

// Normalise so consumers can set value with or without trailing slash.
const normalize = (value) => (value.endsWith("/") ? value.slice(0, -1) : value);
const resolvedBase = normalize(envBase ?? `${runtimeOrigin}/api`);
const backendOrigin = resolvedBase.replace(/\/api$/, "");

const ensureLeadingSlash = (path) => (path.startsWith("/") ? path : `/${path}`);

export const apiBaseUrl = resolvedBase;
export const resolveBackendUrl = (path = "") => {
  if (!path) {
    return backendOrigin;
  }

  if (/^https?:\/\//i.test(path)) {
    return path;
  }

  return `${backendOrigin}${ensureLeadingSlash(path)}`;
};

export const backendBaseOrigin = backendOrigin;

const http = axios.create({
  baseURL: resolvedBase,
  headers: { "Content-Type": "application/json" },
  withCredentials: true,
});

//  JWT 자동 첨부
http.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem("accessToken");
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
  },
  (error) => Promise.reject(error)
);

// 401 에러 전역 처리 (선택 사항)
// 만약 특정 컴포넌트에서 catch하지 않은 401 에러가 콘솔에 찍히는 것을 막고 싶다면
// 아래와 같이 interceptor를 추가할 수 있습니다.
// 하지만 axios는 기본적으로 에러를 throw하므로, catch하지 않으면 Uncaught error가 발생합니다.
// 여기서는 단순히 에러를 다시 throw하되, 필요하다면 로깅을 억제하는 로직을 추가할 수 있습니다.

export default http;