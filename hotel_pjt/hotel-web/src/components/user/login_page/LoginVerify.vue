<template>
  <div class="verify-page">
    <!-- 전체 인증 박스 -->
    <div class="verify-container">
      
      <!-- 왼쪽: 인증 폼 -->
      <div class="verify-left">
        <div class="back">
          <span @click="goBack">← 돌아가기</span>
        </div>

        <h2>인증하기</h2>
        <p class="sub-text">이메일로 받은 인증번호를 입력해주세요</p>

        <div class="input-group">
          <label for="code">코드 입력</label>
          <div class="input-wrapper">
            <input id="code" v-model="code" placeholder="인증번호 입력" />
          </div>
        </div>

        <div class="resend">
          <a href="#" @click.prevent="resendCode">재전송하기</a>
        </div>

        <button class="verify-btn" @click="$router.push('/password-reset')">인증하기</button>
      </div>

      <!-- 오른쪽: 로고 --> 
      <div class="verify-right">
        <img src="/egodaLogo.png" alt="egoda logo" class="logo" />
      </div>
    </div>
  </div>
</template>

<style scoped src="@/assets/css/login/loginVerify.css"></style>

<script setup>
import { ref } from "vue";

const code = ref("");

const resendCode = () => {
  alert("인증번호가 재전송되었습니다.");
};

const verifyCode = () => {
  if (!code.value) {
    alert("인증번호를 입력해주세요.");
    return;
  }
  alert("인증되었습니다!");
};

const goBack = () => {
  window.history.back();
};
</script>
