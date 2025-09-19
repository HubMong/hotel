<template>
  <div class="searchbar-wrap">
    <div class="searchbar">
      <!-- 목적지 -->
      <div class="cell cell--search">
        <div class="icon" aria-hidden="true">
          <svg viewBox="0 0 24 24" width="18" height="18">
            <path d="M15.5 14h-.79l-.28-.27a6.471 6.471 0 0 0 1.57-4.23C16 6.01 13.99 4 11.5 4S7 6.01 7 9.5 9.01 15 11.5 15c1.61 0 3.09-.59 4.23-1.57l.27.28v.79L20 19.49 21.49 18 15.5 14zM11.5 13C9.57 13 8 11.43 8 9.5S9.57 6 11.5 6 15 7.57 15 9.5 13.43 13 11.5 13z" fill="currentColor"/>
          </svg>
        </div>
        <input
          v-model="q"
          class="input"
          type="text"
          placeholder="도시 / 호텔 이름"
          @keyup.enter="emitSearch"
        />
      </div>

      <!-- 체크인(표시 전용 버튼) -->
      <button class="cell cell--date" type="button" @click="openCalendar">
        <div class="icon" aria-hidden="true">
          <svg viewBox="0 0 24 24" width="18" height="18">
            <path d="M19 4h-1V2h-2v2H8V2H6v2H5a2 2 0 0 0-2 2v12a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2V6a2 2 0 0 0-2-2zm0 16H5V9h14v11z" fill="currentColor"/>
          </svg>
        </div>
        <div class="date-text">
          <div class="date-main">{{ displayCheckIn.main }}</div>
          <div class="date-sub">{{ displayCheckIn.sub }}</div>
        </div>
      </button>

      <!-- 체크아웃(표시 전용 버튼) -->
      <button class="cell cell--date" type="button" @click="openCalendar">
        <div class="icon" aria-hidden="true">
          <svg viewBox="0 0 24 24" width="18" height="18">
            <path d="M19 4h-1V2h-2v2H8V2H6v2H5a2 2 0 0 0-2 2v12a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2V6a2 2 0 0 0-2-2zm0 16H5V9h14v11z" fill="currentColor"/>
          </svg>
        </div>
        <div class="date-text">
          <div class="date-main">{{ displayCheckOut.main }}</div>
          <div class="date-sub">{{ displayCheckOut.sub }}</div>
        </div>
      </button>

      <!-- 인원(성인/아동) -->
      <div class="cell cell--guest">
        <button class="guest-button" type="button" @click="toggleTravelerMenu">
          <div class="icon" aria-hidden="true">
            <svg viewBox="0 0 24 24" width="18" height="18">
              <path d="M16 11c1.66 0 2.99-1.34 2.99-3S17.66 5 16 5s-3 1.34-3 3 1.34 3 3 3zm-8 0c1.66 0 2.99-1.34 2.99-3S9.66 5 8 5 5 6.34 5 8s1.34 3 3 3zm0 2c-2.33 0-7 1.17-7 3.5V19h14v-2.5C15 14.17 10.33 13 8 13zm8 0c-.29 0-.62.02-.97.05 1.16.84 1.97 1.97 1.97 3.45V19h6v-2.5c0-2.33-4.67-3.5-7-3.5z" fill="currentColor"/>
            </svg>
          </div>
          <div class="guest-summary">
            <div class="guest-main">성인 {{ adults }}명<span v-if="children > 0"> · 아동 {{ children }}명</span></div>
            <div class="guest-sub">인원 선택</div>
          </div>
          <div class="chev" aria-hidden="true">▾</div>
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
          <div class="traveler-actions">
            <button class="ok" @click="closeTravelerMenu">확인</button>
          </div>
        </div>
      </div>

      <!-- 검색 버튼 -->
      <button class="cell btn" @click="emitSearch">검색하기</button>
    </div>

    <!-- 실제 캘린더(range) : 숨김 -->
    <FlatPickr
      ref="rangePicker"
      v-model="dateRange"
      :config="dateRangeConfig"
      class="hidden-picker"
    />
  </div>
</template>

<script setup>
import { ref, computed, watchEffect, onMounted, onBeforeUnmount, nextTick } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import FlatPickr from 'vue-flatpickr-component'
import 'flatpickr/dist/flatpickr.css'
import { Korean } from 'flatpickr/dist/l10n/ko.js'

const route = useRoute()
const router = useRouter()

// 상태
const q        = ref(route.query.q ?? '')
const adults   = ref(route.query.adults ? Number(route.query.adults) : 1)
const children = ref(route.query.children ? Number(route.query.children) : 0)
const dateRange = ref([]) // [Date, Date]
const rangePicker = ref(null)
const showTravelerMenu = ref(false)

// 유틸
const toDate = (s) => {
  if (!s) return null
  const [y,m,d] = s.split('-').map(Number)
  if (!y || !m || !d) return null
  return new Date(y, m-1, d)
}
const toYmd = (d) => {
  if (!d) return undefined
  const dt = d instanceof Date ? d : new Date(d)
  const mm = String(dt.getMonth()+1).padStart(2,'0')
  const dd = String(dt.getDate()).padStart(2,'0')
  return `${dt.getFullYear()}-${mm}-${dd}`
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

// flatpickr (range 한 개만)
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
  onClose: (selectedDates, dateStr, instance) => {
    // 최소 1박 보정
    if (selectedDates.length === 2) {
      const [ci, co] = selectedDates
      if (!co || co <= ci) {
        const next = new Date(ci)
        next.setDate(next.getDate() + 1)
        instance.setDate([ci, next], true)
      }
    }
  },
  onReady: () => nextTick(() => {})
}

// URL → 상태 싱크
watchEffect(() => {
  q.value        = route.query.q ?? ''
  adults.value   = route.query.adults ? Number(route.query.adults) : 1
  children.value = route.query.children ? Number(route.query.children) : 0

  const ci = toDate(route.query.checkIn)
  const co = toDate(route.query.checkOut)
  dateRange.value = (ci && co) ? [ci, co] : []
})

// 외부 클릭으로 인원 메뉴 닫기
function onDocClick(e){
  const menu = document.querySelector('.traveler-menu')
  const btn  = document.querySelector('.guest-button')
  if (!menu || !btn) return
  if (!menu.contains(e.target) && !btn.contains(e.target)) showTravelerMenu.value = false
}
onMounted(()=> document.addEventListener('click', onDocClick))
onBeforeUnmount(()=> document.removeEventListener('click', onDocClick))

function openCalendar () {
  const fp = rangePicker.value?.fp || rangePicker.value?._flatpickr
  if (fp) fp.open()
}

function toggleTravelerMenu(){ showTravelerMenu.value = !showTravelerMenu.value }
function closeTravelerMenu(){ showTravelerMenu.value = false }
function inc(key){ if (key==='adults') adults.value++; else children.value++; }
function dec(key){ if (key==='adults' && adults.value>1) adults.value--; else if (key==='children' && children.value>0) children.value--; }

// 검색
function emitSearch () {
  let ci = dateRange.value?.[0] ? new Date(dateRange.value[0]) : null
  let co = dateRange.value?.[1] ? new Date(dateRange.value[1]) : null

  // 최소 1박 보정
  if (ci && (!co || co <= ci)) {
    const next = new Date(ci)
    next.setDate(next.getDate() + 1)
    co = next
  }
  if (adults.value < 1) adults.value = 1
  if (children.value < 0) children.value = 0

  router.push({
    path: '/search',
    query: {
      q: q.value || undefined,
      checkIn: ci ? toYmd(ci) : undefined,
      checkOut: co ? toYmd(co) : undefined,
      adults: adults.value || undefined,
      children: children.value ?? undefined
    }
  })
}
</script>

<style scoped>
:root{
  --brand-green:#2ecf8a;        /* 메인 초록 */
  --brand-green-dark:#22b87b;   /* hover/그라데이션 하단 */
  --cell-bg:#ffffff;
  --cell-border:#e6e6e6;
  --ink:#111;
  --ink-sub:#768097;
}

/* 상단에 자연스럽게 붙는 그린 배경 바 */
.searchbar-wrap{
  position: sticky; top: 0; z-index: 20;
  background: linear-gradient(180deg, var(--brand-green) 0%, var(--brand-green-dark) 100%);
  padding: 10px 14px;   /* 위아래 여백 + 좌우 완충 */
}

/* 바 자체 컨테이너 */
.searchbar{
  display: grid;
  grid-template-columns: 1.6fr 1.1fr 1.1fr 1.2fr 0.9fr;
  gap: 14px;                      /* 셀 간격 ↑ */
  max-width: 1180px;
  margin: 0 auto;
}

/* 공통 셀 */
.cell{
  display:flex; align-items:center; gap:12px;
  height: 56px;
  padding: 0 16px;                /* 내부 여백 ↑ */
  background: var(--cell-bg);
  border: 1px solid var(--cell-border);
  border-radius: 16px;            /* 둥근 모서리 ↑ */
  box-shadow: 0 4px 14px rgba(0,0,0,.06);
}

/* 검색 입력 */
.cell--search .input{
  border:none; outline:none; font-size:16px; width:100%; color:var(--ink);
}
.icon{ color:#1d4e40; display:flex; align-items:center; }

/* 날짜 셀: 두 줄(굵은 본문 + 연한 요일) */
.cell--date{ justify-content:flex-start; text-align:left; }
.date-text{ display:flex; flex-direction:column; line-height:1.15; }
.date-main{ font-weight:800; color:var(--ink); font-size:16px; }
.date-sub{ color:var(--ink-sub); font-size:13px; margin-top:2px; }

/* 인원 셀 */
.cell--guest{ position:relative; }
.guest-button{
  display:flex; align-items:center; gap:12px; width:100%;
  background:transparent; border:none; padding:0; cursor:pointer;
}
.guest-summary{ display:flex; flex-direction:column; text-align:left; }
.guest-main{ font-weight:800; color:var(--ink); font-size:16px; }
.guest-sub{ color:var(--ink-sub); font-size:13px; margin-top:2px; }
.chev{ margin-left:auto; color:#2a6f5c; }

/* 드롭다운 */
.traveler-menu{
  position:absolute; top:calc(100% + 10px); right:0;
  width:320px; background:#fff; border:1px solid #e9ecef; border-radius:16px;
  box-shadow:0 16px 28px rgba(0,0,0,.12); padding:14px; z-index:10000;
}
.traveler-item{ display:flex; align-items:center; justify-content:space-between; padding:10px 0; }
.counter{ display:flex; align-items:center; gap:12px; }
.counter button{
  width:32px; height:32px; border-radius:10px; border:1px solid #e6e6e6; background:#f7f8fa; cursor:pointer;
}
.traveler-actions{ text-align:right; margin-top:8px; }
.ok{
  height:34px; padding:0 14px; border:none; border-radius:10px;
  background: var(--brand-green); color:#fff; font-weight:700; cursor:pointer;
}
.ok:hover{ filter:brightness(0.95); }

/* 검색 버튼 */
.btn{
  justify-content:center;
  background:#0f5132; color:#fff; border:none; font-weight:800;
}
.btn:hover{ filter:brightness(0.96); }

/* 숨김 range 인풋 (실제 캘린더) */
.hidden-picker{ position:absolute; left:-9999px; width:1px; height:1px; opacity:0; }

/* flatpickr z-index 보정 */
:deep(.flatpickr-calendar){ z-index: 10001; }

/* 반응형 */
@media (max-width: 1120px){
  .searchbar{ grid-template-columns: 1fr; }
}
</style>
