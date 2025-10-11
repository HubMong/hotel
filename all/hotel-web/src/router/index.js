// src/router/index.js
import { createRouter, createWebHistory } from "vue-router"

// ===== Auth & static =====
import Login from "@/components/user/login_page/Login.vue"
import Register from "@/components/user/login_page/Register.vue"
import ForgotPassword from "@/components/user/login_page/ForgotPassword.vue"
import LoginVerify from "@/components/user/login_page/LoginVerify.vue"
import PasswordReset from "@/components/user/login_page/PasswordReset.vue"
import OAuth2Redirect from "@/components/user/login_page/OAuth2Redirect.vue"

import MainPage from "@/components/user/main_page/MainPage.vue"
import TermsPage from "@/components/user/main_page/Terms.vue"
import PrivacyPage from "@/components/user/main_page/Privacy.vue"
import BusinessApply from "@/components/user/main_page/BusinessApply.vue"

// ===== My page & etc =====
import MyAccount from "@/components/user/my_page/MyAccount.vue" // 계정 정보
import MyHistory from "@/components/user/my_page/MyHistory.vue"  // 예약 내역
import MyReserDetail from "@/components/user/my_page/MyReser.vue" // 예약 상세
import MyWishlist from "@/components/user/my_page/MyWishlist.vue"     // 찜 목록
import MyReview from "@/components/user/my_page/MyReview.vue"     // 리뷰
import MyReserList from "@/components/user/myreser/DetailMyReservation.vue" // 예약 목록
import HotelSupport from "@/components/user/support_page/HotelSupport.vue" // 호텔 고객센터
import WebsiteSupport from "@/components/user/support_page/WebsiteSupport.vue"  // 웹사이트 고객센터

// ===== Hotel search/detail =====
import Search from "@/components/user/hotel_page/Search.vue"
const HotelDetailView = () => import("@/components/user/hotel_page/HotelDetailView.vue")

// ===== Checkout pages =====
const ReservationCheckout = () => import("@/components/user/hotel_checkout/ReservationCheckout.vue")
const ReservationResult   = () => import("@/components/user/hotel_checkout/ReservationResult.vue")

// ===== Payments (Toss 등) =====
const PaymentCheckout = () => import("@/components/user/hotel_checkout/PaymentCheckout.vue")
const PaymentSuccess  = () => import("@/components/user/hotel_checkout/PaymentSuccess.vue")
const PaymentFailure  = () => import("@/components/user/hotel_checkout/PaymentFailure.vue")

// ===== Admin =====
import AdminLayout from '@/components/admin/AdminLayout.vue'
import AdminDashboard from '@/components/admin/AdminDashboard.vue'
import UserManagement from '@/components/admin/UserManagement.vue'
import PaymentManagement from '@/components/admin/PaymentManagement.vue'
import ReviewManagement from '@/components/admin/ReviewManagement.vue'
import CouponManagement from '@/components/admin/CouponManagement.vue'
import SalesManagement from '@/components/admin/SalesManagement.vue'
import HotelManagement from '@/components/admin/HotelManagement.vue'       // (기존) 사업자/호텔 일반 관리
import InquiryManagement from '@/components/admin/InquiryManagement.vue'
// ★ 신규: 호텔 승인 전용 페이지
import AdminHotelApprovals from '@/components/admin/AdminHotelApprovals.vue'

// ==== Owner =====
import OwnerMain from "@/components/owner/OwnerMain.vue";
import OwnerDashboard from "@/components/owner/OwnerDashboard.vue";
import OwnerRoom from "@/components/owner/OwnerRoom.vue";
import OwnerRoomRegister from "@/components/owner/OwnerRoomRegister.vue";
import OwnerReservation from "@/components/owner/OwnerReservation.vue";
import OwnerReview from "@/components/owner/OwnerReview.vue";
import OwnerHotelList from '@/components/owner/OwnerHotelList.vue'
import OwnerHotelCreate from '@/components/owner/OwnerHotelCreate.vue'
import OwnerHotelEdit from '@/components/owner/OwnerHotelEdit.vue'
import OwnerSupport from "@/components/owner/OwnerSupport.vue"; 

const routes = [
  { path: "/", name: "Home", component: MainPage },

  // 검색/상세
  { path: "/search", name: "Search", component: Search },
  { path: "/hotels/:id", name: "HotelDetail", component: HotelDetailView, props: true },
  { path: "/hotels", redirect: "/hotels/1" },

  // 예약 상세/체크아웃/결과
  { path: "/reservations/:id", name: "ReservationDetail", component: MyReserDetail, props: true },
  { path: "/reservations/:id/checkout", name: "ReservationCheckout", component: ReservationCheckout, props: true },
  { path: "/reservations/:id/result", name: "ReservationResult", component: ReservationResult, props: true },

  // 결제(토스)
  { path: "/payments/:id", name: "PaymentCheckout", component: PaymentCheckout, props: true },
  { path: "/payment/success", name: "PaymentSuccess", component: PaymentSuccess },
  { path: "/payment/fail",    name: "PaymentFailure",  component: PaymentFailure },

  // ===== 마이페이지 (각각 독립) =====
  { path: "/myaccount", name: "MyAccount", component: MyAccount },
  { path: "/myhistory", name: "MyHistory", component: MyHistory },
  { path: "/mywishlist",  name: "MyWishlist",  component: MyWishlist },
  { path: "/myreview",    name: "MyReview",    component: MyReview },

  // 예약 목록 (별도 페이지)
  { path: "/myreservation", name: "MyReser", component: MyReserList },

  // 정책/비즈니스 신청
  { path: "/terms", name: "Terms", component: TermsPage },
  { path: "/privacy", name: "Privacy", component: PrivacyPage },
  { path: "/business/apply", name: "BusinessApply", component: BusinessApply },

  // 고객센터
  { path: "/hotelsupport", name: "HotelSupport", component: HotelSupport },
  { path: "/websitesupport", name: "WebsiteSupport", component: WebsiteSupport },

  // Auth
  { path: "/login", name: "Login", component: Login },
  { path: "/register", name: "Register", component: Register },
  { path: "/verify", name: "LoginVerify", component: LoginVerify },
  { path: "/oauth2/redirect", name: "OAuth2Redirect", component: OAuth2Redirect },

  // 비밀번호/리셋: kebab-case 기본 + camelCase alias 호환
  { path: "/forgot-password", name: "ForgotPassword", component: ForgotPassword, alias: ["/forgotPassword"] },
  { path: "/password-reset",  name: "PasswordReset",  component: PasswordReset,  alias: ["/passwordReset"] },

  { // 업주 페이지 
    path: "/owner",
    component: OwnerMain,
    children: [
      { path: '', redirect: { name: 'OwnerDashboard' } },
      { path: 'dashboard',      name: 'OwnerDashboard',    component: OwnerDashboard },
      { path: 'rooms',          name: 'OwnerRoom',         component: OwnerRoom },
      { path: 'roomRegister',   name: 'OwnerRoomRegister', component: OwnerRoomRegister },
      { path: 'rooms/edit/:id', name: 'OwnerRoomUpdate',   component: OwnerRoomRegister, props: true },
      { path: 'reservations',   name: 'OwnerReservation',  component: OwnerReservation },
      { path: 'reviews',        name: 'OwnerReview',       component: OwnerReview },
      { path: 'hotels',         name: 'OwnerHotelList',    component: OwnerHotelList },
      { path: 'hotels/new',     name: 'OwnerHotelCreate',  component: OwnerHotelCreate },
      { path: 'hotels/:id/edit',name: 'OwnerHotelEdit',    component: OwnerHotelEdit, props: true },
      { path: 'support',        name: 'OwnerSupport',      component: OwnerSupport }, 
    ]
  }, 

  // Admin
  {
    path: '/admin',
    component: AdminLayout,
    meta: { requiresAdmin: true },
    children: [
      { path: '', redirect: '/admin/dashboard' },
      { path: 'dashboard',   name: 'AdminDashboard',   component: AdminDashboard },
      { path: 'users',       name: 'AdminUsers',       component: UserManagement },
      { path: 'businesses',  name: 'AdminBusinesses',  component: HotelManagement },      // (기존) 사업자 관리
      { path: 'hotels',      name: 'AdminHotels',      component: AdminHotelApprovals },  // ★ 신규: 호텔 승인
      { path: 'payments',    name: 'AdminPayments',    component: PaymentManagement },
      { path: 'reviews',     name: 'AdminReviews',     component: ReviewManagement },
      { path: 'sales',       name: 'AdminSales',       component: SalesManagement },
      { path: 'inquiries',   name: 'AdminInquiries',   component: InquiryManagement },
      { path: 'coupons',     name: 'AdminCoupons',     component: CouponManagement }
    ]
  },

  // 404 → 홈
  { path: "/:pathMatch(.*)*", redirect: "/" }
]

const router = createRouter({
  history: createWebHistory(),
  routes,
  scrollBehavior: () => ({ top: 0 })
})

// 간단한 어드민 가드
router.beforeEach((to, from, next) => {
  const userStr = localStorage.getItem('user')
  let role = null
  try { role = userStr ? JSON.parse(userStr)?.role : null } catch (_) {}

  // 이미 어드민이면 로그인 페이지 접근 시 /admin으로
  if (to.path === '/login' && role === 'ADMIN') return next('/admin')

  // 어드민 영역 보호
  if (to.matched.some(r => r.meta?.requiresAdmin)) {
    if (!role) return next('/login')
    if (role !== 'ADMIN') return next('/')
  }
  next()
})

export default router
