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
import { loadTossPayments, ANONYMOUS } from "@tosspayments/tosspayments-sdk";
import api from "@/api/http";
import { onMounted, ref } from 'vue';
import { useRoute } from 'vue-router';

const route = useRoute();
const paymentId = route.params.id;

const amount = {
  currency: "KRW",
  value: 0,
};
const PaymentContent = {
  orderId: window.btoa(Math.random()).slice(0, 20),
  orderName: "",
  successUrl: window.location.origin + "/payment/success",
  failUrl: window.location.origin + "/payment/fail",
  customerEmail: "",
  customerName: "",
  customerMobilePhone: ""
};

onMounted(async () => {
  await getPayment(paymentId);
  await main();
});
const getPayment = async (paymentid) => { 
        try {
          const recvdata = await api.get(`/payments/${paymentid}`)
          console.log(recvdata);
          PaymentContent.orderId = recvdata.data.orderId;
          PaymentContent.orderName = recvdata.data.orderName;
          PaymentContent.customerName = recvdata.data.customerName;
          PaymentContent.customerEmail = recvdata.data.email;
          PaymentContent.customerMobilePhone = recvdata.data.phone;
          amount.value = parseInt(recvdata.data.amount); 
        } catch (error) {
          console.log(error);
        }
      };

const main = async () => {
  const tossPayments = await loadTossPayments(
    "test_gck_docs_Ovk5rk1EwkEbP0W43n07xlzm"
  );
  const widgets = tossPayments.widgets({
    customerKey: ANONYMOUS
  })

  /**
   * 위젯의 결제금액을 결제하려는 금액으로 초기화하세요.
   * renderPaymentMethods, renderAgreement, requestPayment 보다 반드시 선행되어야 합니다.
   * @docs https://docs.tosspayments.com/sdk/v2/js#widgetssetamount
   */
  await widgets.setAmount(amount);

  await Promise.all([
    /**
     * 결제창을 렌더링합니다.
     * @docs https://docs.tosspayments.com/sdk/v2/js#widgetsrenderpaymentmethods
     */
    widgets.renderPaymentMethods({
      selector: "#payment-method",
      // 렌더링하고 싶은 결제 UI의 variantKey
      // 결제 수단 및 스타일이 다른 멀티 UI를 직접 만들고 싶다면 계약이 필요해요.
      // @docs https://docs.tosspayments.com/guides/v2/payment-widget/admin#새로운-결제-ui-추가하기
      variantKey: "DEFAULT",
    }),
    /**
     * 약관을 렌더링합니다.
     * @docs https://docs.tosspayments.com/reference/widget-sdk#renderagreement선택자-옵션
     */
    widgets.renderAgreement({ selector: "#agreement", variantKey: "AGREEMENT" }),
  ]);

  const paymentRequestButton = document.getElementById('payment-request-button');

  paymentRequestButton.addEventListener('click', async () => {
    try {      
      /**
       * 결제 요청
       * 결제를 요청하기 전에 orderId, amount를 서버에 저장하세요.
       * 결제 과정에서 악의적으로 결제 금액이 바뀌는 것을 확인하는 용도입니다.
       * @docs https://docs.tosspayments.com/sdk/v2/js#widgetsrequestpayment
       */
      await widgets.requestPayment(PaymentContent);

    } catch (err) {
      console.log(err)
    }
  });
}




</script>
<style scoped>
div {
  font-family: 'Noto Sans KR', sans-serif;
  box-sizing: border-box;
}

input[type="text"],
input[type="email"] {
  display: block;
  width: 100%;
  max-width: 540px;
  margin: 10px auto;
  padding: 12px 16px;
  border: 1px solid #ccc;
  border-radius: 6px;
  font-size: 16px;
}

button {
  display: block;
  width: 100%;
  max-width: 540px;
  margin: 20px auto;
  padding: 14px 20px;
  background-color: #0066ff;
  color: white;
  font-size: 16px;
  font-weight: bold;
  border: none;
  border-radius: 6px;
  cursor: pointer;
  transition: background-color 0.3s ease;
}

button:hover {
  background-color: #0052cc;
}

hr {
  border: none;
  border-top: 1px solid #eee;
  margin: 20px 0;
}

.wrapper {
  margin-top: 40px;
  padding: 20px;
  background-color: #fafafa;
  border-radius: 8px;
}

.btn-wrapper {
  margin-top: 20px;
}
</style>