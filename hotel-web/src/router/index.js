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

// My page & etc
import MyReser from "@/components/user/my_page/MyReser.vue"
import MyPage from "@/components/user/my_page/MyPage.vue"
import Support from "@/components/user/support_page/Support.vue"

// AllReservation
import AllReservation from "@/components/user/myreser/allreservation.vue"

// Hotel search/detail
import Search from "@/components/user/hotel_page/Search.vue"
const HotelDetailView = () => import("@/components/user/hotel_page/HotelDetailView.vue")

// Checkout pages
const ReservationCheckout = () => import("@/components/user/hotel_checkout/ReservationCheckout.vue")
const ReservationResult   = () => import("@/components/user/hotel_checkout/ReservationResult.vue")

// Payments (Toss 등)
const PaymentCheckout = () => import("@/components/user/hotel_checkout/PaymentCheckout.vue")
const PaymentSuccess  = () => import("@/components/user/hotel_checkout/PaymentSuccess.vue")
const PaymentFailure  = () => import("@/components/user/hotel_checkout/PaymentFailure.vue")

const routes = [
  { path: "/", name: "Home", component: MainPage },

  // 검색/상세
  { path: "/search", name: "Search", component: Search },
  { path: "/hotels/:id", name: "HotelDetail", component: HotelDetailView, props: true },
  { path: "/hotels", redirect: "/hotels/1" },

  // 예약 상세 / 결제
  { path: "/reservations/:id", name: "ReservationDetail", component: MyReser, props: true },
  { path: "/reservations/:id/checkout", name: "ReservationCheckout", component: ReservationCheckout, props: true },
  { path: "/reservations/:id/result", name: "ReservationResult", component: ReservationResult, props: true },

  // 결제(토스 SDK 페이지/상세)
  { path: "/payments/:id", name: "PaymentCheckout", component: PaymentCheckout, props: true },
  { path: "/payment/success", name: "PaymentSuccess", component: PaymentSuccess },
  { path: "/payment/fail",    name: "PaymentFailure", component: PaymentFailure },

  // 마이페이지 / 고객센터
  { path: "/mypage", name: "MyPage", component: MyPage },
  { path: "/support", name: "Support", component: Support },

  //나의 모든 예약목록
  { path: "/allreservation", name: "AllReservation", component: AllReservation },

  // Auth / 정책
  { path: "/login", name: "Login", component: Login },
  { path: "/register", name: "Register", component: Register },
  { path: "/terms", name: "Terms", component: TermsPage },
  { path: "/privacy", name: "Privacy", component: PrivacyPage },
  { path: "/verify", name: "LoginVerify", component: LoginVerify },
  { path: "/oauth2/redirect", name: "OAuth2Redirect", component: OAuth2Redirect },

  // 비밀번호/리셋: kebab-case로 통일, camelCase는 alias로 호환
  { path: "/forgot-password", name: "ForgotPassword", component: ForgotPassword, alias: ["/forgotPassword"] },
  { path: "/password-reset",  name: "PasswordReset",  component: PasswordReset,  alias: ["/passwordReset"] },

  // (선택) 404는 홈으로
  { path: "/:pathMatch(.*)*", redirect: "/" }
]

export default createRouter({
  history: createWebHistory(),
  routes,
  scrollBehavior: () => ({ top: 0 })
})
