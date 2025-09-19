<template>
  <div class="reservation-page">
    <div class="reservation-container">
      <h2 class="page-title">예약 상세 정보</h2>
      <div class="hotel-info-section">
        <img :src="hotel.image" :alt="hotel.name" class="hotel-image">
        <div class="hotel-details">
          <h3>{{ hotel.name }}</h3>
          <p class="hotel-description">{{ hotel.description }}</p>
          <div class="hotel-meta">
            <span>{{ hotel.rating }}</span> | <span>총 {{ hotel.capacity }}명 수용</span>
          </div>
        </div>
      </div>

      <div class="reservation-details-section">
        <h3 class="section-title">내 예약 정보</h3>
        <div class="detail-group">
          <div class="detail-item">
            <span class="detail-label">확인 번호</span>
            <span class="detail-value">{{ reservation.room_id }}</span>
          </div>
          <div class="detail-item">
            <span class="detail-label">체크-인</span>
            <span class="detail-value">{{ reservation.checkInDate }}</span>
          </div>
          <div class="detail-item">
            <span class="detail-label">체크-아웃</span>
            <span class="detail-value">{{ reservation.checkOutDate }}</span>
          </div>
          <div class="detail-item">
            <span class="detail-label">투숙 인원</span>
            <span class="detail-value">성인 {{ reservation.adults }}명, 어린이 {{ reservation.children }}명</span>
          </div>
          <div class="detail-item total-price">
            <span class="detail-label">총 결제 금액</span>
            <span class="detail-value">{{ reservation.totalPrice.toLocaleString() }}원</span>
          </div>
        </div>
      </div>
      
      <div class="actions-section">
        <button class="btn btn--small btn--ghost" @click="openPopup">약관(요금, 투숙)</button>
        <div>
          <button class="btn btn--ghost">예약 취소</button>
        </div>
      </div>
    </div>

    <div v-if="isPopupVisible" class="popup-overlay" @click.self="closePopup">
      <div class="popup-content">
        <div class="popup-header">
          <h4>약관 및 규정</h4>
          <button @click="closePopup" class="close-btn">&times;</button>
        </div>
        <div class="popup-body">
          <h5>요금 규정</h5>
          <p>예약 요금은 VAT 및 서비스 요금이 포함된 금액입니다. 추가 요금이 발생할 수 있으며, 이는 현장에서 별도로 지불해야 합니다.</p>
          <p>예약 취소 및 환불 규정은 체크인 날짜를 기준으로 합니다.</p>
          <ul>
            <li>체크인 7일 전: 100% 환불</li>
            <li>체크인 3일 전: 50% 환불</li>
            <li>체크인 당일: 환불 불가</li>
          </ul>
          
          <h5>투숙 규정</h5>
          <p>체크인 시간: 오후 3시</p>
          <p>체크아웃 시간: 오전 11시</p>
          <p>객실 내 흡연은 절대 금지됩니다.</p>
          <p>반려동물 동반 투숙은 불가합니다.</p>
          
          <h5>편의 시설 규정</h5>
          </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue';

//이미지

// 팝업 상태 관리 변수
const isPopupVisible = ref(false);

// 하드 코딩된 단일 호텔 정보
const hotel = ref({
  id: 1, 
  name: '서울 신라 호텔', 
  rating: '5성급 ★★★★★', 
  image: 'https://picsum.photos/id/10/400/300', 
  description: '서울 강남에 위치한 최고급 호텔로, 훌륭한 서비스와 편안한 객실을 자랑합니다.',
  capacity: 2,
});

// 하드 코딩된 예약 정보
const reservation = ref({
  room_id: 'D2025SHIN',
  checkInDate: '2025-10-10',
  checkOutDate: '2025-10-12',
  adults: 2,
  children: 2,
  totalPrice: 900000,
});

// 팝업 열기/닫기 함수
const openPopup = () => {
  isPopupVisible.value = true;
};
const closePopup = () => {
  isPopupVisible.value = false;
};
</script>

<style scoped>
:root {
  --brand: #4a90e2; /* 부드러운 파란색 */
  --brand-primary: #4a90e2;
  --ink: #333333; /* 기존보다 부드러운 텍스트 색상 */
  --line: #e0e0e0;
  --bg: #faf7f5; /* 따뜻한 베이지 배경 */
  --bg-white: #ffffff;
}

.reservation-page {
  display: flex;
  justify-content: center;
  padding: 40px 120px;
  background-color: var(--bg); /* 변경된 배경 변수 적용 */
}

.reservation-container {
  width: 100%;
  max-width: 800px;
  background: var(--bg-white);
  border-radius: 16px; /* 더 둥글게 */
  box-shadow: 0 6px 20px rgba(0, 0, 0, 0.08); /* 더 부드러운 그림자 */
  padding: 24px;
}

.page-title {
  font-size: 28px;
  font-weight: 700;
  color: var(--ink);
  margin-bottom: 32px;
  text-align: center;
}

.hotel-info-section {
  display: flex;
  gap: 24px;
  align-items: center;
  margin-bottom: 40px;
}

.hotel-image {
  width: 250px;
  height: 200px;
  object-fit: cover;
  border-radius: 12px; /* 더 둥글게 */
}

.hotel-details h3 {
  font-size: 24px;
  font-weight: 600;
  margin: 0 0 8px;
}

.hotel-description {
  color: #555;
  font-size: 14px;
  line-height: 1.5;
}

.hotel-meta {
  font-size: 14px;
  color: #777;
  margin-top: 8px;
}

.reservation-details-section {
  margin-bottom: 40px;
}

.section-title {
  font-size: 20px;
  font-weight: 600;
  color: var(--brand);
  border-bottom: 2px solid var(--line);
  padding-bottom: 8px;
  margin-bottom: 20px;
}

.detail-group {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.detail-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 0;
  border-bottom: 1px solid #eee;
}

.detail-item:last-child {
  border-bottom: none;
}

.detail-label {
  font-weight: 500;
  color: #333;
}

.detail-value {
  font-weight: 600;
  color: var(--ink); /* 변수 적용 */
}

.total-price .detail-label,
.total-price .detail-value {
  font-size: 18px;
  font-weight: 700;
  color: var(--brand);
}

.actions-section {
  display: flex;
  justify-content: space-between;
  gap: 16px;
}

.btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  height: 40px;
  padding: 0 20px;
  border-radius: 20px;
  font-weight: 600;
  font-size: 14px;
  border: 1px solid transparent;
  text-decoration: none;
  transition: .15s ease;
}

.btn--small {
  height: 32px;
  padding: 0 16px;
  font-size: 12px;
}

.btn--ghost {
  background: var(--bg); /* 배경색 변경 */
  color: #555; /* 텍스트 색상 변경 */
  border-color: var(--line);
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.05);
}
.btn--ghost:hover {
    background: #e9e9e9;
}

.btn--primary {
  background: var(--brand);
  color: #fff;
  border-color: var(--brand);
}

/* 팝업 스타일 */
.popup-overlay {
  position: fixed;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background-color: rgba(0, 0, 0, 0.5);
  display: flex;
  justify-content: center;
  align-items: center;
  z-index: 1000;
}
.popup-content {
  background-color: #fff;
  border-radius: 12px;
  width: 90%;
  max-width: 500px;
  padding: 24px;
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.2);
}
.popup-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  border-bottom: 1px solid #eee;
  padding-bottom: 12px;
  margin-bottom: 16px;
}
.popup-header h4 {
  font-size: 18px;
  margin: 0;
}
.popup-body {
  font-size: 14px;
  line-height: 1.6;
}
.popup-body h5 {
  margin-top: 1.5rem;
  margin-bottom: 0.5rem;
  font-size: 16px;
  font-weight: 600;
}
.popup-body ul {
  padding-left: 20px;
  margin: 0.5rem 0 1rem;
}
.close-btn {
  background: none;
  border: none;
  font-size: 24px;
  cursor: pointer;
  color: #999;
}

/* 반응형 모바일 최적화 */
@media (max-width: 768px) {
  .reservation-page {
    padding: 20px 16px;
  }
  .reservation-container {
    padding: 24px;
  }
  .hotel-info-section {
    flex-direction: column;
    gap: 16px;
  }
  .hotel-image {
    width: 100%;
    height: auto;
  }
  .hotel-details {
    text-align: center;
  }
  .actions-section {
    flex-direction: column;
    gap: 12px;
  }
  .actions-section > div {
    display: flex;
    flex-direction: column;
    gap: 12px;
  }
  .btn, .btn--small {
    width: 100%;
    justify-content: center;
  }
}
</style>