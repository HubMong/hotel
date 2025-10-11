<template>
  <!-- 🔒 상단 고정 락 타이머 배너 -->
  <div class="hold-banner" :class="{ danger: remainSec <= 60 }" v-if="loaded && !error">
    <span>숙소 요청으로 <b>{{ mmss }}</b> 동안 본 조건 유지 중</span>
  </div>

  <section class="checkout-page">
    <header class="page-head">
      <h1>예약 정보 확인</h1>
      <div class="remain-chip" :class="{ danger: remainSec <= 60 }">
        남은 시간: <b>{{ mmss }}</b>
      </div>
    </header>

    <div v-if="loading" class="skeleton">불러오는 중…</div>
    <div v-else-if="error" class="error-box">{{ error }}</div>

    <div v-else class="checkout-grid">
      <!-- 👈 왼쪽: 투숙객 정보/동의 -->
      <div class="left">
        <section class="card">
          <h2 class="card-title">대표 투숙객 정보</h2>
          <div class="form-grid">
            <div class="field">
              <label>영문 이름(First Name) <span class="req">*</span></label>
              <input v-model.trim="form.firstName" placeholder="KIM" />
            </div>
            <div class="field">
              <label>영문 성(Last Name) <span class="req">*</span></label>
              <input v-model.trim="form.lastName" placeholder="SANG" />
            </div>
            <div class="field col-2">
              <label>이메일 주소 <span class="req">*</span></label>
              <input v-model.trim="form.email" type="email" placeholder="you@example.com" />
            </div>
            <div class="field">
              <label>전화번호 <span class="req">*</span></label>
              <input v-model.trim="form.phone" placeholder="010-1234-5678" />
            </div>
            <div class="field">
              <label>거주 국가/지역 <span class="req">*</span></label>
              <select v-model="form.country">
                <option value="">선택</option>
                <option value="KR">대한민국</option>
                <option value="JP">일본</option>
                <option value="US">미국</option>
                <option value="CN">중국</option>
                <option value="TH">태국</option>
              </select>
            </div>
          </div>
        </section>

        <section class="card">
          <h2 class="card-title">다음의 모든 항목에 동의합니다:</h2>

          <!-- ✅ 전체 동의 -->
          <label class="chk all">
            <input
              type="checkbox"
              ref="agreeAllEl"
              :checked="allAgree"
              @change="toggleAll($event.target.checked)"
              aria-checked="allAgree ? 'true' : (someAgree ? 'mixed' : 'false')"
            />
            <strong>전체 동의</strong>
            <span class="subhint">아래 3개 항목을 한 번에 선택/해제</span>
          </label>

          <div class="hr"></div>

          <!-- 개별 동의 -->
          <label class="chk">
            <input type="checkbox" v-model="agree.tos" />
            이용약관에 동의하며, 만 18세 이상임을 확인합니다.
          </label>
          <label class="chk">
            <input type="checkbox" v-model="agree.privacyCollect" />
            개인정보 처리방침에 따라 개인정보의 수집 및 이용에 동의합니다.
          </label>
          <label class="chk">
            <input type="checkbox" v-model="agree.privacyShare" />
            개인정보 처리방침에 따라 국내외 제3자에게 개인정보를 공유하는 것에 동의합니다.
          </label>

          <p v-if="!canBook" class="warn">선택하신 날짜에 이 요금으로 이용 가능한 마지막 아고다 객실입니다.</p>

          <div class="cta-row">
            <button class="btn primary" :disabled="!canProceed || busy || remainSec <= 0" @click="confirmPay">
              다음: 마지막 단계
            </button>
            <button class="btn ghost" :disabled="busy" @click="cancelHold">취소</button>
          </div>
          <div class="hint">즉시 예약에 이 객실과 요금을 확보하세요!</div>
        </section>
      </div>

      <!-- 👉 오른쪽: 예약 요약 카드 -->
      <aside class="right">
        <section class="card sticky">
          <div class="dates">
            <div class="date">
              <div class="label">체크인</div>
              <div class="value">
                {{ fmt(detail.startDate) }} <span class="dow">{{ dow(detail.startDate) }}</span>
              </div>
            </div>
            <div class="date">
              <div class="label">체크아웃</div>
              <div class="value">
                {{ fmt(detail.endDate) }} <span class="dow">{{ dow(detail.endDate) }}</span>
              </div>
            </div>
            <div class="nights">{{ nights }}박</div>
          </div>

          <div class="hotel">
            <img
              :src="summary.photo || 'https://picsum.photos/seed/hotel/160/120'"
              :alt="summary.hotelName || '숙소 이미지'"
              @error="onImgErr"
            />
            <div class="meta">
              <div class="hname">{{ summary.hotelName || '호텔명' }}</div>
              <div class="sub">
                <span v-if="summary.stars">★ {{ summary.stars }}</span>
                <span v-if="summary.city"> · {{ summary.city }}</span>
                <span v-if="summary.country">, {{ summary.country }}</span>
              </div>
              <div class="roomline">
                {{ detail.numRooms }} x {{ summary.roomName || '객실' }}
              </div>
              <ul class="perks">
                <li v-if="summary.freeWifi">무료 Wi-Fi</li>
                <li>무료 취소(일부 날짜 제외)</li>
                <li>현장결제/선결제</li>
              </ul>
            </div>
          </div>

          <div class="pricebox" v-if="summary.price || summary.originalPrice">
            <div class="orig" v-if="summary.originalPrice">{{ money(summary.originalPrice) }}</div>
            <div class="now">{{ money(summary.price || 0) }}</div>
            <div class="tax">세금/봉사료 제외 · 1박당</div>
          </div>

          <!-- 총 결제 예상액 -->
          <div class="total" v-if="summary.price">
            총 결제 예상액: <b>{{ money((summary.price ?? 0) * (detail.numRooms ?? 1) * nights) }}</b>
          </div>
        </section>
      </aside>
    </div>
  </section>
</template>

<script setup>
import { ref, computed, watch, onMounted, onBeforeUnmount } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import ReservationApi from '@/api/ReservationApi'
import HotelApi from '@/api/HotelApi'
import http from '@/api/http'

const route = useRoute()
const router = useRouter()

const id = Number(route.params.id)
const loading = ref(true)
const loaded = ref(false)
const error = ref(null)
const busy = ref(false)

const detail = ref({
  id, status: 'PENDING', startDate: null, endDate: null, expiresAt: null,
  roomId: null, hotelId: null, numRooms: 1
})

const summary = ref({
  hotelId: null, hotelName: '', roomName: '', photo: '',
  stars: null, city: '', country: '', freeWifi: true,
  price: null, originalPrice: null
})

const remainSec = ref(0)
let timer = null

const form = ref({ firstName: '', lastName: '', email: '', phone: '', country: 'KR' })
const agree = ref({ tos: false, privacyCollect: false, privacyShare: false })

/* =========================
   ✅ 전체 동의 (3개 동기화)
   ========================= */
const agreeAllEl = ref(null)
const allAgree = computed({
  get: () => agree.value.tos && agree.value.privacyCollect && agree.value.privacyShare,
  set: (v) => {
    agree.value.tos = v
    agree.value.privacyCollect = v
    agree.value.privacyShare = v
  }
})
const someAgree = computed(() => {
  const a = agree.value
  return (a.tos || a.privacyCollect || a.privacyShare) && !allAgree.value
})
function toggleAll(v) { allAgree.value = v }
watch([allAgree, someAgree], ([all, some]) => {
  if (agreeAllEl.value) agreeAllEl.value.indeterminate = !all && some
})

/* 타이머/포맷 */
const mmss = computed(() => {
  const s = Math.max(0, remainSec.value)
  const m = String(Math.floor(s / 60)).padStart(2, '0')
  const sec = String(s % 60).padStart(2, '0')
  return `${m}:${sec}`
})
function toYmd(s){
  if (!s) return ''
  const t = String(s)
  return t.includes('T') ? t.split('T')[0] : t
}
function diffNights(startYmd, endYmd){
  if (!startYmd || !endYmd) return 0
  const [sy, sm, sd] = startYmd.split('-').map(Number)
  const [ey, em, ed] = endYmd.split('-').map(Number)
  const utcA = Date.UTC(sy, sm - 1, sd)
  const utcB = Date.UTC(ey, em - 1, ed)
  return Math.max(1, Math.round((utcB - utcA) / 86400000))
}
const nights = computed(() => diffNights(toYmd(detail.value.startDate), toYmd(detail.value.endDate)))

const canProceed = computed(() => {
  const emailOk = !!form.value.email && /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(form.value.email)
  const requiredOk = form.value.firstName && form.value.lastName && form.value.phone && form.value.country
  const agreeOk = agree.value.tos && agree.value.privacyCollect && agree.value.privacyShare
  return emailOk && requiredOk && agreeOk
})
const canBook = computed(() => true)

function money(n){ return typeof n==='number' ? '₩ ' + n.toLocaleString('ko-KR') : '-' }
function fmt(iso){ if(!iso) return '-'; const d=new Date(iso); const y=d.getFullYear(); const m=String(d.getMonth()+1).padStart(2,'0'); const day=String(d.getDate()).padStart(2,'0'); return `${y}년 ${m}월 ${day}일` }
function dow(iso){ if(!iso) return ''; const d=new Date(iso).getDay(); return ['일','월','화','수','목','금','토'][d] }
function onImgErr(e){ e.target.src = 'https://picsum.photos/seed/room/160/120' }

async function load(){
  try{
    loading.value = true
    const r = await ReservationApi.get(id)
    detail.value = {
      id: r.id, status: r.status, startDate: r.startDate, endDate: r.endDate,
      expiresAt: r.expiresAt, roomId: r.roomId, hotelId: r.hotelId ?? null, numRooms: r.numRooms ?? 1
    }
    const now = Date.now()
    const exp = detail.value.expiresAt ? new Date(detail.value.expiresAt).getTime() : now
    remainSec.value = Math.max(0, Math.ceil((exp - now) / 1000))
    startTimer()
    await loadSummary()
    loaded.value = true
  }catch(e){
    error.value = e?.response?.data?.message || '예약 정보를 불러오지 못했습니다.'
  }finally{
    loading.value = false
  }
}

async function loadSummary(){
  const roomId = detail.value.roomId
  const hotelId = detail.value.hotelId || route.query.hotelId
  try{
    if (roomId && HotelApi.getRoomSummary) {
      const s = await HotelApi.getRoomSummary(roomId)
      fillSummaryFromApi(s)
      return
    }
  }catch(_){}

  try{
    if (hotelId && HotelApi.getDetail) {
      const d = await HotelApi.getDetail(hotelId)
      const roomsArr = d?.rooms ?? []
      const picked = roomsArr.find(r => String(r?.id) === String(roomId))
      summary.value = {
        hotelId,
        hotelName: d?.hotel?.name || d?.name || '',
        roomName: picked?.name || '',
        photo: (d?.hotel?.images?.[0]) || (d?.images?.[0]) || (picked?.photos?.[0]) || '',
        stars: d?.hotel?.starRating || d?.starRating || null,
        city: d?.hotel?.address || d?.address || '',
        country: d?.hotel?.country || d?.country || '',
        freeWifi: true,
        price: picked?.price ?? null,
        originalPrice: picked?.originalPrice ?? null
      }
      return
    }
  }catch(_){}
  summary.value = { hotelId: detail.value.hotelId || null, hotelName:'', roomName:'', photo:'', stars:null, city:'', country:'', freeWifi:true, price:null, originalPrice:null }
}
function fillSummaryFromApi(s){
  summary.value = {
    hotelId: s.hotelId ?? null,
    hotelName: s.hotelName ?? '',
    roomName: s.roomName ?? '',
    photo: s.photo ?? '',
    stars: s.stars ?? null,
    city: s.city ?? '',
    country: s.country ?? '',
    freeWifi: s.freeWifi ?? true,
    price: s.price ?? null,
    originalPrice: s.originalPrice ?? null
  }
}

function startTimer(){ stopTimer(); timer = setInterval(async () => {
  if (remainSec.value <= 0){ stopTimer(); await safeCancel(); goBackToHotel(); return }
  remainSec.value -= 1
}, 1000) }
function stopTimer(){ if (timer){ clearInterval(timer); timer = null } }
async function safeCancel(){ try{ await ReservationApi.expire(id) }catch(_){} }
function resolveHotelId(){ return detail.value.hotelId || summary.value.hotelId || route.query.hotelId || null }
function goBackToHotel(){ const hid = resolveHotelId(); if (hid) router.replace(`/hotels/${hid}`); else router.replace('/search') }

function formatDateToCustomString(date) {
  const yy = String(date.getFullYear()).slice(2)
  const mm = String(date.getMonth() + 1).padStart(2, '0')
  const dd = String(date.getDate()).padStart(2, '0')
  const HH = String(date.getHours()).padStart(2, '0')
  const MM = String(date.getMinutes()).padStart(2, '0')
  const ss = String(date.getSeconds()).padStart(2, '0')
  const ms = String(date.getMilliseconds()).padStart(3, '0')
  return `${yy}${mm}${dd}${HH}${MM}${ss}${ms}`
}

async function confirmPay(){
  if (!canProceed.value) return
  busy.value = true
  try{
    const r = await ReservationApi.get(id)
    detail.value = {
      id: r.id, status: r.status, startDate: r.startDate, endDate: r.endDate,
      expiresAt: r.expiresAt, roomId: r.roomId, hotelId: r.hotelId ?? null, numRooms: r.numRooms ?? 1
    }
    stopTimer()

    const nightsVal = diffNights(toYmd(detail.value.startDate), toYmd(detail.value.endDate))
    const unitPrice = Number(summary.value.price ?? 0)
    const totalAmount = unitPrice * (detail.value.numRooms ?? 1) * nightsVal
    if (!unitPrice || !totalAmount) { alert('결제 금액 계산에 실패했습니다. 잠시 후 다시 시도해주세요.'); return }

    const orderName = `${summary.value.hotelName || '호텔'} - ${summary.value.roomName || '객실'} (${nightsVal}박/${detail.value.numRooms}객실)`

    // ⚠️ 앞 슬래시 금지 (baseURL=/api)
    const { data } = await http.post('payments/add', {
      reservationId: r.id,
      orderId: formatDateToCustomString(new Date()) + "H" + r.hotelId + "R" + r.roomId,
      orderName,
      paymentMethod: 'TOSS_PAY',
      basePrice: unitPrice,
      tax: 0,
      discount: 0,
      amount: totalAmount,
      customerName: `${form.value.lastName} ${form.value.firstName}`.trim(),
      email: form.value.email,
      phone: form.value.phone,
      paymentKey: ''
    })

    const pid = data?.id ?? data?.paymentId ?? null
    if (!pid) { console.error('payments/add 응답에 id가 없습니다:', data); alert('결제 세션 생성 실패'); return }

    sessionStorage.setItem('lastPaymentId', String(pid))
    router.push(`/payments/${pid}`)
  }catch(e){
    alert(e?.response?.data?.message || '확정 실패')
  }finally{
    busy.value = false
  }
}

async function cancelHold(){
  busy.value = true
  try{ await safeCancel(); stopTimer(); goBackToHotel() } finally { busy.value = false }
}

onMounted(load)
onBeforeUnmount(stopTimer)
</script>

<style scoped>
/* ===== SearchPage 톤 공통 토큰 ===== */
.checkout-page{
  --ink:#1f2937;
  --ink-2:#4b5563;
  --ink-3:#6b7280;
  --line:#e5e7eb;
  --accent:#39c5a0;      /* ✅ 검색 페이지와 동일 */
  --card-bg:#ffffff;
}

/* ===== 상단 락 배너 ===== */
.hold-banner{
  position: sticky; top: 0; z-index: 50;
  background:#fde9e4; color:#8a2d1e;
  border-bottom:1px solid #f6c8bb; padding:10px 16px; text-align:center;
  font-weight:700; letter-spacing:.2px;
}
.hold-banner.danger{ background:#ffe8e8; color:#b3261e; }

/* ===== 페이지 레이아웃 ===== */
.checkout-page{ max-width:1120px; margin:0 auto; padding:16px; color:var(--ink); }
.page-head{ display:flex; align-items:center; justify-content:space-between; margin:10px 0 16px; }
.page-head h1{ font-size:22px; margin:0; }
.remain-chip{ background:#eef6ff; color:#0b57d0; padding:6px 10px; border-radius:10px; font-weight:700; }
.remain-chip.danger{ background:#ffeaea; color:#b00020; }

.checkout-grid{ display:grid; grid-template-columns:2fr 1.2fr; gap:16px; }
.left, .right{ min-width:0; }

/* ===== 공통 카드 ===== */
.card{
  background:var(--card-bg); border:1px solid var(--line); border-radius:16px; padding:16px;
  box-shadow: 0 6px 18px rgba(0,0,0,.04); margin-bottom:14px;
}
.card-title{ font-size:18px; font-weight:900; margin:2px 0 12px; }

/* 로딩/에러 */
.skeleton{ background:#f7f8fa; border:1px solid #eee; border-radius:12px; padding:16px; }
.error-box{ background:#fff3f4; color:#b00020; border:1px solid #ffd1d6; padding:16px; border-radius:12px; }

/* ===== 폼 ===== */
.form-grid{ display:grid; grid-template-columns:1fr 1fr; gap:12px; }
.field{ display:flex; flex-direction:column; }
.field.col-2{ grid-column: 1 / span 2; }
.field label{ font-size:13px; color:#394a58; margin-bottom:6px; }
.field input, .field select{
  height:44px; border:1px solid #dfe3e7; border-radius:12px; padding:0 12px; outline:none; font-size:15px; background:#fff;
}
.field input:focus, .field select:focus{ border-color:#0b57d0; box-shadow:0 0 0 3px rgba(11,87,208,.08); }
.req{ color:#b00020; }

/* 동의 섹션 */
.chk{ display:flex; gap:10px; align-items:flex-start; margin:8px 0; line-height:1.4; }
.chk input{ margin-top:3px; }
.chk.all{
  padding:10px 12px; border:1px dashed var(--line); border-radius:12px; background:#f8fafc;
  align-items:center;
}
.chk.all strong{ font-weight:900; }
.chk .subhint{ color:#6b7280; font-size:12px; margin-left:4px; }

.hr{ height:1px; background:var(--line); margin:10px 0; }
.warn{ color:#b00020; text-align:center; margin:10px 0; }

/* 버튼 */
.cta-row{ display:flex; gap:10px; margin-top:10px; }
.btn{
  height:46px; padding:0 16px; border-radius:12px; border:1px solid var(--line); cursor:pointer; background:#f7f8fa; font-weight:900;
}
.btn.primary{ background:var(--accent); border-color:#2bb38f; color:#fff; }
.btn.ghost{ background:#fff; }
.btn:disabled{ opacity:.6; cursor:not-allowed; }
.hint{ margin-top:8px; color:#1b5e20; font-size:13px; }

/* 우측 요약 */
.sticky{ position: sticky; top: 72px; }
.dates{ display:flex; align-items:center; gap:12px; margin-bottom:12px; }
.date .label{ color:#64748b; font-size:12px; }
.date .value{ font-weight:800; }
.nights{ margin-left:auto; background:#eef6ff; color:#0b57d0; border-radius:999px; padding:4px 10px; font-weight:800; }

.hotel{ display:flex; gap:12px; }
.hotel img{ width:108px; height:80px; object-fit:cover; border-radius:12px; border:1px solid var(--line); }
.meta{ flex:1; min-width:0; }
.hname{ font-weight:900; margin-bottom:2px; }
.sub{ color:#6b7280; font-size:13px; }
.roomline{ margin-top:4px; font-weight:700; }
.perks{ margin:8px 0 0; padding:0 0 0 16px; color:#0f5132; }

.pricebox{ margin-top:12px; text-align:right; }
.pricebox .orig{ text-decoration:line-through; color:#9aa3af; }
.pricebox .now{ font-size:20px; font-weight:900; color:var(--accent); }
.pricebox .tax{ color:#6b7280; font-size:12px; }

.total{ margin-top:6px; text-align:right; font-weight:800; }

@media (max-width:960px){
  .checkout-grid{ grid-template-columns:1fr; }
  .sticky{ position:static; }
}
</style>
