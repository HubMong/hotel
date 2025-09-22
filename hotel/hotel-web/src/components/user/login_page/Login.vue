<template>
  <div class="login-page">
    <div class="login-container">
      <!-- 왼쪽 : 로그인 폼 -->
      <div class="login-form">
  <div class="back-link" @click="$router.push('/')" style="cursor: pointer;">← 돌아가기</div>
        <h1>로그인</h1>
        <p class="sub-text">Login</p>

        <!-- 이메일 -->
        <div class="input-group">
          <label for="email">이메일</label>
          <input type="email" id="email" v-model="email" placeholder="이메일" />
        </div>

        <!-- 비밀번호 -->
        <div class="input-group">
            <label for="password">비밀번호</label>
            <div class="password-box">
                <input :type="showPassword ? 'text' : 'password'" id="password" v-model="password" placeholder="비밀번호"/>
                <button type="button" class="toggle-btn" @click="togglePassword">
                {{ showPassword ? '숨김' : '보기' }}
                </button>
            </div>
        </div>

        <!-- 비밀번호 저장 -->
        <div class="options">
          <label>
            <input type="checkbox" v-model="remember" /> 비밀번호 저장하기
          </label>
          <a href="#" class="forgot" @click="$router.push('forgotPassword')">비밀번호를 잊으셨나요?</a>
        </div>

        <!-- 로그인 버튼 -->
        <button class="login-btn" @click="login">로그인</button>

        <button class="register-btn" @click="$router.push('/register')">회원가입</button>

        <!-- 구분선 -->
        <div class="divider">-------------------------------- 또는 ---------------------------------</div>

        <!-- 소셜 로그인 -->
        <div class="social-login">
          <div class="social-box" @click="naverLogin">
            <img src="/naverLogo.png" alt="네이버 로그인" />
          </div>
          <div class="social-box" @click="googleLogin">
            <img src="/googleLogo.png" alt="구글 로그인" />
          </div>
          <div class="social-box" @click="kakaoLogin">
            <img src="/kakaoLogo.png" alt="카카오 로그인" />
          </div>
        </div>
      </div>

      <!-- 오른쪽 : 로고 -->
      <div class="login-logo">
        <img src="/egodaLogo.png" alt="egoda" />
      </div>
    </div>
  </div>
</template>

<style scoped src="@/assets/css/login/login.css"></style>

<script>
import http from "@/api/http";

export default {
  name: "LoginPage",
  data() {
    return {
      email: "",
      password: "",
      remember: false,
      showPassword: false,
    };
  },
  created() {
    const savedEmail = localStorage.getItem('savedEmail');
    const savedPassword = localStorage.getItem('savedPassword');
    if (savedEmail && savedPassword) {
      this.email = savedEmail;
      this.password = savedPassword;
      this.remember = true;
    }
  },
  methods: {
    togglePassword() {
      this.showPassword = !this.showPassword;
    },
    async login() {
      // 입력 검증
      if (!this.email || !this.password) {
        alert("이메일과 비밀번호를 입력해주세요.");
        return;
      }

      if (!this.isValidEmail(this.email)) {
        alert("올바른 이메일 형식을 입력해주세요.");
        return;
      }

      try {
const response = await http.post("/users/login", {
  email: this.email,
  password: this.password,
});
        console.log("로그인 성공:", response.data);
        
        // JWT 토큰을 로컬 스토리지에 저장
        if (response.data.token) {
          localStorage.setItem('token', response.data.token);
          localStorage.setItem('user', JSON.stringify(response.data.user));
        }
        // 비밀번호 저장(remember) 처리
        if (this.remember) {
          localStorage.setItem('savedEmail', this.email);
          localStorage.setItem('savedPassword', this.password);
        } else {
          localStorage.removeItem('savedEmail');
          localStorage.removeItem('savedPassword');
        }

        // 로그인 후 메인 페이지 이동
        this.$router.push('/');
      } catch (error) {
        console.error("로그인 실패:", error.response?.data || error.message);
        alert(error.response?.data || '로그인에 실패했습니다.');
      }
    },
    isValidEmail(email) {
      const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
      return emailRegex.test(email);
    },
    naverLogin() {
      // 네이버 OAuth2 로그인 URL로 리다이렉트
  window.location.href = "/oauth2/authorization/naver";
    },
    googleLogin() {
      // 구글 OAuth2 로그인 URL로 리다이렉트
  window.location.href = "/oauth2/authorization/google";
    },
    kakaoLogin() {
      // 카카오 OAuth2 로그인 URL로 리다이렉트
  window.location.href = "/oauth2/authorization/kakao";
    },
  },
};
</script>
