<template>
  <section class="container result">
    <h1>예약이 확정되었습니다 🎉</h1>
    <div class="box" v-if="detail">
      <p>예약번호: <b>{{ detail.id }}</b></p>
      <p>호텔로 이동: 
        <router-link v-if="detail.hotelId" class="btn" :to="`/hotels/${detail.hotelId}`">숙소로 돌아가기</router-link>
        <span v-else>호텔 정보 없음</span>
      </p>
      <router-link class="btn" to="/search">다른 숙소 보기</router-link>
    </div>
  </section>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import ReservationApi from '@/api/ReservationApi'

const route = useRoute()
const id = Number(route.params.id)
const detail = ref(null)

onMounted(async () => {
  try {
    detail.value = await ReservationApi.get(id)
  } catch (e) {
    console.error(e)
  }
})
</script>
