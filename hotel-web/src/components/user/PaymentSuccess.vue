<template>
  <div class="wrapper w-100">
    <!-- 결제 요청 중 화면 -->
    <div v-if="isLoading" class="flex-column align-center confirm-loading w-100 max-w-540">
      <div class="flex-column align-center">
        <img src="https://static.toss.im/lotties/loading-spot-apng.png" width="120" height="120" />
        <h2 class="title text-center">결제 요청까지 성공했어요.</h2>
        <h4 class="text-center description">결제 승인하고 완료해보세요.</h4>
      </div>
      <div class="w-100">
        <button @click="confirmPayment" class="btn primary w-100">결제 승인하기</button>
      </div>
    </div>

    <!-- 결제 완료 화면 -->
    <div v-if="isSuccess" class="flex-column align-center confirm-success w-100 max-w-540">
      <img src="https://static.toss.im/illusts/check-blue-spot-ending-frame.png" width="120" height="120" />
      <h2 class="title">결제를 완료했어요</h2>
      <div class="response-section w-100">
        <div class="flex justify-between">
          <span class="response-label">결제 금액</span>
          <span class="response-text">{{ amount }}원</span>
        </div>
        <div class="flex justify-between">
          <span class="response-label">주문번호</span>
          <span class="response-text">{{ orderId }}</span>
        </div>
        <div class="flex justify-between">
          <span class="response-label">paymentKey</span>
          <span class="response-text">{{ paymentKey }}</span>
        </div>
      </div>

      <div class="w-100 button-group">
        <div class="flex" style="gap: 16px;">
          <a class="btn w-100" href="/checkout">다시 테스트하기</a>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import api from "@/api/api";
export default {
  data() {
    return {
      paymentKey: '',
      orderId: '',
      amount: '',
      isLoading: true, // 결제 로딩 상태
      isSuccess: false, // 결제 성공 여부
    };
  },
  mounted() {
    const urlParams = new URLSearchParams(window.location.search);
    this.paymentKey = urlParams.get('paymentKey');
    this.orderId = urlParams.get('orderId');
    this.amount = urlParams.get('amount');
  },
  methods: {
    async confirmPayment() {
  try {
    const response = await api.post('/payments/confirm', {
      paymentKey: this.paymentKey,
      orderId: this.orderId,
      amount: this.amount,
    });  

    if (response.status == 200) {
      const responseBody = await response.data;  // 응답 본문을 JSON 형식으로 파싱      

      if (responseBody.message === "결제 승인 완료") {
        this.isLoading = false;  // 로딩 상태 종료
        this.isSuccess = true;   // 결제 성공
      } else {        
        alert('결제 확인에 실패했습니다.');
      }
        } else {            
            alert('결제 확인에 실패했습니다.');
        }
    } catch (error) {
        console.error('결제 확인 중 오류 발생:', error);
        alert('서버에 연결할 수 없습니다. 나중에 다시 시도해주세요.');
    }
},
  },
};
</script>
