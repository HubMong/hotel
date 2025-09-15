<template>
  <div class="register-page">
        <div class="register-container">
            <!-- 왼쪽 : 로고 -->
            <div class="register-logo">
                <img src="/egodaLogo.png" alt="egoda" />
            </div>

            <!-- 오른쪽 : 회원가입 폼 -->
            <div class="register-form">
                <div class="back-link" @click=$router.go(-1)>← 돌아가기</div>
                
                <h1>회원가입</h1>
                <p class="sub-text">Register</p>

                <!-- 성 & 이름 -->
                <div class="name-row">
                <div class="input-group half">
                    <label for="lastName">성</label>
                    <input type="text" id="lastName" v-model="lastName" placeholder="성" />
                </div>
                <div class="input-group half">
                    <label for="firstName">이름</label>
                    <input type="text" id="firstName" v-model="firstName" placeholder="이름" />
                </div>
                </div>

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

                <!-- 비밀번호 확인 -->
                <div class="input-group">
                <label for="confirmPassword">비밀번호 확인</label>
                    <div class="password-box">
                        <input :type="showConfirmPassword ? 'text' : 'password'" id="confirmPassword" v-model="confirmPassword" placeholder="비밀번호 확인"/>
                        <button type="button" class="toggle-btn" @click="toggleConfirmPassword">
                        {{ showConfirmPassword ? '숨김' : '보기' }}
                        </button>
                    </div>
                </div>

                <!-- 동의 체크박스 -->
                <div class="options">
                    <label>
                        <input type="checkbox" v-model="agree" /> 동의하기
                    </label>
                </div>

                <!-- 회원가입 버튼 -->
                <button class="register-btn" @click="register">계정 생성</button>

                <!-- 구분선 -->
                <div class="divider">----------------------------------------- 또는 -----------------------------------------<div>

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
            </div>
        </div>
    </div>
  </div>
</template>

<style scoped src="@/assets/css/login/register.css"></style>

<script>
import axios from "axios";

export default {
  name: "RegisterPage",
  data() {
    return {
      firstName: "",
      lastName: "",
      email: "",
      password: "",
      confirmPassword: "",
      agree: false,
      showPassword: false,
      showConfirmPassword: false,
    };
  },
  methods: {
    togglePassword() {
      this.showPassword = !this.showPassword;
    },
    toggleConfirmPassword() {
      this.showConfirmPassword = !this.showConfirmPassword;
    },
    async register() {
      if (!this.agree) {
        alert("약관에 동의해야 회원가입이 가능합니다.");
        return;
      }
      if (this.password !== this.confirmPassword) {
        alert("비밀번호가 일치하지 않습니다.");
        return;
      }

      try {
        const response = await axios.post("http://localhost:8888/users/register", {
          name: this.firstName + this.lastName,
          email: this.email,
          password: this.password,
          phone: "010-0000-0000",
          address: "서울",
          role: "USER"
        });
        alert("회원가입 성공!");
        this.$router.push("/login");
      } catch (error) {
        console.error("회원가입 실패:", error.response?.data || error.message);
        alert("회원가입 실패!");
      }
    },
  },
};
</script>
