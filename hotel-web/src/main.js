import { createApp } from 'vue'
import App from './App.vue'
import router from './router'

// 전역(사용자 사이트) 스타일만
import '@/assets/styles/app.css'
import '@/assets/styles/hotel-detail.css'

createApp(App).use(router).mount('#app')
