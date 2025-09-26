<template>
  <div class="wrapper w-100">
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
import api from "@/api/http";
import router from "@/router";
export default {
  data() {
    return {
      paymentKey: '',
      orderId: '',
      amount: '',
      isSuccess: false, // 결제 성공 여부
    };
  },
  mounted() {
    const urlParams = new URLSearchParams(window.location.search);
    this.paymentKey = urlParams.get('paymentKey');
    this.orderId = urlParams.get('orderId');
    this.amount = urlParams.get('amount');
    this.confirmPayment();
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
      const responseBody = await response.data;
      console.log(responseBody);
      const targetid = responseBody.reservationId;
      console.log(targetid);
      api.post(`/reservations/${responseBody.reservationId}/confirm`);
      router.push(`/reservations/${targetid}/result`)
      } 
    } catch (error) {
        console.error('결제 확인 중 오류 발생:', error);
        alert('서버에 연결할 수 없습니다. 나중에 다시 시도해주세요.');
    }
},
  },
};
</script>
