// src/router/index.js (예시)
import { createRouter, createWebHistory } from "vue-router";

// Auth components (정적 import 유지: 로그인/회원가입 초기 진입 속도)
import Login from "@/components/user/login_page/Login.vue";
import Register from "@/components/user/login_page/Register.vue";
import ForgotPassword from "@/components/user/login_page/ForgotPassword.vue";
import LoginVerify from "@/components/user/login_page/LoginVerify.vue";
import PasswordReset from "@/components/user/login_page/PasswordReset.vue";
import OAuth2Redirect from "@/components/user/login_page/OAuth2Redirect.vue";

// Page components
import MainPage from "@/components/user/main_page/MainPage.vue";
import TermsPage from "@/components/user/main_page/Terms.vue";
import PrivacyPage from "@/components/user/main_page/Privacy.vue";


import Search from '@/components/user/hotel_page/Search.vue'
import Reser from '@/components/user/hotel_page/Reser.vue'

// User pages (호텔 상세는 지연 로딩으로 번들 최적화)
const HotelDetailView = () =>
  import("@/components/user/hotel_page/HotelDetailView.vue");

const routes = [
  // 1) 기본 메인 유지 (기존 의도 살림)
  { path: "/", component: MainPage },

  // 2) 호텔 상세 (지연 로딩 + 동적 파라미터 전달)
  { path: "/hotels/:id", component: HotelDetailView, props: true },

  // 3) /hotels 진입 시 기본 호텔로 리다이렉트 (두 번째 스니펫 의도 보존)
  { path: "/hotels", redirect: "/hotels/1" },

  { path: '/Search', name: 'Search', component: Search },
  { path: '/Reser', name: 'Reser', component: Reser },

  // 4) 인증/정책 페이지들
  { path: "/login", component: Login },
  { path: "/register", component: Register },
  { path: "/terms", component: TermsPage },
  { path: "/privacy", component: PrivacyPage },
  { path: "/forgotPassword", component: ForgotPassword },
  { path: "/forgot-password", component: ForgotPassword }, // alias 유지
  { path: "/verify", component: LoginVerify },
  { path: "/passwordReset", component: PasswordReset },
  { path: "/password-reset", component: PasswordReset },   // alias 유지
  { path: "/oauth2/redirect", component: OAuth2Redirect },

];

const router = createRouter({
  history: createWebHistory(),
  routes,
  // 두 번째 스니펫의 scrollBehavior 반영
  scrollBehavior: () => ({ top: 0 }),
});

export default router;
