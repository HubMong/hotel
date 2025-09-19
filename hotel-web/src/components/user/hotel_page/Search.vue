<!-- src/views/Search.vue -->
<template>
  <div class="search-page">
    <!-- 상단 고정: 검색 전용 바 -->
    <SearchBarCompact />

    <div class="search-layout">
      <!-- 좌측 필터 -->
      <aside class="filter-sidebar">
        <h3>상세 조건</h3>

        <div class="filter-group">
          <h4>1박 요금</h4>
          <div class="price-range">
            <span>{{ minPrice.toLocaleString() }}원</span>
            <span>{{ maxPrice.toLocaleString() }}원</span>
          </div>
          <input
            type="range"
            min="0"
            max="1000000"
            step="10000"
            v-model.number="maxPrice"
            class="price-slider"
          />
        </div>

        <div class="filter-group">
          <h4>숙박 시설 등급</h4>
          <div v-for="type in accommodationTypes" :key="type" class="checkbox-item">
            <input type="checkbox" :id="type" :value="type" v-model="selectedTypes" />
            <label :for="type">{{ type }}</label>
          </div>
        </div>

        <div class="filter-group">
          <h4>편의 시설</h4>
          <div class="hint">* 백엔드 연동 전이므로 표시용</div>
          <div v-for="amenity in amenitiesList" :key="amenity" class="checkbox-item">
            <input type="checkbox" :id="amenity" :value="amenity" v-model="selectedAmenities" />
            <label :for="amenity">{{ amenity }}</label>
          </div>
        </div>
      </aside>

      <!-- 우측 결과 -->
      <main class="main-content">
        <div class="search-results-header">
          <h2 class="page-title">호텔 검색 결과</h2>
        </div>

        <div class="results-container">
          <div v-if="isLoading" class="loading">불러오는 중…</div>
          <div v-else-if="loadError" class="error">{{ loadError }}</div>

          <template v-else>
            <div v-if="filteredResults.length > 0" class="hotel-list">
              <p class="results-count">
                <strong>{{ filteredResults.length }}개</strong>의 검색 결과
              </p>

              <RouterLink
                v-for="hotel in filteredResults"
                :key="hotel.id"
                :to="`/hotels/${hotel.id}`"
                class="hotel-card-link"
              >
                <article class="hotel-card">
                  <img :src="thumbOf(hotel.id)" :alt="hotel.name" class="hotel-image" />

                  <div class="hotel-details">
                    <span class="hotel-rating">{{ hotel.rating || '등급 미표기' }}</span>
                    <h4 class="hotel-name">{{ hotel.name }}</h4>
                    <p class="hotel-city">{{ hotel.city }}</p>
                  </div>

                  <div class="hotel-price-block">
                    <span class="price">
                      {{ (hotel.lowestPrice ?? 0).toLocaleString() }}원
                    </span>
                    <p class="per-night">1박 최저가</p>
                  </div>
                </article>
              </RouterLink>
            </div>

            <div v-else class="no-results">
              <p>조건에 맞는 호텔이 없습니다.</p>
            </div>
          </template>
        </div>
      </main>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, watch, onMounted } from 'vue'
import { RouterLink, useRoute } from 'vue-router'
import SearchBarCompact from '@/components/user/hotel_page/SearchBarCompact.vue'
import http from '@/api/http'

import hotel1Image from '@/images/hotel1.png'
import hotel2Image from '@/images/hotel2.png'
import hotel3Image from '@/images/hotel3.png'
import hotel4Image from '@/images/hotel4.png'
import hotel5Image from '@/images/hotel5.png'

const route = useRoute()

// 서버 결과
const isLoading = ref(false)
const loadError = ref(null)
const results = ref([])

// 사이드바 로컬 필터 상태
const amenitiesList = ref(['무료 Wi-Fi', '주차 가능', '수영장', '피트니스 센터'])
const selectedAmenities = ref([])
const accommodationTypes = ref(['5성급(*****)', '4성급(****)', '3성급(***)', '2성급(**)', '1성급(*)'])
const selectedTypes = ref([])
const minPrice = ref(0)
const maxPrice = ref(500000)

// 쿼리 객체를 문자열로만 구성 (URLSearchParams 안전하게)
function buildQueryFromRoute() {
  const qObj = {}
  for (const [k, v] of Object.entries(route.query)) {
    if (v !== undefined && v !== null && v !== '') qObj[k] = String(v)
  }
  return qObj
}

// API 호출
async function fetchResults () {
  isLoading.value = true
  loadError.value = null
  try {
    const params = buildQueryFromRoute()

    // ✅ http는 axios 래퍼라고 가정: response.data 사용
    const { data } = await http.get('/hotels', { params }) // baseURL이 /api 라면 '/hotels'로 충분
    // 백엔드가 Page를 준다면 data.content, 배열이면 data
    const list = Array.isArray(data) ? data : (Array.isArray(data?.content) ? data.content : [])
    results.value = list

    // 가격 슬라이더 상한 보정
    const prices = results.value.map(r => r.lowestPrice ?? 0)
    const maxInResult = prices.length ? Math.max(...prices) : 500000
    maxPrice.value = Math.max(500000, maxInResult)
  } catch (e) {
    console.error('검색 API 실패:', e)
    loadError.value = '검색 결과를 불러오지 못했어요.'
    results.value = []
  } finally {
    isLoading.value = false
  }
}

onMounted(fetchResults)
// 라우터 쿼리 바뀔 때마다 재검색
watch(() => route.query, fetchResults, { deep: true })

// 필터 적용
const filteredResults = computed(() => {
  return results.value.filter(h => {
    const p = h.lowestPrice ?? 0
    const priceOK = p >= minPrice.value && p <= maxPrice.value

    // rating이 숫자라면 타입 맞춰 비교하거나, 표시용 텍스트로 매핑 필요
    const typeOK =
      selectedTypes.value.length === 0
        ? true
        : selectedTypes.value.includes(h.rating)

    const amenityOK = true // TODO: 백엔드 연동 시 수정
    return priceOK && typeOK && amenityOK
  })
})

// 썸네일 선택기
function thumbOf (id) {
  const map = { 1: hotel1Image, 2: hotel2Image, 3: hotel3Image, 4: hotel4Image, 5: hotel5Image }
  return map[id] || `https://picsum.photos/seed/hotel${id}/400/300`
}
</script>


<style scoped>
.search-page { background: var(--bg, #fff); }

/* 레이아웃 */
.search-layout {
  display: flex;
  gap: 48px;
  padding: 12px 120px 60px;
}

/* 사이드바 */
.filter-sidebar {
  flex-basis: 260px;
  flex-shrink: 0;
  border-right: 1px solid var(--line, #eee);
  padding-right: 32px;
}
.filter-group { margin-bottom: 2rem; }
.filter-group h4 { margin-bottom: 1rem; font-size: 16px; color: var(--ink-light, #666); }
.hint { font-size: 12px; color: #9aa0a6; margin-bottom: 8px; }
.checkbox-item { display: flex; align-items: center; margin-bottom: 8px; }
.checkbox-item input { margin-right: 8px; width: 16px; height: 16px; }

/* 우측 본문 */
.main-content { flex: 1; overflow-x: hidden; }
.page-title { font-size: 28px; margin: 12px 0 24px; }
.results-count { margin-bottom: 16px; color: var(--ink, #222); }

.hotel-list { display: flex; flex-direction: column; gap: 16px; }
.hotel-card-link { text-decoration: none; color: inherit; }
.hotel-card {
  display: flex; gap: 20px; align-items: center;
  border: 1px solid var(--line, #eee);
  border-radius: 8px; padding: 16px 20px; background: #fff;
  box-shadow: 0 2px 8px rgba(0,0,0,.04); transition: .2s ease;
}
.hotel-card:hover { border-color: #39c5a0; box-shadow: 0 6px 18px rgba(0,0,0,.08); transform: translateY(-2px); }

.hotel-image { width: 200px; height: 160px; object-fit: cover; border-radius: 6px; flex-shrink: 0; }
.hotel-details { flex: 1; }
.hotel-rating { font-size: 12px; color: #666; display: block; margin-bottom: 4px; }
.hotel-name { font-size: 18px; font-weight: 700; margin: 0 0 4px; }
.hotel-city { font-size: 14px; color: #777; }

.hotel-price-block { text-align: right; }
.hotel-price-block .price { font-size: 22px; font-weight: 800; color: #39c5a0; }
.hotel-price-block .per-night { font-size: 13px; color: #888; margin-top: 4px; }

.price-range { display: flex; justify-content: space-between; margin-bottom: 8px; font-size: 14px; color: #666; }
.price-slider { width: 100%; cursor: pointer; }

.loading, .error, .no-results { padding: 24px 8px; color: #666; }

/* 반응형 */
@media (max-width: 992px) {
  .search-layout { flex-direction: column; gap: 24px; padding: 12px 20px 40px; }
  .filter-sidebar {
    border-right: none; border-bottom: 1px solid var(--line, #eee);
    padding-right: 0; padding-bottom: 20px; margin-bottom: 8px;
  }
  .hotel-card { flex-direction: column; align-items: flex-start; }
  .hotel-image { width: 100%; height: 200px; }
  .hotel-price-block { width: 100%; text-align: left; }
}
</style>
