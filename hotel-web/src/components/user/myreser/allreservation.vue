<template>
  <div class="my-reservation-page">
    <Header :isLoggedIn="isLoggedIn" :user="user" @logout="handleLogout" />

    <main class="reservation-container">
      <h1 class="page-title">내 예약 내역</h1>

      <div v-if="isLoading" class="loading">예약 내역을 불러오는 중...</div>
      <div v-else-if="reservations.length === 0" class="no-data">예약 내역이 없습니다.</div>

      <div v-else class="reservation-list">
        <div
          v-for="reservation in reservations"
          :key="reservation.id"
          class="reservation-card"
        >
          <div class="reservation-header">
            <span class="hotel-name">{{ reservation.hotelName }}</span>
            <span class="dates">{{ formatDate(reservation.startDate) }} ~ {{ formatDate(reservation.endDate) }}</span>
          </div>

          <div class="reservation-body">
            <p><strong>객실 타입:</strong> {{ reservation.roomName }}</p>
            <p><strong>인원:</strong> 성인 {{ reservation.adults }}명 / 아동 {{ reservation.children }}명</p>
            <p><strong>객실 수:</strong> {{ reservation.numRooms }}개</p>
          </div>

          <div class="reservation-footer">
            <span :class="['status-badge', reservation.status]">{{ reservation.status }}</span>
            <button @click="goToReservationDetail(reservation.id)" class="btn-detail">
              상세보기
            </button>
          </div>
        </div>
      </div>
    </main>

    <Footer />
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from "vue";
import { useRouter } from "vue-router";
import Header from "@/components/user/main_page/Header.vue";
import Footer from "@/components/user/main_page/Footer.vue";
import ReservationApi from "@/api/ReservationApi";
import http from "@/api/http";

const router = useRouter();

// 로그인 상태 & 사용자
const isLoggedIn = ref(false);
const user = reactive({});

// 예약 목록
const reservations = ref([]);
const isLoading = ref(true);

const formatDate = (isoString) => {
  if (!isoString) return "";
  const d = new Date(isoString);
  return d.toLocaleDateString("ko-KR");
};

const fetchReservations = async () => {
  isLoading.value = true;
  try {
    const baseReservations = await ReservationApi.getByUserId(user.id);
    if (!baseReservations || baseReservations.length === 0) {
      reservations.value = [];
      return;
    }

    // ★★★ 변경: 호텔 이름과 객실 이름 정보를 백엔드에서 함께 가져오기
    const reservationsWithDetails = await Promise.all(
      baseReservations.map(async (r) => {
        let hotelName = `호텔 ID: ${r.hotelId}`;
        let roomName = `객실 ID: ${r.roomId}`;

        try {
          // 호텔 정보 조회
          const hotelRes = await http.get(`/hotels/${r.hotelId}`);
          if (hotelRes.data.hotel) {
            hotelName = hotelRes.data.hotel.name;
          }

          // 객실 정보 조회
          const roomRes = await http.get(`/rooms/${r.roomId}`);
          if (roomRes.data) {
            roomName = roomRes.data.name; // 백엔드 RoomController에서 name 필드를 반환한다고 가정
          }
        } catch (e) {
          console.error(`호텔 또는 객실 정보 조회 실패 (ID: ${r.hotelId}/${r.roomId})`, e);
        }

        return {
          ...r,
          hotelName,
          roomName,
        };
      })
    );

    reservations.value = reservationsWithDetails;
  } catch (e) {
    console.error("예약 내역 조회 실패", e);
    reservations.value = [];
  } finally {
    isLoading.value = false;
  }
};

const goToReservationDetail = (reservationId) => {
  router.push(`/reservations/${reservationId}`);
};

// 로그인 확인
const checkAuthStatus = () => {
  const token = localStorage.getItem("token");
  const userInfo = localStorage.getItem("user");
  if (token && userInfo) {
    isLoggedIn.value = true;
    Object.assign(user, JSON.parse(userInfo));
  } else {
    alert("로그인이 필요한 페이지입니다.");
    router.push("/login");
  }
};

// 로그아웃
const handleLogout = () => {
  localStorage.removeItem("token");
  localStorage.removeItem("user");
  isLoggedIn.value = false;
  Object.keys(user).forEach((k) => delete user[k]);
  alert("로그아웃 되었습니다.");
  router.push("/").then(() => window.location.reload());
};

onMounted(() => {
  checkAuthStatus();
  if (isLoggedIn.value) fetchReservations();
});
</script>

<style scoped src="@/assets/css/myreser/allreservation.css">
</style>