<template>
  <div class="privacy-page">
    <button class="close-btn" @click="$router.back()">✕</button>
    <h1>개인정보 처리방침</h1>
    <p>
      본 개인정보 처리방침은 트립닷컴(이하 "회사")가 수집하는 개인정보의 종류, 수집 및 이용 목적, 보관 기간,
      제3자 제공, 이용자 권리 등을 규정합니다.
    </p>
    <h2>1. 수집하는 개인정보 항목</h2>
    <ul>
      <li>이름, 이메일, 비밀번호 등 회원가입 정보</li>
      <li>서비스 이용 기록, 접속 로그, 쿠키 등</li>
    </ul>
    <h2>2. 개인정보 수집 및 이용 목적</h2>
    <p>회원 관리, 서비스 제공, 문의 대응, 마케팅 및 광고에 활용할 수 있습니다.</p>
    <h2>3. 개인정보 보관 기간</h2>
    <p>회원 탈퇴 시까지 보관하며, 관계 법령에 따라 일정 기간 보관 후 파기합니다.</p>
    <h2>4. 이용자의 권리와 행사 방법</h2>
    <p>언제든지 개인정보 열람, 정정, 삭제를 요청할 수 있으며, 회사는 지체 없이 처리합니다.</p>
  </div>
</template>

<script>
export default { name: 'PrivacyPage' };
</script>

<style scoped>
.privacy-page { padding: 20px; max-width: 800px; margin: 0 auto; }
.privacy-page h1, .privacy-page h2 { margin-top: 16px; }
.close-btn { position: absolute; top: 20px; right: 20px; background: none; border: none; font-size: 24px; cursor: pointer; }
</style>