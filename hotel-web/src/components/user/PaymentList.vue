<template>
  <div class="payments">
    <h2>결제 목록</h2>
    <div v-if="payments.length === 0">결제 내역이 없습니다.</div>
    <ul>
      <li v-for="payment in payments" :key="payment.id" class="payment-item">
        ID: {{ payment.id }} || 금액: {{ payment.amount }} 원 || 상태: {{ payment.status }}
        <button class="button-view" @click="viewPayment(payment.id)">내역 확인</button>
        <button class="button-cancel" v-show="payment.status !== 'CANCELED'" @click="cancelPayment(payment.id)">결제 취소</button>
      </li>
    </ul>
  </div>
  
  <div class="payment-detail" v-if="paymentDetail">
    <h3>결제 상세 내역</h3>
    <p><strong>주문 ID:</strong> {{ paymentDetail.orderId }}</p>
    <p><strong>주문명:</strong> {{ paymentDetail.orderName }}</p>
    <p><strong>결제 금액:</strong> {{ paymentDetail.amount }} 원</p>
    <p><strong>상태:</strong> {{ paymentDetail.status }}</p>
    <p><strong>요청일:</strong> {{ formatDate(paymentDetail.requestedAt) }}</p>
    <p><strong>승인일:</strong> {{ formatDate(paymentDetail.approvedAt) }}</p>
    <p><strong>영수증:</strong> 
      <a :href="paymentDetail.receiptUrl" target="_blank" rel="noopener noreferrer">영수증 보기</a>
    </p>
  </div>
</template>

<script setup>
import { ref, onMounted } from "vue";
import api from "@/api/api";

const payments = ref([]);
const paymentDetail = ref(null);

const fetchPayments = async () => {
  const res = await api.get("/payments/lists");
  payments.value = res.data;
};

const cancelPayment = async (paymentId) => {
  try {
    const response = await api.get(`/payments/cancel/${paymentId}`);

    if (response.status === 200) {
      const responseBody = response.data;
      console.log(responseBody);

      if (responseBody.message === "결제 취소 완료") {
        alert('결제가 정상적으로 취소되었습니다.');
      } else {
        console.error('결제 취소 실패:', responseBody.message);
        alert('결제 취소에 실패했습니다. 사유: ' + responseBody.message);
      }
    } else {
      console.error('결제 취소 실패:', response.status);
      alert('결제 취소에 실패했습니다. 서버 응답 오류.');
    }
  } catch (error) {
    console.error('결제 취소 중 오류 발생:', error);
    alert('서버에 연결할 수 없습니다. 나중에 다시 시도해주세요.');
  } finally {    
    fetchPayments();
  }
}

const viewPayment = async (paymentId) => {
  try {
    const response = await api.get(`/payments/view/${paymentId}`);
    const data = response.data;
    paymentDetail.value = {
      orderId: data.orderId,
      orderName: data.orderName,
      amount: data.amount,  // JSON에서 amount 필드 이름 맞게 확인하세요
      status: data.status,
      requestedAt: data.requestedAt,
      approvedAt: data.approvedAt,
      receiptUrl: data.receipt?.url || null
    };
  } catch (error) {
    console.error('오류발생', error);
    alert('오류가 발생하였습니다');
  } finally {
    fetchPayments();
  }
}

const formatDate = (dateStr) => {
  if (!dateStr) return '-';
  const d = new Date(dateStr);
  return d.toLocaleString();
}

onMounted(fetchPayments);
</script>

<style scoped>
.payments {
  max-width: 600px;
  margin: 20px auto;
}
.payment-item {
  display: flex;
  justify-content: space-between;
  padding: 8px;
  border-bottom: 1px solid #ddd;
}
.button-view {
  background: green;
  color: white;
  border: none;
  border-radius: 4px;
  padding: 4px 8px;
  cursor: pointer;
}
.button-view:hover {
  background: darkgreen;
}
.button-cancel {
  background: red;
  color: white;
  border: none;
  border-radius: 4px;
  padding: 4px 8px;
  cursor: pointer;
}
.button-cancel:hover {
  background: darkred;
}
.payment-detail {
  max-width: 600px;
  margin: 30px auto;
  padding: 16px;
  border: 1px solid #ccc;
  border-radius: 8px;
  background: #f9f9f9;
}
.payment-detail h3 {
  margin-bottom: 12px;
}
.payment-detail p {
  margin: 6px 0;
}
.payment-detail a {
  color: #007bff;
  text-decoration: none;
}
.payment-detail a:hover {
  text-decoration: underline;
}
</style>
