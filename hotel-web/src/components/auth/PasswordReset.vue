<template>
  <div class="reset-page">
    <div class="reset-container">
      
      <!-- 왼쪽 폼 -->
      <div class="reset-left">
        <div class="back-link" @click="$router.go(-1)">← 돌아가기</div>
        
        <h2>비밀번호 재설정</h2>
        <p class="sub-text">{{ email }}로 발송된 인증코드를 입력하고 새 비밀번호를 설정하세요</p>

        <!-- 인증코드 입력 -->
        <div class="input-group">
          <label for="verificationCode">인증코드</label>
          <input
            id="verificationCode"
            type="text"
            v-model="verificationCode"
            placeholder="6자리 인증코드 입력"
            maxlength="6"
            :disabled="loading"
          />
        </div>

        <!-- 새 비밀번호 -->
        <div class="input-group">
          <label for="password">새 비밀번호</label>
          <div class="input-wrapper">
            <input
              id="password"
              :type="showPassword ? 'text' : 'password'"
              v-model="newPassword"
              placeholder="새 비밀번호 입력 (6자 이상)"
              :disabled="loading"
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
              :type="showConfirm ? 'text' : 'password'"
              v-model="confirmPassword"
              placeholder="비밀번호를 다시 입력해주세요"
              :disabled="loading"
            />
            <button type="button" class="toggle" @click="toggleConfirm">
              {{ showConfirm ? "🙈" : "👁️" }}
            </button>
          </div>
        </div>

        <div v-if="message" :class="messageClass">{{ message }}</div>

        <!-- 버튼 -->
        <button 
          class="reset-btn" 
          @click="resetPassword"
          :disabled="loading || !verificationCode || !newPassword || !confirmPassword"
        >
          {{ loading ? '처리 중...' : '비밀번호 재설정' }}
        </button>
      </div>

      <!-- 오른쪽 로고 -->
      <div class="reset-right">
        <img src="/egodaLogo.png" alt="egoda logo" class="logo" />
      </div>
    </div>
  </div>
</template>

<style scoped src="@/assets/css/login/passwordReset.css"></style>

<script>
import axios from "axios";

export default {
  data() {
    return {
      email: "",
      verificationCode: "",
      newPassword: "",
      confirmPassword: "",
      showPassword: false,
      showConfirm: false,
      loading: false,
      message: "",
      messageType: "success"
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
  mounted() {
    // URL 쿼리에서 이메일 가져오기
    this.email = this.$route.query.email || "";
    if (!this.email) {
      this.showMessage("이메일 정보가 없습니다. 다시 시도해주세요.", "error");
      setTimeout(() => {
        this.$router.push('/forgot-password');
      }, 2000);
    }
  },
  methods: {
    togglePassword() {
      this.showPassword = !this.showPassword;
    },

    toggleConfirm() {
      this.showConfirm = !this.showConfirm;
    },

    async resetPassword() {
      // 입력값 검증
      if (!this.verificationCode) {
        this.showMessage("인증코드를 입력해주세요.", "error");
        return;
      }

      if (this.verificationCode.length !== 6) {
        this.showMessage("인증코드는 6자리여야 합니다.", "error");
        return;
      }

      if (!this.newPassword) {
        this.showMessage("새 비밀번호를 입력해주세요.", "error");
        return;
      }

      if (this.newPassword.length < 6) {
        this.showMessage("비밀번호는 6자 이상이어야 합니다.", "error");
        return;
      }

      if (this.newPassword !== this.confirmPassword) {
        this.showMessage("비밀번호가 일치하지 않습니다.", "error");
        return;
      }

      this.loading = true;
      this.message = "";

      try {
        const response = await axios.post(`http://localhost:8888/api/password/reset/verify-and-change`, null, {
          params: {
            email: this.email,
            verificationCode: this.verificationCode,
            newPassword: this.newPassword
          }
        });

        if (response.data.success) {
          this.showMessage("비밀번호가 성공적으로 변경되었습니다! 로그인 페이지로 이동합니다.", "success");
          
          // 3초 후 로그인 페이지로 이동
          setTimeout(() => {
            this.$router.push('/login');
          }, 3000);
        } else {
          this.showMessage(response.data.message || "비밀번호 변경에 실패했습니다.", "error");
        }
      } catch (error) {
        console.error("비밀번호 재설정 오류:", error);
        
        if (error.response?.data?.message) {
          this.showMessage(error.response.data.message, "error");
        } else {
          this.showMessage("서버 오류가 발생했습니다. 잠시 후 다시 시도해주세요.", "error");
        }
      } finally {
        this.loading = false;
      }
    },

    showMessage(text, type) {
      this.message = text;
      this.messageType = type;
    }
  }
};
</script>
