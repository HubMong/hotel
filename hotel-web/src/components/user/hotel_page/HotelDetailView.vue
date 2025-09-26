<!-- src/components/HotelDetailView.vue -->
<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import HotelApi from '@/api/HotelApi'
import ReservationApi from '@/api/ReservationApi'

const route = useRoute()
const router = useRouter()

// 상태
const isLoading = ref(true)
const loadError = ref(null)
const hotel = ref(null)
const rooms = ref([])
const reserving = ref(false)

// URL 쿼리에서 날짜/인원
const checkInStr  = computed(() => route.query.checkIn || null)
const checkOutStr = computed(() => route.query.checkOut || null)
const adultsUrl   = computed(() => route.query.adults ? Number(route.query.adults) : null)
const childrenUrl = computed(() => route.query.children ? Number(route.query.children) : null)

// 돈 포맷
const money = (n) => Number.isFinite(n) ? '₩ ' + Number(n).toLocaleString('ko-KR') : '요금 문의'

// ✅ 템플릿에서 참조하는 계산값들(안전 기본값 제공)
const gallery        = computed(() => hotel.value?.images ?? [])
const badges         = computed(() => hotel.value?.badges ?? [])
const highlights     = computed(() => hotel.value?.highlights ?? [])
const amenitiesLeft  = computed(() => hotel.value?.amenities?.left ?? [])
const amenitiesRight = computed(() => hotel.value?.amenities?.right ?? [])
const nearby         = computed(() => hotel.value?.nearby ?? [])
const rating         = computed(() => hotel.value?.rating ?? { score: 0, subs: {} })

onMounted(async () => {
  try {
    const data = await HotelApi.getDetail(route.params.id)
    console.log('[detail] api raw =', data)

    // 응답 유연 매핑: {hotel, rooms} or { ...hotelFields, rooms } 모두 지원
    const h = data?.hotel ?? data ?? null
    hotel.value = h

    const roomsArray = data?.rooms ?? data?.roomList ?? h?.rooms ?? []
    rooms.value = (Array.isArray(roomsArray) ? roomsArray : []).map(r => ({
      // 백엔드 속성명이 다를 수 있으니 안전 병합
      id: r.id,
      name: r.name,
      size: r.room_size ?? r.size ?? r.roomSize ?? '',
      view: r.view ?? r.view_name ?? '',
      bed: r.bed ?? '',
      bath: r.bath ?? '',
      smoke: r.smoke ?? false,
      sharedBath: r.shared_bath ?? r.sharedBath ?? false,
      window: r.has_window ?? r.window ?? false,
      aircon: r.aircon ?? false,
      water: r.free_water ?? r.water ?? false,
      wifi: r.wifi ?? true,
      cancelPolicy: r.cancel_policy ?? r.cancelPolicy ?? '무료취소(일부 날짜 제외)',
      payment: r.payment ?? '현장결제/선결제',
      lastBookedHours: r.lastBookedHours ?? null,
      originalPrice: r.original_price ?? r.originalPrice ?? null,
      price: r.price ?? null,
      photos: r.photos ?? r.images ?? [],
      qty: r.qty ?? 1,
    }))
  } catch (e) {
    console.error(e)
    loadError.value = '숙소 정보를 불러오지 못했어요.'
  } finally {
    isLoading.value = false
  }
})

function ymdGuard(s) {
  return typeof s === 'string' && /^\d{4}-\d{2}-\d{2}$/.test(s) ? s : null
}

async function reserve(room) {
  if (!room?.id) { alert('객실 정보 오류'); return }
  const ci = ymdGuard(checkInStr.value)
  const co = ymdGuard(checkOutStr.value)
  if (!ci || !co) { alert('상단에서 체크인/아웃을 먼저 선택해주세요.'); return }

  const qty = Number(room.qty || 1)
  if (!Number.isFinite(qty) || qty < 1) { alert('수량을 1 이상'); return }

  reserving.value = true
  try {
    const res = await ReservationApi.hold({
      userId: 1,           // 데모
      roomId: room.id,
      qty,
      checkIn: ci,
      checkOut: co,
      adults: adultsUrl.value ?? 1,
      children: childrenUrl.value ?? 0,
      holdSeconds: 60 // 비관적 타임 시간
    })
    // 체크아웃 페이지로 이동
    router.push({ name: 'ReservationCheckout', params: { id: res.reservationId }, query: { hotelId: route.params.id } })
  } catch (e) {
    alert(e?.response?.data?.message || '홀드 실패')
    console.error(e)
  } finally {
    reserving.value = false
  }
}
</script>

<template>
  <section class="hotel-detail container">
    <!-- 로딩/에러 -->
    <div v-if="isLoading" class="loading">불러오는 중…</div>
    <div v-else-if="loadError" class="error">{{ loadError }}</div>

    <template v-else>
      <!-- 갤러리 -->
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

      <!-- 타이틀/부제 -->
      <div class="titlebox">
        <h1>{{ hotel?.name || '숙소명' }}</h1>
        <p>{{ hotel?.address }}</p>
      </div>

      <!-- 개요 -->
      <section class="overview-grid">
        <div class="ov-left">
          <div class="badges">
            <span v-for="b in badges" :key="b" class="badge">{{ b }}</span>
          </div>

          <div class="name-row">
            <h2 class="kname">지금 예약하면 더 저렴!</h2>
            <div class="price-inline">
              <span class="from">시작가</span>
              <strong>{{ money(rooms[0]?.price) }}</strong>
              <button class="btn small">객실 상품 보기</button>
            </div>
          </div>

          <p class="addr">
            {{ hotel?.address }} ·
            <a href="#" @click.prevent>지도에서 보기</a>
          </p>

          <div class="highlights">
            <div v-for="h in highlights" :key="h.title" class="hi">
              <div class="hi-ic">{{ h.ic }}</div>
              <div class="hi-txt">
                <div class="hi-title">{{ h.title }}</div>
                <div class="hi-sub">{{ h.sub }}</div>
              </div>
            </div>
          </div>

          <div class="panel">
            <div class="panel-title">편의 시설/서비스</div>
            <div class="amenities">
              <div class="col">
                <div v-for="a in amenitiesLeft" :key="a" class="amen">✔ {{ a }}</div>
              </div>
              <div class="col">
                <div v-for="a in amenitiesRight" :key="a" class="amen">✔ {{ a }}</div>
              </div>
            </div>
          </div>

          <div class="panel">
            <div class="panel-title">숙소 소개</div>
            <p class="desc">
              {{ hotel?.description || '설명 준비중' }}
              <a href="#" @click.prevent>더 보기</a>
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

          <div class="map-card">
            <div class="map-ph">지도에서 보기 (placeholder)</div>
            <div class="near">
              <div class="near-title">도보 거리에 위치한 명소</div>
              <ul>
                <li v-for="n in nearby" :key="n.name">{{ n.name }} <span>{{ n.dist }}</span></li>
              </ul>
              <a href="#" class="link" @click.prevent>숙소 인근 명소 보기</a>
            </div>
          </div>
        </aside>
      </section>

      <!-- 객실 리스트 -->
      <section class="rooms">
        <h2>객실을 선택하세요</h2>

        <article v-for="room in rooms" :key="room.id" class="room">
          <!-- 좌: 사진/스펙 -->
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
            <ul class="spec" aria-label="객실 정보">
              <li>객실 크기: {{ room.size }}㎡</li>
              <li>전망: {{ room.view }}</li>
              <li>침대: {{ room.bed }}</li>
              <li>욕실: {{ room.bath }}</li>
              <li>흡연: {{ room.smoke ? '가능' : '금연' }}</li>
              <li v-if="room.sharedBath">공용 욕실</li>
              <li v-if="room.window">창문</li>
              <li v-if="room.aircon">에어컨</li>
              <li v-if="room.water">무료 생수</li>
            </ul>
          </div>

          <!-- 중: 혜택/정책 -->
          <div class="r-benefits">
            <div class="r-title">
              <h3>{{ room.name }}</h3>
              <span class="lastbooked" v-if="room.lastBookedHours">최근 {{ room.lastBookedHours }}시간 내 예약됨</span>
            </div>

            <div class="policy">
              <div>✓ 취소 정책: {{ room.cancelPolicy }}</div>
              <div>✓ 결제: {{ room.payment }}</div>
              <div v-if="room.wifi">✓ 무료 Wi-Fi</div>
            </div>

            <div class="promos" v-if="room.promos?.length">
              <div v-for="(p,idx) in room.promos" :key="idx" class="promo" :class="p.type">
                <span class="p-title">{{ p.title }}</span>
                <span class="p-desc">{{ p.desc }}</span>
              </div>
            </div>
          </div>

          <!-- 우: 가격/수량/CTA -->
          <div class="r-cta">
            <div class="price">
              <div class="orig" v-if="room.originalPrice">{{ money(room.originalPrice) }}</div>
              <div class="now">{{ money(room.price) }}</div>
              <div class="tax">1박당 요금(세금/봉사료 제외)</div>
            </div>

            <div class="qty">
              <input type="number" v-model.number="room.qty" min="1" aria-label="수량" />
            </div>

            <button class="btn primary" :disabled="reserving" @click="reserve(room)">
              {{ reserving ? '처리 중…' : '지금 예약하기' }}
            </button>
          </div>
        </article>
      </section>
    </template>
  </section>
</template>

<style src="@/assets/css/hotel_detail/hotel_detail.css"></style>
