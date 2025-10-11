<!-- src/pages/SearchPage.vue -->
<template>
  <div class="search-page">
    <SearchBarCompact />

    <div class="search-layout">
      <!-- =======================
           사이드바(필터)
           ======================= -->
      <aside class="filter-sidebar">
        <h3>상세 조건</h3>

        <!-- 1) 가격 -->
        <div class="filter-group">
          <h4>1박 요금</h4>
          <div class="price-range">
            <span>{{ minPrice.toLocaleString() }}원</span>
            <span>{{ maxPrice.toLocaleString() }}원</span>
          </div>
          <input
            type="range"
            min="0"
            :max="sliderMax"
            step="10000"
            v-model.number="maxPrice"
            class="price-slider"
          />
          <div class="hint">슬라이더로 최대 한도를 조정하세요.</div>
        </div>

        <!-- 2) 성급 -->
        <div class="filter-group">
          <h4>숙박 시설 등급</h4>
          <div class="stars-wrap">
            <label
              v-for="opt in starOptions"
              :key="opt.n"
              class="star-chip"
              :class="{ on: selectedTypes.includes(opt.label) }"
            >
              <input type="checkbox" class="sr-only" :value="opt.label" v-model="selectedTypes" />
              <span class="star-ic" aria-hidden="true">{{ starIcon(opt.n) }}</span>
              <span class="star-txt">{{ opt.n }}성급</span>
            </label>
          </div>
          <div class="hint">여러 성급을 함께 선택할 수 있어요.</div>
        </div>

        <!-- 3) 편의시설 -->
        <div class="filter-group">
          <h4>편의 시설</h4>
          <div class="hint">선택한 항목을 모두 갖춘 숙소만 표시됩니다.</div>
          <div class="amen-list">
            <label v-for="amenity in amenityOptions" :key="amenity" class="chk">
              <input type="checkbox" :value="amenity" v-model="selectedAmenities" />
              <span class="ic">{{ iconOf(amenity) }}</span>
              <span>{{ amenity }}</span>
            </label>
          </div>
        </div>
      </aside>

      <!-- =======================
           본문(결과)
           ======================= -->
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
                class="hotel-card-link"
                :to="{ path: `/hotels/${hotel.id}`, query: keepQuery() }"
              >
                <article class="hotel-card">
                  <img
                    :src="hotel.coverImage || thumbOf(hotel.id)"
                    :alt="hotel.name"
                    class="hotel-image"
                  />
                  <div class="hotel-details">
                    <span class="hotel-rating">
                      {{ hotel.rating || '등급 미표기' }}
                    </span>
                    <h4 class="hotel-name">{{ hotel.name }}</h4>
                    <p class="hotel-city">{{ hotel.city || '지역 정보 없음' }}</p>

                    <!-- (선택) 대표 편의 몇 개만 프리뷰 -->
                    <div v-if="hotel.amenKeys?.length" class="amen-preview">
                      <span
                        v-for="(k, i) in hotel.amenKeys.slice(0, 3)"
                        :key="k + i"
                        class="amen-tag"
                      >
                        {{ labelOfKey(k) }}
                      </span>
                      <span v-if="hotel.amenKeys.length > 3" class="amen-more">
                        +{{ hotel.amenKeys.length - 3 }}
                      </span>
                    </div>
                  </div>
                  <div class="hotel-price-block">
                    <span class="price">
                      {{
                        hotel.lowestPrice == null
                          ? '가격 정보 없음'
                          : `${hotel.lowestPrice.toLocaleString()}원`
                      }}
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

const isLoading = ref(false)
const loadError = ref(null)
const results = ref([])

/* =======================
   편의시설 정의 + 유사어
   ======================= */
const AMEN_DEF = [
  { key:'wifi',            label:'무료 Wi-Fi',            ic:'📶', syn:['무료 Wi-Fi','와이파이','wifi'] },
  { key:'parking',         label:'주차',                  ic:'🅿️', syn:['주차','주차 가능','parking'] },
  { key:'pool',            label:'수영장',                ic:'🏊', syn:['수영장','pool'] },
  { key:'fitness',         label:'피트니스',              ic:'🏋️', syn:['피트니스','피트니스 센터','헬스장','gym'] },
  { key:'spa',             label:'스파',                  ic:'💆', syn:['스파','spa'] },
  { key:'breakfast',       label:'조식',                  ic:'🥐', syn:['조식','아침식사','breakfast'] },
  { key:'frontdesk24',     label:'24시간 프런트 데스크',   ic:'🕘', syn:['24시간 프런트 데스크','24시간 체크인','checkin_24h'] },
  { key:'tour',            label:'투어',                  ic:'🗺️', syn:['투어','tour'] },
  { key:'airport_shuttle', label:'공항 이동 서비스',       ic:'🚌', syn:['공항 이동 서비스','공항 셔틀','공항 픽업','셔틀','shuttle','airport'] },
  { key:'laundry',         label:'세탁',                  ic:'🧺', syn:['세탁','laundry'] },
  { key:'luggage',         label:'여행 가방 보관',         ic:'🧳', syn:['여행 가방 보관','수하물 보관','luggage'] },
  { key:'taxi',            label:'택시 서비스',            ic:'🚕', syn:['택시 서비스','택시','taxi'] },
]
const norm = s => (s ?? '').toString().normalize('NFKC').replace(/\s+/g,'').replace(/[-_]/g,'').toLowerCase()
const AMEN_SYNONYM = (() => {
  const m = new Map()
  for (const a of AMEN_DEF) {
    m.set(norm(a.label), a.key)
    for (const s of a.syn) m.set(norm(s), a.key)
  }
  return m
})()
const amenLabelToKey = (s) => AMEN_SYNONYM.get(norm(s)) || null
const labelOfKey = (k) => AMEN_DEF.find(a => a.key === k)?.label ?? k
const iconOf = (label) => {
  const key = amenLabelToKey(label)
  return AMEN_DEF.find(a => a.key === key)?.ic ?? '•'
}

/* 사이드바 표시용 옵션/선택값 */
const amenityOptions = ref(AMEN_DEF.map(a => a.label))
const selectedAmenities = ref([])

/* 성급 옵션 (스타칩) */
function starToLabel(n) {
  const nInt = Number(n ?? 0)
  if (nInt < 1 || nInt > 5) return null
  return `${nInt}성급(${('*'.repeat(nInt)).padEnd(5,' ')})`.replace(/ +$/,'')
}
const starOptions = ref([5,4,3,2,1].map(n => ({ n, label: starToLabel(n) })))
const starIcon = n => '★'.repeat(n) + '☆'.repeat(5-n)
const selectedTypes = ref([])

/* 가격 */
const minPrice = ref(0)
const maxPrice = ref(500000)
const sliderMax = ref(1000000)

/** 상세 페이지 이동 시에도 0 값은 제거해서 전달 */
const keepQuery = () => {
  const { checkIn, checkOut, adults, children, q } = route.query
  const out = {}
  if (q != null && String(q).trim() !== '') out.q = String(q).trim()

  const ymd = s => typeof s === 'string' && /^\d{4}-\d{2}-\d{2}$/.test(s)
  if (ymd(checkIn))  out.checkIn  = checkIn
  if (ymd(checkOut)) out.checkOut = checkOut

  const toNum = v => Number(String(v).replace(/[^\d.-]/g, ''))
  const ad = toNum(adults)
  const ch = toNum(children)
  if (Number.isFinite(ad) && ad >= 1) out.adults = String(ad)
  if (Number.isFinite(ch) && ch >  0) out.children = String(ch)

  return out
}

/** API로 보낼 파라미터 구성 */
function buildQueryFromRoute() {
  const qObj = {}
  const q = route.query || {}

  if (q.q != null && String(q.q).trim() !== '') qObj.q = String(q.q).trim()

  const ymd = s => typeof s === 'string' && /^\d{4}-\d{2}-\d{2}$/.test(s)
  if (ymd(q.checkIn))  qObj.checkIn  = q.checkIn
  if (ymd(q.checkOut)) qObj.checkOut = q.checkOut

  const toNum = v => Number(String(v).replace(/[^\d.-]/g, ''))
  const ad = toNum(q.adults)
  const ch = toNum(q.children)
  if (Number.isFinite(ad) && ad >= 1) qObj.adults   = ad
  if (Number.isFinite(ch) && ch >  0) qObj.children = ch

  // (선택) 서버가 지원하면 편의시설 키 CSV로 전달
  const amenKeys = selectedAmenities.value.map(amenLabelToKey).filter(Boolean)
  if (amenKeys.length) qObj.amenities = amenKeys.join(',')

  return qObj
}

const qRaw = computed(() => String(route.query.q ?? '').trim().replace(/\s+/g, ' '))

function extractCity(address) {
  if (!address) return null
  const parts = address.split(/\s+/)
  return parts.length >= 2 ? `${parts[0]} ${parts[1]}` : parts[0]
}

async function fetchResults () {
  isLoading.value = true
  loadError.value = null
  results.value = []

  try {
    const params = buildQueryFromRoute()
    // ⚠️ 앞 슬래시 금지 (baseURL=/api)
    const { data } = await http.get('hotels', { params })

    const pageLike = (data && typeof data === 'object') ? (data.data ?? data) : data
    const list =
      Array.isArray(pageLike) ? pageLike :
      Array.isArray(pageLike?.content) ? pageLike.content :
      Array.isArray(pageLike?.items) ? pageLike.items :
      Array.isArray(pageLike?.results) ? pageLike.results :
      []

    results.value = list.map(h => {
      // 백엔드 라벨(left/right) → 표준 amenKeys 로 평탄화
      const leftLabels  = Array.isArray(h?.amenities?.left)  ? h.amenities.left  : []
      const rightLabels = Array.isArray(h?.amenities?.right) ? h.amenities.right : []
      const fromKeys    = Array.isArray(h?.amenityKeys) ? h.amenityKeys : []
      const amenKeys = Array.from(new Set([
        ...fromKeys.filter(Boolean),
        ...leftLabels.map(amenLabelToKey).filter(Boolean),
        ...rightLabels.map(amenLabelToKey).filter(Boolean),
      ]))

      return {
        id: h.id,
        name: h.name,
        address: h.address ?? '',
        city: h.city ?? extractCity(h.address),
        rating: h.rating ?? h.ratingLabel ?? starToLabel(h.starRating),
        lowestPrice: (h.lowestPrice != null && !Number.isNaN(+h.lowestPrice)) ? +h.lowestPrice : null,
        coverImage: h.coverImage ?? null,
        amenKeys, // ← 필터 비교에 사용
      }
    })

    const prices = results.value.map(r => r.lowestPrice).filter(v => typeof v === 'number' && v >= 0)
    const maxInResult = prices.length ? Math.max(...prices) : 0
    sliderMax.value = Math.max(1_000_000, maxInResult)
    maxPrice.value  = sliderMax.value
  } catch (e) {
    console.error('검색 API 실패:', e)
    loadError.value = '검색 결과를 불러오지 못했어요.'
    results.value = []
  } finally {
    isLoading.value = false
  }
}

onMounted(fetchResults)
watch(() => route.query, fetchResults, { deep: true })
// 서버 필터가 있다면 주석 해제해서 재조회
// watch(selectedAmenities, fetchResults)
// watch(selectedTypes, fetchResults)

const normalize = s => (s ?? '').toString().normalize('NFKC').toLowerCase().trim()

const filteredResults = computed(() => {
  const q = normalize(qRaw.value)
  const wantedAmenKeys = selectedAmenities.value.map(amenLabelToKey).filter(Boolean)

  return results.value.filter(h => {
    const priceVal = (typeof h.lowestPrice === 'number') ? h.lowestPrice : minPrice.value
    const priceOK = priceVal >= minPrice.value && priceVal <= maxPrice.value

    const typeOK = selectedTypes.value.length === 0 ? true : selectedTypes.value.includes(h.rating)

    const hay = normalize(`${h.name} ${h.address} ${h.city ?? ''}`)
    const textOK = !q || hay.includes(q)

    const amenOK = wantedAmenKeys.length === 0
      ? true
      : wantedAmenKeys.every(k => h.amenKeys?.includes(k))

    return priceOK && typeOK && textOK && amenOK
  })
})

/* 디버깅 헬퍼 */
watch(results, r => console.log('[SearchPage] results len =', r.length, r.slice(0, 3)))
watch(filteredResults, fr => console.log('[SearchPage] filtered len =', fr.length, fr.slice(0, 3)))
watch([minPrice, maxPrice], ([lo, hi]) => console.log('[SearchPage] price filter =', lo, ' ~ ', hi))
watch(sliderMax, v => console.log('[SearchPage] sliderMax ->', v))

onMounted(() => {
  window.__searchDebug = {
    results, filteredResults, minPrice, maxPrice, sliderMax, selectedTypes,
    selectedAmenities, amenLabelToKey,
    dump() {
      const rows = (results.value || []).map(h => ({
        id: h.id, name: h.name, lowestPrice: h.lowestPrice, rating: h.rating, cover: !!h.coverImage, amenKeys: h.amenKeys
      }))
      console.table(rows)
    },
    showAmen() {
      (results.value || []).forEach(h => console.log(h.id, h.name, h.amenKeys))
    },
    setPriceRange(lo, hi) { minPrice.value = +lo || 0; maxPrice.value = +hi || sliderMax.value },
    clearFilters() { selectedTypes.value = []; selectedAmenities.value = []; minPrice.value = 0; maxPrice.value = sliderMax.value },
  }
  console.log('🔎 window.__searchDebug 준비됨')
})

function thumbOf (id) {
  const map = { 1: hotel1Image, 2: hotel2Image, 3: hotel3Image, 4: hotel4Image, 5: hotel5Image }
  return map[id] || `https://picsum.photos/seed/hotel${id}/400/300`
}
</script>

<style scoped>
/* 레이아웃 기본 */
.search-page { background: var(--bg, #fff); }
.search-layout { display: flex; gap: 48px; padding: 12px 120px 60px; }
.filter-sidebar { flex-basis: 280px; flex-shrink: 0; border-right: 1px solid var(--line, #eee); padding-right: 32px; }
.main-content { flex: 1; overflow-x: hidden; }
.page-title { font-size: 28px; margin: 12px 0 24px; }

/* 필터 공통 */
.filter-group { margin-bottom: 2rem; }
.filter-group h4 { margin-bottom: 10px; font-size: 16px; color: var(--ink-light, #666); }
.hint { font-size: 12px; color: #9aa0a6; margin-top: 6px; }

/* 가격 */
.price-range { display: flex; justify-content: space-between; margin-bottom: 8px; font-size: 14px; color: #666; }
.price-slider { width: 100%; cursor: pointer; }

/* 성급(칩) */
.stars-wrap { display: flex; flex-wrap: wrap; gap: 8px; }
.star-chip{
  display:inline-flex; align-items:center; gap:8px; padding:8px 10px;
  border:1px solid #e5e7eb; border-radius:999px; background:#fff; cursor:pointer; user-select:none;
  font-weight:700; color:#374151; transition:.15s ease;
}
.star-chip:hover{ box-shadow:0 4px 12px rgba(0,0,0,.06); transform: translateY(-1px); }
.star-chip.on{ border-color:#39c5a0; background: #eafff7; color:#065f46; }
.star-chip .star-ic{ font-size:14px; line-height:1; }
.star-chip .star-txt{ font-size:13px; }

/* 편의시설 체크 */
.amen-list{ display:grid; grid-template-columns: 1fr; gap:6px; }
.chk{ display:flex; align-items:center; gap:8px; font-size:14px; }
.chk input{ width:16px; height:16px; }
.chk .ic{ width:18px; text-align:center; }

/* 결과 카드 */
.results-count { margin-bottom: 16px; color: var(--ink, #222); }
.hotel-list { display: flex; flex-direction: column; gap: 16px; }
.hotel-card-link { text-decoration: none; color: inherit; }
.hotel-card { display: flex; gap: 20px; align-items: center; border: 1px solid var(--line, #eee); border-radius: 12px; padding: 16px 20px; background: #fff; box-shadow: 0 2px 8px rgba(0,0,0,.04); transition: .2s ease; }
.hotel-card:hover { border-color: #39c5a0; box-shadow: 0 6px 18px rgba(0,0,0,.08); transform: translateY(-2px); }
.hotel-image { width: 200px; height: 160px; object-fit: cover; border-radius: 8px; flex-shrink: 0; }
.hotel-details { flex: 1; min-width: 0; }
.hotel-rating { font-size: 12px; color: #666; display: block; margin-bottom: 4px; }
.hotel-name { font-size: 18px; font-weight: 800; margin: 0 0 4px; }
.hotel-city { font-size: 14px; color: #777; }

.amen-preview{ margin-top:8px; display:flex; flex-wrap:wrap; gap:6px; }
.amen-tag{
  font-size:12px; color:#065f46; background:#eafff7; border:1px solid #a7f3d0;
  border-radius:999px; padding:2px 8px; font-weight:700;
}
.amen-more{ font-size:12px; color:#6b7280; }

.hotel-price-block { text-align: right; }
.hotel-price-block .price { font-size: 22px; font-weight: 800; color: #39c5a0; }
.hotel-price-block .per-night { font-size: 13px; color: #888; margin-top: 4px; }

.loading, .error, .no-results { padding: 24px 8px; color: #666; }

/* 반응형 */
@media (max-width: 992px) {
  .search-layout { flex-direction: column; gap: 24px; padding: 12px 20px 40px; }
  .filter-sidebar { border-right: none; border-bottom: 1px solid var(--line, #eee); padding-right: 0; padding-bottom: 20px; margin-bottom: 8px; }
  .hotel-card { flex-direction: column; align-items: flex-start; }
  .hotel-image { width: 100%; height: 200px; }
  .hotel-price-block { width: 100%; text-align: left; }
}
</style>
