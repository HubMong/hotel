<template>
    <div class="main-layout">
    <!-- 헤더 -->
    <Header :isLoggedIn="isLoggedIn" :user="user" @logout="handleLogout" />
  
    <!-- 컨텐츠 영역 -->
    <main><router-view /></main>
  
    <!-- 푸터 -->
    <Footer />
    </div>
  </template>
  
  <script>
  import Header from "@/components/user/main_page/Header.vue";
  import Footer from "@/components/user/main_page/Footer.vue";
  import http from "@/api/http";
  import UserApi from "@/api/UserApi";
  import { getAuthUser, setAuthUser, clearAuthUser, notifyAuthChanged } from "@/utils/auth-storage";

  export default {
    name: "MainLayout",
    components: { Header, Footer },
    data() {
      return {
        isLoggedIn: false,
        user: { name: "홍길동" }
      };
    },
    created() {
      this.initializeAuth();
      window.addEventListener("storage", this.onStorageEvent);
      window.addEventListener("authchange", this.initializeAuth);
    },
    beforeUnmount() {
      window.removeEventListener("storage", this.onStorageEvent);
      window.removeEventListener("authchange", this.initializeAuth);
    },
    methods: {
      async initializeAuth() {
        const stored = getAuthUser();
        if (stored) {
          this.isLoggedIn = true;
          this.user = stored;
          return;
        }

        try {
          const data = await UserApi.getInfo();
          if (data) {
            setAuthUser(data);
            this.isLoggedIn = true;
            this.user = data;
            return;
          }
        } catch (error) {
          // 401(Unauthorized)은 비로그인 상태이므로 로그 출력 생략
          const status = error.response?.status;
          if (status === 401 || error.message?.includes('401')) {
            // pass
          } else {
            console.warn("사용자 정보를 불러올 수 없습니다.", error);
          }
        }

        this.resetAuthState();
      },
      resetAuthState() {
        this.isLoggedIn = false;
        this.user = { name: "홍길동" };
        clearAuthUser();
      },
      async handleLogout() {
        try {
          await http.post("/users/logout");
        } catch (error) {
          console.warn("로그아웃 요청 중 오류", error);
        }
        this.resetAuthState();
        notifyAuthChanged();
        this.$router.push('/');
      },
      onStorageEvent(event) {
        if (event.key === 'auth.lastChange') {
          this.initializeAuth();
        }
      }
    },
    watch: {
      $route() {
        this.initializeAuth();
      }
    }
  };
</script>