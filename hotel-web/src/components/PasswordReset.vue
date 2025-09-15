<template>
  <div class="reset-page">
    <div class="reset-container">
      
      <!-- 왼쪽 폼 -->
      <div class="reset-left">
        <h2>비밀번호 설정</h2>
        <p class="sub-text">비밀번호를 다시 설정하기</p>

        <!-- 새 비밀번호 -->
        <div class="input-group">
          <label for="password">비밀번호 생성</label>
          <div class="input-wrapper">
            <input
              id="password"
              type="password"
              v-model="password"
              placeholder="비밀번호 생성"
            />
            <button type="button" class="toggle" @click="togglePassword">
              {{ showPassword ? "🙈" : "👁️" }}
            </button>
          </div>
        </div>

        <!-- 비밀번호 확인 -->
        <div class="input-group">
          <label for="confirm">비밀번호 확인</label>
          <div class="input-wrapper">
            <input
              id="confirm"
              type="password"
              v-model="confirm"
              placeholder="비밀번호를 다시 입력해주세요."
            />
            <button type="button" class="toggle" @click="toggleConfirm">
              {{ showConfirm ? "🙈" : "👁️" }}
            </button>
          </div>
        </div>

        <!-- 버튼 -->
        <button class="reset-btn" @click="$router.push('/login')">비밀번호 설정</button>
      </div>

      <!-- 오른쪽 로고 -->
      <div class="reset-right">
        <img src="/egodaLogo.png" alt="egoda logo" class="logo" />
      </div>
    </div>
  </div>
</template>

<style scoped src="@/assets/css/login/passwordReset.css"></style>

<script setup>
import { ref } from "vue";

const password = ref("");
const confirm = ref("");
const showPassword = ref(false);
const showConfirm = ref(false);

const togglePassword = () => {
  showPassword.value = !showPassword.value;
  const input = document.getElementById("password");
  input.type = showPassword.value ? "text" : "password";
};

const toggleConfirm = () => {
  showConfirm.value = !showConfirm.value;
  const input = document.getElementById("confirm");
  input.type = showConfirm.value ? "text" : "password";
};

const resetPassword = () => {
  if (!password.value || !confirm.value) {
    alert("비밀번호를 모두 입력해주세요.");
    return;
  }
  if (password.value !== confirm.value) {
    alert("비밀번호가 일치하지 않습니다.");
    return;
  }
  alert("비밀번호가 성공적으로 설정되었습니다!");
};
</script>
