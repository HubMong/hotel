<!-- src/components/user/my_page/MyReser.vue -->
<template>
  <div class="reservation-page-layout">
    <Header :isLoggedIn="isLoggedIn" :user="user" @logout="handleLogout" />
    <div class="reservation-page">
      <!-- 1) 로딩/에러 -->
      <div v-if="isLoading" class="status-message">예약 정보를 불러오는 중...</div>
      <div v-else-if="error" class="status-message error">{{ error }}</div>

      <!-- 2) 성공 -->
      <div v-else-if="reservation && hotel" class="reservation-container">
        <h2 class="page-title">예약 상세 정보</h2>

        <!-- 호텔 정보 -->
        <div class="hotel-info-section">
          <img :src="displayHotel.image" :alt="displayHotel.name" class="hotel-image" />
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

        <!-- 예약 정보 -->
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
              <span class="detail-value">
                {{ Number(displayReservation.totalPrice ?? 0).toLocaleString() }}원
              </span>
            </div>

            <!-- ✅ 상태/시간 -->
            <div class="detail-item">
              <span class="detail-label">현재 예약 상태</span>
              <span class="detail-value">{{ displayReservation.statusText }}</span>
            </div>
            <div class="detail-item">
              <span class="detail-label">체크-인 시간</span>
              <span class="detail-value">{{ displayTimes.checkInTime }}</span>
            </div>
            <div class="detail-item">
              <span class="detail-label">체크-아웃 시간</span>
              <span class="detail-value">{{ displayTimes.checkOutTime }}</span>
            </div>
            <div class="detail-item">
              <span class="detail-label">예약 생성 시각</span>
              <span class="detail-value">{{ displayReservation.createdAt }}</span>
            </div>
          </div>
        </div>

        <!-- 액션 -->
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
          <p>객실 내 흡연은 금지입니다.</p>
          <p>반려동물 동반 투숙은 불가합니다.</p>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, computed } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import Header from "@/components/user/main_page/Header.vue";
import ReservationApi from '@/api/ReservationApi';
import http from '@/api/http';

const isLoading = ref(true);
const error = ref(null);
const route = useRoute();
const router = useRouter();

const reservation = ref(null);
const hotel = ref(null);
const room = ref(null); // 체크인/아웃 시간용
const isPopupVisible = ref(false);

const isLoggedIn = ref(false);
const user = reactive({});

// 상태 매핑 (엔티티와 일치: PENDING/COMPLETED/CANCELLED)
const statusMap = {
  PENDING: "예약 대기중",
  COMPLETED: "이용 완료",
  CANCELLED: "예약 취소됨"
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
const formatDateTime = (isoString) => {
  if (!isoString) return '—';
  const d = new Date(isoString);
  return d.toLocaleString('ko-KR', {
    year: 'numeric', month: 'long', day: 'numeric',
    hour: '2-digit', minute: '2-digit'
  });
};
const formatTime = (t) => {
  if (!t) return '—';
  if (typeof t === 'string') return t.slice(0,5); // "15:00:00" -> "15:00"
  return '—';
};

const loadReservationDetails = async () => {
  isLoading.value = true;
  error.value = null;
  try {
    const reservationId = route.params.id;
    if (!reservationId) throw new Error("예약 ID가 없습니다.");

    // 1) 예약 상세
    const resData = await ReservationApi.get(reservationId);

    // 2) 호텔
    const hotelDataResponse = await http.get(`/hotels/${resData.hotelId}`);

    // 3) 룸(체크인/아웃 시간 우선 소스)
    let roomData = null;
    try {
      const roomRes = await http.get(`/rooms/${resData.roomId}`);
      roomData = roomRes.data?.room ?? roomRes.data ?? null;
    } catch {
      roomData = null; // 없으면 폴백
    }

    reservation.value = resData;
    hotel.value = hotelDataResponse.data.hotel;
    room.value = roomData;

  } catch (err) {
    console.error("예약 상세 정보를 불러오는 데 실패:", err);
    error.value = "예약 정보를 불러올 수 없습니다. 다시 시도해주세요.";
  } finally {
    isLoading.value = false;
  }
};

const cancelReservation = async () => {
  if (!reservation.value) return;
  const ok = confirm('정말로 이 예약을 취소하시겠습니까?');
  if (!ok) return;
  try {
    await ReservationApi.cancel(reservation.value.id);
    alert('예약이 성공적으로 취소되었습니다.');
    router.push('/mypage');
  } catch (err) {
    console.error("예약 취소 실패:", err);
    alert(`예약 취소에 실패했습니다: ${err.response?.data?.message || err.message}`);
  }
};

// 호텔 카드 표시용
const displayHotel = computed(() => {
  if (!hotel.value) return {};
  const formatRating = (ratingData) => {
    if (!ratingData || typeof ratingData.score !== 'number') {
      return { score: '평점 정보 없음', subs: '' };
    }
    const score = `⭐ ${ratingData.score.toFixed(1)}`;
    const subs = Object.entries(ratingData.subs || {})
      .map(([k, v]) => `${k} ${v}`)
      .join(' / ');
    return { score, subs: subs ? `(${subs})` : '' };
  };
  return {
    ...hotel.value,
    image: hotel.value.images?.[0] || 'https://placehold.co/400x300/e0e0e0/777?text=No+Image',
    rating: formatRating(hotel.value.rating),
  };
});

// 예약 상세 표시용
const displayReservation = computed(() => {
  if (!reservation.value) return {};
  return {
    ...reservation.value,
    id: `R-${String(reservation.value.id).padStart(6, '0')}`,
    checkInDate: formatDate(reservation.value.startDate),
    checkOutDate: formatDate(reservation.value.endDate),
    totalPrice: reservation.value.totalPrice ?? 0,        // 백엔드에 없으면 0 처리
    adults: reservation.value.adults ?? reservation.value.numAdult ?? 0,
    children: reservation.value.children ?? reservation.value.numKid ?? 0,
    statusText: statusMap[reservation.value.status] || reservation.value.status,
    createdAt: formatDateTime(reservation.value.createdAt) // 백엔드가 주면 표시, 없으면 '—'
  };
});

// 체크인/체크아웃 시간(Room → Hotel → 기본값)
const displayTimes = computed(() => {
  const ci = room.value?.checkInTime ?? hotel.value?.checkInTime ?? '15:00';
  const co = room.value?.checkOutTime ?? hotel.value?.checkOutTime ?? '11:00';
  return {
    checkInTime: formatTime(ci),
    checkOutTime: formatTime(co)
  };
});

// 취소 가능 조건: PENDING일 때만 허용(네 백엔드 상태머신과 일치)
const canCancel = computed(() => reservation.value?.status === 'PENDING');

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
