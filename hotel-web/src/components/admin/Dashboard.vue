<script setup>
import { ref, onMounted } from 'vue'
// 필요 시 /api 호출로 교체 가능
const stats = ref({ pendingBusinesses: 0, hotels: 0, reservationsToday: 0 })
onMounted(() => {
  // TODO: http.get('/api/admin/dashboard')
  stats.value = { pendingBusinesses: 3, hotels: 12, reservationsToday: 18 }
})
</script>

<template>
  <h1 class="h4 mb-4">대시보드</h1>

  <div class="row g-3">
    <div class="col-sm-6 col-lg-4">
      <div class="card shadow-sm">
        <div class="card-body">
          <div class="text-muted small">승인 대기 사업자</div>
          <div class="display-6 fw-bold">{{ stats.pendingBusinesses }}</div>
        </div>
      </div>
    </div>
    <div class="col-sm-6 col-lg-4">
      <div class="card shadow-sm">
        <div class="card-body">
          <div class="text-muted small">등록 호텔 수</div>
          <div class="display-6 fw-bold">{{ stats.hotels }}</div>
        </div>
      </div>
    </div>
    <div class="col-sm-6 col-lg-4">
      <div class="card shadow-sm">
        <div class="card-body">
          <div class="text-muted small">오늘 예약</div>
          <div class="display-6 fw-bold">{{ stats.reservationsToday }}</div>
        </div>
      </div>
    </div>
  </div>
</template>
