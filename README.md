Payment 기능 추가

-- backend

/dto/Payment.java
Payment
    id : 고유 순번
    orderId : 주문번호
    paymentKey : 결제 승인을 위한 고유 키 (TossAPI)
    amount : 결제 금액
    status : 결제 상태 (PENDING/COMPLETED/CANCELED)

/dto/TossPaymentResponse.java
TossAPI 요청으로 수신되는 객체 Parsing용 (JPA Repository 없어서 DB생성하지 않음)



/controller/PaymentController.java
URL 요청 받은 기능을 처리
POST /payments/add : 결제 요청 추가
POST /payments/confirm : 결제 승인 요청 (변조 확인 후 TossAPI에 요청)
GET /payments/lists : Payment DB Table 목록 불러오기
GET /payments/view/{ID} : id에 해당하는 결제 정보 불러오기 (TossAPI에서 전송한 객체의 Parsing된 내용 전달받음)
GET /payments/cancel/{ID} : id에 해당하는 결제 정보 취소하기 (GET → 정보 조회 → POST TossAPI 취소 요청)


/repository/PaymentRepository.java
Payment.java JPA 처리용


/WebConfig.java
CORS 처리용 (임시)


-- front

/api/api.js
axios 설정관련 (추후 JWT Token 적용으로 필요, 현재는 주석처리)

/components/user
Checkout.vue : 거래정보 입력 후 결제 요청
PaymentSuccess.vue : 사용자 결제 성공 및 서버 승인 요청
PaymentFailure.vue : 결제 실패관련 페이지
PaymentList.vue : Payment DB Table에 등록된 목록 / 취소 / 상세내역 확인


그 외
index.js 사용자 영역에 Route 정보 추가
App.vue에 기능 확인용 버튼 추가

