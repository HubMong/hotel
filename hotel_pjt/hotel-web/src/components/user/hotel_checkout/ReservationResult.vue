<template>
  <div class="reservation-result-page">
    <!-- 공통 헤더 -->
    <Header :isLoggedIn="isLoggedIn" :user="user" @logout="handleLogout" />

    <section class="container result">
      <h1>예약이 확정되었습니다 🎉</h1>
      <div class="box" v-if="detail">
        <p>예약번호: <b>{{ detail.id }}</b></p>

        <div class="button-group">
          <router-link
            v-if="detail.hotelId"
            class="btn"
            :to="`/hotels/${detail.hotelId}`"
          >
            숙소로 돌아가기
          </router-link>
          <span v-else>호텔 정보 없음</span>
          <router-link class="btn" to="/search">다른 숙소 보기</router-link>
        </div>
      </div>
    </section>

    <!-- 공통 푸터 -->
    <Footer />
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from "vue"
import { useRoute, useRouter } from "vue-router"
import ReservationApi from "@/api/ReservationApi"
import Header from "@/components/user/main_page/Header.vue"
import Footer from "@/components/user/main_page/Footer.vue"

const route = useRoute()
const router = useRouter()
const id = Number(route.params.id)
const detail = ref(null)

// ✅ 로그인 상태 & 사용자 정보
const isLoggedIn = ref(false)
const user = reactive({})

// 로그인 체크
const checkAuthStatus = () => {
  const token = localStorage.getItem("token")
  const userInfo = localStorage.getItem("user")
  if (token && userInfo) {
    isLoggedIn.value = true
    Object.assign(user, JSON.parse(userInfo))
  } else {
    isLoggedIn.value = false
    Object.keys(user).forEach(k => delete user[k])
  }
}

// 로그아웃
const handleLogout = () => {
  localStorage.removeItem("token")
  localStorage.removeItem("user")
  isLoggedIn.value = false
  Object.keys(user).forEach(k => delete user[k])
  alert("로그아웃 되었습니다.")
  router.push("/").then(() => window.location.reload())
}

onMounted(async () => {
  checkAuthStatus()
  try {
    detail.value = await ReservationApi.get(id)
  } catch (e) {
    console.error(e)
  }
})
</script>

<style scoped>
@import "@/assets/css/hotel_checkout/ReservationResult.css";
</style>
