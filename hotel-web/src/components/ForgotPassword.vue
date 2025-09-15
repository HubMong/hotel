<template>
  <div class="forgot-password-page">
    <div class="container">
      <!-- 왼쪽 : 비밀번호 찾기 폼 -->
      <div class="form-section">
        <div class="back-link" @click="$router.go(-1)">← 돌아가기</div>
        <h2>비밀번호 찾기</h2>
        <p>비밀번호를 찾아보세요</p>

        <input type="email" v-model="email" placeholder="Email" />
        <button @click="$router.push('/verify')">제출</button>

        <!-- 구분선 -->
        <div class="divider">------------------------------------ 또는 ------------------------------------</div>

        <!-- 소셜 로그인 -->
        <div class="social-login">
          <div class="social-box">
            <img src="/naverLogo.png" alt="네이버 로그인" />
          </div>
          <div class="social-box">
            <img src="/googleLogo.png" alt="구글 로그인" />
          </div>
          <div class="social-box">
            <img src="/kakaoLogo.png" alt="카카오 로그인" />
          </div>
        </div>
      </div>

      <!-- 오른쪽 : 로고 -->
      <div class="logo-section">
        <img src="/egodaLogo.png" alt="egoda" />
      </div>
    </div>
  </div>
</template>

<style scoped src="@/assets/css/login/forgotPassword.css"></style>

<script>
import axios from "axios";

export default {
  data() {
    return {
      email: "",
    };
  },
  methods: {
    submit() {
      if (!this.email) {
        alert("이메일을 입력해주세요");
        return;
      }
      alert(`이메일: ${this.email} 로 비밀번호 찾기 요청`);
    },
  },
};
</script>
