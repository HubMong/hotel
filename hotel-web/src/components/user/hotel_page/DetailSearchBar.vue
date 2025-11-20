<template>
  <div class="searchbar-wrap" :style="stickyStyle">
    <div class="searchbar">
      <!-- 목적지 (수정 불가) -->
      <div class="cell cell--search">
        <input
          :value="qDisplay"
          class="input"
          type="text"
          placeholder="도시 / 호텔 이름"
          readonly
          aria-readonly="true"
          title="도시/호텔명은 상세 페이지에서 수정할 수 없습니다."
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

      <!-- 인원 (UI 유지, URL은 변경하지 않음) -->
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

      <!-- 검색 버튼 (동작: 날짜만 URL 반영) -->
      <!-- <button class="cell btn" @click="replaceOnlyDates(ci, co, adults, children)">설정하기</button> -->
    </div>

    <!-- 실제 캘린더(range) -->
    <FlatPickr
      ref="rangePicker"
      :config="dateRangeConfig"
      :value="dateRange"  
      @change="onDateChange" 
      class="hidden-picker"
    />
  </div>
</template>

<script setup>
import { ref, computed, watch, onMounted, onBeforeUnmount, nextTick, getCurrentInstance } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import FlatPickr from 'vue-flatpickr-component'
import { Korean } from 'flatpickr/dist/l10n/ko.js'
import 'flatpickr/dist/flatpickr.css'

/** 상단 고정 여백(px) 커스터마이즈 가능 */
const props = defineProps({ top: { type: Number, default: 0 } })

const route = useRoute()
const router = useRouter()

// q는 표시만 (수정 불가)
const qDisplay = computed(() => String(route.query.q ?? ''))

// 상태 (인원은 UI만, URL 반영 안 함)
const adults = ref(1)
const children = ref(0)
const dateRange = ref([])     // [Date|string, Date|string] 또는 범위 문자열
const rangePicker = ref(null)
const showTravelerMenu = ref(false)
const lastGood = ref({ ci: null, co: null })

function onDateChange(selectedDates) {
  // selectedDates는 FlatPickr에서 반환된 날짜 배열입니다.
  const ci = coerceDate(selectedDates[0]);
  let co = coerceDate(selectedDates[1]);

  if (ci && co && co <= ci) {
    co = new Date(ci);
    co.setDate(co.getDate() + 1);  // 체크인 날짜보다 체크아웃 날짜가 작으면 +1일
  }

  if (ci && co && (ci !== lastGood.value.ci || co !== lastGood.value.co)) {
    dateRange.value = [ci, co];
    lastGood.value = { ci, co };

    // URL 갱신
    replaceOnlyDates(ci, co);
  }
}
// ---- util
const isValidDateObj = (d) => d instanceof Date && !Number.isNaN(d.getTime())
const asArray = (v) => {
  if (Array.isArray(v)) return v
  if (v == null) return []
  if (typeof v === 'string') {
    const s = v.trim()
    for (const sep of [' to ', ' ~ ']) {
      if (s.includes(sep)) { const [a,b]=s.split(sep).map(x=>x.trim()); return [a,b] }
    }
    return [v]
  }
  return [v]
}
const parseYmdLocal = (s) => {
  if (typeof s !== 'string') return null
  const m = s.match(/^(\d{4})-(\d{2})-(\d{2})$/)
  return m ? new Date(+m[1], +m[2]-1, +m[3]) : null
}
const coerceDate = (v) => {
  if (Array.isArray(v)) return coerceDate(v[0])
  if (isValidDateObj(v)) return v
  if (typeof v === 'string') {
    const s = v.trim()
    if (/^\d{4}-\d{2}-\d{2}T/.test(s)) return parseYmdLocal(s.slice(0,10))
    if (/^\d{4}-\d{2}-\d{2}$/.test(s))  return parseYmdLocal(s)
    if (/^\d{4}\/\d{2}\/\d{2}$/.test(s)) { const [y,m,d]=s.split('/'); return new Date(+y,+m-1,+d) }
    const d = new Date(s); return isValidDateObj(d) ? d : null
  }
  if (typeof v === 'number' && v>1e11) { const d=new Date(v); return isValidDateObj(d)?d:null }
  return null
}
const firstValidDate = (...xs) => { for (const x of xs) { const d=coerceDate(x); if (d) return d } return null }
const toYmd = (d) => {
  const dt = coerceDate(d); if (!dt) return undefined
  const y = dt.getFullYear(); if (y<2015||y>2035) return undefined
  const mm = String(dt.getMonth()+1).padStart(2,'0'); const dd = String(dt.getDate()).padStart(2,'0')
  return `${y}-${mm}-${dd}`
}
const weekdayKo = (d) => {
  const dt = coerceDate(d); if (!dt) return ''
  return ['일요일','월요일','화요일','수요일','목요일','금요일','토요일'][dt.getDay()]
}
const disp = (d) => {
  const dt = coerceDate(d)
  if (!dt) return { main: '날짜 선택', sub: '' }
  return { main: `${dt.getFullYear()}년 ${dt.getMonth()+1}월 ${dt.getDate()}일`, sub: weekdayKo(dt) }
}

// 표시
const displayCheckIn  = computed(() => disp(asArray(dateRange.value)[0]))
const displayCheckOut = computed(() => disp(asArray(dateRange.value)[1]))

// sticky style
const stickyStyle = computed(() => ({ position: 'sticky', top: `${props.top}px`, zIndex: 1000, background:'transparent' }))

// flatpickr 설정
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
    const base = firstValidDate(asArray(dateRange.value)[0], route.query.checkIn, lastGood.value.ci)
    instance.jumpToDate(base || new Date())
  },

  // ✅ 핵심: 두 날짜가 모두 선택됐을 때만 URL 갱신
  onChange: (sd) => {
    const len = Array.isArray(sd) ? sd.length : (sd ? 1 : 0)
    if (len === 0) return

    const ci = coerceDate(sd[0])
    let co  = coerceDate(sd[1])

    if (ci && co && co <= ci) { co = new Date(ci); co.setDate(co.getDate() + 1) }

    if (len === 1) {
      // 첫 클릭: 내부 상태만 유지, URL 건드리지 않음
      dateRange.value = [ci]
      lastGood.value = { ci: ci || null, co: null }
      return
    }

    // len === 2 → 내부 상태 업데이트 + URL 갱신
    if (ci && co && (ci !== dateRange.value[0] || co !== dateRange.value[1])) {
      dateRange.value = [ci, co]
      lastGood.value = { ci: ci || null, co: co || null }
      replaceOnlyDates(ci, co, adults, children)
    }
  },

  onClose: (sd, _str, instance) => {
    if (sd.length === 2) {
      const ci = coerceDate(sd[0]); let co = coerceDate(sd[1])
      if (ci && (!co || co <= ci)) { const next = new Date(ci); next.setDate(next.getDate()+1); instance.setDate([ci, next], true) }
    }
  },
}

// 라우터 → 바 동기화
function setRange(ci, co) {
  ci = coerceDate(ci); co = coerceDate(co)
  const y1 = ci?.getFullYear?.(), y2 = co?.getFullYear?.()
  if ((y1 && (y1 < 2015 || y1 > 2035)) || (y2 && (y2 < 2015 || y2 > 2035))) return
  if (ci && co && (ci !== dateRange.value[0] || co !== dateRange.value[1])) {
    dateRange.value = [ci, co]
    lastGood.value  = { ci, co }
    const fp = rangePicker.value?._flatpickr || rangePicker.value?.fp
    if (fp) { fp.setDate([ci, co], false); fp.jumpToDate(ci) }
  }
}

function syncFromRoute() {
  adults.value   = route.query.adults ? Number(route.query.adults) : 1
  children.value = route.query.children ? Number(route.query.children) : 0

  const ci = coerceDate(route.query.checkIn)
  const co = coerceDate(route.query.checkOut)
  if (ci && co && (ci !== dateRange.value[0] || co !== dateRange.value[1])) {
     setRange(ci, co)
  }
}
watch(() => route.fullPath, (newPath) => {
  // URL에서 날짜나 인원 등의 값이 변경되었을 때만 동기화
  if (route.query.checkIn !== lastGood.value.ci || route.query.checkOut !== lastGood.value.co) {
    syncFromRoute();
  }
}, { immediate: true });

// 드롭다운 외부 클릭 닫기
function onDocClick(e){
  const menu = document.querySelector('.traveler-menu')
  const btn  = document.querySelector('.guest-button')
  if (!menu || !btn) return
  if (!menu.contains(e.target) && !btn.contains(e.target)) showTravelerMenu.value = false
}
onMounted(()=>{
  document.addEventListener('click', onDocClick)
  const inst = getCurrentInstance()
})
onBeforeUnmount(()=> document.removeEventListener('click', onDocClick))

function openCalendar () {
  const fp = rangePicker.value?._flatpickr || rangePicker.value?.fp
  if (fp) fp.open()
}
function toggleTravelerMenu(){ showTravelerMenu.value = !showTravelerMenu.value }
function closeTravelerMenu(){ 
  showTravelerMenu.value = false; 
  const query = {
    ...route.query,
    checkIn: route.query.checkIn,
    checkOut: route.query.checkOut,
    adults: adults.value,
    children: children.value
  }
  router.replace({path : route.path, query });
}
function inc(k){ if (k==='adults') adults.value++; else children.value++; }
function dec(k){ if (k==='adults' && adults.value>1) adults.value--; else if (k==='children' && children.value>0) children.value--; }

// ✅ URL에서 checkIn/checkOut만 갱신(이동 없음)
function replaceOnlyDates(ci, co, adults, children) {
  const currentCheckIn = route.query.checkIn;
  const currentCheckOut = route.query.checkOut;
  if (toYmd(ci) !== currentCheckIn || toYmd(co) !== currentCheckOut) {
    const query = {
      ...route.query,
      checkIn: toYmd(ci),
      checkOut: toYmd(co),
      adults: adults.value,
      children: children.value
    };    
      Object.keys(query).forEach(k => (query[k] == null || query[k] === '') && delete query[k]);
      router.replace({ path: route.path, query });
  }
}

// ✅ 버튼 클릭 시: 첫 날짜만 있으면 +1일 보정, 그리고 URL 갱신
function applyQueryOnly () {
  const arr = asArray(dateRange.value)
  let ci = coerceDate(arr[0]) || coerceDate(route.query.checkIn) || lastGood.value.ci
  let co = coerceDate(arr[1]) || coerceDate(route.query.checkOut) || lastGood.value.co

  if (ci && (!co || co <= ci)) { co = new Date(ci); co.setDate(co.getDate() + 1) }

  // 내부 상태 동기화
  dateRange.value = (ci && co) ? [ci, co] : []
  lastGood.value  = { ci: ci || null, co: co || null }

  if (ci && co) replaceOnlyDates(ci, co, adults.value, children.value)
}
</script>

<style scoped>
.hidden-picker{ position:absolute; left:-9999px; width:1px; height:1px; opacity:0; }
:deep(.flatpickr-calendar){ z-index: 2200; }

/* 상단 고정 바 */
.searchbar-wrap{ position: sticky; top: 0; z-index: 1000; padding: 10px 14px; }
.searchbar{ display:grid; grid-template-columns: 1.6fr 1.1fr 1.1fr 1.2fr 0.9fr; gap:14px; max-width:1180px; margin:0 auto; }
.cell{ display:flex; align-items:center; gap:12px; height:56px; padding:0 16px; background:#fff; border:1px solid #54e363; border-radius:16px; box-shadow:0 4px 14px rgba(0,0,0,.06); }
.input{ border:none; outline:none; font-size:16px; width:100%; color:#111; }
.cell--date{ justify-content:flex-start; text-align:left; }
.date-text{ display:flex; flex-direction:column; line-height:1.15; }
.date-main{ font-weight:800; color:#111; font-size:16px; }
.date-sub{ color:#768097; font-size:13px; margin-top:2px; }
.cell--guest{ position:relative; }
.guest-button{ display:flex; align-items:center; gap:12px; width:100%; background:transparent; border:none; padding:0; cursor:pointer; }
.guest-summary{ display:flex; flex-direction:column; text-align:left; }
.guest-main{ font-weight:800; color:#111; font-size:16px; }
.guest-sub{ color:#768097; font-size:13px; }
.traveler-menu{ position:absolute; top:calc(100% + 10px); right:0; width:320px; background:#fff; border:1px solid #e9ecef; border-radius:16px; box-shadow:0 16px 28px rgba(0,0,0,.12); padding:14px; z-index:2199; }
.traveler-actions{ text-align:right; margin-top:8px; }
.ok{ height:34px; padding:0 14px; border:none; border-radius:10px; background:#2ecf8a; color:#fff; font-weight:700; }
.btn{ justify-content:center; background:#0f5132; color:#fff; border:none; font-weight:800; }
</style>
