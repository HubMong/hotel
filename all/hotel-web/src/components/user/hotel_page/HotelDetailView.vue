<!-- src/components/user/hotel_page/HotelDetail.vue (가칭: 현재 이 파일에 붙여 넣기) -->
<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import HotelApi from '@/api/HotelApi'
import http from '@/api/http'
import ReservationApi from '@/api/ReservationApi'
import DetailSearchBar from './DetailSearchBar.vue'

const route = useRoute()
const router = useRouter()

const isLoggedIn = () => !!localStorage.getItem('token')
const currentFullPath = () => router.currentRoute.value.fullPath
const redirectToLogin = () => router.push({ path: '/login', query: { redirect: currentFullPath() } })

const isLoading = ref(true)
const loadError = ref(null)
const hotel = ref(null)
const rooms = ref([])
const reserving = ref(false)

/* 🔖 위시리스트 상태 */
const wished   = ref(false)
const wishBusy = ref(false)

const checkInStr  = computed(() => route.query.checkIn || null)
const checkOutStr = computed(() => route.query.checkOut || null)
const adultsUrl   = computed(() => route.query.adults   ? Number(route.query.adults)   : 0)
const childrenUrl = computed(() => route.query.children ? Number(route.query.children) : 0)

const money = (n) => Number.isFinite(n) ? '₩ ' + Number(n).toLocaleString('ko-KR') : '요금 문의'
const fmtTime = (t) => (typeof t === 'string' ? t.slice(0,5) : null)

const gallery  = computed(() => hotel.value?.images ?? [])
const badges   = computed(() => hotel.value?.badges ?? [])
const rating   = computed(() => hotel.value?.rating ?? { score: 0, subs: {} })

const sizeText = (s) => {
  if (s == null) return '-'
  if (typeof s === 'number' && Number.isFinite(s)) return `${s}㎡`
  const n = Number(String(s).replace(/[^\d]/g, ''))
  return Number.isFinite(n) && n > 0 ? `${n}㎡` : '-'
}

const HIGHLIGHT_DEF = {
  city_center:     { ic:'📍', title:'도심 근접',       sub:'중심가 위치' },
  activity:        { ic:'⭐', title:'다양한 액티비티',   sub:'투어/이벤트' },
  airport_shuttle: { ic:'🚌', title:'공항 이동 교통편', sub:'셔틀/픽업' },
  checkin_24h:     { ic:'🕒', title:'24시간 체크인',    sub:'야간 도착 OK' },
}
const highlights = computed(() => {
  const direct = hotel.value?.highlights
  if (Array.isArray(direct) && direct.length) return direct
  const keys = hotel.value?.highlightKeys
  if (Array.isArray(keys) && keys.length) return keys.map(k => HIGHLIGHT_DEF[k]).filter(Boolean)
  return []
})

const amenitiesLeft  = computed(() => hotel.value?.amenities?.left  || [])
const amenitiesRight = computed(() => hotel.value?.amenities?.right || [])

const mapSrc = computed(() => {
  const lat = Number(hotel.value?.lat)
  const lng = Number(hotel.value?.lng)
  if (!Number.isNaN(lat) && !Number.isNaN(lng) && hotel.value?.lat != null && hotel.value?.lng != null) {
    return `https://www.google.com/maps?q=${lat},${lng}&hl=ko&z=15&output=embed`
  }
  const q = hotel.value?.address?.trim() || hotel.value?.name?.trim()
  return q ? `https://www.google.com/maps?q=${encodeURIComponent(q)}&hl=ko&z=15&output=embed` : ''
})

const toBool = (v) => v === true || v === 1 || v === '1' || v === 'true' || v === 'TRUE'

onMounted(async () => {
  try {
    const data = await HotelApi.getDetail(route.params.id)
    const h = data?.hotel ?? data ?? null
    hotel.value = h

    // YYYY-MM-DD 그대로 전달(타임존 혼선 방지)
    const ciYmd = checkInStr.value || null
    const coYmd = checkOutStr.value || null

    const roomsArray = data?.rooms ?? data?.roomList ?? h?.rooms ?? []
    const roomIds = roomsArray.map(r => r?.id).filter(Boolean)

    let reservedRooms = { }
    if (ciYmd && coYmd && roomIds.length > 0) {
      // ⚠️ baseURL=/api 이므로 앞 슬래시 금지
      const res = await http.get('reservations/findoverlap', {
        params: { checkIn: ciYmd, checkOut: coYmd, roomIds }
      })
      reservedRooms = res?.data ?? {}
    }

    rooms.value = (Array.isArray(roomsArray) ? roomsArray : []).map(r => {
      const reservedQty = reservedRooms[r.id] ?? 0
      const stock = (r.qty ?? r.room_count ?? r.roomCount ?? 0)
      const remain = Math.max(0, stock - reservedQty)

      const capMax = r.capacity_max ?? r.capacityMax ?? r.capacity ?? 0
      const people = (adultsUrl.value + childrenUrl.value)
      const minRooms = capMax > 0 ? Math.ceil(people / capMax) : 1

      const sizeParsed = (() => {
        const raw = r.size ?? r.room_size ?? r.roomSize ?? null
        if (raw == null) return null
        const n = typeof raw === 'number' ? raw : Number(String(raw).replace(/[^0-9]/g, ''))
        return Number.isFinite(n) ? n : null
      })()

      return {
        id: r.id,
        name: r.name,
        size: sizeParsed,
        capacityMin: r.capacity_min ?? r.capacityMin ?? null,
        capacityMax: capMax || null,
        checkInTime:  r.check_in_time  ?? r.checkInTime  ?? null,
        checkOutTime: r.check_out_time ?? r.checkOutTime ?? null,
        aircon:      toBool(r.aircon),
        bath:        r.bath ?? null,
        bed:         r.bed ?? '',
        cancelPolicy:r.cancel_policy ?? r.cancelPolicy ?? '',
        freeWater:   toBool(r.free_water),
        hasWindow:   toBool(r.has_window),
        originalPrice: r.original_price ?? r.originalPrice ?? null,
        payment:     r.payment ?? '',
        price:       r.price ?? null,              // 판매가
        sharedBath:  toBool(r.shared_bath),
        smoke:       toBool(r.smoke),
        view:        r.view_name ?? r.viewName ?? '',
        wifi:        toBool(r.wifi),
        status:      r.status ?? '',
        roomType:    r.room_type ?? r.roomType ?? '',
        photos:      r.photos ?? r.images ?? [],
        qty:         remain,                       // 남은 객실
        roomCount:   Math.max(1, minRooms)
      }
    })

    // 🔖 내 위시여부 로드
    await loadWishState()
  } catch (e) {
    console.error(e)
    loadError.value = '숙소 정보를 불러오지 못했어요.'
  } finally {
    isLoading.value = false
  }
})

/* YYYY-MM-DD 가드 */
function ymdGuard(s) {
  return typeof s === 'string' && /^\d{4}-\d{2}-\d{2}$/.test(s) ? s : null
}

/* 예약 */
async function reserve(room) {
  if (!room?.id) { alert('객실 정보 오류'); return }
  const ci = ymdGuard(checkInStr.value)
  const co = ymdGuard(checkOutStr.value)
  if (!ci || !co) { alert('상단 바에서 체크인/아웃을 먼저 선택해주세요.'); return }

  if (!isLoggedIn()) {
    alert('예약은 로그인 후 이용할 수 있어요.')
    return redirectToLogin()
  }

  const qty = Number(room.roomCount || 1)
  if (!Number.isFinite(qty) || qty < 1) { alert('수량을 1 이상으로 입력해주세요.'); return }
  if (room.qty != null && qty > room.qty) { alert(`남은 객실은 최대 ${room.qty}개입니다.`); return }

  reserving.value = true
  try {
    const payload = {
      userId: 1,
      roomId: room.id,
      qty,
      checkIn: ci,
      checkOut: co,
      adults: adultsUrl.value || 1,
      children: childrenUrl.value || 0,
      holdSeconds: 60
    }
    const res = await ReservationApi.hold(payload)
    router.push({ name: 'ReservationCheckout', params: { id: res.reservationId }, query: { hotelId: route.params.id } })
  } catch (e) {
    const status = e?.response?.status
    if (status === 401 || status === 403) {
      alert('로그인이 필요합니다.')
      return redirectToLogin()
    }
    alert(e?.response?.data?.message || '홀드 실패')
    console.error(e)
  } finally {
    reserving.value = false
  }
}

/* ==============================
 * 🔖 위시리스트(찜) 클라이언트
 * ============================== */
async function loadWishState() {
  if (!isLoggedIn()) { wished.value = false; return }
  try {
    const { data } = await http.get('wishlists')
    const hid = String(route.params.id)
    wished.value = Array.isArray(data) && data.some(w => String(w.hotelId) === hid)
  } catch (_) {
    wished.value = false
  }
}

async function toggleWish() {
  if (!isLoggedIn()) {
    alert('찜은 로그인 후 이용할 수 있어요.')
    return redirectToLogin()
  }
  if (!hotel.value?.id && !route.params.id) return

  wishBusy.value = true
  try {
    const hid = Number(route.params.id)
    if (wished.value) {
      await http.delete(`wishlists/${hid}`)
      wished.value = false
    } else {
      await http.post('wishlists', { hotelId: hid })
      wished.value = true
    }
  } catch (e) {
    alert(e?.response?.data?.message || '처리에 실패했습니다.')
  } finally {
    wishBusy.value = false
  }
}
</script>

<template>
  <section class="hotel-detail container">
    <div v-if="isLoading" class="loading">불러오는 중…</div>
    <div v-else-if="loadError" class="error">{{ loadError }}</div>

    <template v-else>
      <DetailSearchBar :top="0" />

      <div class="gallery" aria-label="숙소 이미지 갤러리">
        <img
          v-if="gallery[0]" class="hero" :src="gallery[0]" alt="대표 이미지"
          loading="lazy" decoding="async"
          @error="e=> e.target.src='https://picsum.photos/seed/fallback/1200/720'"
        />
        <img
          v-if="gallery[1]" class="thumb" :src="gallery[1]" alt="보조 이미지 1"
          loading="lazy" decoding="async"
          @error="e=> e.target.src='https://picsum.photos/seed/fallback1/600/360'"
        />
        <img
          v-if="gallery[2]" class="thumb" :src="gallery[2]" alt="보조 이미지 2"
          loading="lazy" decoding="async"
          @error="e=> e.target.src='https://picsum.photos/seed/fallback2/600/360'"
        />
      </div>

      <div class="titlebox">
        <div class="title-meta">
          <h1>{{ hotel?.name || '숙소명' }}</h1>
          <p class="addr">{{ hotel?.address }}</p>
        </div>

        <!-- 💗 찜 버튼 -->
        <button
          class="wish-btn"
          :class="{ on: wished }"
          :disabled="wishBusy"
          @click="toggleWish"
          :aria-pressed="wished"
          :title="wished ? '찜 취소' : '찜하기'"
        >
          <span class="heart" aria-hidden="true">♥</span>
          <span class="txt">{{ wished ? '찜함' : '찜하기' }}</span>
        </button>
      </div>

      <section class="overview-grid">
        <div class="ov-left">
          <div class="badges" v-if="badges.length">
            <span v-for="b in badges" :key="b" class="badge">{{ b }}</span>
          </div>

          <div class="highlights" v-if="highlights.length">
            <div v-for="h in highlights" :key="h.title + h.sub" class="hi">
              <div class="hi-ic">{{ h.ic }}</div>
              <div class="hi-txt">
                <div class="hi-title">{{ h.title }}</div>
                <div class="hi-sub">{{ h.sub }}</div>
              </div>
            </div>
          </div>

          <div class="panel" v-if="amenitiesLeft.length || amenitiesRight.length">
            <div class="panel-title">편의 시설/서비스</div>
            <div class="amenities">
              <div class="col">
                <div v-for="a in amenitiesLeft" :key="'L'+a" class="amen">✔ {{ a }}</div>
              </div>
              <div class="col">
                <div v-for="a in amenitiesRight" :key="'R'+a" class="amen">✔ {{ a }}</div>
              </div>
            </div>
          </div>

          <div class="panel" v-if="hotel?.description">
            <div class="panel-title">숙소 소개</div>
            <p class="desc">
              {{ hotel?.description }}
            </p>
          </div>

          <div class="notice" role="note" aria-live="polite" v-if="hotel?.notice">
            <strong>인기 많은 숙소입니다!</strong>
            <span>{{ hotel.notice }}</span>
          </div>
        </div>

        <aside class="ov-right">
          <div class="rating-card" aria-label="이용후기 요약">
            <div class="score">
              <strong>{{ rating.score }}</strong>
              <span>평점</span>
            </div>
            <div class="metrics">
              <div v-for="(v,k) in rating.subs" :key="k">{{ k }} <b>{{ v }}</b></div>
            </div>
            <a href="#" class="link" @click.prevent>이용후기 모두 보기</a>
          </div>

          <div class="map-card" v-if="mapSrc">
            <div class="map-embed">
              <iframe
                :src="mapSrc"
                style="border:0"
                loading="lazy"
                referrerpolicy="no-referrer-when-downgrade"
                allowfullscreen
              ></iframe>
            </div>
          </div>
        </aside>
      </section>

      <section class="rooms">
        <h2>객실을 선택하세요</h2>

        <article v-for="room in rooms" :key="room.id" class="room">
          <div class="r-media">
            <div class="photos">
              <img
                :src="room.photos?.[0]" :alt="room.name" loading="lazy" decoding="async"
                @error="e=> e.target.src='https://picsum.photos/seed/room_fallback/480/320'"
              />
              <div class="thumbs">
                <img
                  v-for="(p,i) in (room.photos || []).slice(1,4)"
                  :key="i" :src="p" :alt="room.name + ' 썸네일 ' + (i+1)"
                  loading="lazy" decoding="async"
                  @error="e=> e.target.src='https://picsum.photos/seed/room_thumb/140/100'"
                />
              </div>
            </div>
          </div>

          <div class="r-benefits">
            <div class="r-title"><h3>{{ room.name }}</h3></div>

            <div class="r-details">
              <div class="detail"><span class="label">객실 크기</span><span class="value">{{ sizeText(room.size) }}</span></div>
              <div class="detail" v-if="room.view"><span class="label">전망</span><span class="value">{{ room.view }}</span></div>
              <div class="detail"><span class="label">침대</span><span class="value">{{ room.bed || '-' }}</span></div>
              <div class="detail" v-if="room.bath != null"><span class="label">욕실</span><span class="value">{{ room.bath }}개</span></div>
              <div class="detail"><span class="label">흡연</span><span class="value">{{ room.smoke ? '가능' : '금연' }}</span></div>
              <div class="detail"><span class="label">창문</span><span class="value">{{ room.hasWindow ? '있음' : '없음' }}</span></div>
              <div class="detail"><span class="label">에어컨</span><span class="value">{{ room.aircon ? '있음' : '없음' }}</span></div>
              <div class="detail"><span class="label">무료 생수</span><span class="value">{{ room.freeWater ? '제공' : '미제공' }}</span></div>
              <div class="detail"><span class="label">Wi-Fi</span><span class="value">{{ room.wifi ? '무료' : '없음' }}</span></div>
              <div class="detail" v-if="room.capacityMin != null || room.capacityMax != null">
                <span class="label">정원</span><span class="value">{{ room.capacityMin ?? '?' }}–{{ room.capacityMax ?? '?' }}명</span>
              </div>
              <div class="detail" v-if="room.checkInTime || room.checkOutTime">
                <span class="label">체크인/아웃</span><span class="value">{{ fmtTime(room.checkInTime) }} / {{ fmtTime(room.checkOutTime) }}</span>
              </div>
              <div class="detail" v-if="room.roomType"><span class="label">타입</span><span class="value">{{ room.roomType }}</span></div>
              <div class="detail" v-if="room.status"><span class="label">상태</span><span class="value">{{ room.status }}</span></div>
              <div class="detail"><span class="label">남은 객실</span><span class="value">{{ room.qty }}개</span></div>
              <div class="detail" v-if="room.cancelPolicy"><span class="label">취소 정책</span><span class="value">{{ room.cancelPolicy }}</span></div>
              <div class="detail" v-if="room.payment"><span class="label">결제 방식</span><span class="value">{{ room.payment }}</span></div>
            </div>
          </div>

          <div class="r-cta">
            <div class="price">
              <div class="orig" v-if="room.originalPrice">{{ money(room.originalPrice) }}</div>
              <div class="now">{{ money(room.price) }}</div>
              <div class="tax">1박당 요금(세금/봉사료 제외)</div>
            </div>

            <div class="qty">
              <input
                type="number"
                v-model.number="room.roomCount"
                :min="Math.max(1, Math.ceil((adultsUrl + childrenUrl) / (room.capacityMax || 1)))"
                :max="room.qty"
                @change="room.roomCount = Math.min(Math.max(1, Number(room.roomCount)), room.qty ?? Number.MAX_SAFE_INTEGER)"
                aria-label="수량"
              />
              <div class="left" v-if="room.qty != null">남은 {{ room.qty }}개</div>
            </div>

            <button
              class="btn primary"
              :disabled="reserving || room.qty === 0"
              @click="reserve(room)"
              :title="room.qty === 0 ? '품절' : '지금 예약하기'"
            >
              {{ room.qty === 0 ? '품절' : (reserving ? '처리 중…' : '지금 예약하기') }}
            </button>
          </div>
        </article>
      </section>
    </template>
  </section>
</template>

<style src="@/assets/css/hotel_detail/hotel_detail.css"></style>
<style scoped>
/* ===========================
   폰트 & 톤 (가독성 향상)
   =========================== */
@import url('https://cdn.jsdelivr.net/gh/orioncactus/pretendard@v1.3.9/dist/web/variable/pretendardvariable-dynamic-subset.css');

.hotel-detail {
  --ink:#1f2937;
  --ink-2:#4b5563;
  --ink-3:#6b7280;
  --line:#e5e7eb;
  --accent:#39c5a0;
  --heart:#ef4444;
  --heart-2:#fee2e2;
  --card-bg:#ffffff;

  font-family: 'Pretendard Variable','Noto Sans KR',system-ui,-apple-system,Segoe UI,Roboto,Helvetica,Arial,'Apple SD Gothic Neo','Malgun Gothic',sans-serif;
  color: var(--ink);
  -webkit-font-smoothing: antialiased;
  -moz-osx-font-smoothing: grayscale;
}

/* 타이틀 행 + 찜 버튼 */
.titlebox{
  display:flex; align-items:flex-start; justify-content:space-between;
  gap:16px; margin:18px 0 14px;
}
.titlebox h1{
  margin:0; font-size:28px; font-weight:900; letter-spacing:-.2px;
}
.titlebox .addr{ margin:6px 0 0; color:var(--ink-3); }

/* 찜 버튼 */
.wish-btn{
  display:inline-flex; align-items:center; gap:8px;
  height:40px; padding:0 14px;
  border:1px solid var(--line); border-radius:999px; background:#fff;
  font-weight:800; cursor:pointer;
  transition: box-shadow .15s ease, transform .1s ease, background .15s ease, color .15s ease, border-color .15s ease;
}
.wish-btn .heart{ font-size:18px; line-height:1; transform: translateY(-1px); transition: transform .12s ease; }
.wish-btn:hover{ box-shadow:0 6px 16px rgba(0,0,0,.06); transform: translateY(-1px); }
.wish-btn:disabled{ opacity:.6; cursor:not-allowed; }
.wish-btn.on{
  color:var(--heart);
  background:var(--heart-2);
  border-color:#fecaca;
}
.wish-btn.on .heart{ transform: scale(1.1); }

/* 카피 톤 소프트하게 */
.badges .badge{ background:#f3f4f6; color:#374151; border:1px solid var(--line); }
.panel{ background:var(--card-bg); border:1px solid var(--line); border-radius:16px; padding:16px; box-shadow:0 6px 18px rgba(0,0,0,.04); }
.panel-title{ font-weight:900; font-size:16px; margin-bottom:8px; }
.desc{ line-height:1.7; color:var(--ink-2); }

.notice{
  background:#f7fee7; color:#3f6212; border:1px solid #d9f99d; border-radius:12px;
  padding:12px 14px; margin-top:12px; font-weight:700;
}

/* 우측 카드 */
.rating-card{
  background:#fff; border:1px solid var(--line); border-radius:16px; padding:16px;
  box-shadow:0 6px 18px rgba(0,0,0,.04); margin-bottom:12px;
}
.rating-card .score strong{ font-size:28px; }
.rating-card .metrics{ color:var(--ink-2); }

.map-embed iframe{ width:100%; height:260px; border-radius:12px; }

/* 객실 리스트 */
.rooms h2{ font-size:22px; margin:18px 0 12px; font-weight:900; }
.room{
  display:grid; grid-template-columns: 1.2fr 1.3fr 1fr; gap:16px;
  border:1px solid var(--line); border-radius:16px; padding:14px;
  background:#fff; box-shadow:0 6px 18px rgba(0,0,0,.04); margin-bottom:14px;
}
.r-media img{ border-radius:12px; border:1px solid var(--line); }
.thumbs{ display:flex; gap:6px; margin-top:6px; }
.thumbs img{ width:74px; height:54px; object-fit:cover; border-radius:8px; border:1px solid var(--line); }

.r-title h3{ margin:0 0 10px; font-size:18px; font-weight:900; letter-spacing:-.2px; }

.r-details{
  display:grid;
  grid-template-columns: 120px 1fr;
  gap:8px 16px;
  font-size:14px;
}
.r-details .label{ color:var(--ink-3); }
.r-details .value{ font-weight:700; color:#0f172a; }

.r-cta{ display:flex; flex-direction:column; gap:10px; align-items:flex-end; }
.r-cta .price{ text-align:right; }
.r-cta .price .orig{ text-decoration:line-through; color:#9aa3af; }
.r-cta .price .now{ font-size:20px; font-weight:900; color:var(--accent); }
.r-cta .price .tax{ color:var(--ink-3); font-size:12px; }

.qty input{
  width:100%; max-width:120px; height:40px; border:1px solid var(--line); border-radius:10px; padding:0 10px; outline:none;
}
.qty input:focus{ border-color:#0b57d0; box-shadow:0 0 0 3px rgba(11,87,208,.08); }
.qty .left{ font-size:12px; color:#768097; margin-top:4px; }

.btn{
  height:44px; padding:0 14px; border-radius:12px; border:1px solid var(--line); cursor:pointer; background:#f7f8fa; font-weight:900;
}
.btn.primary{ background:var(--accent); border-color:#2bb38f; color:#fff; }
.btn:disabled{ opacity:.6; cursor:not-allowed; }

/* 갤러리 */
.gallery .hero{ border-radius:16px; border:1px solid var(--line); }
.gallery .thumb{ border-radius:12px; border:1px solid var(--line); }

/* 로딩/에러 */
.loading, .error{ padding:18px 8px; color:var(--ink-2); }
</style>
