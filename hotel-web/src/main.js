import './assets/main.css'

import { createApp } from "vue";
import App from "./App.vue";
import router from "./router";
import "@/assets/css/homepage/calendar.css"

createApp(App).use(router).mount("#app");


