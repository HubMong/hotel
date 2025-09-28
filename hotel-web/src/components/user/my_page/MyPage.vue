<template>
  <div class="mypage-layout">
    <Header :isLoggedIn="isLoggedIn" :user="user" @logout="handleLogout" />
    <SearchForm />

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
        <div class="tab" :class="{ active: selectedTab === 'account' }" @click="selectedTab = 'account'">
          계정
        </div>
        <div class="tab" :class="{ active: selectedTab === 'history' }" @click="selectedTab = 'history'">
          예약 내역
        </div>
      </div>

      <div class="my-page1" v-if="selectedTab === 'account'">
        <div v-if="isLoading.user" class="loading">정보를 불러오는 중...</div>
        <div v-else class="user-info">
          <div class="info-item"><span class="label">이름</span><span class="value">{{ user.name }}</span></div>
          <div class="info-item">
            <span class="label">이메일</span>
            <template v-if="!editStates.email">
              <span class="value">{{ user.email }}</span>
              <button @click="toggleEdit('email')" class="btn-change">수정하기</button>
            </template>
            <template v-else>
              <input type="email" v-model="editableUser.email" class="input-edit" />
              <button @click="saveChanges('email')" class="btn-save">저장</button>
              <button @click="cancelEdit('email')" class="btn-cancel">취소</button>
            </template>
          </div>
          <div class="info-item">
            <span class="label">비밀번호</span>
            <template v-if="!editStates.password">
              <span class="value">{{ '*'.repeat(user.passwordLength || 4) }}</span>
              <button @click="toggleEdit('password')" class="btn-change">변경하기</button>
            </template>
            <template v-else>
              <div class="password-edit-form">
                <input type="password" v-model="editableUser.currentPassword" placeholder="현재 비밀번호" class="input-edit" />
                <input type="password" v-model="editableUser.newPassword" placeholder="새 비밀번호" class="input-edit" />
                <input type="password" v-model="editableUser.confirmPassword" placeholder="새 비밀번호 확인" class="input-edit" />
                <div class="password-buttons">
                  <button @click="savePasswordChanges" class="btn-save">저장</button>
                  <button @click="cancelEdit('password')" class="btn-cancel">취소</button>
                </div>
              </div>
            </template>
          </div>
        </div>
      </div>

      <div class="my-page2" v-if="selectedTab === 'history'">
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
                
                <button 
                  v-if="reservation.status === 'CONFIRMED' || reservation.status === 'HOLD'"
                  @click.stop="cancelReservation(reservation.id)" 
                  class="btn-cancel"
                  style="margin-left: 5px;">
                  예약 취소
                </button>

                <span :class="['status-badge', reservation.status]" style="font-size: 0.8rem;">
                  {{ reservation.statusText }}
                </span>
                <span class="arrow-icon">▼</span>
              </div>
            </div>
            
            <div class="details">
              <div class="detail-grid">
                <div class="detail-item"><span class="label">객실 타입</span><span class="value">{{ reservation.roomName }}</span></div>
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
import http from '@/api/http';
import Header from "@/components/user/main_page/Header.vue";
import Footer from "@/components/user/main_page/Footer.vue";
import { getMy } from '@/api/ReservationApi';
import UserApi from '@/api/UserApi';
// import SearchForm from "@/components/user/main_page/SearchForm.vue";


const user = reactive({});
const editableUser = reactive({});
const reservations = ref([]);
const isLoading = reactive({ user: true, history: true });
const selectedTab = ref('account');
const profileImage = ref('');
const fileInput = ref(null);
const isLoggedIn = ref(false);
const editStates = reactive({ email: false, password: false });
const router = useRouter();

const statusMap = {
  CONFIRMED: "예약 확정",
  COMPLETED: "이용 완료",
  CANCELLED: "취소됨",
  HOLD: "대기중"
};

const formatDate = (isoString) => {
  if (!isoString) return '';
  const d = new Date(isoString);
  return d.toLocaleDateString('ko-KR');
};

const toggleReservation = (reservation) => {
  reservation.active = !reservation.active;
};

const fetchReservations = async () => {
  isLoading.history = true;
  try {
    const baseReservations = await getMy(0, 10);

    if (!baseReservations || baseReservations.length === 0) {
      reservations.value = [];
      return;
    }

    const promises = baseReservations.map(async r => {
      let hotelName = `호텔 ID: ${r.hotelId}`;
      let roomName = `객실 ID: ${r.roomId}`;

      try {
        const hotelRes = await http.get(`/hotels/${r.hotelId}`);
        if (hotelRes.data.hotel) {
          hotelName = hotelRes.data.hotel.name;
        }

        const roomRes = await http.get(`/rooms/${r.roomId}`);
        if (roomRes.data) {
          roomName = roomRes.data.name; 
        }
      } catch (e) {
        console.error(`호텔 또는 객실 정보 조회 실패 (ID: ${r.hotelId}/${r.roomId})`, e);
      }
      
      return {
        ...r,
        hotelName,
        roomName,
        active: false,
        statusText: statusMap[r.status] || r.status
      };
    });

    reservations.value = await Promise.all(promises);
  } catch (e) {
    console.error("예약 내역 조회 실패", e?.response?.status, e?.response?.data || e);
    reservations.value = [];
  } finally {
    isLoading.history = false;
  }
};

const goToReservationDetail = (reservationId) => {
  router.push(`/reservations/${reservationId}`);
};

/*
- - - - - - - - - - - - - - - - - - - - - - - - - - - -
[ ✨ 새롭게 추가된 함수 ]

[ 변경 이유 ]
템플릿에 추가된 '예약 취소' 버튼을 눌렀을 때 실제로 서버에 취소 요청을 보내고,
결과에 따라 화면을 갱신하는 로직이 필요하여 cancelReservation 함수를 새로 작성했습니다.
(이전 코드에는 이 함수가 존재하지 않았으므로 '수정 전 코드'는 없습니다.)
- - - - - - - - - - - - - - - - - - - - - - - - - - - -
*/
const cancelReservation = async (reservationId) => {
  if (!confirm("정말로 이 예약을 취소하시겠습니까?")) {
    return;
  }

  try {
    await http.delete(`/reservations/${reservationId}`);
    alert("예약이 성공적으로 취소되었습니다.");
    await fetchReservations();
  } catch (error) {
    console.error("예약 취소 실패:", error);
    alert("예약 취소에 실패했습니다. 다시 시도해 주세요.");
  }
};

const checkAuthStatus = () => {
  const token = localStorage.getItem('token') || localStorage.getItem('access_token')
                || sessionStorage.getItem('token') || sessionStorage.getItem('access_token');
  const userInfo = localStorage.getItem('user');
  if (token && userInfo) {
    isLoggedIn.value = true;
    Object.assign(user, JSON.parse(userInfo));
  } else {
    alert("로그인이 필요한 페이지입니다.");
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
  } catch {
    alert("사용자 정보를 불러올 수 없습니다.");
  } finally {
    isLoading.user = false;
  }
};

const onImageClick = () => fileInput.value.click();
const onFileChange = async (event) => {
  const file = event.target.files[0];
  if (!file) return;
  const reader = new FileReader();
  reader.onload = (e) => (profileImage.value = e.target.result);
  reader.readAsDataURL(file);

  const formData = new FormData();
  formData.append('profileImage', file);
  try {
    await http.post('/users/me/profile-image', formData, { headers: { 'Content-Type': 'multipart/form-data' } });
    alert("프로필 이미지가 변경되었습니다.");
    await fetchUserProfile();
  } catch {
    alert("이미지 업로드에 실패했습니다.");
  }
};

const toggleEdit = (field) => {
  if (field === 'password') {
    editableUser.currentPassword = '';
    editableUser.newPassword = '';
    editableUser.confirmPassword = '';
  } else {
    Object.assign(editableUser, user);
  }
  editStates[field] = true;
};

const cancelEdit = (field) => {
  editStates[field] = false;
  for (const key in editableUser) delete editableUser[key];
};

const saveChanges = async (field) => {
  const updatedData = { [field]: editableUser[field] };
  try {
    // ▼▼▼ [수정] '/users/me' 대신 동적으로 URL 생성 ▼▼▼
    const res = await http.patch(`/users/${user.id}`, updatedData);

    Object.assign(user, res.data);
    alert(`${field} 정보가 수정되었습니다.`);
    cancelEdit(field);
  } catch {
    alert("정보 수정에 실패했습니다.");
  }
};

const savePasswordChanges = async () => {
  const { currentPassword, newPassword, confirmPassword } = editableUser;
  if (!currentPassword || !newPassword || !confirmPassword) { alert("모든 필드를 입력해주세요."); return; }
  if (newPassword !== confirmPassword) { alert("비밀번호 확인이 일치하지 않습니다."); return; }
  try {
     // ▼▼▼ [수정] '/users/me/password' 대신 동적으로 URL 생성 ▼▼▼
    await http.patch(`/users/${user.id}/password`, { currentPassword, newPassword });

    alert("비밀번호가 변경되었습니다.");  
    cancelEdit('password');
  } catch {
    alert("비밀번호 변경에 실패했습니다.");
  }
};

onMounted(() => {
  checkAuthStatus();
  if (isLoggedIn.value) {
    fetchUserProfile().then(fetchReservations);
  }
});
</script>

<style scoped>
@import "@/assets/css/mypage/mypage.css";
</style>