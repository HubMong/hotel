// src/router/index.js
import { createRouter, createWebHistory } from "vue-router"

// Auth & static
import Login from "@/components/user/login_page/Login.vue"
import Register from "@/components/user/login_page/Register.vue"
import ForgotPassword from "@/components/user/login_page/ForgotPassword.vue"
import LoginVerify from "@/components/user/login_page/LoginVerify.vue"
import PasswordReset from "@/components/user/login_page/PasswordReset.vue"
import OAuth2Redirect from "@/components/user/login_page/OAuth2Redirect.vue"
import MainPage from "@/components/user/main_page/MainPage.vue"
import TermsPage from "@/components/user/main_page/Terms.vue"
import PrivacyPage from "@/components/user/main_page/Privacy.vue"

import MyReser from "@/components/user/my_page/MyReser.vue"
import MyPage from "@/components/user/my_page/MyPage.vue"
import Support from "@/components/user/support_page/Support.vue"


// Hotel search/detail (네가 쓰던 경로 유지)
import Search from "@/components/user/hotel_page/Search.vue"
// 상세는 지연 로딩 권장
const HotelDetailView = () => import("@/components/user/hotel_page/HotelDetailView.vue")

// Checkout pages (요청한 디렉토리)
const ReservationCheckout = () => import("@/components/user/hotel_checkout/ReservationCheckout.vue")
const ReservationResult   = () => import("@/components/user/hotel_checkout/ReservationResult.vue")

const routes = [
  { path: "/", component: MainPage },

  // 검색/상세
  { path: "/search", name: "Search", component: Search },
  { path: "/hotels/:id", component: HotelDetailView, props: true },
  { path: "/hotels", redirect: "/hotels/1" },

  // 체크아웃/결과
  { path: "/reservations/:id", name: "ReservationCheckout", component: ReservationCheckout },
  { path: "/reservations/:id/result", name: "ReservationResult", component: ReservationResult },

// 4) 마이페이지 관련
  { path: '/MyReser', name: 'MyReser', component: MyReser },
  { path: '/MyPage', name: 'MyPage', component: MyPage },

  // 5)고객센터
  { path: '/Support', name: 'Support', component: Support },


  // Auth / 정책
  { path: "/login", component: Login },
  { path: "/register", component: Register },
  { path: "/terms", component: TermsPage },
  { path: "/privacy", component: PrivacyPage },
  { path: "/forgotPassword", component: ForgotPassword },
  { path: "/forgot-password", component: ForgotPassword },
  { path: "/verify", component: LoginVerify },
  { path: "/passwordReset", component: PasswordReset },
  { path: "/password-reset", component: PasswordReset },
  { path: "/oauth2/redirect", component: OAuth2Redirect },

]

export default createRouter({
  history: createWebHistory(),
  routes,
  scrollBehavior: () => ({ top: 0 })
})
