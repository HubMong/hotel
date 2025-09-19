import { createRouter, createWebHistory } from 'vue-router'

const routes = [
  { path: '/', redirect: '/hotels/1' },

  // 사용자 영역
  { path: '/hotels/:id', component: () => import('@/components/user/HotelDetailView.vue'), props: true },
  { path: "/checkout", component: () => import('@/components/user/Checkout.vue'), props: true },
  { path: "/payment/lists", component: () => import('@/components/user/PaymentList.vue'), props: true },
  { path: "/payment/success", component: () => import('@/components/user/PaymentSuccess.vue'), props: true },
  { path: "/payment/fail", component: () => import('@/components/user/PaymentFailure.vue'), props: true },

  // 관리자 영역 (메타로 'admin' 레이아웃 표시)
// src/router/index.js (요약)
{
  path: '/admin',
  component: () => import('@/components/admin/AdminLayout.vue'),
  children: [
    { path: '', component: () => import('@/components/admin/Dashboard.vue') },
    { path: 'businesses', component: () => import('@/components/admin/BusinessApproval.vue') },
    { path: 'hotels', component: () => import('@/components/admin/HotelList.vue') },
    { path: 'hotels/:id', component: () => import('@/components/admin/HotelEdit.vue'), props: true },
  ]
},

{
  path: '/owner',
  component: () => import('@/components/owner/OwnerLayout.vue'),
  meta: { requiresBusiness: true },
  children: [
    { path: '', component: () => import('@/components/owner/OwnerDashboard.vue') },
    { path: 'hotels', component: () => import('@/components/owner/OwnerHotelList.vue') },
    { path: 'hotels/new', component: () => import('@/components/owner/OwnerHotelEdit.vue') },
    { path: 'hotels/:id', component: () => import('@/components/owner/OwnerHotelEdit.vue'), props: true },
  ]
}

]

const router = createRouter({
  history: createWebHistory(),
  routes,
  scrollBehavior: () => ({ top: 0 })
})

// 아주 단순한 ADMIN 가드 (임시)
router.beforeEach((to, _from, next) => {
  if (to.meta?.requiresAdmin) {
    const role = localStorage.getItem('role') || ''
    return role === 'ADMIN' ? next() : next('/')
  }
  if (to.meta?.requiresBusiness) {
    const role = localStorage.getItem('role') || ''
    return role === 'BUSINESS' ? next('/login') : next()
  }
  next()
})
export default router
