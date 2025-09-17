// src/router/index.js
import { createRouter, createWebHistory } from 'vue-router'

const routes = [
  { path: '/hotels/:id', component: () => import('@/components/HotelDetailView.vue'), props: true },
  { path: '/:pathMatch(.*)*', redirect: '/hotels/1' } // 데모용
]

const router = createRouter({
  history: createWebHistory(),
  routes,
  scrollBehavior: () => ({ top: 0 }),
})

export default router
