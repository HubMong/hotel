<template>
  <div class="forgot-password-page">
    <div class="container">
      <!-- 왼쪽 : 비밀번호 찾기 폼 -->
      <div class="form-section">
        <div class="back-link" @click="$router.go(-1)">← 돌아가기</div>
        <h2>비밀번호 찾기</h2>
        <p>비밀번호를 찾아보세요</p>

        <input type="email" v-model="email" placeholder="Email" :disabled="loading" />
        <button @click="sendVerificationCode" :disabled="loading || !email">
          {{ loading ? '전송 중...' : '인증코드 발송' }}
        </button>

        <div v-if="message" :class="messageClass">{{ message }}</div>

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
import http from "@/api/http";

export default {
  data() {
    return {
      email: "",
      loading: false,
      message: "",
      messageType: "success" // success 또는 error
    };
  },
  computed: {
    messageClass() {
      return {
        'message-success': this.messageType === 'success',
        'message-error': this.messageType === 'error'
      };
    }
  },
  methods: {
    async sendVerificationCode() {
      if (!this.email) {
        this.showMessage("이메일을 입력해주세요.", "error");
        return;
      }

      if (!this.isValidEmail(this.email)) {
        this.showMessage("올바른 이메일 형식을 입력해주세요.", "error");
        return;
      }

      this.loading = true;
      this.message = "";

      try {
const response = await http.post(`/password/reset/send-code`, {
  email: this.email
});

        if (response.data.success) {
          this.showMessage("인증코드가 발송되었습니다. 이메일을 확인해주세요.", "success");
          // 3초 후 인증 페이지로 이동
          setTimeout(() => {
            this.$router.push({
              path: '/password-reset', 
              query: { email: this.email }
            });
          }, 3000);
        } else {
          this.showMessage(response.data.message || "인증코드 발송에 실패했습니다.", "error");
        }
      } catch (error) {
        console.error("인증코드 발송 오류:", error);
        
        if (error.response?.data?.message) {
          this.showMessage(error.response.data.message, "error");
        } else {
          this.showMessage("서버 오류가 발생했습니다. 잠시 후 다시 시도해주세요.", "error");
        }
      } finally {
        this.loading = false;
      }
    },

    isValidEmail(email) {
      const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
      return emailRegex.test(email);
    },

    showMessage(text, type) {
      this.message = text;
      this.messageType = type;
    },

    submit() {
      this.sendVerificationCode();
    },
  },
};
</script>
