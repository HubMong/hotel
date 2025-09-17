<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import OwnerApi from '@/api/OwnerApi'

const router = useRouter()
const loading = ref(true)
const err = ref(null)
const hotels = ref([])

onMounted(async () => {
  try {
    hotels.value = await OwnerApi.myHotels()
  } catch (e) {
    console.error(e)
    err.value = '대시보드 데이터를 불러오지 못했습니다.'
  } finally {
    loading.value = false
  }
})

const totalHotels   = computed(() => hotels.value.length)
const approvedCount = computed(() => hotels.value.filter(h => h.status === 'APPROVED').length)
const pendingCount  = computed(() => hotels.value.filter(h => h.status === 'PENDING').length)

// TODO: 백엔드 준비되면 교체
const todayReservations = ref(0)
const monthRevenueKRW   = ref(0)

function goNewHotel(){ router.push('/owner/hotels/new') }
function goMyHotels(){ router.push('/owner/hotels') }
</script>

<template>
  <div class="page-head">
    <h1>대시보드</h1>
    <div class="sub">내 호텔 현황과 빠른 작업을 한 곳에서 확인하세요.</div>
  </div>

  <div class="cards-grid">
    <div class="card">
      <div class="card-title">전체 호텔</div>
      <div class="card-value">{{ totalHotels }}</div>
    </div>
    <div class="card">
      <div class="card-title">승인 완료</div>
      <div class="card-value">{{ approvedCount }}</div>
    </div>
    <div class="card">
      <div class="card-title">승인 대기</div>
      <div class="card-value">{{ pendingCount }}</div>
    </div>
    <div class="card">
      <div class="card-title">오늘 예약 수</div>
      <div class="card-value">{{ todayReservations }}</div>
    </div>
    <div class="card">
      <div class="card-title">이번 달 매출(₩)</div>
      <div class="card-value">{{ monthRevenueKRW.toLocaleString('ko-KR') }}</div>
    </div>
  </div>

  <div class="panel toolbar">
    <button class="admin-btn primary" @click="goNewHotel">+ 새 호텔 등록</button>
    <button class="admin-btn" @click="goMyHotels">내 호텔 관리</button>
  </div>

  <div class="panel">
    <div class="panel-title">최근 등록한 호텔</div>

    <div v-if="loading" class="muted">불러오는 중…</div>
    <div v-else-if="err" class="error">{{ err }}</div>

    <table v-else class="admin-table">
      <thead>
        <tr>
          <th>호텔</th>
          <th style="width:18%">상태</th>
          <th style="width:18%">작업</th>
        </tr>
      </thead>
      <tbody>
        <tr v-if="!hotels.length">
          <td colspan="3" class="center muted">등록된 호텔이 없습니다. “새 호텔 등록”을 눌러 시작하세요.</td>
        </tr>
        <tr v-for="h in hotels.slice(0,5)" :key="h.id">
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

  <div v-if="pendingCount" class="panel info">
    <div class="panel-title">승인 대기 안내</div>
    <p class="muted">승인 대기 중인 호텔은 공개되지 않으며, 관리자의 승인이 완료되면 자동으로 노출됩니다.</p>
  </div>
</template>
