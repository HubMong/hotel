// src/main.js
import { createApp } from 'vue'
import App from './App.vue'
import router from './router'

// 전역 스타일
import './assets/styles/hotel-detail.css'

createApp(App).use(router).mount('#app')
