<!-- src/components/owner/OwnerHotelList.vue -->
<template>
  <div class="page">
    <header class="page-header">
      <h2>내 호텔</h2>
      <div class="toolbar">
        <select v-model="status" class="select">
          <option value="">전체 상태</option>
          <option value="PENDING">PENDING</option>
          <option value="APPROVED">APPROVED</option>
          <option value="REJECTED">REJECTED</option>
          <option value="SUSPENDED">SUSPENDED</option>
        </select>
        <button class="btn-primary" @click="$router.push({ name: 'OwnerHotelCreate' })">호텔 추가</button>
      </div>
    </header>

    <div v-if="loading" class="hint">불러오는 중…</div>
    <div v-else-if="error" class="error">{{ error }}</div>

    <div v-else>
      <div v-if="filtered.length === 0" class="empty">조건에 맞는 호텔이 없습니다.</div>

      <ul v-else class="hotel-list">
        <li v-for="h in filtered" :key="h.id" class="hotel-item">
          <div class="title">
            <strong>{{ h.name }}</strong>
            <span class="meta">({{ h.approvalStatus ?? 'UNKNOWN' }})</span>
          </div>
          <div class="sub">
            {{ h.address }} · {{ h.starRating }}성급
          </div>
          <div class="actions">
            <RouterLink :to="{ name: 'OwnerRoom', query: { hotelId: h.id } }" class="link">객실 관리</RouterLink>
            <RouterLink :to="{ name: 'OwnerHotelEdit', params: { id: String(h.id) } }" class="link">정보 수정</RouterLink>
            <RouterLink :to="{ path: `/hotels/${h.id}` }" class="link" target="_blank">미리보기(사용자)</RouterLink>
            <button class="link danger" @click="deleteHotel(h.id)">삭제</button>
          </div>
        </li>
      </ul>
    </div>
  </div>
</template>

<script setup>
import { onMounted, ref, computed } from 'vue'
import { RouterLink } from 'vue-router'
import http from '@/api/http'

const hotels = ref([])
const loading = ref(false)
const error = ref(null)
const status = ref('')

async function load() {
  loading.value = true
  error.value = null
  try {
    const { data } = await http.get('owner/hotels/my-hotels')
    hotels.value = Array.isArray(data) ? data : []
  } catch (e) {
    console.error(e)
    error.value = '내 호텔 목록을 불러오지 못했습니다.'
  } finally {
    loading.value = false
  }
}

const filtered = computed(() => {
  if (!status.value) return hotels.value
  return hotels.value.filter(h => (h.approvalStatus || '') === status.value)
})

async function deleteHotel(id) {
  if (!id) return
  if (!confirm('정말 이 호텔을 삭제할까요? (객실이 남아있으면 삭제가 제한될 수 있어요)')) return
  try {
    await http.delete(`/owner/hotels/my-hotels/${id}`)
    await load()
    alert('삭제되었습니다.')
  } catch (e) {
    console.error(e)
    const msg = e?.response?.data?.message || '삭제에 실패했습니다.'
    alert(msg)
  }
}

onMounted(load)
</script>

<style scoped>
/* 기존 스타일 그대로 + 삭제 버튼 경고색상 */
.page { padding: 24px; }
.page-header { display:flex; align-items:center; justify-content:space-between; margin-bottom:16px; gap:12px; }
.toolbar{ display:flex; gap:8px; align-items:center; }
.select{ height:38px; padding:0 10px; border:1px solid #e5e7eb; border-radius:8px; background:#fff; }
.btn-primary { background:#4f46e5; color:#fff; border:none; padding:10px 14px; border-radius:8px; cursor:pointer; font-weight:700; }
.btn-primary:hover{ background:#4338ca; }
.hint,.error,.empty{ color:#666; padding:16px 0; }
.hotel-list{ display:flex; flex-direction:column; gap:12px; }
.hotel-item{ background:#fff; border:1px solid #eee; border-radius:8px; padding:12px 14px; }
.title{ font-size:16px; }
.meta{ margin-left:8px; color:#888; font-weight:500; }
.sub{ margin-top:4px; color:#666; }
.actions{ margin-top:8px; display:flex; gap:12px; flex-wrap: wrap; }
.link{ color:#2563eb; text-decoration:none; }
.link:hover{ text-decoration:underline; }
.link.danger{ color:#dc2626; border:none; background:none; cursor:pointer; }
.link.danger:hover{ text-decoration:underline; }
</style>
