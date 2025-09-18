<template>
  <header class="main-header">
    <div class="header-content">
      <div class="header-left">
        <button class="hamburger-menu" @click="toggleSidebar">
          <svg width="32" height="32" viewBox="0 0 24 24" fill="none"
            xmlns="http://www.w3.org/2000/svg">
            <path d="M3 6h18M3 12h18M3 18h18"
              stroke="rgba(0,0,0,0.54)" stroke-width="2"
              stroke-linecap="round" />
          </svg>
        </button>

        <div class="sidebar" :class="{ open: isSidebarOpen }">
          <button class="close-btn" @click="toggleSidebar">×</button>
          <ul>
            <li @click="$router.push('/')">홈</li>
            <template v-if="!isLoggedIn">
              <li @click="$router.push('/login')">로그인</li>
              <li @click="$router.push('/register')">회원가입</li>
            </template>

            <li @click="$router.push('/search')">호텔 검색</li>
            <li @click="$router.push('/wishlist')">찜 목록</li>
            <li @click="$router.push('/myreservation')">내 예약</li>
            <li @click="$router.push('/mypage')">마이페이지</li>
            <li @click="$router.push('/support')">고객센터</li>

            <template v-if="isLoggedIn">
              <li @click="logout">로그아웃</li>
            </template>
          </ul>
        </div>

        <div class="overlay" v-if="isSidebarOpen" @click="toggleSidebar"></div>

        <div class="logo-container">
          <img src="/egodaLogo2.png" alt="egoda" class="egoda-logo" />
        </div>
      </div>

      <div class="header-right">
        <template v-if="!isLoggedIn">
            <span class="login-text" @click="$router.push('/login')">로그인</span>
            <span>|</span>
            <span class="register-text" @click="$router.push('/register')">회원가입</span>
        </template>
 
        <template v-else>
            <span class="user-name">{{ user.name }}님</span>
            <span>|</span>
            <span class="logout-text" @click="logout">로그아웃</span>
        </template>
      </div>
    </div>
  </header>
</template>

<style scoped src="@/assets/css/homepage/header.css"></style>

<script>
export default {
  name: "Header",
  props: {
    isLoggedIn: { type: Boolean, default: false },
    user: { type: Object, default: () => ({ name: "홍길동" }) }
  },
  // ❗️ 추가된 부분: 사이드바의 상태를 관리하기 위한 데이터
  data() {
    return {
      isSidebarOpen: false
    };
  },
  methods: {
    // ❗️ 추가된 부분: isSidebarOpen 값을 반전시켜 사이드바를 열고 닫습니다.
    toggleSidebar() {
      this.isSidebarOpen = !this.isSidebarOpen;
    },
    logout() {
        // 토큰과 사용자 정보 제거
        localStorage.removeItem('token');
        localStorage.removeItem('user');
        
        // 부모 컴포넌트에 로그아웃 이벤트 전달
        this.$emit('logout');
        
        alert("로그아웃 되었습니다.");
        
        // 메인 페이지로 이동 및 새로고침으로 상태 초기화
        this.$router.push('/').then(() => {
          window.location.reload();
        });
    }
  }
};
</script>