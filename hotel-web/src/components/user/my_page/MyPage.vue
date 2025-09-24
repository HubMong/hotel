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
        <div 
          class="tab" 
          :class="{ active: selectedTab === 'account' }" 
          @click="selectedTab = 'account'">
          계정
        </div>
        <div 
          class="tab" 
          :class="{ active: selectedTab === 'history' }" 
          @click="selectedTab = 'history'">
          예약 내역
        </div>
        <div 
          class="tab" 
          :class="{ active: selectedTab === 'payment' }" 
          @click="selectedTab = 'payment'">
          결제 수단
        </div>
      </div>

      <div class="my-page1" v-if="selectedTab === 'account'">
        <div v-if="isLoading.user" class="loading">정보를 불러오는 중...</div>
        <div v-else class="user-info">
          <div class="info-item">
            <span class="label">이름</span>
            <span class="value">{{ user.name }}</span>
          </div>
          
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
            <span class="label">패스워드</span>
            <template v-if="!editStates.password">
              <span class="value">********</span>
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
          
          <div class="info-item">
            <span class="label">전화번호</span>
            <template v-if="!editStates.phone">
              <span class="value">{{ user.phone || '-' }}</span>
              <button @click="toggleEdit('phone')" class="btn-change">수정하기</button>
            </template>
            <template v-else>
              <input type="tel" v-model="editableUser.phone" class="input-edit" />
              <button @click="saveChanges('phone')" class="btn-save">저장</button>
              <button @click="cancelEdit('phone')" class="btn-cancel">취소</button>
            </template>
          </div>
          
          <div class="info-item">
            <span class="label">주소</span>
            <template v-if="!editStates.address">
              <span class="value">{{ user.address || '-' }}</span>
              <button @click="toggleEdit('address')" class="btn-change">수정하기</button>
            </template>
            <template v-else>
              <input type="text" v-model="editableUser.address" class="input-edit" />
              <button @click="saveChanges('address')" class="btn-save">저장</button>
              <button @click="cancelEdit('address')" class="btn-cancel">취소</button>
            </template>
          </div>

          <div class="info-item">
            <span class="label">생년월일</span>
            <span class="value">{{ user.date_of_birth || '-' }}</span>
          </div>
        </div>
      </div>

      <div class="my-page2" v-if="selectedTab === 'history'">
        <div v-if="isLoading.history" class="loading">예약 내역을 불러오는 중...</div>
        <div v-else-if="reservations.length === 0" class="no-data">예약 내역이 없습니다.</div>
        <div v-else v-for="reservation in reservations" :key="reservation.id" class="user-info-card">
          <div class="user-info">
            <div class="info-item">
              <span class="label">호텔 이름</span>
              <span class="value">{{ reservation.hotelName }}</span>
            </div>
            <div class="info-item">
              <span class="label">체크-인</span>
              <span class="value">{{ reservation.checkInDate }}</span>
            </div>
            <div class="info-item">
              <span class="label">체크-아웃</span>
              <span class="value">{{ reservation.checkOutDate }}</span>
            </div>
            <div class="info-item">
              <span class="label">객실 타입</span>
              <span class="value">{{ reservation.roomType }}</span>
            </div>
          </div>
        </div>
      </div>

      <div class="my-page3" v-if="selectedTab === 'payment'">
        <div v-if="isLoading.payment" class="loading">결제 정보를 불러오는 중...</div>
        <div v-else-if="!paymentInfo" class="no-data">등록된 결제 수단이 없습니다.</div>
        <div v-else class="user-info">
          <div class="info-item">
            <span class="label">결제 수단</span>
            <span class="value">{{ paymentInfo.payment_method }}</span>
          </div>
          <button class="btn-change">결제 관리</button>
        </div>
      </div>
    </div>

    <Footer />
  </div>
</template>

<style scoped src="@/assets/css/mypage/mypage.css"></style>

<script setup>
import { ref, onMounted, reactive } from 'vue';
import { useRouter } from 'vue-router';
import http from '@/api/http';

import Header from "@/components/user/main_page/Header.vue";
import Footer from "@/components/user/main_page/Footer.vue";
// import SearchForm from "@/components/user/main_page/SearchForm.vue";

const user = ref({});
const reservations = ref([]);
const paymentInfo = ref(null);
const router = useRouter();

const isLoading = reactive({
  user: true,
  history: true,
  payment: true,
});
const selectedTab = ref('account');

const profileImage = ref('');
const fileInput = ref(null);

const isLoggedIn = ref(false);

const editStates = reactive({
  email: false,
  phone: false,
  address: false,
  password: false,
});
const editableUser = ref({});

const checkAuthStatus = () => {
  const token = localStorage.getItem('token');
  const userInfo = localStorage.getItem('user');
  
  if (token && userInfo) {
    isLoggedIn.value = true;
    user.value = JSON.parse(userInfo);
  } else {
    isLoggedIn.value = false;
    alert("로그인이 필요한 페이지입니다.");
    router.push('/login');
  }
};

const handleLogout = () => {
  localStorage.removeItem('token');
  localStorage.removeItem('user');
  isLoggedIn.value = false;
  user.value = {};
  alert("로그아웃 되었습니다.");
  router.push('/').then(() => {
    window.location.reload();
  });
};

const fetchUserProfile = async () => {
  isLoading.user = true;
  try {
    const response = await http.get('/users/me'); 
    user.value = response.data;
    profileImage.value = response.data.profileImageUrl || '';
    localStorage.setItem('user', JSON.stringify(response.data));
  } catch (e) {
    console.error("사용자 정보 조회 오류:", e);
    if (e.response && e.response.status === 401) {
        handleLogout();
    } else {
        alert("사용자 정보를 불러올 수 없습니다.");
    }
  } finally {
    isLoading.user = false;
  }
};

const fetchReservations = async () => {
  isLoading.history = true;
  try {
    const response = await http.get('/reservations/my');
    reservations.value = response.data; 
  } catch (e) {
    console.error("예약 내역 조회 오류:", e);
  } finally {
    isLoading.history = false;
  }
};

const fetchPaymentInfo = async () => {
  isLoading.payment = true;
  try {
    const response = await http.get('/payments/my');
    paymentInfo.value = response.data;
  } catch (e) {
    console.error("결제 정보 조회 오류:", e);
  } finally {
    isLoading.payment = false;
  }
};

const onImageClick = () => {
  fileInput.value.click();
};

const onFileChange = async (event) => {
  const file = event.target.files[0];
  if (!file) return;

  const reader = new FileReader();
  reader.onload = (e) => {
    profileImage.value = e.target.result;
  };
  reader.readAsDataURL(file);

  const formData = new FormData();
  formData.append('profileImage', file);

  try {
    await http.post('/users/me/profile-image', formData, {
      headers: { 'Content-Type': 'multipart/form-data' }
    });
    alert("프로필 이미지가 성공적으로 변경되었습니다.");
    await fetchUserProfile();
  } catch (e) {
    console.error("프로필 이미지 업로드 오류:", e);
    alert("이미지 업로드에 실패했습니다.");
    await fetchUserProfile(); // 실패 시 원래 이미지로 복구
  }
};

const toggleEdit = (field) => {
  if (field === 'password') {
    editableUser.value = { currentPassword: '', newPassword: '', confirmPassword: '' };
  } else {
    editableUser.value = { ...user.value };
  }
  editStates[field] = true;
};

const cancelEdit = (field) => {
  editStates[field] = false;
  editableUser.value = {};
};

const saveChanges = async (field) => {
  if (!editableUser.value) return;
  const updatedData = { [field]: editableUser.value[field] };

  try {
    const response = await http.patch('/users/me', updatedData); 
    user.value = response.data;
    alert(`${field} 정보가 성공적으로 수정되었습니다.`);
    cancelEdit(field);
  } catch (e) {
    console.error("정보 수정 오류:", e);
    alert("정보 수정에 실패했습니다.");
  }
};

const savePasswordChanges = async () => {
  if (!editableUser.value) return;
  const { currentPassword, newPassword, confirmPassword } = editableUser.value;

  if (!currentPassword || !newPassword || !confirmPassword) {
    alert("모든 비밀번호 필드를 입력해주세요.");
    return;
  }
  if (newPassword !== confirmPassword) {
    alert("새 비밀번호와 새 비밀번호 확인이 일치하지 않습니다.");
    return;
  }
  if (newPassword.length < 8) {
    alert("새 비밀번호는 8자 이상이어야 합니다.");
    return;
  }

  try {
    await http.patch('/users/me/password', { currentPassword, newPassword });
    alert("비밀번호가 성공적으로 변경되었습니다.");
    cancelEdit('password');
  } catch (error) {
    console.error("비밀번호 변경 오류:", error);
    if (error.response && (error.response.status === 401 || error.response.status === 400)) {
      alert("현재 비밀번호가 올바르지 않습니다.");
    } else {
      alert("비밀번호 변경에 실패했습니다. 다시 시도해주세요.");
    }
  }
};

onMounted(() => {
  checkAuthStatus();
  if (isLoggedIn.value) {
    fetchUserProfile();
    fetchReservations();
    fetchPaymentInfo();
  }
});
</script>
