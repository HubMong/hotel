<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import AdminApi from '@/api/AdminApi'

const router = useRouter()
const loading = ref(true)
const err = ref(null)
const rows = ref([])

const q = ref('')
const onlyInactive = ref(false)

async function fetchList(){
  loading.value = true
  err.value = null
  try{
    const data = await AdminApi.getHotels({ q: q.value, inactive: onlyInactive.value })
    rows.value = Array.isArray(data) ? data : (data.items || [])
  }catch(e){
    console.error(e); err.value = '호텔 목록을 불러오지 못했습니다.'
  }finally{ loading.value = false }
}
onMounted(fetchList)

function goEdit(id){ router.push(`/admin/hotels/${id}`) }
async function toggleActive(row){
  await AdminApi.toggleHotelActive(row.id, !row.active)
  await fetchList()
}
</script>

<template>
  <div class="page-head">
    <h1>호텔 관리</h1>
    <div class="sub">호텔을 검색/활성화/수정할 수 있습니다.</div>
  </div>

  <div class="panel toolbar">
    <div class="tool-left">
      <input class="input" v-model.trim="q" placeholder="호텔명/주소 검색" @keydown.enter="fetchList" />
      <label class="chk">
        <input type="checkbox" v-model="onlyInactive" @change="fetchList" />
        비활성만 보기
      </label>
      <button class="admin-btn" @click="fetchList">검색</button>
    </div>
    <div class="tool-right">
      <button class="admin-btn primary" @click="goEdit('new')">+ 새 호텔 등록</button>
    </div>
  </div>

  <div class="panel">
    <div v-if="loading" class="muted">불러오는 중…</div>
    <div v-else-if="err" class="error">{{ err }}</div>

    <table v-else class="admin-table">
      <thead>
        <tr>
          <th>호텔</th>
          <th style="width:14%">성급</th>
          <th style="width:24%">주소</th>
          <th style="width:12%">상태</th>
          <th style="width:16%">작업</th>
        </tr>
      </thead>
      <tbody>
        <tr v-if="!rows.length">
          <td colspan="5" class="muted center">목록이 없습니다.</td>
        </tr>
        <tr v-for="row in rows" :key="row.id">
          <td>
            <div class="cell-title">{{ row.name }}</div>
            <div class="cell-sub">{{ row.ownerName || '—' }}</div>
          </td>
          <td>{{ row.starRating ? row.starRating + '★' : '-' }}</td>
          <td>{{ row.address }}</td>
          <td>
            <span class="tag" :class="row.active ? 'approved':'rejected'">{{ row.active ? '활성' : '비활성' }}</span>
          </td>
          <td class="right">
            <div class="btns">
              <button class="admin-btn" @click="toggleActive(row)">{{ row.active ? '비활성' : '활성' }}</button>
              <button class="admin-btn primary" @click="goEdit(row.id)">수정</button>
            </div>
          </td>
        </tr>
      </tbody>
    </table>
  </div>
</template>

<style scoped>
.page-head{ margin: 4px 0 12px; }
.page-head h1{ margin:0; font-size:22px; }
.page-head .sub{ color:#6b7280; margin-top:4px; }

.toolbar{ display:flex; align-items:center; justify-content:space-between; gap:8px; margin-bottom:12px; }
.tool-left{ display:flex; align-items:center; gap:8px; flex-wrap:wrap; }
.input{ height:34px; padding:0 10px; border:1px solid var(--line); border-radius:8px; min-width:260px; }
.chk{ display:flex; align-items:center; gap:6px; color:#475569; }
.admin-table{ width:100%; border-collapse:collapse; }
.admin-table th, .admin-table td{ border-bottom:1px solid var(--line); padding:10px 8px; vertical-align:middle; }
.admin-table th{ text-align:left; color:#475569; background:#fafbff; }
.cell-title{ font-weight:700; }
.cell-sub{ color:#6b7280; font-size:12px; margin-top:2px; }
.tag{ display:inline-block; height:24px; padding:0 8px; border-radius:12px; font-size:12px; line-height:24px; background:#eef2ff; color:#1e3a8a; }
.tag.approved{ background:#ecfdf5; color:#065f46; }
.tag.rejected{ background:#fef2f2; color:#991b1b; }
.btns{ display:flex; gap:6px; justify-content:flex-end; }
.right{ text-align:right; }
.center{ text-align:center; }
.muted{ color:#6b7280; }
.error{ color:#b42318; }
</style>
