<template>
  <div class="card">
    <h2>호텔 승인 대기 목록</h2>

    <div class="toolbar">
      <button class="btn reload" @click="load" :disabled="loading">새로고침</button>
      <span class="hint" v-if="total !== null">총 {{ total }}건</span>
    </div>

    <div v-if="loading" class="hint">불러오는 중…</div>
    <div v-else-if="error" class="error">{{ error }}</div>

    <table v-else class="table">
      <thead>
        <tr>
          <th>ID</th><th>이름</th><th>주소</th><th>등급</th><th>상태</th><th>작업</th>
        </tr>
      </thead>
      <tbody>
        <tr v-for="h in hotels" :key="h.id">
          <td>{{ h.id }}</td>
          <td>{{ h.name }}</td>
          <td>{{ h.address }}</td>
          <td>{{ h.starRating }}</td>
          <td>{{ h.status }}</td>
          <td class="actions">
            <button @click="approve(h.id)" class="btn ok" :disabled="busy[h.id]">승인</button>
            <button @click="reject(h.id)"  class="btn danger" :disabled="busy[h.id]">반려</button>
          </td>
        </tr>
        <tr v-if="!hotels.length">
          <td colspan="6" class="hint">승인 대기 호텔이 없습니다.</td>
        </tr>
      </tbody>
    </table>
  </div>
</template>

<script setup>
import { ref, onMounted, onBeforeUnmount } from 'vue'
import http from '@/api/http'

const loading = ref(false)
const error   = ref(null)
const hotels  = ref([])
const page    = ref(0)
const size    = ref(10)
const total   = ref(null)
const busy    = ref({})

function pickContent(res) {
  // ApiResponse<PageResponse<HotelAdminDto>> 형태 가정
  const payload = res?.data?.data ?? res?.data
  total.value = payload?.totalElements ?? payload?.count ?? null
  return payload?.content ?? payload?.items ?? (Array.isArray(payload) ? payload : [])
}

async function load() {
  loading.value = true
  error.value = null
  try {
    // ✅ 쿼리파라미터 버전 권장 (/api/admin/hotels?status=PENDING&page=..)
    const res = await http.get('/admin/hotels', {
      params: { status: 'PENDING', page: page.value, size: size.value, sort: 'id,desc' }
    })
    hotels.value = pickContent(res)
  } catch (e) {
    console.error(e)
    error.value = '목록을 불러오지 못했습니다.'
    hotels.value = []
  } finally {
    loading.value = false
  }
}

async function approve(id) {
  if (!confirm(`#${id} 호텔을 승인할까요?`)) return
  busy.value = { ...busy.value, [id]: true }
  try {
    // ✅ 컨트롤러가 @RequestParam("note") 사용 시 쿼리로 전달
    const note = ''
    await http.put(`/admin/hotels/${id}/approve`, null, { params: { note } })
    await load()
    alert('승인 완료')
  } catch (e) {
    console.error(e)
    alert('승인 실패')
  } finally {
    busy.value = { ...busy.value, [id]: false }
  }
}

async function reject(id) {
  const reason = prompt('반려 사유를 적어주세요 (선택)') || ''
  busy.value = { ...busy.value, [id]: true }
  try {
    // ✅ 컨트롤러가 @RequestParam("reason") 사용 시 쿼리로 전달
    await http.put(`/admin/hotels/${id}/reject`, null, { params: { reason } })
    await load()
    alert('반려 처리 완료')
  } catch (e) {
    console.error(e)
    alert('반려 실패')
  } finally {
    busy.value = { ...busy.value, [id]: false }
  }
}

onMounted(() => {
  load()
  // (선택) 다른 곳에서 이벤트 쏘면 자동 갱신
  window.addEventListener('admin:refresh-hotels', load)
})
onBeforeUnmount(() => {
  window.removeEventListener('admin:refresh-hotels', load)
})
</script>

<style scoped>
.card{ background:#fff; border:1px solid #eee; border-radius:10px; padding:16px; }
.toolbar{ display:flex; align-items:center; gap:8px; margin-bottom:8px; }
.table{ width:100%; border-collapse:collapse; }
th,td{ padding:10px; border-bottom:1px solid #f1f1f1; text-align:left; }
.actions .btn{ margin-right:6px; padding:6px 10px; border-radius:6px; border:1px solid #ddd; background:#fff; cursor:pointer; }
.btn.ok{ border-color:#16a34a; color:#16a34a; }
.btn.ok:hover{ background:#16a34a; color:#fff; }
.btn.danger{ border-color:#dc2626; color:#dc2626; }
.btn.danger:hover{ background:#dc2626; color:#fff; }
.btn.reload{ border:1px solid #888; }
.hint{ color:#666; padding:12px 0; }
.error{ color:#dc2626; padding:12px 0; }
</style>
