<template>
  <div class="main-page">
    <header class="main-header">
      <div class="header-content">
        <div class="header-left">
          <button class="hamburger-menu">
            <svg width="32" height="32" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
              <path d="M3 6h18M3 12h18M3 18h18" stroke="rgba(0,0,0,0.54)" stroke-width="2" stroke-linecap="round"/>
            </svg>
          </button>
          <div class="logo-container">
            <img src="/egodaLogo.png" alt="egoda icon" class="logo-icon" />
            <span class="logo-text-placeholder">egoda</span>
          </div>
        </div>

        <div class="header-right">
          <span class="login-text" @click="$router.push('/login')">로그인</span>
          <span>|</span>
          <span class="register-text" @click="$router.push('/register')">회원가입</span>
        </div>
      </div>
    </header>

    <div class="hero-section" style="background-image: url('https://api.builder.io/api/v1/image/assets/TEMP/8646b4d9a3fc120edf8a5960adb52153b4b0e339?width=2880')">
      <div class="hero-overlay"></div>
      
      <div class="hero-text-container">
        <div class="hero-text">
          <p class="hero-subtitle">
            검색을 통해 요금을 비교하고 무료 취소 포함한 특가도 확인하세요!
          </p>
        </div>
      </div>
    </div>

    <div class="search-form-wrapper">
      <div class="search-form-card">
        <div class="search-form-tabs">
          <button class="tab-button active">
             <svg width="24" height="24" viewBox="0 0 24" fill="none" xmlns="http://www.w3.org/2000/svg">
              <path d="M20.25 10.8141C19.7772 10.6065 19.2664 10.4996 18.75 10.5H5.25C4.73368 10.4995 4.22288 10.6063 3.75 10.8136C3.08166 11.1059 2.51294 11.5865 2.11336 12.1968C1.71377 12.8071 1.50064 13.5205 1.5 14.25V19.5C1.5 19.6989 1.57902 19.8897 1.71967 20.0303C1.86032 20.171 2.05109 20.25 2.25 20.25C2.44891 20.25 2.63968 20.171 2.78033 20.0303C2.92098 19.8897 3 19.6989 3 19.5V19.125C3.00122 19.0259 3.04112 18.9312 3.11118 18.8612C3.18124 18.7911 3.27592 18.7512 3.375 18.75H20.625C20.7241 18.7512 20.8188 18.7911 20.8888 18.8612C20.9589 18.9312 20.9988 19.0259 21 19.125V19.5C21 19.6989 21.079 19.8897 21.2197 20.0303C21.3603 20.171 21.5511 20.25 21.75 20.25C21.9489 20.25 22.1397 20.171 22.2803 20.0303C22.421 19.8897 22.5 19.6989 22.5 19.5V14.25C22.4993 13.5206 22.2861 12.8073 21.8865 12.1971C21.4869 11.5869 20.9183 11.1063 20.25 10.8141ZM17.625 3.75H6.375C5.67881 3.75 5.01113 4.02656 4.51884 4.51884C4.02656 5.01113 3.75 5.67881 3.75 6.375V9.75C3.75002 9.77906 3.75679 9.80771 3.76979 9.8337C3.78278 9.85969 3.80163 9.8823 3.82486 9.89976C3.84809 9.91721 3.87505 9.92903 3.90363 9.93428C3.93221 9.93953 3.96162 9.93806 3.98953 9.93C4.39896 9.81025 4.82341 9.74964 5.25 9.75H5.44828C5.49456 9.75029 5.53932 9.73346 5.57393 9.70274C5.60855 9.67202 5.63058 9.62958 5.63578 9.58359C5.67669 9.21712 5.85115 8.87856 6.12586 8.63256C6.40056 8.38656 6.75625 8.25037 7.125 8.25H9.75C10.119 8.25003 10.475 8.38606 10.75 8.63209C11.025 8.87812 11.1997 9.21688 11.2406 9.58359C11.2458 9.62958 11.2679 9.67202 11.3025 9.70274C11.3371 9.73346 11.3818 9.75029 11.4281 9.75H12.5747C12.621 9.75029 12.6657 9.73346 12.7003 9.70274C12.735 9.67202 12.757 9.62958 12.7622 9.58359C12.8031 9.21736 12.9773 8.87899 13.2517 8.63303C13.5261 8.38706 13.8815 8.25072 14.25 8.25H16.875C17.244 8.25003 17.6 8.38606 17.875 8.63209C18.15 8.87812 18.3247 9.21688 18.3656 9.58359C18.3708 9.62958 18.3929 9.67202 18.4275 9.70274C18.4621 9.73346 18.5068 9.75029 18.5531 9.75H18.75C19.1766 9.74979 19.6011 9.81057 20.0105 9.93047C20.0384 9.93854 20.0679 9.94 20.0965 9.93473C20.1251 9.92945 20.1521 9.91759 20.1753 9.90009C20.1986 9.88258 20.2174 9.8599 20.2304 9.83385C20.2433 9.8078 20.2501 9.7791 20.25 9.75V6.375C20.25 5.67881 19.9734 5.01113 19.4812 4.51884C18.9889 4.02656 18.3212 3.75 17.625 3.75Z" fill="#112211"/>
            </svg>
            <span>숙소</span>
          </button>
        </div>
        <h2 class="search-form-title">어디를 가실건가요 ?</h2>
        
        <div class="search-form-grid">
          <div class="search-form-item destination">
            <label class="search-form-label">목적지</label>
            <div class="input-field">
              <input type="text" class="input-text" placeholder="목적지 또는 숙소명" />
            </div>
          </div>

          <div class="search-form-item">
            <label class="search-form-label">체크인</label>
            <div class="input-field">
              <input type="text" class="input-text" placeholder="날짜 추가" />
              <svg width="24" height="24" viewBox="0 0 24" fill="none" xmlns="http://www.w3.org/2000/svg" class="input-icon">
                <path d="M21 6.85708C21 6.1751 20.7291 5.52105 20.2468 5.03881C19.7646 4.55657 19.1106 4.28566 18.4286 4.28566H17.7857V3.66088C17.7857 3.31494 17.5197 3.01802 17.1738 3.00074C17.0869 2.99656 17 3.01006 16.9185 3.04044C16.8369 3.07081 16.7624 3.11743 16.6994 3.17746C16.6364 3.23749 16.5862 3.30969 16.552 3.38968C16.5177 3.46966 16.5 3.55578 16.5 3.6428V4.28566H7.5V3.66088C7.5 3.31494 7.23402 3.01802 6.88808 3.00074C6.80116 2.99656 6.71429 3.01006 6.63275 3.04044C6.5512 3.07081 6.47667 3.11743 6.41367 3.17746C6.35067 3.23749 6.30051 3.30969 6.26624 3.38968C6.23197 3.46966 6.21429 3.55578 6.21429 3.6428V4.28566H5.57143C4.88944 4.28566 4.23539 4.55657 3.75315 5.03881C3.27092 5.52105 3 6.1751 3 6.85708V7.33923C3 7.38185 3.01693 7.42273 3.04707 7.45287C3.07721 7.48301 3.11809 7.49994 3.16071 7.49994H20.8393C20.8819 7.49994 20.9228 7.48301 20.9529 7.45287C20.9831 7.42273 21 7.38185 21 7.33923V6.85708ZM3 18.4285C3 19.1105 3.27092 19.7645 3.75315 20.2468C4.23539 20.729 4.88944 20.9999 5.57143 20.9999H18.4286C19.1106 20.9999 19.7646 20.729 20.2468 20.2468C20.7291 19.7645 21 19.1105 21 18.4285V8.90619C21 8.87422 20.9873 8.84356 20.9647 8.82096C20.9421 8.79835 20.9114 8.78566 20.8795 8.78566H3.12054C3.08857 8.78566 3.05791 8.79835 3.0353 8.82096C3.0127 8.84356 3 8.87422 3 8.90619V18.4285Z" fill="#112211"/>
              </svg>
            </div>
          </div>

          <div class="search-form-item">
            <label class="search-form-label">체크아웃</label>
            <div class="input-field">
              <input type="text" class="input-text" placeholder="날짜 추가" />
              <svg width="24" height="24" viewBox="0 0 24" fill="none" xmlns="http://www.w3.org/2000/svg" class="input-icon">
                <path d="M21 6.85708C21 6.1751 20.7291 5.52105 20.2468 5.03881C19.7646 4.55657 19.1106 4.28566 18.4286 4.28566H17.7857V3.66088C17.7857 3.31494 17.5197 3.01802 17.1738 3.00074C17.0869 2.99656 17 3.01006 16.9185 3.04044C16.8369 3.07081 16.7624 3.11743 16.6994 3.17746C16.6364 3.23749 16.5862 3.30969 16.552 3.38968C16.5177 3.46966 16.5 3.55578 16.5 3.6428V4.28566H7.5V3.66088C7.5 3.31494 7.23402 3.01802 6.88808 3.00074C6.80116 2.99656 6.71429 3.01006 6.63275 3.04044C6.5512 3.07081 6.47667 3.11743 6.41367 3.17746C6.35067 3.23749 6.30051 3.30969 6.26624 3.38968C6.23197 3.46966 6.21429 3.55578 6.21429 3.6428V4.28566H5.57143C4.88944 4.28566 4.23539 4.55657 3.75315 5.03881C3.27092 5.52105 3 6.1751 3 6.85708V7.33923C3 7.38185 3.01693 7.42273 3.04707 7.45287C3.07721 7.48301 3.11809 7.49994 3.16071 7.49994H20.8393C20.8819 7.49994 20.9228 7.48301 20.9529 7.45287C20.9831 7.42273 21 7.38185 21 7.33923V6.85708ZM3 18.4285C3 19.1105 3.27092 19.7645 3.75315 20.2468C4.23539 20.729 4.88944 20.9999 5.57143 20.9999H18.4286C19.1106 20.9999 19.7646 20.729 20.2468 20.2468C20.7291 19.7645 21 19.1105 21 18.4285V8.90619C21 8.87422 20.9873 8.84356 20.9647 8.82096C20.9421 8.79835 20.9114 8.78566 20.8795 8.78566H3.12054C3.08857 8.78566 3.05791 8.79835 3.0353 8.82096C3.0127 8.84356 3 8.87422 3 8.90619V18.4285Z" fill="#112211"/>
              </svg>
            </div>
          </div>

          <div class="search-form-item">
            <label class="search-form-label">여행자 수</label>
            <div class="input-field">
               <svg width="24" height="24" viewBox="0 0 24" fill="none" xmlns="http://www.w3.org/2000/svg" class="input-icon">
                <path d="M12 12c2.21 0 4-1.79 4-4s-1.79-4-4-4-4 1.79-4 4 1.79 4 4 4zm0 2c-2.67 0-8 1.34-8 4v2h16v-2c0-2.66-5.33-4-8-4z" fill="#112211"/>
               </svg>
               <input type="text" class="input-text" placeholder="여행자 1명" />
               <svg width="24" height="24" viewBox="0 0 24" fill="none" xmlns="http://www.w3.org/2000/svg">
                  <path d="M7 10l5 5 5-5z" fill="#112211"/>
               </svg>
            </div>
          </div>
        </div>
      </div>
    </div>

    <div class="main-content-spacer"></div>

    <footer class="main-footer">
      <div class="footer-content-wrapper"> <div class="footer-logo-container">
          <img src="/egodaLogo.png" alt="egoda icon" class="footer-logo-icon" />
          <span class="footer-logo-text-placeholder">egoda</span>
        </div>
        <div class="footer-text-container">
          All material herein © 2005 – 2025 Agoda Company Pte. Ltd., All Rights Reserved.<br>
          이거다는 온라인 여행 및 관련 서비스 분야의 세계적인 선도 기업인 Booking Holdings Inc.의 일부입니다.
        </div>
      </div>
    </footer>
  </div>
</template>

<script setup>
</script>

<style scoped>
@import url('https://fonts.googleapis.com/css2?family=Montserrat:wght@400;600;700&display=swap');

.main-page {
  font-family: 'Montserrat', sans-serif;
  min-height: 100vh;
  background-color: #FAFBFC;
}

/* Header */
.main-header {
  background-color: #fff;
  box-shadow: 0 4px 16px 0 rgba(17, 34, 17, 0.05);
  position: relative;
  z-index: 20;
}

.header-content {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 2%;
  max-width: 1200px;
  margin: 0 auto;
  height: 87px;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 16px;
}

.hamburger-menu {
  padding: 12px;
  border: none;
  background: none;
  cursor: pointer;
}

.hamburger-menu svg {
  width: 32px;
  height: 32px;
}

.logo-container {
  display: flex;
  align-items: center;
}

.logo-icon {
  width: 48px;
  height: 48px;
}

.logo-text-placeholder {
  font-size: 32px;
  font-weight: 700;
  color: #8DD3BB;
  margin-left: 8px;
  font-family: 'Montserrat', sans-serif;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 16px;
  font-size: 14px;
  font-weight: 600;
  color: #121211;
}

.login-text, .register-text {
  cursor: pointer;
}

/* Hero Section */
.hero-section {
  position: relative;
  height: 450px;
  background-size: cover;
  background-position: center;
  display: flex;
  justify-content: center;
}

.hero-overlay {
  position: absolute;
  inset: 0;
  background-color: rgba(0, 0, 0, 0.2);
}

.hero-text-container {
  position: relative;
  max-width: 1200px;
  width: 100%;
  height: 100%;
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: flex-start;
  padding: 0 4%;
  z-index: 1;
}

.hero-text {
  color: #fff;
  max-width: 600px;
}

.hero-title {
  display: none;
}

.hero-subtitle {
  font-size: 16px;
  opacity: 0.9;
  margin-top: 0;
}

/* Search Form */
.search-form-wrapper {
  max-width: 1200px;
  width: 90%;
  margin: 0 auto;
  margin-top: -120px;
  position: relative;
  z-index: 10;
}

.search-form-card {
  background-color: #fff;
  border-radius: 12px;
  box-shadow: 0 4px 16px 0 rgba(17, 34, 17, 0.05);
  padding: 16px;
}

.search-form-tabs {
  display: flex;
  margin-bottom: 24px;
}

.tab-button {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 16px;
  border: none;
  background-color: transparent;
  font-size: 16px;
  font-weight: 600;
  color: #112211;
  cursor: pointer;
  border-bottom: 2px solid transparent;
}

.tab-button.active {
  border-bottom: 2px solid #8DD3BB;
}

.search-form-title {
  font-size: 20px;
  font-weight: 600;
  color: #121211;
  margin-bottom: 16px;
  padding: 0 16px;
}

.search-form-grid {
  display: grid;
  grid-template-columns: 2fr 1fr 1fr 1fr;
  gap: 16px;
  padding: 0 16px 16px;
}

.search-form-item {
  position: relative;
}

.search-form-label {
  display: block;
  font-size: 14px;
  color: #112211;
  margin-bottom: 8px;
  font-weight: 600;
}

.input-field {
  display: flex;
  align-items: center;
  border: 1px solid #D8D8D8;
  border-radius: 4px;
  height: 56px;
  padding: 0 16px;
  background-color: #fff;
}

.input-field:focus-within {
  border-color: #8DD3BB;
}

.input-icon {
  margin-right: 8px;
  flex-shrink: 0;
}

.input-text {
  flex: 1;
  border: none;
  outline: none;
  font-size: 16px;
  width: 100%;
}

.input-text::placeholder {
  color: #A9A9A9;
}


.main-content-spacer {
  height: 550px;
}


/* Footer */
.main-footer {
  background-color: #DBDCDA;
  display: flex;
  flex-direction: column; /* 기존 유지 (내부 컨텐츠 정렬을 위해) */
  align-items: center; /* 내부 컨텐츠 중앙 정렬 */
  justify-content: center;
  padding: 40px 20px;
  height: auto;
}

/* 새로운 래퍼 스타일 */
.footer-content-wrapper {
  display: flex; /* 가로 정렬 */
  align-items: center; /* 세로 중앙 정렬 */
  gap: 20px; /* 로고와 텍스트 사이 간격 */
  max-width: 1200px;
  width: 100%;
  justify-content: center; /* 전체 푸터 내용 중앙 정렬 */
}

.footer-logo-container {
  display: flex;
  align-items: center;
  /* 기존 justify-content: center; 제거 */
}

.footer-logo-icon {
  width: 48px; /* 크기 조정 */
  height: 48px; /* 크기 조정 */
  margin-right: 8px; /* 간격 조정 */
}

.footer-logo-text-placeholder {
  font-size: 32px; /* 크기 조정 */
  font-weight: 600;
  color: #8DD3BB;
  font-family: 'Montserrat', sans-serif;
}

.footer-text-container {
  text-align: left; /* 텍스트 왼쪽 정렬 */
  color: #1a1a1a;
  font-size: 14px;
  font-weight: 400;
  max-width: 723px; /* 필요에 따라 조정 */
  margin-top: 0; /* 기존 margin-top 제거 */
  line-height: 1.6;
}

/* Responsive styles */
@media (max-width: 1024px) {
  .hero-section {
    height: 400px;
  }
  
  .search-form-wrapper {
    width: 95%;
    margin-top: -100px;
  }
  
  .search-form-grid {
    grid-template-columns: 1fr;
  }

  .main-content-spacer {
    height: 100px;
  }

  .footer-content-wrapper {
    flex-direction: column; /* 모바일에서는 다시 세로 정렬 */
    text-align: center;
    gap: 15px;
  }

  .footer-text-container {
    text-align: center;
  }
}

@media (max-width: 768px) {
  .header-content {
    padding: 0 4%;
    height: 60px;
  }
  .hamburger-menu svg {
    width: 24px;
    height: 24px;
  }
  .logo-text-placeholder {
    font-size: 24px;
  }
  .logo-icon {
    width: 32px;
    height: 32px;
  }
  .header-right {
    font-size: 12px;
    gap: 8px;
  }
  .hero-title {
    font-size: 28px;
  }
  .hero-subtitle {
    font-size: 14px;
  }

  .footer-logo-icon {
    width: 32px; /* 모바일 푸터 로고 크기 조정 */
    height: 32px;
  }
  .footer-logo-text-placeholder {
    font-size: 24px; /* 모바일 푸터 텍스트 크기 조정 */
  }
}
</style>