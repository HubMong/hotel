import { createRouter, createWebHistory } from "vue-router";
// Auth components
import Login from "@/components/auth/Login.vue";
import Register from "@/components/auth/Register.vue";
import ForgotPassword from "@/components/auth/ForgotPassword.vue";
import LoginVerify from "@/components/auth/LoginVerify.vue";
import PasswordReset from "@/components/auth/PasswordReset.vue";
import OAuth2Redirect from "@/components/auth/OAuth2Redirect.vue";
// Page components
import MainPage from "@/components/page/MainPage.vue";
import TermsPage from "@/components/page/Terms.vue";
import PrivacyPage from "@/components/page/Privacy.vue";

const routes = [
  { path: "/", component: MainPage }, // 기본 경로를 MainPage로 설정
  { path: "/login", component: Login },
  { path: "/register", component: Register },
  { path: "/terms", component: TermsPage },
  { path: "/privacy", component: PrivacyPage },
  { path: "/forgotPassword", component: ForgotPassword },
  { path: "/forgot-password", component: ForgotPassword }, // 추가 경로
  { path: "/verify", component: LoginVerify },
  { path: "/passwordReset", component: PasswordReset },
  { path: "/password-reset", component: PasswordReset }, // 추가 경로
  { path: "/oauth2/redirect", component: OAuth2Redirect },
];
const router = createRouter({
  history: createWebHistory(),
  routes,
});

export default router;