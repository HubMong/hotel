<template>
  <div class="reservation-result-layout">
    <Header :isLoggedIn="isLoggedIn" :user="user" @logout="handleLogout" />
    <main class="result-container">
      <h1 class="page-title">예약이 완료되었습니다 🎉</h1>
      <div v-if="isLoading" class="status-message">예약 정보를 불러오는 중...</div>
      <div v-else-if="error" class="status-message error">{{ error }}</div>
      <div v-else-if="reservation && hotel" class="reservation-content">
        <div class="hotel-info-section">
          <img :src="displayHotel.image" :alt="displayHotel.name" class="hotel-image" />
          <div class="hotel-details">
            <h3>{{ displayHotel.name }}</h3>
            <p class="hotel-description">{{ displayHotel.description }}</p>
          </div>
        </div>
        <div class="reservation-details-section">
          <h3 class="section-title">내 예약 정보</h3>
          <div class="detail-group">
            <div class="detail-item">
              <span class="detail-label">예약 시간</span>
              <span class="detail-value">{{ displayReservation.bookingTime }}</span>
            </div>
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
              <span class="detail-label">예약 인원</span>
              <span class="detail-value">
                성인 {{ displayReservation.adults ?? 0 }}명
                <template v-if="displayReservation.children > 0">
                  , 아동 {{ displayReservation.children }}명
                </template>
              </span>
            </div>
            <div class="detail-item">
              <span class="detail-label">객실 수</span>
              <span class="detail-value">{{ displayReservation.numRooms ?? 0 }}개</span>
            </div>
            <div class="detail-item">
              <span class="detail-label">총 결제 금액</span>
              <span class="detail-value total-price">
                {{ Number(displayReservation.totalPrice ?? 0).toLocaleString() }}원
              </span>
            </div>
            <div class="detail-item">
              <span class="detail-label">현재 예약 상태</span>
              <span class="detail-value status-badge" :class="reservation.status">
                {{ displayReservation.statusText }}
              </span>
            </div>
          </div>
        </div>
      </div>
      <div class="bottom-button-group">
        <button @click="goHome" class="btn">Home</button>
        <router-link class="btn" to="/search">다른 숙소 보기</router-link>
      </div>
    </main>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, computed } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import Header from "@/components/user/main_page/Header.vue";
// import Footer from "@/components/user/main_page/Footer.vue";
import ReservationApi from '@/api/ReservationApi';
import http from '@/api/http';

const route = useRoute();
const router = useRouter();

// --- 상태 변수 ---
const isLoading = ref(true);
const error = ref(null);
const reservation = ref(null);
const hotel = ref(null);
const room = ref(null);

// --- 로그인/로그아웃 로직 (손상 없이 유지) ---
const isLoggedIn = ref(false);
const user = reactive({});

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
  Object.keys(user).forEach(key => delete user[key]);
  alert("로그아웃 되었습니다.");
  router.push('/').then(() => window.location.reload());
};

const goHome = () => {
  router.push('/'); // Home 버튼 클릭 시 메인 페이지로 이동
};

// --- 데이터 로딩 (MyReser의 상세 로직 가져오기) ---
const loadReservationDetails = async () => {
  isLoading.value = true;
  error.value = null;
  try {
    const reservationId = route.params.id;
    if (!reservationId) throw new Error("예약 ID가 없습니다.");

    // 1) 예약 상세
    const resData = await ReservationApi.get(reservationId);
    // 2) 호텔 상세
    const hotelDataResponse = await http.get(`/hotels/${resData.hotelId}`);
    // 3) 룸 상세 (선택적)
    let roomData = null;
    try {
      const roomRes = await http.get(`/rooms/${resData.roomId}`);
      roomData = roomRes.data?.room ?? roomRes.data ?? null;
    } catch { roomData = null; }

    reservation.value = resData;
    hotel.value = hotelDataResponse.data.hotel;
    room.value = roomData;

  } catch (err) {
    console.error("예약 정보를 불러오는 데 실패:", err);
    error.value = "예약 정보를 불러올 수 없습니다.";
  } finally {
    isLoading.value = false;
  }
};

onMounted(() => {
  checkAuthStatus();
  loadReservationDetails();
});


// --- 데이터 포맷팅 (MyReser의 computed 로직 가져오기) ---
const statusMap = {
  CONFIRMED: "예약 확정",
  COMPLETED: "이용 완료",
  CANCELLED: "취소됨",
  PENDING: "결제 대기중",
  HOLD: "임시 예약"
};

const formatDate = (isoString) => {
  if (!isoString) return '';
  return new Date(isoString).toLocaleDateString('ko-KR', { year: 'numeric', month: 'long', day: 'numeric' });
};

const displayHotel = computed(() => {
  if (!hotel.value) return {};
  return {
    ...hotel.value,
    image: hotel.value.images?.[0] || 'https://placehold.co/400x300/e0e0e0/777?text=No+Image',
  };
});

// ★★★ 추가된 함수: 예약 시간(년월일시분초) 포맷팅
const formatDateTime = (isoString) => {
  if (!isoString) return '';
  const date = new Date(isoString);
  const options = {
    year: 'numeric',
    month: 'long',
    day: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
    second: '2-digit',
    hour12: false
  };
  return date.toLocaleDateString('ko-KR', options) + ' ' + date.toLocaleTimeString('ko-KR', options);
};

const displayReservation = computed(() => {
  if (!reservation.value) return {};
  return {
    ...reservation.value,
    id: `R-${String(reservation.value.id).padStart(6, '0')}`,
    checkInDate: formatDate(reservation.value.startDate),
    checkOutDate: formatDate(reservation.value.endDate),
    totalPrice: reservation.value.totalPrice ?? 0,
    statusText: statusMap[reservation.value.status] || reservation.value.status,
    
    //추가된 부분
    adults: reservation.value.adults,
    children: reservation.value.children,
    numRooms: reservation.value.numRooms,
    bookingTime: formatDateTime(reservation.value.createdAt),
  };
});
</script>

<style scoped>
@import "@/assets/css/hotel_checkout/ReservationResult.css";
</style>