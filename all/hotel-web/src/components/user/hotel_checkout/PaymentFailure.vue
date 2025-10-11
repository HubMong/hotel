<template>
  <div class="wrapper w-100">
    <div class="flex-column align-center w-100 max-w-540">
      <img
        src="https://static.toss.im/lotties/error-spot-no-loop-space-apng.png"
        width="160" height="160" alt="Error Image"
      />
      <h2 class="title">결제를 실패했어요</h2>
      <div class="response-section w-100">
        <div class="flex justify-between">
          <span class="response-label">code</span>
          <span class="response-text">{{ errorCode }}</span>
        </div>
        <div class="flex justify-between">
          <span class="response-label">message</span>
          <span class="response-text">{{ errorMessage }}</span>
        </div>
      </div>
      <div class="w-100 button-group">
        <a class="btn" href="/checkout">다시 테스트하기</a>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
const errorCode = ref(''); const errorMessage = ref('')
onMounted(() => {
  const urlParams = new URLSearchParams(window.location.search)
  errorCode.value = urlParams.get('code') || 'N/A'
  errorMessage.value = urlParams.get('message') || 'N/A'
})
</script>

<style scoped>
.wrapper{ display:flex; justify-content:center; padding:40px 16px; }
.max-w-540{ max-width:540px; }
.title{ margin:16px 0; text-align:center; }
.response-section{ background:#fff; border:1px solid #eee; border-radius:8px; padding:16px; margin:16px 0; }
.flex{ display:flex; }
.flex-column{ display:flex; flex-direction:column; }
.align-center{ align-items:center; }
.justify-between{ justify-content:space-between; }
.response-label{ color:#666; }
.response-text{ font-weight:700; }
.btn{
  display:block; width:100%; padding:12px 16px; background:#0066ff; color:#fff; font-weight:700;
  text-align:center; border-radius:6px; text-decoration:none;
}
</style>
