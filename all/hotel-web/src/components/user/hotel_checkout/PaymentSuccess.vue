<!-- src/pages/PaymentSuccess.vue -->
<template>
  <div class="wrapper w-100">
    <div v-if="isSuccess" class="flex-column align-center confirm-success w-100 max-w-540">
      <img
        src="https://static.toss.im/illusts/check-blue-spot-ending-frame.png"
        width="120" height="120" alt="결제 완료"
      />
      <h2 class="title">결제를 완료했어요</h2>

      <div class="response-section w-100">
        <div class="flex justify-between">
          <span class="response-label">결제 금액</span>
          <span class="response-text">{{ Number(amount || 0).toLocaleString('ko-KR') }}원</span>
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

    <div v-else class="flex-column align-center w-100 max-w-540" style="padding: 32px;">
      <h3>결제 확인 중...</h3>
    </div>
  </div>
</template>

<script>
import api from "@/api/http";
import router from "@/router";

export default {
  name: "PaymentSuccess",
  data() {
    return {
      paymentKey: "",
      orderId: "",
      amount: "",      // 화면 표시용(문자열)
      amountNum: 0,    // 실제 숫자 값(서버 confirm 바디에 사용)
      isSuccess: false,
      paymentId: null, // sessionStorage.lastPaymentId (fallback)
    };
  },
  async mounted() {
    const urlParams = new URLSearchParams(window.location.search);
    this.paymentKey = urlParams.get("paymentKey") || "";
    this.orderId = urlParams.get("orderId") || "";

    // ❌ URL의 amount는 절대 신뢰 금지
    this.paymentId = sessionStorage.getItem("lastPaymentId");

    await this.fetchAmountSafely();    // 화면 표시용 금액 (서버에서 조회)
    await this.confirmPaymentSafely(); // 서버 DB 금액(또는 위에서 조회한 금액)으로 Toss Confirm
  },
  methods: {
    // 서버에 저장된 결제 금액을 조회 (표시 + confirm 바디용 숫자)
    async fetchAmountSafely() {
      try {
        // (선호) 주문번호 기반 조회 엔드포인트가 있다면 이걸 먼저 사용
        // const r1 = await api.get(`payments/by-order/${encodeURIComponent(this.orderId)}`);
        // if (r1?.data) {
        //   const a = Number(r1.data.amount ?? 0);
        //   this.amount = String(a);
        //   this.amountNum = a;
        //   return;
        // }

        // fallback: lastPaymentId 로 조회
        if (this.paymentId) {
          // ⚠️ baseURL=/api 이므로 앞 슬래시 금지
          const r2 = await api.get(`payments/${this.paymentId}`);
          const a = Number(r2?.data?.amount ?? 0);
          this.amount = String(a);
          this.amountNum = a;
        }
      } catch (e) {
        console.warn("표시용 결제 금액 조회 실패:", e);
        // 실패해도 confirm에서 서버 DB 금액으로 처리 시도
      }
    },

    async confirmPaymentSafely() {
      try {
        if (!this.paymentKey || !this.orderId) {
          alert("결제 정보가 유효하지 않습니다. (paymentKey/orderId 누락)");
          return;
        }

        // ✅ 서버 confirm 바디: paymentKey, orderId는 필수
        // 서버가 amount를 파싱(Long.parseLong 등)한다면 null 문자열 문제를 막기 위해 안전하게 숫자 동봉
        const payload = {
          paymentKey: this.paymentKey,
          orderId: this.orderId,
        };
        if (Number.isFinite(this.amountNum) && this.amountNum > 0) {
          payload.amount = this.amountNum; // 숫자 그대로 전달
        }

        // ⚠️ 앞 슬래시 금지 (baseURL=/api 가 붙음)
        const response = await api.post("payments/confirm", payload);

        if (response.status === 200) {
          const body = response.data || {};
          const reservationId = body.reservationId;

          // 예약 확정(백엔드가 따로 확정 엔드포인트를 요구하는 경우)
          if (reservationId) {
            await api.post(`reservations/${reservationId}/confirm`);
            this.isSuccess = true;
            // 절대경로로 이동
            return router.push(`/reservations/${reservationId}/result`);
          }

          // 예약ID가 없어도 일단 성공 화면 표시
          this.isSuccess = true;
        } else {
          throw new Error(`Confirm 실패: ${response.status}`);
        }
      } catch (error) {
        // 서버가 "For input string: "null"" 같은 에러를 보낼 때 메시지 추출
        const msg =
          error?.response?.data?.message ||
          error?.response?.data?.error ||
          error?.message ||
          "서버 확인 실패";
        console.error("결제 확인 중 오류:", error?.response?.data || error);
        alert(`[결제 확인 실패] ${msg}`);
      }
    },
  },
};
</script>

<style scoped>
div { font-family: 'Noto Sans KR', sans-serif; box-sizing: border-box; }
.wrapper{ margin-top:40px; padding:20px; background:#fafafa; border-radius:8px; }
.max-w-540{ max-width:540px; margin:0 auto; }
.response-section{ margin-top: 16px; border:1px solid #eaeaea; border-radius: 8px; padding: 12px; background: #fff; }
.response-label{ color:#666; }
.response-text{ font-weight:800; }
.title{ margin: 12px 0; }
.btn{
  display:inline-flex; justify-content:center; align-items:center;
  padding:12px 16px; border-radius:8px; background:#0066ff; color:#fff; font-weight:800;
  text-decoration:none;
}
</style>
