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

        <div class="recaptcha-box">
          <div ref="recaptcha"></div>
          <p v-if="recaptchaError" class="recaptcha-error">{{ recaptchaError }}</p>
        </div>

        <!-- 로그인 버튼 -->
        <button class="login-btn" @click="login" :disabled="isSubmitting">
          {{ isSubmitting ? '로그인 중...' : '로그인' }}
        </button>
        <button class="register-btn" @click="$router.push('/register')">회원가입</button>

        <!-- 구분선 -->
        <div class="divider">------------------------------- 또는 -------------------------------</div>

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
import http, { resolveBackendUrl } from "@/api/http";
import UserApi from "@/api/UserApi";
import { setAuthUser, clearAuthUser, notifyAuthChanged } from "@/utils/auth-storage";

const REMEMBER_EMAIL_KEY = "auth.savedEmail";
const REMEMBER_PASSWORD_KEY = "auth.savedPassword";

const providerDisplayMap = {
  NAVER: "네이버",
  GOOGLE: "구글",
  KAKAO: "카카오",
  LOCAL: "일반",
};

const readRememberedCredentials = () => {
  if (typeof window === "undefined") {
    return { email: "", password: "" };
  }
  return {
    email: window.localStorage.getItem(REMEMBER_EMAIL_KEY) || "",
    password: window.localStorage.getItem(REMEMBER_PASSWORD_KEY) || "",
  };
};

const persistRememberedCredentials = (email, password) => {
  if (typeof window === "undefined") return;
  if (email && password) {
    window.localStorage.setItem(REMEMBER_EMAIL_KEY, email);
    window.localStorage.setItem(REMEMBER_PASSWORD_KEY, password);
  } else {
    window.localStorage.removeItem(REMEMBER_EMAIL_KEY);
    window.localStorage.removeItem(REMEMBER_PASSWORD_KEY);
  }
};

export default {
  name: "LoginPage",
  data() {
    return {
      email: "",
      password: "",
      remember: false,
      showPassword: false,
      recaptchaToken: "",
      recaptchaVerified: false,
      recaptchaError: "",
      recaptchaWidgetId: null,
      isSubmitting: false,
      recaptchaSiteKey: "",
    };
  },
  created() {
    const remembered = readRememberedCredentials();
    if (remembered.email && remembered.password) {
      this.email = remembered.email;
      this.password = remembered.password;
      this.remember = true;
    }
  },
  async mounted() {
    const params = new URLSearchParams(window.location.search);
    const provider = params.get('provider');
    const token = params.get('token');
    if (token) {
      await this.handleLegacySocialLogin(token, provider);
    } else if (provider) {
      await this.handleOAuthLogin(provider);
    }

    await this.fetchRecaptchaSiteKey();
    if (!this.recaptchaSiteKey) {
      return;
    }

    this.loadRecaptchaScript();
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
      if (!this.recaptchaSiteKey) {
        this.recaptchaError = "reCAPTCHA 설정이 완료되지 않았습니다. 잠시 후 다시 시도해주세요.";
        alert("reCAPTCHA 설정을 불러오는 중입니다. 잠시 후 다시 시도해주세요.");
        return;
      }
      if (!this.recaptchaToken) {
        this.recaptchaError = "reCAPTCHA 인증이 필요합니다.";
        return;
      }

      try {
        this.isSubmitting = true;
        const response = await http.post("/users/login", {
          email: this.email,
          password: this.password,
          recaptchaToken: this.recaptchaToken,
        });

        const user = response.data?.user;
        await this.finishLogin(user, user?.provider || "LOCAL");

        if (this.remember) {
          persistRememberedCredentials(this.email, this.password);
        } else {
          persistRememberedCredentials("", "");
        }
      } catch (error) {
        console.error("로그인 실패:", error.response?.data || error.message);
        alert(error.response?.data || '로그인에 실패했습니다.');
        clearAuthUser();
        this.resetRecaptcha();
      }
      this.isSubmitting = false;
    },
    async handleOAuthLogin(provider) {
      try {
        const userInfo = await UserApi.getInfo();
        await this.finishLogin(userInfo, provider || userInfo?.provider);
        this.cleanQueryString();
      } catch (error) {
        const status = error.response?.status;
        if (status === 401 || error.message?.includes('401')) {
          // 401 오류는 무시
        } else {
          console.error('소셜 로그인 처리 실패:', error);
          alert('로그인 처리 중 오류가 발생했습니다.');
        }
        clearAuthUser();
        notifyAuthChanged();
        this.$router.push('/login');
      }
    },
    async handleLegacySocialLogin(token, provider) {
      try {
        const response = await http.get('/user/info', {
          headers: { Authorization: `Bearer ${token}` }
        });
        await this.finishLogin(response.data, provider || response.data?.provider);
        this.cleanQueryString();
      } catch (error) {
        console.error('소셜 로그인 처리 실패:', error);
        alert('로그인 처리 중 오류가 발생했습니다.');
        clearAuthUser();
        notifyAuthChanged();
        this.$router.push('/login');
      }
    },
    async finishLogin(userInfo, provider) {
      if (userInfo) {
        setAuthUser(userInfo);
      }
      notifyAuthChanged();


      const role = userInfo?.role;
      if (role === 'ADMIN') {
        this.$router.push('/admin');
      } else {
        this.$router.push('/');
      }
    },
    cleanQueryString() {
      if (window.history?.replaceState) {
        const cleanUrl = window.location.origin + this.$route.path;
        window.history.replaceState({}, document.title, cleanUrl);
      }
    },
    isValidEmail(email) {
      const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
      return emailRegex.test(email);
    },
    redirectToOAuth(provider) {
      const target = resolveBackendUrl(`/api/oauth2/authorization/${provider}`);
      window.location.href = target;
    },
    naverLogin() {
      this.redirectToOAuth("naver");
    },
    googleLogin() {
      this.redirectToOAuth("google");
    },
    kakaoLogin() {
      this.redirectToOAuth("kakao");
    },
    renderRecaptcha() {
      if (!this.recaptchaSiteKey) {
        this.recaptchaError = "reCAPTCHA 사이트 키가 설정되지 않았습니다.";
        return;
      }

      if (!this.$refs.recaptcha || !window.grecaptcha) {
        this.recaptchaError = "reCAPTCHA를 불러오지 못했습니다. 새로고침 후 다시 시도해주세요.";
        return;
      }
      this.recaptchaWidgetId = window.grecaptcha.render(this.$refs.recaptcha, {
        sitekey: this.recaptchaSiteKey,
        callback: (token) => this.onCaptchaSuccess(token),
        'expired-callback': () => this.onCaptchaExpired(),
        'error-callback': () => this.onCaptchaError(),
      });
    },
    onCaptchaSuccess(token) {
      this.recaptchaToken = token;
      this.recaptchaVerified = true;
      this.recaptchaError = "";
    },
    onCaptchaExpired() {
      this.recaptchaToken = "";
      this.recaptchaVerified = false;
      this.recaptchaError = "reCAPTCHA 인증이 만료되었습니다. 다시 인증해주세요.";
    },
    onCaptchaError() {
      this.recaptchaToken = "";
      this.recaptchaVerified = false;
      this.recaptchaError = "reCAPTCHA 로딩 중 오류가 발생했습니다.";
    },
    resetRecaptcha() {
      this.recaptchaToken = "";
      this.recaptchaVerified = false;
      if (window.grecaptcha && this.recaptchaWidgetId !== null) {
        window.grecaptcha.reset(this.recaptchaWidgetId);
      }
    },
    async fetchRecaptchaSiteKey() {
      try {
        const response = await http.get("/config/recaptcha-site-key");
        if (response?.data?.siteKey) {
          this.recaptchaSiteKey = response.data.siteKey;
          this.recaptchaError = "";
        } else {
          this.recaptchaError = "reCAPTCHA 사이트 키를 불러오지 못했습니다.";
        }
      } catch (error) {
        console.error("reCAPTCHA 사이트 키 로드 실패:", error.response?.data || error.message);
        this.recaptchaError = error.response?.data?.message || "reCAPTCHA 설정을 불러오는 중 오류가 발생했습니다.";
      }
    },
    loadRecaptchaScript() {
      if (!this.recaptchaSiteKey) {
        return;
      }

      if (window.grecaptcha) {
        this.renderRecaptcha();
        return;
      }

      window.onRecaptchaLoad = () => {
        this.renderRecaptcha();
      };

      if (!document.getElementById('recaptcha-script')) {
        const script = document.createElement('script');
        script.id = 'recaptcha-script';
        script.src = 'https://www.google.com/recaptcha/api.js?render=explicit&onload=onRecaptchaLoad';
        script.async = true;
        script.defer = true;
        document.head.appendChild(script);
      }
    },
  },
};
</script>
