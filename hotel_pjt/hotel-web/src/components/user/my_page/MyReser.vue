<template>
  <div class="reservation-page-layout">
    <Header :isLoggedIn="isLoggedIn" :user="user" @logout="handleLogout" />
    <div class="reservation-page">
      <!-- 1. 로딩 및 오류 상태 표시 -->
      <div v-if="isLoading" class="status-message">예약 정보를 불러오는 중...</div>
      <div v-else-if="error" class="status-message error">{{ error }}</div>
      
      <!-- 2. 데이터 로딩 성공 시 표시 -->
      <div v-else-if="reservation && hotel" class="reservation-container">
        <h2 class="page-title">예약 상세 정보</h2>
        <div class="hotel-info-section">
          <img :src="displayHotel.image" :alt="displayHotel.name" class="hotel-image">
          <div class="hotel-details">
            <h3>{{ displayHotel.name }}</h3>
            <p class="hotel-description">{{ displayHotel.description }}</p>
            <div class="hotel-meta">
              <div class="hotel-rating">
                <span class="rating-score">{{ displayHotel.rating.score }}</span>
                <span class="rating-subs">{{ displayHotel.rating.subs }}</span>
              </div>
            </div>
          </div>
        </div>

        <div class="reservation-details-section">
          <h3 class="section-title">내 예약 정보</h3>
          <div class="detail-group">
            <div class="detail-item">
              <span class="detail-label">예약 번호</span>
              <span class="detail-value">{{ displayReservation.id }}</span>
            </div>
            <div class="detail-item">
              <span class="detail-label">체크-인</span>
              <span class="detail-value">{{ displayReservation.checkInDate }}</span>
            </div>
            <div class="detail-item">
              <span class="detail-label">체크-아웃</span>
              <span class="detail-value">{{ displayReservation.checkOutDate }}</span>
            </div>
            <div class="detail-item">
              <span class="detail-label">투숙 인원</span>
              <span class="detail-value">성인 {{ displayReservation.adults }}명, 아동 {{ displayReservation.children }}명</span>
            </div>
            <div class="detail-item total-price">
              <span class="detail-label">총 결제 금액</span>
              <span class="detail-value">{{ displayReservation.totalPrice.toLocaleString() }}원</span>
            </div>

            <!-- ✅ 예약 상태 표시 추가 -->
            <div class="detail-item">
              <span class="detail-label">현재 예약 상태</span>
              <span class="detail-value">{{ displayReservation.statusText }}</span>
            </div>
          </div>
        </div>
        
        <div class="actions-section">
          <button class="btn btn--small btn--ghost" @click="openPopup">약관(요금, 투숙)</button>
          <div>
            <button 
              v-if="canCancel" 
              class="btn btn--ghost btn--danger" 
              @click="cancelReservation"
            >
              예약 취소
            </button>
            <span v-else class="status-info">취소 불가능한 상태입니다.</span>
          </div>
        </div>
      </div>
    </div>

    <!-- 약관 팝업 -->
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
        </div>
      </div>
    </div>
    <!-- <Footer /> -->
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, computed } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import Header from "@/components/user/main_page/Header.vue";
// import Footer from "@/components/user/main_page/Footer.vue";
import ReservationApi from '@/api/ReservationApi';
import http from '@/api/http';

const isLoading = ref(true);
const error = ref(null);
const route = useRoute();
const router = useRouter();

const reservation = ref(null);
const hotel = ref(null);
const isPopupVisible = ref(false);

const isLoggedIn = ref(false);
const user = reactive({});

// ✅ 상태 매핑 (사용자 친화적 텍스트)
const statusMap = {
  CONFIRMED: "예약 확정됨",
  COMPLETED: "이용 완료",
  CANCELLED: "예약 취소됨",
  HOLD: "예약 대기중"
};


const checkAuthStatus = () => {
  const token = localStorage.getItem('token');
  const userInfo = localStorage.getItem('user');
  if (token && userInfo) {
    isLoggedIn.value = true;
    Object.assign(user, JSON.parse(userInfo));
  }
};

const handleLogout = () => {
  localStorage.removeItem('token');
  localStorage.removeItem('user');
  isLoggedIn.value = false;
  Object.keys(user).forEach(k => delete user[k]);
  alert("로그아웃 되었습니다.");
  router.push('/').then(() => window.location.reload());
};

const formatDate = (isoString) => {
  if (!isoString) return '';
  return new Date(isoString).toLocaleDateString('ko-KR', { year: 'numeric', month: 'long', day: 'numeric' });
};

const loadReservationDetails = async () => {
  isLoading.value = true;
  error.value = null;
  try {
    const reservationId = route.params.id;
    if (!reservationId) throw new Error("예약 ID가 없습니다.");
    
    const resData = await ReservationApi.get(reservationId);
    const hotelDataResponse = await http.get(`/hotels/${resData.hotelId}`);
    
    reservation.value = resData;
    hotel.value = hotelDataResponse.data.hotel;

  } catch (err) {
    console.error("예약 상세 정보를 불러오는 데 실패했습니다:", err);
    error.value = "예약 정보를 불러올 수 없습니다. 다시 시도해주세요.";
  } finally {
    isLoading.value = false;
  }
};

const cancelReservation = async () => {
  if (!reservation.value) return;
  const isConfirmed = confirm('정말로 이 예약을 취소하시겠습니까?');
  if (isConfirmed) {
      try {
          await ReservationApi.cancel(reservation.value.id);
          alert('예약이 성공적으로 취소되었습니다.');
          router.push('/mypage'); // 마이페이지로 이동
      } catch(err) {
          console.error("예약 취소 실패:", err);
          alert(`예약 취소에 실패했습니다: ${err.response?.data?.message || err.message}`);
      }
  }
};

const displayHotel = computed(() => {
  if (!hotel.value) return {};

  const formatRating = (ratingData) => {
      if (!ratingData || typeof ratingData.score !== 'number') {
          return { score: '평점 정보 없음', subs: '' };
      }
      const score = `⭐ ${ratingData.score.toFixed(1)}`;
      const subs = Object.entries(ratingData.subs || {})
          .map(([key, value]) => `${key} ${value}`)
          .join(' / ');
      
      return { score, subs: subs ? `(${subs})` : '' };
  };

  return {
      ...hotel.value,
      image: hotel.value.images?.[0] || 'https://placehold.co/400x300/e0e0e0/777?text=No+Image',
      rating: formatRating(hotel.value.rating),
  }
});

// ✅ statusText 추가
const displayReservation = computed(() => {
  if (!reservation.value) return {};
  return {
    ...reservation.value,
    id: `R-${String(reservation.value.id).padStart(6, '0')}`,
    checkInDate: formatDate(reservation.value.startDate),
    checkOutDate: formatDate(reservation.value.endDate),
    totalPrice: reservation.value.totalPrice || 550000,
    statusText: statusMap[reservation.value.status] || reservation.value.status
  }
});

// ✅ "CONFIRMED"일 때만 취소 가능
const canCancel = computed(() => {
  return reservation.value?.status === "CONFIRMED";
});

onMounted(() => {
  checkAuthStatus();
  loadReservationDetails();
});

const openPopup = () => { isPopupVisible.value = true; };
const closePopup = () => { isPopupVisible.value = false; };
</script>

<style scoped>
@import '@/assets/css/mypage/myreser.css';
</style>
