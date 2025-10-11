<template>
  <div class="wrapper w-100">
    <div class="max-w-540 w-100">
      <div id="payment-method" class="w-100"></div>
      <div id="agreement" class="w-100"></div>
      <div class="btn-wrapper w-100">
        <button id="payment-request-button" class="btn primary w-100">결제하기</button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { loadTossPayments, ANONYMOUS } from "@tosspayments/tosspayments-sdk"
import api from "@/api/http"
import { computed, onMounted } from "vue"
import { useRoute, useRouter } from "vue-router"

const route = useRoute()
const router = useRouter()

// 결제ID 복구: params → query → sessionStorage
const paymentId = computed(() => route.params.id || route.query.id || sessionStorage.getItem("lastPaymentId"))

const amount = { currency: "KRW", value: 0 }
const PaymentContent = {
  orderId: window.btoa(Math.random()).slice(0, 20), // 서버 값으로 덮어씀
  orderName: "",
  successUrl: window.location.origin + "/payment/success",
  failUrl: window.location.origin + "/payment/fail",
  customerEmail: "",
  customerName: "",
  customerMobilePhone: ""
}

onMounted(async () => {
  if (!paymentId.value) {
    alert("결제ID가 없습니다. 처음부터 다시 진행해주세요.")
    router.replace("/")
    return
  }
  await getPayment(paymentId.value)
  await main()
})

async function getPayment(pid){
  try{
    // ⚠️ 앞 슬래시 금지
    const recv = await api.get(`payments/${pid}`)
    const d = recv?.data ?? {}
    PaymentContent.orderId = d.orderId || PaymentContent.orderId
    PaymentContent.orderName = d.orderName || "숙박 결제"
    PaymentContent.customerName = d.customerName || ""
    PaymentContent.customerEmail = d.email || ""
    const raw = d.phone || ""
 const digits = String(raw).replace(/\D/g, "")
 if (digits.length === 10 || digits.length === 11) {
   PaymentContent.customerMobilePhone = digits
 } else {
   delete PaymentContent.customerMobilePhone // 유효하지 않으면 아예 제외
 }
    amount.value = Number(d.amount ?? 0) // ✅ 서버 계산 총액
  }catch(e){
    console.error(e)
    alert("결제 정보를 불러오지 못했습니다.")
    router.replace("/")
  }
}

async function main(){
  // 사전 검증
  if (!PaymentContent.orderName?.trim()) { alert("상품명이 비어 있습니다."); return }
  if (!amount.value || amount.value <= 0) { alert("결제 금액이 올바르지 않습니다."); return }

  const tossPayments = await loadTossPayments("test_gck_docs_Ovk5rk1EwkEbP0W43n07xlzm")
  const widgets = tossPayments.widgets({ customerKey: ANONYMOUS })

  // ✅ 반드시 선행: 올바른 시그니처
  await widgets.setAmount({ currency: amount.currency, value: amount.value })

  await Promise.all([
    widgets.renderPaymentMethods({ selector: "#payment-method", variantKey: "DEFAULT" }),
    widgets.renderAgreement({ selector: "#agreement", variantKey: "AGREEMENT" }),
  ])

  const paymentRequestButton = document.getElementById("payment-request-button")
  paymentRequestButton.addEventListener("click", async () => {
    try{
      await widgets.requestPayment(PaymentContent)
    }catch(err){
      console.log(err)
    }
  })
}
</script>

<style scoped>
div { font-family: 'Noto Sans KR', sans-serif; box-sizing: border-box; }
.wrapper{ margin-top:40px; padding:20px; background:#fafafa; border-radius:8px; }
.max-w-540{ max-width:540px; margin:0 auto; }
.btn-wrapper{ margin-top:20px; }
button{
  display:block; width:100%; max-width:540px; margin:20px auto; padding:14px 20px;
  background:#0066ff; color:#fff; font-size:16px; font-weight:700; border:none; border-radius:6px;
  cursor:pointer; transition:background-color .3s;
}
button:hover{ background:#0052cc; }
</style>
