
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


// Hotel search/detail
import Search from "@/components/user/hotel_page/Search.vue"
import DetailMyReservation from "@/components/user/myreser/DetailMyReservation.vue"
const HotelDetailView = () => import("@/components/user/hotel_page/HotelDetailView.vue")

// Checkout pages
const ReservationCheckout = () => import("@/components/user/hotel_checkout/ReservationCheckout.vue")
const ReservationResult   = () => import("@/components/user/hotel_checkout/ReservationResult.vue")

const routes = [
  { path: "/", component: MainPage },

  // 검색/상세
  { path: "/search", name: "Search", component: Search },
  { path: "/hotels/:id", component: HotelDetailView, props: true },
  { path: "/hotels", redirect: "/hotels/1" },

  // ==========================================================
  // ▼▼▼▼▼ 여기가 핵심 수정 부분입니다 ▼▼▼▼▼
  // ==========================================================
  
  // 모든 예약 상세
  { path: "/detailmyreservation", name: "DetailMyReservation", component: DetailMyReservation },

  // 1. 예약 상세 페이지 (MyReser.vue)가 이 경로를 사용하도록 변경
  { path: "/reservations/:id", name: "ReservationDetail", component: MyReser },

  // 2. 결제 페이지는 '/checkout'을 추가하여 경로 중복을 피함
  { path: "/reservations/:id/checkout", name: "ReservationCheckout", component: ReservationCheckout },
  
  // 예약 완료 페이지
  { path: '/ReservationResult', name: 'ReserRes', component: ReservationCheckout },

  // 3. 결과 페이지는 그대로 유지
  { path: "/reservations/:id/result", name: "ReservationResult", component: ReservationResult },

  // 4. 마이페이지 관련
  // { path: '/MyReser', name: 'MyReser', component: MyReser }, // 👆 위에서 동적 경로로 대체되었으므로 삭제
  { path: '/mypage', name: 'MyPage', component: MyPage }, // 경로를 소문자로 통일하는 것을 권장

  // ==========================================================

  // 5) 고객센터
  { path: '/support', name: 'Support', component: Support }, // 경로를 소문자로 통일하는 것을 권장


  // Auth / 정책
  { path: "/login", component: Login },
  { path: "/register", component: Register },
  { path: "/terms", component: TermsPage },
  { path: "/privacy", component: PrivacyPage },
  { path: "/forgot-password", component: ForgotPassword },
  { path: "/verify", component: LoginVerify },
  { path: "/password-reset", component: PasswordReset },
  { path: "/oauth2/redirect", component: OAuth2Redirect },

]

export default createRouter({
  history: createWebHistory(),
  routes,
  scrollBehavior: () => ({ top: 0 })
})