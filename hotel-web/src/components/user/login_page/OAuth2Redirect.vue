<template>
  <div class="oauth-redirect">
    <div class="loading">
      <h2>로그인 처리중...</h2>
      <p>잠시만 기다려주세요.</p>
    </div>
  </div>
</template>

<script>
import UserApi from "@/api/UserApi";
import { setAuthUser, clearAuthUser, notifyAuthChanged } from "@/utils/auth-storage";

const providerNames = {
  NAVER: "네이버",
  GOOGLE: "구글",
  KAKAO: "카카오",
  LOCAL: "일반",
};

export default {
  name: "OAuth2Redirect",
  mounted() {
    this.handleOAuth2Redirect();
  },
  methods: {
    async handleOAuth2Redirect() {
      const urlParams = new URLSearchParams(window.location.search);
      const currentProvider = urlParams.get('provider');

      try {
        const userInfo = await UserApi.getInfo();
        setAuthUser(userInfo);

        const displayProvider = currentProvider || userInfo?.provider;
        const providerName = providerNames[displayProvider] || displayProvider || "소셜";

        notifyAuthChanged();
        this.$router.push(userInfo?.role === 'ADMIN' ? '/admin' : '/');
      } catch (error) {
        const status = error.response?.status;
        if (status === 401 || error.message?.includes('401')) {
          // 401 오류는 무시
        } else {
          console.error('사용자 정보 가져오기 실패:', error);
          alert('로그인 처리 중 오류가 발생했습니다.');
        }
        clearAuthUser();
        notifyAuthChanged();
        this.$router.push('/login');
      }
    }
  }
};
</script>

<style scoped>
.oauth-redirect {
  display: flex;
  justify-content: center;
  align-items: center;
  height: 100vh;
  background-color: #f5f5f5;
}

.loading {
  text-align: center;
  padding: 2rem;
  background: white;
  border-radius: 8px;
  box-shadow: 0 2px 10px rgba(0,0,0,0.1);
}

.loading h2 {
  color: #333;
  margin-bottom: 1rem;
}

.loading p {
  color: #666;
}
</style>
