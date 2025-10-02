<template>
  <div class="mypage-layout">
    <Header :isLoggedIn="isLoggedIn" :user="user" @logout="handleLogout" />
    <SearchForm v-if="SearchForm" />

    <div class="allcard">
      <div class="intro">
        <h2>내 정보</h2>
      </div>

      <div class="image">
        <img 
          :src="profileImage || 'https://cdn-icons-png.flaticon.com/512/3135/3135715.png'"
          alt="Profile Image" 
          @click="onImageClick"
          style="cursor: pointer; border-radius: 50%; width: 150px; height: 150px; object-fit: cover;"
        />
        <input type="file" ref="fileInput" accept="image/*" @change="onFileChange" style="display: none;" />
      </div>

      <div class="menu-tabs">
        <div 
          class="tab" 
          @click="router.push({ name: 'MyAccount' })"
        >
          계정
        </div>
        <div class="tab active">
          예약 내역
        </div>
      </div>
      
      <div class="my-page2">
        <div v-if="isLoading.history" class="loading">예약 내역을 불러오는 중...</div>
        <div v-else-if="reservations.length === 0" class="no-data">예약 내역이 없습니다.</div>

        <div v-else class="reservation-container">
          <div 
            v-for="reservation in reservations" 
            :key="reservation.id" 
            class="reservation-card"
            :class="{ active: reservation.active }"
            @click="toggleReservation(reservation)"
          >
            <div class="summary">
              <div class="hotel-info">
                <span class="hotel-name">{{ reservation.hotelName }}</span>
                <span class="dates">{{ formatDate(reservation.startDate) }} ~ {{ formatDate(reservation.endDate) }}</span>
              </div>
              
              <div class="summary-right">
                <button @click.stop="goToReservationDetail(reservation.id)" class="btn-detail">상세보기</button>
                <span :class="['status-badge', reservation.status]" style="font-size: 0.8rem;">
                  {{ reservation.statusText }}
                </span>
                <span class="arrow-icon">▼</span>
              </div>
            </div>
            
            <div class="details">
              <div class="detail-grid">
                <div class="detail-item"><span class="label">객실 타입</span><span class="value">{{ reservation.roomType }}</span></div>
                <div class="detail-item"><span class="label">인원</span><span class="value">성인 {{ reservation.adults }}명 / 아동 {{ reservation.children }}명</span></div>
                <div class="detail-item"><span class="label">객실 수</span><span class="value">{{ reservation.numRooms }}개</span></div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <Footer />
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue';
import { useRouter } from 'vue-router';
// 컴포넌트
import Header from "@/components/user/main_page/Header.vue";
import Footer from "@/components/user/main_page/Footer.vue";
// API 모듈
import http from '@/api/http';
import { getMy } from '@/api/ReservationApi';
import UserApi from '@/api/UserApi';

// 1. 상태 정의
const user = reactive({});
const reservations = ref([]);
const isLoading = reactive({ user: true, history: true }); 
const profileImage = ref('');
const isLoggedIn = ref(false);
const router = useRouter();
const fileInput = ref(null);

// 2. 🔥 로직 복구
const statusMap = { 'COMPLETED': "예약 완료", 'CANCELLED': "취소됨", 'CONFIRMED': '예약 확정', 'PENDING': '예약 대기' };

const formatDate = (isoString) => {
    if (!isoString) return ''; 
    return new Date(isoString).toLocaleDateString('ko-KR'); 
};

const toggleReservation = (reservation) => { 
    reservation.active = !reservation.active; 
};

const goToReservationDetail = (reservationId) => { 
    router.push(`/reservations/${reservationId}`); 
};

const checkAuthStatus = () => {
    const token = localStorage.getItem('token') || sessionStorage.getItem('token');
    const userInfo = localStorage.getItem('user');
    if (token && userInfo) {
      isLoggedIn.value = true;
      Object.assign(user, JSON.parse(userInfo));
      user.passwordLength = user.provider === 'LOCAL' ? 8 : 0; 
    } else {
      router.push('/login');
    }
};

const handleLogout = () => {
    ['token','access_token'].forEach(k => { localStorage.removeItem(k); sessionStorage.removeItem(k); });
    localStorage.removeItem('user');
    isLoggedIn.value = false;
    Object.keys(user).forEach(k => delete user[k]);
    alert("로그아웃 되었습니다.");
    router.push('/').then(() => window.location.reload());
};

const fetchUserProfile = async () => {
    isLoading.user = true;
    try {
        const data = await UserApi.getInfo();
        Object.assign(user, data);
        profileImage.value = data.profileImageUrl || '';
        localStorage.setItem('user', JSON.stringify(data));
        user.passwordLength = user.provider === 'LOCAL' ? 8 : 0;
    } catch {
        // alert("사용자 정보를 불러올 수 없습니다.");
    } finally {
        isLoading.user = false;
    }
};

const fetchReservations = async () => {
    isLoading.history = true;
    try {
        const baseReservations = await getMy(0, 20); // API 호출

        const promises = baseReservations.map(r => 
            http.get(`/hotels/${r.hotelId}`)
                .then(res => res.data.hotel.name)
                .catch(() => `호텔 ID: ${r.hotelId}`)
        );

        const hotelNames = await Promise.all(promises);

        reservations.value = baseReservations.map((r, index) => ({ 
            ...r, 
            hotelName: hotelNames[index],
            roomType: r.roomType || '스탠다드', // 더미값 사용
            statusText: statusMap[r.status] || r.status,
            active: false
        }));
    } catch (e) {
        console.error("예약 내역 조회 실패", e?.response?.status, e?.response?.data || e);
        reservations.value = [];
    } finally {
        isLoading.history = false;
    }
};

const onImageClick = () => { fileInput.value.click(); };
const onFileChange = async (event) => { /* ... (로직 생략) ... */ };


// 3. 라이프사이클 훅
onMounted(() => {
    checkAuthStatus();
    if (isLoggedIn.value) {
        // 사용자 정보 로딩 후 예약 내역 로드
        fetchUserProfile().then(() => fetchReservations());
    } else {
        // 비로그인 상태일 경우 로딩 상태를 즉시 종료
        isLoading.user = false;
        isLoading.history = false;
    }
});
</script>

<style scoped>
/* CSS 파일명이 myhistory.css로 되어 있으므로, 파일 경로를 정확히 확인해야 합니다. */
@import "@/assets/css/mypage/myhistory.css"; 
</style>