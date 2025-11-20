  <template>
  <div class="hotel-review-page">
    <h2>이용후기</h2>

    <!-- ✅ 리뷰 요약 -->
    <section class="review-summary" v-if="reviews.length">
      <div class="summary-left">
        <h2 class="avg">{{ overallAverage.toFixed(1) }}</h2>
        <p class="label">{{ overallLabel }}</p>
        <p class="desc">이용후기 {{ reviewStats.count || reviews.length }}건 기준</p>
      </div>

      <div class="summary-right">
        <div v-for="([k, v]) in orderedDetailList" :key="k" class="bar-item">
          <span class="bar-label">{{ translateLabel(k) }}</span>
          <div class="bar-track">
            <div
              class="bar-fill"
              :class="scoreBucket(v)"
              :style="{ width: (Number(v || 0) / 5) * 100 + '%' }"
            ></div>
          </div>
          <span class="bar-value">{{ Number(v).toFixed(1) }}</span>
        </div>
      </div>
    </section>

    <!-- ✅ 내 리뷰 작성 / 수정 -->
    <section class="write-review" v-if="isLoggedIn">
      <h3>{{ editingReview ? '내 리뷰 수정' : '내 리뷰 작성' }}</h3>

      <!-- ⭐ 별점 -->
      <div class="rating-group">
        <div v-for="(v, key) in ratingFields" :key="key" class="rating-item">
          <label>{{ v.label }}</label>
          <div class="star-rating">
            <span
              v-for="n in 5"
              :key="n"
              class="star"
              :class="{ active: n <= (v.hover || v.value) }"
              @mouseover="v.hover = n"
              @mouseleave="v.hover = 0"
              @click="v.value = n"
            >★</span>
          </div>
        </div>
      </div>

      <textarea
        v-model="newReview"
        placeholder="리뷰를 입력하세요"
        rows="4"
      ></textarea>

      <!-- ✅ 기존 이미지 (수정 모드에서만) -->
      <div v-if="editingReview && existingImages.length" class="existing-images">
        <h4>현재 등록된 이미지 (클릭 시 삭제)</h4>
        <div class="image-list">
          <div
            v-for="(img, i) in existingImages"
            :key="i"
            class="image-box"
            @click="toggleDeleteExisting(img)"
          >
                <img
                  :src="toImageUrl(img)"
              :class="{ 'to-delete': deleteImages.includes(img) }"
              alt="리뷰 이미지"
            />
          </div>
        </div>
      </div>

      <!-- ✅ 새 이미지 업로드 -->
      <input type="file" multiple @change="handleFileChange" />
      <div v-if="imagePreview.length" class="preview-container">
        <h4>새로 추가된 이미지</h4>
        <div class="image-list">
          <div v-for="(img, i) in imagePreview" :key="i" class="image-box">
            <img :src="img" alt="미리보기" />
          </div>
        </div>
      </div>

      <button @click="onSubmit">
        {{ editingReview ? '수정 완료' : '등록' }}
      </button>
    </section>

    <p v-else class="login-prompt">
      리뷰를 작성하려면 <router-link to="/login">로그인</router-link>이 필요합니다.
    </p>

    <!-- ✅ 필터 / 정렬 -->
    <div class="filter-sort" v-if="reviews.length">
      <select v-model="filterRating">
        <option value="">별점 전체</option>
        <option value="4.5">4.5점 이상</option>
        <option value="4.0">4.0점 이상</option>
        <option value="3.5">3.5점 이상</option>
        <option value="3.0">3.0점 이상</option>
        <option value="2.5">2.5점 이하</option>
      </select>

      <select v-model="sortOption">
        <option value="latest">최신순</option>
        <option value="rating">평점순</option>
      </select>
    </div>

    <!-- ✅ 리뷰 목록 -->
    <section class="review-list">
      <h3>전체 후기 ({{ filteredReviews.length }})</h3>
      <ul>
        <li 
          v-for="r in sortedReviews" 
          :key="r.id" 
          :id="'review-' + r.id"
          class="review-item"
          :class="{ 'highlighted-review': highlightId === r.id }"
        >
          <strong>{{ r.userName || '익명 사용자' }}</strong>
          <p class="date">{{ formatDate(r.createdAt) }}</p>
          <p class="rating">⭐ {{ Number(r.rating).toFixed(1) }} / 5</p>
          <p class="content">{{ r.content }}</p>

          <div class="subratings">
            <p>서비스: ⭐ {{ r.service?.toFixed(1) ?? '-' }}</p>
            <p>부대시설: ⭐ {{ r.facilities?.toFixed(1) ?? '-' }}</p>
            <p>가격 대비 만족도: ⭐ {{ r.value?.toFixed(1) ?? '-' }}</p>
            <p>숙소 청결 상태: ⭐ {{ r.cleanliness?.toFixed(1) ?? '-' }}</p>
            <p>위치: ⭐ {{ r.location?.toFixed(1) ?? '-' }}</p>
          </div>

          <!-- ✅ 이미지 클릭 확대 -->
          <div class="images" v-if="r.images && r.images.length">
            <img
              v-for="(img, i) in r.images"
              :key="i"
                :src="toImageUrl(img)"
              alt="리뷰 이미지"
                @click="openImage(toImageUrl(img))"
            />
          </div>

          <div v-if="r.adminReply" class="reply">
            <strong>사장님 답글:</strong>
            <p>{{ r.adminReply }}</p>
          </div>

          <div v-if="r.userId === userId" class="review-actions">
            <button @click="startEdit(r)">수정</button>
            <button class="delete" @click="deleteReview(r.id)">삭제</button>
          </div>
        </li>
      </ul>
    </section>

    <!-- ✅ 이미지 확대 모달 -->
    <div v-if="showModal" class="image-modal" @click="closeModal">
      <img :src="modalImage" alt="확대 이미지" @click.stop />
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onBeforeUnmount, nextTick } from 'vue'
import { useRoute } from 'vue-router'
import http, { resolveBackendUrl } from '@/api/http'
import { getAuthUser } from '@/utils/auth-storage'

const route = useRoute()
const hotelId = route.params.id
const reservationId = route.query.reservationId

const reviews = ref([])
const reviewStats = ref({ average: 0, count: 0, details: {} })
const newReview = ref('')
const imageFiles = ref([])
const imagePreview = ref([])
const existingImages = ref([])
const deleteImages = ref([])
const editingReview = ref(null)
const filterRating = ref('')
const sortOption = ref('latest')
const highlightId = ref(null)

const showModal = ref(false)
const modalImage = ref('')
const authUser = ref(getAuthUser())
const userId = computed(() => authUser.value?.id || null)
const isLoggedIn = computed(() => !!authUser.value)

const toImageUrl = (path) => {
  if (!path) return ''
  if (/^https?:\/\//i.test(path)) return path
  const normalized = path.startsWith('/') ? path : `/${path}`
  return resolveBackendUrl(normalized)
}

const syncAuthUser = () => {
  authUser.value = getAuthUser()
}

const handleStorageEvent = (event) => {
  if (event.key === 'auth.lastChange') syncAuthUser()
}

const ratingFields = ref({
  cleanliness: { label: '숙소 청결 상태', value: 0, hover: 0 },
  service: { label: '서비스', value: 0, hover: 0 },
  value: { label: '가격 대비 만족도', value: 0, hover: 0 },
  location: { label: '위치', value: 0, hover: 0 },
  facilities: { label: '부대시설', value: 0, hover: 0 },
})

const KEY_MAP = {
  '숙소 청결 상태': 'cleanliness',
  '서비스': 'service',
  '가격 대비 만족도': 'value',
  '위치': 'location',
  '부대시설': 'facilities',
}

const normalizeDetailKeys = (det = {}) => {
  const out = {}
  for (const [k, v] of Object.entries(det)) {
    const key = KEY_MAP[k] || k
    out[key] = Number(v) || 0
  }
  return out
}

const DETAIL_KEYS = ['cleanliness', 'service', 'value', 'location', 'facilities']
const DETAIL_ORDER = ['service', 'facilities', 'value', 'cleanliness', 'location']
const sortDetailEntries = (obj = {}) => {
  const arr = Object.entries(obj).map(([k, v]) => [k, Number(v) || 0])
  return arr.sort((a, b) => {
    const ia = DETAIL_ORDER.indexOf(a[0]); const ib = DETAIL_ORDER.indexOf(b[0])
    if (ia !== -1 || ib !== -1) {
      if (ia === -1) return 1
      if (ib === -1) return -1
      if (ia !== ib) return ia - ib
    }
    return b[1] - a[1]
  })
}
const orderedDetailList = computed(() => sortDetailEntries(reviewStats.value.details || {}))

const overallAverage = computed(() => Number(reviewStats.value.average || 0))

// ✅ 리뷰 리스트 정렬
const filteredReviews = computed(() => {
  if (!filterRating.value) return reviews.value
  const min = Number(filterRating.value)
  if (min === 2.5) return reviews.value.filter(r => r.rating <= 2.5)
  return reviews.value.filter(r => r.rating >= min)
})
const sortedReviews = computed(() => {
  const arr = [...filteredReviews.value]
  return sortOption.value === 'rating'
    ? arr.sort((a, b) => b.rating - a.rating)
    : arr.sort((a, b) => new Date(b.createdAt) - new Date(a.createdAt))
})

function deriveStatsFromReviews(list = []) {
  if (!Array.isArray(list) || list.length === 0) {
    return { average: 0, count: 0, details: Object.fromEntries(DETAIL_KEYS.map(k => [k, 0])) }
  }

  const detailTotals = Object.fromEntries(DETAIL_KEYS.map(k => [k, 0]))
  const detailCounts = Object.fromEntries(DETAIL_KEYS.map(k => [k, 0]))

  list.forEach(item => {
    DETAIL_KEYS.forEach(key => {
      const raw = Number(item?.[key])
      if (Number.isFinite(raw) && raw >= 0) {
        detailTotals[key] += raw
        detailCounts[key] += 1
      }
    })
  })

  const detailAverages = Object.fromEntries(
    DETAIL_KEYS.map(key => {
      const count = detailCounts[key] || 0
      const avg = count > 0 ? detailTotals[key] / count : 0
      return [key, Math.round(avg * 10) / 10]
    })
  )

  const sum = DETAIL_KEYS.reduce((acc, key) => acc + detailAverages[key], 0)
  const avgTotal = DETAIL_KEYS.length > 0 ? sum / DETAIL_KEYS.length : 0

  return {
    average: Math.round(avgTotal * 10) / 10,
    count: list.length,
    details: detailAverages,
  }
}

function formatStats(raw, fallbackList = reviews.value) {
  if (!raw || typeof raw !== 'object') {
    return deriveStatsFromReviews(fallbackList)
  }

  const subs = raw.subs || raw.summary || {}
  const average = Number(raw.average ?? raw.score ?? 0) || 0
  const count = Number(
    raw.count ?? raw.reviewCount ?? raw.total ?? subs['리뷰수'] ?? subs.reviewCount ?? subs.count ?? (Array.isArray(fallbackList) ? fallbackList.length : 0)
  ) || 0

  const details = normalizeDetailKeys(raw.details || {})
  DETAIL_KEYS.forEach(key => {
    if (typeof details[key] === 'undefined') {
      details[key] = 0
    }
  })

  return {
    average: Math.round(average * 10) / 10,
    count,
    details,
  }
}

// ✅ 라벨 번역
function translateLabel(key) {
  const map = {
    cleanliness: '숙소 청결 상태',
    service: '서비스',
    value: '가격 대비 만족도',
    location: '위치',
    facilities: '부대시설',
  }
  return map[key] || key
}

const overallRating = computed(() => {
  const vals = Object.values(ratingFields.value).map(v => v.value)
  return vals.reduce((sum, v) => sum + v, 0) / vals.length
})

async function fetchReviews() {
  const res = await http.get(`/reviews/hotels/${hotelId}`)
  const list = res.data.reviews || []
  reviews.value = list
  reviewStats.value = formatStats(res.data?.stats, list)

  if (route.query.reviewId) {
    const targetId = Number(route.query.reviewId)
    highlightId.value = targetId
    await nextTick()
    const el = document.getElementById(`review-${targetId}`)
    if (el) {
      el.scrollIntoView({ behavior: 'smooth', block: 'center' })
    }
  }
}

function handleFileChange(e) {
  const files = Array.from(e.target.files)
  imageFiles.value = files
  imagePreview.value = files.map(f => URL.createObjectURL(f))
}

function toggleDeleteExisting(img) {
  if (deleteImages.value.includes(img))
    deleteImages.value = deleteImages.value.filter(i => i !== img)
  else deleteImages.value.push(img)
}

function openImage(url) {
  modalImage.value = url
  showModal.value = true
}

function closeModal() {
  showModal.value = false
  modalImage.value = ''
}

function resetForm() {
  newReview.value = ''
  imageFiles.value = []
  imagePreview.value = []
  existingImages.value = []
  deleteImages.value = []
  for (const k in ratingFields.value) ratingFields.value[k].value = 0
  editingReview.value = null
}

function onSubmit() {
  editingReview.value ? updateReview() : submitReview()
}

/* ✅ 상세/리뷰 공통 그래프 버킷 */
function scoreBucket(score) {
  const s = Number(score) || 0
  if (s >= 4.5) return 'excellent'
  if (s >= 4.0) return 'verygood'
  if (s >= 3.5) return 'good'
  if (s >= 3.0) return 'medium'
  return 'low'
}

async function submitReview() {
  if (!reservationId) return alert('예약 정보가 없습니다.')
  if (!newReview.value.trim()) return alert('리뷰 내용을 입력하세요.')
  if (!userId.value) return alert('로그인이 필요합니다.')

  const allFilled = Object.values(ratingFields.value).every(v => v.value > 0)
  if (!allFilled) return alert('모든 항목의 평점을 입력하세요.')

  const form = new FormData()
  form.append('userId', userId.value)
  form.append('content', newReview.value)
  form.append('rating', overallRating.value.toFixed(1))
  for (const [k, v] of Object.entries(ratingFields.value)) form.append(k, v.value)
  imageFiles.value.forEach(f => form.append('files', f))

  await http.post(`/reviews/reservations/${reservationId}`, form, {
    headers: { 'Content-Type': 'multipart/form-data' },
  })

  alert('리뷰가 등록되었습니다.')
  resetForm()
  fetchReviews()
}

function startEdit(r) {
  editingReview.value = r
  newReview.value = r.content
  for (const key in ratingFields.value)
    ratingFields.value[key].value = r[key] || 0
  existingImages.value = r.images || []
  deleteImages.value = []
  imageFiles.value = []
  imagePreview.value = []
}

async function updateReview() {
  if (!userId.value) {
    alert('로그인이 필요합니다.')
    return
  }
  const form = new FormData()
  form.append('userId', userId.value)
  form.append('content', newReview.value)
  form.append('rating', overallRating.value.toFixed(1))
  for (const [k, v] of Object.entries(ratingFields.value)) form.append(k, v.value)
  imageFiles.value.forEach(f => form.append('files', f))
  if (deleteImages.value.length)
    form.append('deleteImages', JSON.stringify(deleteImages.value))

  await http.put(`/reviews/${editingReview.value.id}`, form, {
    headers: { 'Content-Type': 'multipart/form-data' },
  })

  alert('리뷰가 수정되었습니다.')
  resetForm()
  fetchReviews()
}

async function deleteReview(id) {
  if (!confirm('정말 삭제하시겠습니까?')) return
  await http.delete(`/reviews/${id}`, { params: { userId: userId.value } })
  alert('리뷰가 삭제되었습니다.')
  fetchReviews()
}

const overallLabel = computed(() => {
  const avg = overallAverage.value
  if (avg >= 4.5) return '최고'
  if (avg >= 4.0) return '우수'
  if (avg >= 3.5) return '좋음'
  if (avg >= 3.0) return '보통'
  return '나쁨'
})

function formatDate(dateStr) {
  return new Date(dateStr).toLocaleDateString('ko-KR', {
    year: 'numeric',
    month: 'short',
    day: 'numeric',
  })
}

onMounted(() => {
  syncAuthUser()
  fetchReviews()
  window.addEventListener('authchange', syncAuthUser)
  window.addEventListener('storage', handleStorageEvent)
})

onBeforeUnmount(() => {
  window.removeEventListener('authchange', syncAuthUser)
  window.removeEventListener('storage', handleStorageEvent)
})
</script>

<style src="@/assets/css/hotel_detail/hotel_review.css"></style>
