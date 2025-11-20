<template>
  <div class="mypage-layout">
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
        <div class="tab" :class="{ active: activeTab === 'account' }" @click="setTab('account')">계정</div>
        <div class="tab" :class="{ active: $route.name === 'MyHistory' }" @click="router.push({ name: 'MyHistory' })">예약 내역</div>
        <div class="tab" :class="{ active: activeTab === 'wishlist' }" @click="setTab('wishlist')">찜</div>
        <div class="tab" :class="{ active: activeTab === 'review' }" @click="setTab('review')">리뷰</div>
      </div>

      <!-- 탭 콘텐츠 -->
      <div class="tab-content">
        <div v-if="activeTab === 'account'">
          <div class="info-item"><span class="label">이름</span><span class="value">{{ user.name }}</span></div>
          <div class="info-item"><span class="label">이메일</span><span class="value">{{ user.email }}</span></div>
          <div class="info-item"><span class="label">전화번호</span><span class="value">{{ user.phone || user.phoneNumber || '정보 없음' }}</span></div>
        </div>

        <!-- ✅ 이곳에서는 embedded 모드로 사용 (래퍼 숨김) -->
        <MyWishlist v-if="activeTab === 'wishlist'" embedded />
        <MyReview  v-if="activeTab === 'review'"  embedded />
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, watch } from "vue"
import { useRouter, useRoute } from "vue-router"

import MyWishlist from "@/components/user/my_page/MyWishlist.vue"
import MyReview from "@/components/user/my_page/MyReview.vue"
import UserApi from "@/api/UserApi"
import { resolveBackendUrl } from "@/api/http"
import { getAuthUser, setAuthUser, notifyAuthChanged } from "@/utils/auth-storage"

const router = useRouter()
const route = useRoute()

const user = reactive({})
const profileImage = ref("")
const isLoggedIn = ref(false)
const activeTab = ref("account")
const fileInput = ref(null)
const uploading = ref(false)

const resolveImageUrl = (url) => {
  if (!url) return ""
  if (/^https?:\/\//i.test(url)) return url
  const normalized = url.startsWith("/") ? url : `/${url}`
  return resolveBackendUrl(normalized)
}

const setTab = (tab) => {
  if (activeTab.value === tab) return
  activeTab.value = tab
  const map = {
    account: "MyAccount",
    wishlist: "MyWishlist",
    review: "MyReview"
  }
  const target = map[tab] || "MyAccount"
  if (route.name !== target) router.push({ name: target })
}

const syncTabWithRoute = () => {
  if (route.name === "MyWishlist") activeTab.value = "wishlist"
  else if (route.name === "MyReview") activeTab.value = "review"
  else activeTab.value = "account"
}

const checkAuthStatus = () => {
  const userInfo = getAuthUser()
  if (userInfo) {
    isLoggedIn.value = true
    Object.assign(user, userInfo)
    if (user.profileImageUrl) {
      profileImage.value = resolveImageUrl(user.profileImageUrl)
    }
  } else {
    router.push('/login')
  }
}

const fetchUserProfile = async () => {
  try {
    const data = await UserApi.getInfo()
    Object.assign(user, data)
    profileImage.value = resolveImageUrl(data.profileImageUrl)
    setAuthUser(data)
    notifyAuthChanged()
  } catch (error) {
    const status = error.response?.status
    if (status === 401 || error.message?.includes('401')) return
    console.warn("사용자 정보를 불러올 수 없습니다.", error)
  }
}

const onImageClick = () => fileInput.value?.click()
const onFileChange = async (e) => {
  const file = e?.target?.files?.[0]
  if (!file) return

  try {
    uploading.value = true
    const result = await UserApi.updateProfileImage(file)
    const newUrl = resolveImageUrl(result?.profileImageUrl)
    if (newUrl) {
      profileImage.value = newUrl
    }
    await fetchUserProfile()
  } catch (err) {
    console.error("프로필 이미지 업데이트 실패", err)
    alert("프로필 이미지 업로드 중 오류가 발생했습니다.")
  } finally {
    uploading.value = false
    if (fileInput.value) fileInput.value.value = ""
  }
}

onMounted(() => {
  checkAuthStatus()
  if (isLoggedIn.value) {
    fetchUserProfile()
  }
  syncTabWithRoute()
})

watch(() => route.name, syncTabWithRoute)
</script>

<!-- ✅ @import 대신 scoped src 사용 -->
<style scoped src="@/assets/css/mypage/myaccount.css"></style>
