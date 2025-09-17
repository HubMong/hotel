<script setup>
import { ref, computed, onMounted } from 'vue'
import AdminApi from '@/api/AdminApi'

const loading = ref(true)
const err = ref(null)
const rows = ref([])

const q = ref('')                   // 검색어
const status = ref('PENDING')       // 필터: PENDING | APPROVED | REJECTED

async function fetchList() {
  loading.value = true
  err.value = null
  try {
    // 백엔드에 맞춰 파라미터 전달
    const data = await AdminApi.getBusinesses({ q: q.value, status: status.value })
    rows.value = Array.isArray(data) ? data : (data.items || [])
  } catch (e) {
    console.error(e)
    err.value = '목록을 불러오지 못했습니다.'
  } finally {
    loading.value = false
  }
}

onMounted(fetchList)

const filtered = computed(() => rows.value)

async function onApprove(row) {
  const ok = confirm(`[승인] ${row.companyName} 승인하시겠어요?`)
  if (!ok) return
  await AdminApi.approveBusiness(row.id, '')
  await fetchList()
}

async function onReject(row) {
  const reason = prompt(`[거절] ${row.companyName} 거절 사유를 입력하세요.`) ?? ''
  await AdminApi.rejectBusiness(row.id, reason)
  await fetchList()
}
</script>

<template>
  <div class="page-head">
    <h1>사업자 승인</h1>
    <div class="sub">등록 요청을 검토하여 승인/거절 처리합니다.</div>
  </div>

  <div class="panel toolbar">
    <div class="tool-left">
      <div class="seg">
        <button class="seg-btn" :class="{on: status==='PENDING'}" @click="status='PENDING'; fetchList()">대기</button>
        <button class="seg-btn" :class="{on: status==='APPROVED'}" @click="status='APPROVED'; fetchList()">승인</button>
        <button class="seg-btn" :class="{on: status==='REJECTED'}" @click="status='REJECTED'; fetchList()">거절</button>
      </div>
      <input class="input" v-model.trim="q" placeholder="상호명/대표자/사업자번호 검색" @keydown.enter="fetchList" />
      <button class="admin-btn" @click="fetchList">검색</button>
    </div>
    <div class="tool-right">
      <button class="admin-btn" @click="fetchList">새로고침</button>
    </div>
  </div>

  <div class="panel">
    <div v-if="loading" class="muted">불러오는 중…</div>
    <div v-else-if="err" class="error">{{ err }}</div>

    <table v-else class="admin-table">
      <thead>
        <tr>
          <th style="width: 28%">상호명</th>
          <th style="width: 16%">대표자</th>
          <th style="width: 18%">사업자번호</th>
          <th style="width: 14%">신청일</th>
          <th style="width: 10%">상태</th>
          <th style="width: 14%">작업</th>
        </tr>
      </thead>
      <tbody>
        <tr v-if="!filtered.length">
          <td colspan="6" class="muted center">목록이 없습니다.</td>
        </tr>
        <tr v-for="row in filtered" :key="row.id">
          <td>
            <div class="cell-title">{{ row.companyName }}</div>
            <div class="cell-sub">{{ row.email }} · {{ row.phone }}</div>
          </td>
          <td>{{ row.ownerName }}</td>
          <td>{{ row.companyRegNo }}</td>
          <td>{{ new Date(row.appliedAt || row.createdAt).toLocaleDateString() }}</td>
          <td>
            <span class="tag" :class="row.status?.toLowerCase()">{{ row.status || 'PENDING' }}</span>
          </td>
          <td class="right">
            <div class="btns" v-if="row.status === 'PENDING' || !row.status">
              <button class="admin-btn primary" @click="onApprove(row)">승인</button>
              <button class="admin-btn" @click="onReject(row)">거절</button>
            </div>
            <div v-else class="muted">처리됨</div>
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
.seg{ display:flex; border:1px solid var(--line); border-radius:8px; overflow:hidden; }
.seg-btn{ height:34px; padding:0 12px; background:#fff; border:0; border-right:1px solid var(--line); cursor:pointer; }
.seg-btn:last-child{ border-right:0; }
.seg-btn.on{ background:#eef2ff; color:#1e3a8a; font-weight:700; }
.input{ height:34px; padding:0 10px; border:1px solid var(--line); border-radius:8px; min-width:260px; }

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
