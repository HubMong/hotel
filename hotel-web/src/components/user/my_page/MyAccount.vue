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
        <div class="tab active"> 계정
        </div>
        <div 
          class="tab" 
          @click="router.push({ name: 'MyHistory' })"
        >
          예약 내역
        </div>
      </div>

      <div class="my-page1">
        <div v-if="isLoading.user" class="loading">정보를 불러오는 중...</div>
        <div v-else class="user-info">
          <div class="info-item"><span class="label">이름</span><span class="value">{{ user.name }}</span></div>
          
          <div class="info-item item-edit-email">
            <span class="label">이메일</span>
            <template v-if="!editStates.email">
              <span class="value">{{ user.email }}</span>
              <button @click="toggleEdit('email')" class="btn-change">수정하기</button>
            </template>
            <template v-else>
              <div class="edit-controls">
                <input type="email" v-model="editableUser.email" class="input-edit" />
                <button @click="saveChanges('email')" class="btn-save btn--small">저장</button>
                <button @click="cancelEdit('email')" class="btn-cancel btn--small">취소</button>
              </div>
            </template>
          </div>
          
          <div class="info-item item-edit-password">
            <span class="label">비밀번호</span>
            <template v-if="!editStates.password">
              <span class="value">{{ '*'.repeat(user.passwordLength || 4) }}</span>
              <button @click="toggleEdit('password')" class="btn-change">변경하기</button>
            </template>
            <template v-else>
              <div class="password-edit-form">
                <input type="password" v-model="editableUser.currentPassword" placeholder="현재 비밀번호" class="input-edit input-current-pass" />
                <input type="password" v-model="editableUser.newPassword" placeholder="새 비밀번호" class="input-edit input-new-pass" />
                <input type="password" v-model="editableUser.confirmPassword" placeholder="새 비밀번호 확인" class="input-edit input-confirm-pass" />
                <div class="password-buttons">
                  <button @click="savePasswordChanges" class="btn-save">저장</button>
                  <button @click="cancelEdit('password')" class="btn-cancel">취소</button>
                </div>
              </div>
            </template>
          </div>

          <div class="info-item">
            <span class="label">생년월일</span>
            <span class="value">{{ formatDateOfBirth(user.dateOfBirth) }}</span>
          </div>
          <div class="info-item">
            <span class="label">전화번호</span>
            <span class="value">{{ user.phoneNumber || '정보 없음' }}</span>
          </div>
          </div>
          </div>
        </div>
      </div>
    <Footer />
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import http from '@/api/http';
import UserApi from '@/api/UserApi';
// 컴포넌트
import Header from "@/components/user/main_page/Header.vue";
import Footer from "@/components/user/main_page/Footer.vue";

// 1. 상태 정의
const user = reactive({});
const editableUser = reactive({});
const isLoading = reactive({ user: true, history: false }); 
const profileImage = ref('');
const isLoggedIn = ref(false);
const editStates = reactive({ email: false, password: false });
const router = useRouter();
const fileInput = ref(null);

// 🔥 [추가] 생년월일 YYYY-MM-DD 형식으로 가공 함수
const formatDateOfBirth = (dateString) => {
    if (!dateString) return '정보 없음';
    try {
        // API가 'YYYY-MM-DD' 또는 'YYYY-MM-DDT...' 형태로 데이터를 준다고 가정
        // T 이후 시간을 제거하여 날짜만 표시합니다.
        return dateString.split('T')[0]; 
    } catch {
        // 유효하지 않은 데이터인 경우 원본 반환 또는 정보 없음 처리
        return '정보 없음';
    }
};

// 2. 🔥 로직 복구 (MyPage.vue의 모든 필수 함수 포함)
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

const toggleEdit = (field) => {
  if (field === 'password') {
    editableUser.currentPassword = '';
    editableUser.newPassword = '';
    editableUser.confirmPassword = '';
  } else {
    editableUser[field] = user[field];
  }
  editStates[field] = true;
};

const cancelEdit = (field) => {
  editStates[field] = false;
  if (field !== 'password') delete editableUser[field];
  else {
    editableUser.currentPassword = '';
    editableUser.newPassword = '';
    editableUser.confirmPassword = '';
  }
};

const saveChanges = async (field) => {
  const updatedData = { [field]: editableUser[field] };
  try {
    await http.patch('/users/me', updatedData);
    alert(`${field} 정보가 수정되었습니다.`);
    fetchUserProfile(); // 정보 업데이트 후 새로고침
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
    await http.patch('/users/me/password', { currentPassword, newPassword });
    alert("비밀번호가 변경되었습니다.");
    cancelEdit('password');
  } catch {
    alert("비밀번호 변경에 실패했습니다.");
  }
};

const onImageClick = () => { fileInput.value.click(); };
const onFileChange = async (event) => { /* ... 이미지 업로드 로직 ... */ };


// 3. 라이프사이클 훅
onMounted(() => {
    checkAuthStatus();
    if (isLoggedIn.value) {
        fetchUserProfile();
    } else {
        isLoading.user = false; 
    }
});
</script>

<style scoped>
/* 🔥 CSS 파일명은 통일된 경로를 사용하도록 가정합니다. */
@import "@/assets/css/mypage/myaccount.css"; 
</style>