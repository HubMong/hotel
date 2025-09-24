<template>
    <div class="wrapper w-100">
      <div class="flex-column align-center w-100 max-w-540">
        <img
          src="https://static.toss.im/lotties/error-spot-no-loop-space-apng.png"
          width="160"
          height="160"
          alt="Error Image"
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
  
  const errorCode = ref('')
  const errorMessage = ref('')
  
  onMounted(() => {
    const urlParams = new URLSearchParams(window.location.search)
    errorCode.value = urlParams.get('code') || 'N/A'
    errorMessage.value = urlParams.get('message') || 'N/A'
  })
  </script>
  
  <style scoped>
  </style>
  