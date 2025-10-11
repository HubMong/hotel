<template>
  <div class="page">
    <header class="page-header">
      <h2>호텔 추가</h2>
    </header>

    <form class="form" @submit.prevent="submit">
      <div class="row">
        <label>호텔명</label>
        <input v-model.trim="form.name" required placeholder="예) 소나무 호텔" />
      </div>

      <div class="row">
        <label>주소</label>
        <input v-model.trim="form.address" required placeholder="예) 서울특별시..." />
      </div>

      <div class="row">
        <label>국가</label>
        <input v-model.trim="form.country" required placeholder="예) KR" />
      </div>

      <div class="row">
        <label>등급</label>
        <select v-model.number="form.starRating" required>
          <option :value="0">0</option>
          <option :value="1">1</option>
          <option :value="2">2</option>
          <option :value="3">3</option>
          <option :value="4">4</option>
          <option :value="5">5</option>
        </select>
      </div>

      <div class="row">
        <label>사업자번호(선택)</label>
        <input v-model.number="form.businessId" type="number" placeholder="숫자만" />
      </div>

      <div class="actions">
        <button type="button" class="btn" @click="$router.push({ name: 'OwnerHotelList' })">목록</button>
        <button type="submit" class="btn-primary" :disabled="submitting">저장</button>
      </div>

      <p v-if="error" class="error">{{ error }}</p>
    </form>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue'
import http from '@/api/http'
import { useRouter } from 'vue-router'

const router = useRouter()
const submitting = ref(false)
const error = ref(null)

const form = reactive({
  name: '',
  address: '',
  country: 'KR',
  starRating: 0,
  businessId: null,
})

async function submit() {
  submitting.value = true
  error.value = null
  try {
    await http.post('/owner/hotels', form) // POST /api/owner/hotels
    alert('호텔을 등록했습니다.')
    router.push({ name: 'OwnerHotelList' })
  } catch (e) {
    console.error(e)
    error.value = '등록 중 오류가 발생했습니다.'
  } finally {
    submitting.value = false
  }
}
</script>

<style scoped>
.page { padding:24px; }
.page-header { margin-bottom:16px; }
.form{ background:#fff; border:1px solid #eee; border-radius:8px; padding:16px; max-width:560px; }
.row{ display:flex; flex-direction:column; gap:8px; margin-bottom:12px; }
label{ font-weight:700; }
input,select{ border:1px solid #ccc; border-radius:8px; padding:10px; }
.actions{ display:flex; gap:12px; margin-top:12px; }
.btn{ background:#e5e7eb; border:none; padding:10px 14px; border-radius:8px; cursor:pointer; }
.btn-primary{ background:#4f46e5; color:#fff; border:none; padding:10px 14px; border-radius:8px; cursor:pointer; font-weight:700; }
.btn-primary:hover{ background:#4338ca; }
.error{ color:#b91c1c; margin-top:8px; }
</style>
