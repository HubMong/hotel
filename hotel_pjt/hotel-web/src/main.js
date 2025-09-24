import { createApp } from 'vue'
import App from './App.vue'
import router from './router'

// 전역(사용자 사이트) 스타일만
import '@/assets/css/hotel_detail/app.css'
import '@/assets/css/hotel_detail/hotel_detail.css'
import '@/assets/css/homepage/calendar.css'


import 'flatpickr/dist/flatpickr.css' 
createApp(App).use(router).mount('#app')
