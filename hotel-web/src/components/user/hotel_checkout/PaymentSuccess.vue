<script>
import router from "@/router";
import axios from "axios";

export default {
  data() {
    return { paymentKey: "", orderId: "", amount: "", isSuccess: false };
  },
  mounted() {
    const q = new URLSearchParams(window.location.search);
    this.paymentKey = q.get("paymentKey") || "";
    this.orderId    = q.get("orderId")    || "";
    this.amount     = q.get("amount")     || "";

    this.confirmPayment();
  },
  methods: {
    async confirmPayment() {
      try {
        const amountNum = Number(this.amount);
        if (!this.paymentKey || !this.orderId || !Number.isFinite(amountNum)) {
          alert("결제 파라미터가 올바르지 않습니다."); return;
        }

        // ★ 절대 URL로 확실하게 백엔드로 보냄
        const { data: p } = await axios.post(
          "http://localhost:8080/api/payments/confirm",
          { paymentKey: this.paymentKey, orderId: this.orderId, amount: amountNum },
          { headers: { "Content-Type": "application/json" } }
        );

        // 성공 시 이어서 예약 확정(선택)
        if (p?.reservationId) {
          try { await axios.post(`http://localhost:8080/api/reservations/${p.reservationId}/confirm`); } catch {}
          router.push(`/reservations/${p.reservationId}/result`);
        } else {
          // 예약ID 없으면 결제 상세로
          router.push({ name: "PaymentDetail", params: { id: p.id } });
        }
      } catch (e) {
        const s   = e?.response?.status;
        const msg = e?.response?.data?.message || e?.message;
        console.error("[confirm]", s, msg);

        if (s === 404) {
          alert("결제 요청(주문번호)이 서버에 없습니다.\n- 결제 전 /payments/add 선등록 했는지,\n- 토스에 보낸 orderId와 동일한지 확인하세요.");
        } else if (s === 409) {
          alert("이미 처리되었거나 상태 충돌이 발생했습니다.");
        } else {
          alert(msg || "결제 승인 실패");
        }
      }
    },
  },
};
</script>
