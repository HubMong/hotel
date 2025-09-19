<template>
<div>
<input type="text" class="" id="orderId" placeholder="주문ID 6자 이상"></input>
<input type="text" class="" id="orderName" placeholder="주문명 ex) **호텔 *인"></input>
<hr></hr>
<input type="text" class="" id="customerName "placeholder="주문자명 ex) 홍길동"></input>
<input type="text" class="" id="customerMobilePhone" placeholder="주문자전화번호 ex) 01012345678"></input>
<input type="email" class="" id="customerEmail" placeholder="주문자이메일 ex) hong@example.com"></input>
<hr></hr>
<input type="text" class="" id="paymentValue" placeholder="결제금액 숫자만 ex) 500"></input>
<hr></hr>
<button @click="applycheckout()">결제 내용 적용</button>
</div>


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

<script>
import { loadTossPayments, ANONYMOUS } from "@tosspayments/tosspayments-sdk";
import api from "@/api/api";

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

export default {
    data() {
    return {
      
        };
    },
    mounted() {
     
    },

    methods: {

      applycheckout() {
        PaymentContent.orderId = document.getElementById("orderId").value;
        PaymentContent.orderName = document.getElementById("orderName").value;
        PaymentContent.customerName = document.getElementById("customerEmail").value;
        PaymentContent.customerEmail = document.getElementById("customerEmail").value;
        PaymentContent.customerMobilePhone = document.getElementById("customerMobilePhone").value;
        amount.value = parseInt(document.getElementById("paymentValue").value);      
        main();
      }
    }
}

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
      await api.post("/payments/add", {        
        orderId: PaymentContent.orderId,
        amount: amount.value,
        paymentKey: '',
        status: 'PENDING'
      })
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