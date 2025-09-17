// 파일 경로: src/router/index.js

import { createRouter, createWebHistory } from 'vue-router';


const routes = [
  {
    path: '/',
    name: 'Main',
    component: () => import('../views/Main.vue') // 지연 로딩 방식
  },
  {
    path: '/Search',
    name: 'Search',
    component: () => import('../views/Search.vue')
  },
  {
    path: '/Reser',
    name: 'Reser',
    component: () => import('../views/Reser.vue')
  },
  //경로 추가
  
];

const router = createRouter({
  history: createWebHistory(),
  routes,
  // ▼▼▼ 2. 페이지 이동 시 항상 맨 위로 스크롤하는 기능 추가 ▼▼▼
  scrollBehavior(to, from, savedPosition) {
    return { top: 0 }
  },
});

export default router;