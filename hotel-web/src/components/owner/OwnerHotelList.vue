<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import OwnerApi from '@/api/OwnerApi'

const router = useRouter()
const loading = ref(true)
const err = ref(null)
const rows = ref([])

async function load(){ 
  loading.value = true; err.value=null
  try { rows.value = await OwnerApi.myHotels() }
  catch(e){ console.error(e); err.value='목록 실패' }
  finally { loading.value=false }
}
onMounted(load)
</script>

<template>
  <div class="page-head">
    <h1>내 호텔</h1>
    <div class="sub">호텔을 등록하거나 수정할 수 있어요.</div>
  </div>

  <div class="panel toolbar">
    <button class="admin-btn primary" @click="$router.push('/owner/hotels/new')">+ 새 호텔 등록</button>
  </div>

  <div class="panel">
    <div v-if="loading" class="muted">불러오는 중…</div>
    <div v-else-if="err" class="error">{{ err }}</div>

    <table v-else class="admin-table">
      <thead>
        <tr>
          <th>호텔</th>
          <th style="width:14%">상태</th>
          <th style="width:16%">작업</th>
        </tr>
      </thead>
      <tbody>
        <tr v-if="!rows.length"><td colspan="3" class="center muted">호텔이 없습니다.</td></tr>
        <tr v-for="h in rows" :key="h.id">
          <td>
            <div class="cell-title">{{ h.name }}</div>
            <div class="cell-sub">{{ h.address }}</div>
          </td>
          <td>
            <span class="tag" :class="(h.status||'').toLowerCase()">{{ h.status }}</span>
          </td>
          <td class="right">
            <div class="btns">
              <button class="admin-btn primary" @click="router.push(`/owner/hotels/${h.id}`)">수정</button>
            </div>
          </td>
        </tr>
      </tbody>
    </table>
  </div>
</template>
