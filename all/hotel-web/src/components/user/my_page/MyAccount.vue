<template>
  <div class="mypage-layout">
    <!-- Header -->
    <Header :isLoggedIn="isLoggedIn" :user="user" @logout="handleLogout" />

    <div class="allcard">
      <div class="intro">
        <h2>내 정보</h2>
      </div>

      <!-- 프로필 -->
      <div class="image">
        <img 
          :src="profileImage || 'https://cdn-icons-png.flaticon.com/512/3135/3135715.png'"
          alt="Profile Image" 
          @click="onImageClick"
          class="profile-img"
        />
        <input type="file" ref="fileInput" accept="image/*" @change="onFileChange" style="display:none;" />
      </div>

      <!-- 메뉴 탭 -->
      <div class="menu-tabs">
        <div 
          class="tab" 
          :class="{ active: activeTab === 'account' }"
          @click="setTab('account')"
        >
          계정
        </div>
        <div 
          class="tab"
          :class="{ active: $route.name === 'MyHistory' }"
          @click="router.push('/myhistory')"
        >
          예약 내역
        </div>
        <div 
          class="tab" 
          :class="{ active: activeTab === 'wishlist' }"
          @click="setTab('wishlist')"
        >
          찜 
        </div>
        <div 
          class="tab" 
          :class="{ active: activeTab === 'review' }"
          @click="setTab('review')"
        >
          리뷰 
        </div>
      </div>

      <!-- 탭 콘텐츠 -->
      <div class="tab-content">
        <div v-if="activeTab === 'account'">
          <div class="info-item"><span class="label">이름</span><span class="value">{{ user.name }}</span></div>
          <div class="info-item"><span class="label">이메일</span><span class="value">{{ user.email }}</span></div>
          <div class="info-item"><span class="label">전화번호</span><span class="value">{{ user.phoneNumber || '정보 없음' }}</span></div>
        </div>

        <MyWishlist v-if="activeTab === 'wishlist'" />
        <MyReview v-if="activeTab === 'review'" />
      </div>
    </div>

    <!-- Footer -->
    <Footer />
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from "vue"
import { useRouter } from "vue-router"
import Header from "@/components/user/main_page/Header.vue"
import Footer from "@/components/user/main_page/Footer.vue"

// 탭별 컴포넌트
import MyWishlist from "@/components/user/my_page/MyWishlist.vue"
import MyReview from "@/components/user/my_page/MyReview.vue"

import UserApi from "@/api/UserApi"

const user = reactive({})
const profileImage = ref("")
const isLoggedIn = ref(false)
const activeTab = ref("account")   // 기본 탭
const fileInput = ref(null)

const router = useRouter()

// 탭 전환 (계정/위시리스트/리뷰만 이 방식)
const setTab = (tab) => {
  activeTab.value = tab
  // URL 동기화
  window.history.pushState({}, "", `/${tab === 'account' ? 'myaccount' : tab === 'wishlist' ? 'mywishlist' : 'myreview'}`)
}

// 로그인 체크 & 프로필 불러오기
const checkAuthStatus = () => {
  const token = localStorage.getItem("token")
  const userInfo = localStorage.getItem("user")
  if (token && userInfo) {
    isLoggedIn.value = true
    Object.assign(user, JSON.parse(userInfo))
  } else {
    router.push('/login')
  }
}

const fetchUserProfile = async () => {
  try {
    const data = await UserApi.getInfo()
    Object.assign(user, data)
    profileImage.value = data.profileImageUrl || ""
    localStorage.setItem("user", JSON.stringify(data))
  } catch {
    console.warn("사용자 정보를 불러올 수 없습니다.")
  }
}

const handleLogout = () => {
  localStorage.clear()
  alert("로그아웃 되었습니다.")
  router.push("/")
}

const onImageClick = () => fileInput.value.click()
const onFileChange = (e) => console.log("이미지 업로드:", e.target.files[0])

onMounted(() => {
  checkAuthStatus()
  fetchUserProfile()

  // URL 보고 탭 초기화
  const path = window.location.pathname
  if (path.includes("mywishlist")) activeTab.value = "wishlist"
  else if (path.includes("myreview")) activeTab.value = "review"
  else activeTab.value = "account"
})
</script>

<style scoped>
@import "@/assets/css/mypage/myaccount.css";
</style>
