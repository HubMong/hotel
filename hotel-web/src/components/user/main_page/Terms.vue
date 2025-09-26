<template>
  <div class="terms-page">
    <button class="close-btn" @click="$router.back()">✕</button>
    <h1>이용약관</h1>
    <p>
      본 이용약관은 트립닷컴(이하 "회사")가 제공하는 모든 서비스(이하 "서비스")의 이용 조건과 절차에 관한 사항을 규정합니다.
    </p>
    <h2>제1조 (목적)</h2>
    <p>이 약관은 회원이 회사가 제공하는 서비스를 이용함에 있어 회사와 회원 간의 권리, 의무 및 책임사항을 규정함을 목적으로 합니다.</p>
    <h2>제2조 (정의)</h2>
    <ul>
      <li>"회원": 회사와 서비스 이용 계약을 체결한 자를 말합니다.</li>
      <li>"회원ID": 회원의 식별과 서비스 이용을 위하여 회원이 설정한 문자 및 숫자의 조합을 말합니다.</li>
    </ul>
    <h2>제3조 (약관의 명시 및 개정)</h2>
    <p>회사는 이 약관의 내용을 회원이 쉽게 확인할 수 있도록 서비스 초기 화면에 게시합니다.</p>
    <h2>제4조 (서비스의 제공 및 변경)</h2>
    <p>회사는 다음과 같은 서비스를 제공합니다. (예: 숙소 예약, 항공권 검색 등)</p>
    <h2>제5조 (회원의 의무)</h2>
    <p>회원은 서비스 이용 시 관련 법령, 이 약관 및 회사가 공지하는 주의사항을 준수하여야 합니다.</p>
    <!-- 추가 약관 내용 -->
  </div>
</template>

<script>
export default {
  name: 'TermsPage'
};
</script>

<style scoped>
.terms-page {
  padding: 20px;
  max-width: 800px;
  margin: 0 auto;
}
.terms-page h1, .terms-page h2 {
  margin-top: 16px;
}
.close-btn {
  position: absolute;
  top: 10px;
  right: 10px;
  background: none;
  border: none;
  font-size: 24px;
  cursor: pointer;
}
</style>
