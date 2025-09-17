<template>
  <div class="oauth-redirect">
    <div class="loading">
      <h2>로그인 처리중...</h2>
      <p>잠시만 기다려주세요.</p>
    </div>
  </div>
</template>

<script>
import http from "@/api/http";

export default {
  name: "OAuth2Redirect",
  mounted() {
    this.handleOAuth2Redirect();
  },
  methods: {
    async handleOAuth2Redirect() {
      const urlParams = new URLSearchParams(window.location.search);
      const token = urlParams.get('token');
      const currentProvider = urlParams.get('provider'); // 현재 로그인에 사용된 provider

      if (token) {
        try {
          // JWT 토큰을 로컬 스토리지에 저장
          localStorage.setItem('token', token);
          
          // API를 호출하여 사용자 정보 가져오기
          const response = await http.get('/user/info', {
            headers: {
              'Authorization': `Bearer ${token}`
            }
          });
          
          const userInfo = response.data;
          localStorage.setItem('userName', userInfo.name);
          localStorage.setItem('userEmail', userInfo.email);
          localStorage.setItem('userProvider', userInfo.provider);
          
          // 현재 로그인에 사용된 provider를 표시 (URL 파라미터 우선)
          const displayProvider = currentProvider || userInfo.provider;
          
          console.log('소셜 로그인 성공:', userInfo);
          console.log('현재 로그인 방식:', displayProvider);
          
          // 한국어로 provider 이름 변환
          const providerNames = {
            'NAVER': '네이버',
            'GOOGLE': '구글', 
            'KAKAO': '카카오',
            'LOCAL': '일반'
          };
          
          const providerDisplayName = providerNames[displayProvider] || displayProvider;
          
          alert(`${userInfo.name}님, ${providerDisplayName} 로그인 성공!`);
          
          // 메인 페이지로 리다이렉트
          this.$router.push('/');
          
        } catch (error) {
          console.error('사용자 정보 가져오기 실패:', error);
          alert('로그인 처리 중 오류가 발생했습니다.');
          localStorage.removeItem('token');
          this.$router.push('/login');
        }
      } else {
        console.error('소셜 로그인 실패: 토큰이 없습니다.');
        alert('로그인 실패!');
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
