<template>
  <div class="mypage-layout">
    <Header :isLoggedIn="isLoggedIn" :user="user" @logout="handleLogout" />
    
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
          
          <div class="edit-actions" v-if="!isEditing">
            <button @click="startEditing" class="btn-change">내 정보 수정</button>
          </div>

          <template v-if="!isEditing">
            <div class="info-item"><span class="label">이름</span><span class="value">{{ user.name }}</span></div>
            <div class="info-item"><span class="label">이메일</span><span class="value">{{ user.email }}</span></div>
            <div v-if="user.provider === 'LOCAL'" class="info-item">
                <span class="label">비밀번호</span><span class="value">********</span>
            </div>
          </template>

          <template v-else>
            <div class="info-item"><span class="label">이름</span><span class="value">{{ user.name }}</span></div>
            <div class="info-item">
                <span class="label">이메일</span>
                <input type="email" v-model="editableUser.email" class="input-edit" />
            </div>
            <div v-if="user.provider === 'LOCAL'" class="info-item password-edit-section">
                <span class="label">비밀번호 변경</span>
                <div class="input-group">
                  <div class="password-input-wrapper">
                    <input 
                      :type="currentPasswordType" 
                      v-model="editableUser.currentPassword" 
                      placeholder="현재 비밀번호" 
                      class="input-edit" 
                    />
                    <button 
                      type="button" 
                      @click="togglePasswordVisibility('current')" 
                      class="btn-toggle-password"
                    >
                      {{ currentPasswordType === 'password' ? '보기' : '숨기기' }}
                    </button>
                  </div>

                  <div class="password-input-wrapper">
                    <input 
                      :type="newPasswordType" 
                      v-model="editableUser.newPassword" 
                      placeholder="새 비밀번호" 
                      class="input-edit" 
                    />
                    <button 
                      type="button" 
                      @click="togglePasswordVisibility('new')" 
                      class="btn-toggle-password"
                    >
                      {{ newPasswordType === 'password' ? '보기' : '숨기기' }}
                    </button>
                  </div>

                  <div class="password-input-wrapper">
                    <input 
                      :type="confirmPasswordType" 
                      v-model="editableUser.confirmPassword" 
                      placeholder="새 비밀번호 확인" 
                      class="input-edit" 
                    />
                    <button 
                      type="button" 
                      @click="togglePasswordVisibility('confirm')" 
                      class="btn-toggle-password"
                    >
                      {{ confirmPasswordType === 'password' ? '보기' : '숨기기' }}
                    </button>
                  </div> 
                <p class="notice">비밀번호를 변경하지 않으려면 비워두세요.</p>
                </div>
            </div>  
            <div class="form-actions">
                <button @click="cancelEditing" class="btn-cancel">취소</button>
                <button @click="saveAllChanges" class="btn-save">수정 완료</button>
            </div>
          </template>
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
                  v-if="reservation.status === 'CONFIRMED' || reservation.status === 'PENDING'"
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
// 💡 [필수 수정] cancel 함수를 명시적으로 import
import { getMy, cancel } from '@/api/ReservationApi';
import UserApi from '@/api/UserApi';

const user = reactive({});
const editableUser = reactive({});
const reservations = ref([]);
const isLoading = reactive({ user: true, history: true });
const selectedTab = ref('account');
const profileImage = ref('');
const fileInput = ref(null);
const isLoggedIn = ref(false);
const isEditing = ref(false);
const router = useRouter();

// [추가된 부분] 비밀번호 입력 필드 타입 관리 상태
const currentPasswordType = ref('password');
const newPasswordType = ref('password');
const confirmPasswordType = ref('password');

const statusMap = {
  PENDING: "결제 대기중",
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

//예약 카드 토글 함수
const toggleReservation = (reservation) => {
  reservation.active = !reservation.active;
};

// [추가된 부분]비밀번호 보기/숨기기 토글 함수: input의 type을 text/password로 전환합니다.
const togglePasswordVisibility = (field) => {
  if (field === 'current') {
    currentPasswordType.value = currentPasswordType.value === 'password' ? 'text' : 'password';
  } else if (field === 'new') {
    newPasswordType.value = newPasswordType.value === 'password' ? 'text' : 'password';
  } else if (field === 'confirm') {
    confirmPasswordType.value = confirmPasswordType.value === 'password' ? 'text' : 'password';
  }
};

// 예약 내역 조회 함수
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
        if (hotelRes.data && hotelRes.data.hotel) {
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
    console.error("예약 내역 조회 실패:", e);
    reservations.value = [];
  } finally {
    isLoading.history = false;
  }
};

// 예약 상세 페이지 이동 함수
const goToReservationDetail = (reservationId) => {
  router.push(`/reservations/${reservationId}`);
};

// 💡 예약 취소 함수: API 모듈의 cancel 함수를 사용
const cancelReservation = async (reservationId) => {
  if (!confirm("정말로 이 예약을 취소하시겠습니까?")) return;
  
  try {
    console.log(`예약 취소 요청: POST /reservations/${reservationId}/cancel`);
    await cancel(reservationId); 
    
    alert("예약이 성공적으로 취소되었습니다.");
    await fetchReservations();
  } catch (error) {
    console.error("예약 취소 실패:", error);
    const errorMessage = error.response?.data?.message || error.message || "알 수 없는 오류가 발생했습니다. (콘솔 확인)";
    console.error("서버 응답 오류 상세:", error.response);
    
    alert(`예약 취소에 실패했습니다.\n오류: ${errorMessage}`);
  }
};

// 로그인 상태 확인 함수 
const checkAuthStatus = () => {
  const token = localStorage.getItem('token') || localStorage.getItem('access_token');
  const userInfo = localStorage.getItem('user');
  if (token && userInfo) {
    isLoggedIn.value = true;
    Object.assign(user, JSON.parse(userInfo));
  } else {
    // 페이지 로드 시에는 alert 대신 조용히 로그인 페이지로 보낼 수 있습니다.
    // 하지만 현재 로직을 유지합니다.
    alert("로그인이 필요한 페이지입니다.");
    router.push('/login');
  }
};

const handleLogout = (showAlert = true) => {
  ['token','access_token','user'].forEach(k => { 
    localStorage.removeItem(k); 
    sessionStorage.removeItem(k); 
  });
  isLoggedIn.value = false;
  Object.keys(user).forEach(k => delete user[k]);
  
  if (showAlert) {
    alert("로그아웃 되었습니다.");
  }
  router.push('/').then(() => window.location.reload());
};

const fetchUserProfile = async () => {
  isLoading.user = true;
  try {
    const data = await UserApi.getInfo();
    Object.assign(user, data);
    profileImage.value = data.profileImageUrl || '';
    localStorage.setItem('user', JSON.stringify(data));
  } catch(e) {
    console.error("사용자 정보 로드 실패:", e);
    alert("사용자 정보를 불러올 수 없습니다. 다시 로그인해주세요.");
    handleLogout(false);
  } finally {
    isLoading.user = false;
  }
};

const onImageClick = () => fileInput.value.click();
const onFileChange = async (event) => {
  const file = event.target.files[0];
  if (!file) return;
  const formData = new FormData();
  formData.append('profileImage', file);
  try {
    const res = await http.post('/users/me/profile-image', formData, { 
      headers: { 'Content-Type': 'multipart/form-data' } 
    });
    alert("프로필 이미지가 변경되었습니다.");
    profileImage.value = res.data.profileImageUrl;
    Object.assign(user, res.data);
    localStorage.setItem('user', JSON.stringify(user));
  } catch {
    alert("이미지 업로드에 실패했습니다.");
  }
};

const startEditing = () => {
  editableUser.email = user.email;
  editableUser.currentPassword = '';
  editableUser.newPassword = '';
  editableUser.confirmPassword = '';
  // 수정 모드 시작 시 비밀번호 타입 초기화
  currentPasswordType.value = 'password';
  newPasswordType.value = 'password';
  confirmPasswordType.value = 'password';
  isEditing.value = true;
};

const cancelEditing = () => {
  isEditing.value = false;
};

// 💡 [수정됨] saveAllChanges 함수: 단일 API 호출로 통합
const saveAllChanges = async () => {
  if (!user.id) {
    alert("사용자 ID가 없어 정보를 수정할 수 없습니다.");
    return;
  }
  
  const isEmailChanged = editableUser.email !== user.email;
  const isPasswordChanged = editableUser.newPassword !== '';
  
  // 유효성 검사 로직
  if (isPasswordChanged) {
    if (!editableUser.currentPassword) {
      alert('비밀번호 변경을 원하시면 현재 비밀번호를 입력해주세요.');
      return;
    }
    if (editableUser.newPassword !== editableUser.confirmPassword) {
      alert('새 비밀번호 확인이 일치하지 않습니다.');
      return;
    }
  }
  if (!isEmailChanged && !isPasswordChanged) {
    alert('변경된 내용이 없습니다.');
    isEditing.value = false;
    return;
  }

  // 💡 단일 API에 보낼 페이로드 객체 준비
  const payload = {
    email: editableUser.email,
    currentPassword: editableUser.currentPassword,
    newPassword: editableUser.newPassword,
  };

  try {
    // 🏆 [단일 API 호출] UserApi.updateProfileAndPass만 호출하도록 변경
    await UserApi.updateProfileAndPass(user.id, payload);

    // 성공 처리: 트랜잭션이 성공했으므로 두 작업 모두 적용되었습니다.
    alert("정보가 성공적으로 수정되었습니다. 보안을 위해 다시 로그인해주세요.");
    
    // 로컬 user 객체 및 localStorage 업데이트 (이메일 변경 시 필수)
    if (isEmailChanged) {
        user.email = editableUser.email;
        localStorage.setItem('user', JSON.stringify(user));
    }
    handleLogout(false);

  } catch (err) {
    console.error("정보 수정 실패:", err);
    // 오류 메시지 강화: 백엔드에서 400 Bad Request 등을 기대
    const status = err.response ? err.response.status : 'N/W';
    const errorMessage = err.response?.data?.message || `네트워크 오류 (코드: ${status})`;
    
    // 💡 이제 오류는 트랜잭션 내부 실패 (예: 비밀번호 불일치)로 인한 것입니다.
    alert(`정보 수정에 실패했습니다. (오류: ${errorMessage})`);
    
    isEditing.value = false;
  }
};

onMounted(async () => {
  checkAuthStatus();
  if (isLoggedIn.value) {
    await fetchUserProfile();
    fetchReservations();
  }
});
</script>

<style scoped>
@import "@/assets/css/mypage/mypage.css";
</style>