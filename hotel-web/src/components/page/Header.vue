<template>
  <header class="main-header">
    <div class="header-content">
      <div class="header-left">
        <button class="hamburger-menu">
          <svg width="32" height="32" viewBox="0 0 24 24" fill="none"
            xmlns="http://www.w3.org/2000/svg">
            <path d="M3 6h18M3 12h18M3 18h18"
              stroke="rgba(0,0,0,0.54)" stroke-width="2"
              stroke-linecap="round" />
          </svg>
        </button>
        <div class="logo-container">
          <img src="/egodaLogo2.png" alt="egoda icon" class="egodaLogo-icon" />
          <img src="/egodaLogo3.png" alt="egoda icon" class="egodaLogo-text"/>
        </div>
      </div>

      <div class="header-right">
        <!-- 로그인 전 -->
        <template v-if="!isLoggedIn">
            <span class="login-text" @click="$router.push('/login')">로그인</span>
            <span>|</span>
            <span class="register-text" @click="$router.push('/register')">회원가입</span>
        </template>
 
        <!-- 로그인 후 -->
        <template v-else>
            <span class="customer-center" @click="$router.push('/support')">고객센터</span>
            <span>|</span>
            <span class="wishlist-text" @click="$router.push('/wishlist')">내 찜 목록</span>
            <span>|</span>
            <span class="my-reservations" @click="$router.push('/reservations')">내 예약</span>
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
  methods: {
    logout() {
        // 실제로는 토큰 제거 + 상태 갱신 로직 필요
        this.isLoggedIn = false;
        alert("로그아웃 되었습니다.");
    }
  }
};

</script>
