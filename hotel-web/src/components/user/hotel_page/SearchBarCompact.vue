<template>
  <div class="searchbar-wrap">
    <div class="searchbar">
      <!-- 목적지 -->
      <div class="cell cell--search">
        <input
          v-model="q"
          class="input"
          type="text"
          placeholder="도시 / 호텔 이름"
          @keyup.enter="emitSearch"
        />
      </div>

      <!-- 체크인 -->
      <button class="cell cell--date" type="button" @click="openCalendar">
        <div class="date-text">
          <div class="date-main">{{ displayCheckIn.main }}</div>
          <div class="date-sub">{{ displayCheckIn.sub }}</div>
        </div>
      </button>

      <!-- 체크아웃 -->
      <button class="cell cell--date" type="button" @click="openCalendar">
        <div class="date-text">
          <div class="date-main">{{ displayCheckOut.main }}</div>
          <div class="date-sub">{{ displayCheckOut.sub }}</div>
        </div>
      </button>

      <!-- 인원 -->
      <div class="cell cell--guest">
        <button class="guest-button" type="button" @click="toggleTravelerMenu">
          <div class="guest-summary">
            <div class="guest-main">성인 {{ adults }}명<span v-if="children > 0"> · 아동 {{ children }}명</span></div>
            <div class="guest-sub">인원 선택</div>
          </div>
        </button>

        <div v-if="showTravelerMenu" class="traveler-menu">
          <div class="traveler-item">
            <span>성인</span>
            <div class="counter">
              <button @click="dec('adults')" :disabled="adults <= 1">−</button>
              <span>{{ adults }}</span>
              <button @click="inc('adults')">＋</button>
            </div>
          </div>
          <div class="traveler-item">
            <span>아동</span>
            <div class="counter">
              <button @click="dec('children')" :disabled="children <= 0">−</button>
              <span>{{ children }}</span>
              <button @click="inc('children')">＋</button>
            </div>
          </div>
          <div class="traveler-actions"><button class="ok" @click="closeTravelerMenu">확인</button></div>
        </div>
      </div>

      <!-- 검색 버튼 -->
      <button class="cell btn" @click="emitSearch">검색하기</button>
    </div>

    <!-- 실제 캘린더(range) -->
    <FlatPickr
      ref="rangePicker"
      v-model="dateRange"
      :config="dateRangeConfig"
      class="hidden-picker"
    />
  </div>
</template>

<script setup>
import { ref, computed, watch, onMounted, onBeforeUnmount, nextTick, getCurrentInstance } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import FlatPickr from 'vue-flatpickr-component'
import { Korean } from 'flatpickr/dist/l10n/ko.js'

const route = useRoute()
const router = useRouter()

// 상태
const q = ref('')
const adults = ref(1)
const children = ref(0)
const dateRange = ref([])     // [Date, Date]
const rangePicker = ref(null)
const showTravelerMenu = ref(false)

// 마지막 정상값 기억(비상용)
const lastGood = ref({ ci: null, co: null })
// 라우터 push 직후 watcher 1회 무시
let justPushed = false

// ---------- utils
const toDate = (s) => {
  if (!s) return null
  const dt = new Date(s)
  return Number.isNaN(dt.getTime()) ? null : dt
}
const toYmd = (d) => {
  if (!d) return undefined
  const dt = d instanceof Date ? d : new Date(d)
  if (Number.isNaN(dt.getTime())) return undefined
  const y = dt.getFullYear()
  // 이상한 년도(스크롤/키보드로 2001 등) 들어오면 버림
  if (y < 2015 || y > 2035) return undefined
  const mm = String(dt.getMonth()+1).padStart(2,'0')
  const dd = String(dt.getDate()).padStart(2,'0')
  return `${y}-${mm}-${dd}`
}
const weekdayKo = (d) => {
  if (!d) return ''
  const names = ['일요일','월요일','화요일','수요일','목요일','금요일','토요일']
  const dt = d instanceof Date ? d : new Date(d)
  return names[dt.getDay()]
}
const disp = (d) => {
  if (!d) return { main: '날짜 선택', sub: '' }
  const dt = d instanceof Date ? d : new Date(d)
  return { main: `${dt.getFullYear()}년 ${dt.getMonth()+1}월 ${dt.getDate()}일`, sub: weekdayKo(dt) }
}
const displayCheckIn  = computed(() => disp(dateRange.value?.[0]))
const displayCheckOut = computed(() => disp(dateRange.value?.[1]))

// ---------- flatpickr config
const dateRangeConfig = {
  mode: 'range',
  showMonths: 2,
  altInput: false,
  dateFormat: 'Y-m-d',
  minDate: 'today',
  maxDate: new Date(2026, 11, 31),
  locale: Korean,
  static: true,
  disableMobile: true,
  clickOpens: true,
  allowInput: false,

  onReady: (_sd, _str, instance) => {
    nextTick(() => {
      const cal = instance.calendarContainer
      if (cal) {
        cal.querySelectorAll('.numInputWrapper, .numInput, .arrowUp, .arrowDown')
          .forEach(el => el.style.display = 'none')
      }
    })
  },

  onOpen: (_sd, _str, instance) => {
    const ci = dateRange.value?.[0]
    instance.jumpToDate(ci || new Date())
  },

  onChange: (sd) => {
    // flatpickr에서 직접 수정 시 lastGood 갱신
    if (sd?.length) {
      const ci = sd[0]
      let co = sd[1]
      if (ci && co && co <= ci) {
        co = new Date(ci); co.setDate(co.getDate() + 1)
      }
      lastGood.value = { ci: ci || null, co: co || null }
    }
    console.log('[SearchBar FP onChange] selected =', sd.map(d => toYmd(d)))
  },

  onClose: (sd, _str, instance) => {
    if (sd.length === 2) {
      const [ci, co] = sd
      if (!co || co <= ci) {
        const next = new Date(ci); next.setDate(next.getDate() + 1)
        instance.setDate([ci, next], true)
      }
    }
  },
}

// ---------- 라우터 → 상태 동기화
function setRange(ci, co, reason='route') {
  // 이상한 년도 방지
  const y1 = ci?.getFullYear?.()
  const y2 = co?.getFullYear?.()
  if ((y1 && (y1 < 2015 || y1 > 2035)) || (y2 && (y2 < 2015 || y2 > 2035))) {
    console.warn(`[SearchBar] ignored out-of-range year (${y1}, ${y2}) from ${reason}`)
    return
  }

  if (ci && co) {
    dateRange.value = [ci, co]
    lastGood.value = { ci, co }
    const fp = rangePicker.value?._flatpickr || rangePicker.value?.fp
    if (fp) { fp.setDate([ci, co], false); fp.jumpToDate(ci) }
  } else if (!ci && !co) {
    // 완전 초기화 요청이 아니면 유지
    // 여기서는 route가 비어오더라도 기존 선택을 유지하도록 그냥 return
    return
  }
}

function syncFromRoute() {
  if (justPushed) {            // 방금 내 코드가 push한 반영 1회 무시
    justPushed = false
    return
  }

  q.value        = route.query.q ?? ''
  adults.value   = route.query.adults ? Number(route.query.adults) : 1
  children.value = route.query.children ? Number(route.query.children) : 0

  const ci = toDate(route.query.checkIn)
  const co = toDate(route.query.checkOut)

  // 라우터가 정상 범위를 주면 적용, 아니면 기존 유지
  if (ci && co) setRange(ci, co, 'route')

  console.log('[SearchBar] route.query =', route.query)
}
watch(() => route.fullPath, syncFromRoute, { immediate: true })

// ---------- 외부 클릭으로 드롭다운 닫기
function onDocClick(e){
  const menu = document.querySelector('.traveler-menu')
  const btn  = document.querySelector('.guest-button')
  if (!menu || !btn) return
  if (!menu.contains(e.target) && !btn.contains(e.target)) showTravelerMenu.value = false
}
onMounted(()=>{
  document.addEventListener('click', onDocClick)
  const inst = getCurrentInstance()
  console.log('[SearchBar] mounted uid=', inst?.uid)
})
onBeforeUnmount(()=> document.removeEventListener('click', onDocClick))

function openCalendar () {
  const fp = rangePicker.value?._flatpickr || rangePicker.value?.fp
  if (fp) fp.open()
}
function toggleTravelerMenu(){ showTravelerMenu.value = !showTravelerMenu.value }
function closeTravelerMenu(){ showTravelerMenu.value = false }
function inc(k){ if (k==='adults') adults.value++; else children.value++; }
function dec(k){ if (k==='adults' && adults.value>1) adults.value--; else if (k==='children' && children.value>0) children.value--; }

// ---------- 검색: 현재/라우터/백업 순서로 안전 사용
function emitSearch () {
  let ci = dateRange.value?.[0] || toDate(route.query.checkIn) || lastGood.value.ci
  let co = dateRange.value?.[1] || toDate(route.query.checkOut) || lastGood.value.co

  if (ci && (!co || co <= ci)) {
    const next = new Date(ci); next.setDate(next.getDate() + 1); co = next
  }
  if (adults.value < 1) adults.value = 1
  if (children.value < 0) children.value = 0

  const query = {
    ...route.query, // 기존 유지
    q: q.value || undefined,
    checkIn: toYmd(ci),
    checkOut: toYmd(co),
    adults: adults.value || undefined,
    children: children.value ?? undefined,
  }

  console.log('[SearchBar emit] pushing query =', query)
  justPushed = true
  router.push({ path: '/search', query })
}
</script>

<style scoped>
.hidden-picker{ position:absolute; left:-9999px; width:1px; height:1px; opacity:0; }
:deep(.flatpickr-calendar){ z-index: 10001; }

/* 너가 쓰던 나머지 스타일 유지 */
.searchbar-wrap{ position: sticky; top: 0; z-index: 20; padding: 10px 14px; }
.searchbar{ display:grid; grid-template-columns: 1.6fr 1.1fr 1.1fr 1.2fr 0.9fr; gap:14px; max-width:1180px; margin:0 auto; }
.cell{ display:flex; align-items:center; gap:12px; height:56px; padding:0 16px; background:#fff; border:1px solid #e6e6e6; border-radius:16px; box-shadow:0 4px 14px rgba(0,0,0,.06); }
.input{ border:none; outline:none; font-size:16px; width:100%; color:#111; }
.cell--date{ justify-content:flex-start; text-align:left; }
.date-text{ display:flex; flex-direction:column; line-height:1.15; }
.date-main{ font-weight:800; color:#111; font-size:16px; }
.date-sub{ color:#768097; font-size:13px; margin-top:2px; }
.cell--guest{ position:relative; }
.guest-button{ display:flex; align-items:center; gap:12px; width:100%; background:transparent; border:none; padding:0; cursor:pointer; }
.guest-summary{ display:flex; flex-direction:column; text-align:left; }
.guest-main{ font-weight:800; color:#111; font-size:16px; }
.guest-sub{ color:#768097; font-size:13px; margin-top:2px; }
.traveler-menu{ position:absolute; top:calc(100% + 10px); right:0; width:320px; background:#fff; border:1px solid #e9ecef; border-radius:16px; box-shadow:0 16px 28px rgba(0,0,0,.12); padding:14px; z-index:10000; }
.traveler-actions{ text-align:right; margin-top:8px; }
.ok{ height:34px; padding:0 14px; border:none; border-radius:10px; background:#2ecf8a; color:#fff; font-weight:700; cursor:pointer; }
.btn{ justify-content:center; background:#0f5132; color:#fff; border:none; font-weight:800; }
</style>
