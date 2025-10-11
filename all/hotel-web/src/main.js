import { createApp } from 'vue'
import App from './App.vue'
import router from './router'
import http from './api/http'

// 전역(사용자 사이트) 스타일만
import '@/assets/css/hotel_detail/app.css'
import '@/assets/css/hotel_detail/hotel_detail.css'
import '@/assets/css/homepage/calendar.css'

import 'flatpickr/dist/flatpickr.css'
 
const app = createApp(App) // 👈 [수정] 앱 인스턴스를 변수에 할당합니다.

// 👇 [추가] 앱에 전역 속성으로 $axios를 설정합니다.
app.config.globalProperties.$axios = http;

// 👇 [수정] 설정이 끝난 후 라우터를 사용하고 마운트합니다.
app.use(router).mount('#app')
