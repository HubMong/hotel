// 파일 경로: src/main.js
// 기존 내용을 모두 지우고 아래 코드로 교체하세요.

// import './assets/main.css'

import { createApp } from 'vue'
import App from './App.vue'
// 1. 우리가 만든 라우터 설정 파일을 불러옵니다.
import router from './router'

const app = createApp(App)

// 2. Vue 앱에 라우터를 사용하겠다고 등록합니다. (가장 중요한 부분!)
app.use(router)

app.mount('#app')