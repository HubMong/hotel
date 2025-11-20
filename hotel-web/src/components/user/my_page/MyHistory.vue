<!-- src/components/user/my_page/MyHistory.vue -->
<template>
  <div class="mypage-layout">
    <div class="allcard">
      <div class="intro"><h2>내 정보</h2></div>

      <!-- 프로필 -->
      <div class="image">
        <img
          :src="profileImage || 'https://cdn-icons-png.flaticon.com/512/3135/3135715.png'"
          alt="Profile Image"
          @click="onImageClick"
          class="profile-img"
        />
        <input ref="fileInput" type="file" accept="image/*" @change="onFileChange" style="display:none;" />
      </div>

      <!-- 탭 -->
      <div class="menu-tabs">
        <div class="tab" :class="{ active: $route.name==='MyAccount' }" @click="router.push({ name:'MyAccount' })">계정</div>
        <div class="tab" :class="{ active: $route.name==='MyHistory' }" @click="router.push({ name:'MyHistory' })">예약 내역</div>
        <div class="tab" :class="{ active: $route.name==='MyWishlist' }" @click="router.push({ name:'MyWishlist' })">찜</div>
        <div class="tab" :class="{ active: $route.name==='MyReview' }" @click="router.push({ name:'MyReview' })">리뷰</div>
      </div>

      <!-- 예약 상태 탭 -->
      <div class="reservation-status-tabs">
        <div
          class="status-tab"
          :class="{ active: currentTab === 'before' }"
          @click="currentTab = 'before'"
        >
          이용 전
        </div>
        <div
          class="status-tab"
          :class="{ active: currentTab === 'after' }"
          @click="currentTab = 'after'"
        >
          이용 후
        </div>
        <div
          class="status-tab"
          :class="{ active: currentTab === 'cancelled' }"
          @click="currentTab = 'cancelled'"
        >
          취소됨
        </div>
      </div>

      <!-- 본문 -->
      <div class="my-page2">
        <div v-if="isLoading.history" class="loading">예약 내역을 불러오는 중...</div>
        <div v-else-if="filteredReservations.length===0" class="no-data">예약 내역이 없습니다.</div>

        <div v-else class="reservation-container">
          <div
            v-for="r in filteredReservations"
            :key="r.id"
            class="reservation-card"
            :class="{ active: r.active }"
          >
            <div class="summary" @click="toggleReservation(r)">
              <div class="hotel-info">
                <span class="hotel-name">{{ r.hotelName }}</span>
                <span class="dates">{{ formatDate(r.startDate) }} ~ {{ formatDate(r.endDate) }}</span>
              </div>

              <div class="summary-right">
                <span :class="['status-badge', r.status]" style="font-size:.8rem;">{{ r.statusText }}</span>
                <span class="arrow-icon">▼</span>
              </div>
            </div>

            <!-- 액션 버튼들 - 항상 보임 -->
            <div class="action-buttons-section">
              <button class="btn-action btn-detail" @click.stop="goToReservationDetail(r.id)">
                상세보기
              </button>
              <button class="btn-action btn-rebook" @click.stop="rebookHotel(r.hotelId)">
                다시 예약
              </button>
              <button
                v-if="currentTab === 'after' && !r.hasReview"
                class="btn-action btn-review"
                @click.stop="openReviewModal(r)"
              >
                리뷰 작성
              </button>
              <button
                v-if="currentTab === 'after' && r.hasReview"
                class="btn-action btn-review-view"
                @click.stop="goToMyReview(r.reviewId)"
              >
                내가 쓴 리뷰 보러 가기
              </button>
            </div>

            <div class="details">
              <div class="detail-grid">
                <div class="detail-item"><span class="label">객실 타입</span><span class="value">{{ r.roomType }}</span></div>
                <div class="detail-item"><span class="label">인원</span><span class="value">성인 {{ r.adults }}명 / 아동 {{ r.children }}명</span></div>
                <div class="detail-item"><span class="label">객실 수</span><span class="value">{{ r.numRooms }}개</span></div>
              </div>
            </div>
          </div><!-- /card -->
        </div>
      </div>
    </div>

    <!-- 리뷰 작성 모달 -->
    <div v-if="showReviewModal" class="modal-overlay" @click.self="closeReviewModal">
      <div class="modal-content review-modal">
        <div class="modal-header">
          <h3>리뷰 작성</h3>
          <button @click="closeReviewModal" class="close-btn">&times;</button>
        </div>
        <div class="modal-body">
          <div v-if="selectedReservation" class="reservation-info">
            <p><strong>호텔:</strong> {{ selectedReservation.hotelName }}</p>
            <p><strong>체크인:</strong> {{ formatDate(selectedReservation.startDate) }}</p>
            <p><strong>체크아웃:</strong> {{ formatDate(selectedReservation.endDate) }}</p>
          </div>

          <div class="review-form">
            <!-- 리뷰 내용 -->
            <div class="form-group">
              <label>리뷰 내용</label>
              <textarea
                v-model="reviewContent"
                rows="5"
                placeholder="숙소에 대한 리뷰를 작성해주세요."
                class="review-textarea"
              ></textarea>
            </div>

            <!-- 세부 평점 -->
            <div class="rating-section">
              <h4>세부 평점</h4>
              <div
                v-for="(label, key) in ratingLabels"
                :key="key"
                class="rating-group"
              >
                <label>{{ label }}</label>
                <div class="star-rating">
                  <span
                    v-for="n in 5"
                    :key="n"
                    class="star"
                    :class="{ active: n <= reviewRatings[key] }"
                    @click="reviewRatings[key] = n"
                  >
                    ★
                  </span>
                  <span class="rating-value">{{ reviewRatings[key] }}</span>
                </div>
              </div>
              <div class="overall-rating">
                <strong>전체 평점:</strong> ⭐ {{ calcOverallRating() }} / 5
              </div>
            </div>

            <!-- 이미지 업로드 -->
            <div class="form-group">
              <label>이미지 추가 (선택사항)</label>
              <input type="file" multiple accept="image/*" @change="handleReviewFile" />
            </div>

            <!-- 이미지 미리보기 -->
            <div v-if="reviewPreview.length" class="preview-container">
              <img
                v-for="(img, i) in reviewPreview"
                :key="i"
                :src="img"
                alt="미리보기"
                class="preview-image"
              />
            </div>
          </div>
        </div>
        <div class="modal-footer">
          <button
            @click="submitReview"
            class="btn-submit"
            :disabled="!isReviewValid"
            :class="{ disabled: !isReviewValid }"
          >
            리뷰 작성
          </button>
          <button @click="closeReviewModal" class="btn-cancel">취소</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, computed } from 'vue'
import { useRouter } from 'vue-router'
import http from '@/api/http'
import { getMy } from '@/api/ReservationApi'
import UserApi from '@/api/UserApi'
import { getAuthUser, setAuthUser, notifyAuthChanged } from '@/utils/auth-storage'

const router = useRouter()

const goToMyReview = (reviewId) => {
  router.push({ name: 'MyReview', query: { highlight: reviewId } })
}

const user = reactive({})
const reservations = ref([])
const isLoading = reactive({ user: true, history: true })
const profileImage = ref('')
const isLoggedIn = ref(false)
const fileInput = ref(null)

// 현재 선택된 탭
const currentTab = ref('before')

// 리뷰 모달 관련
const showReviewModal = ref(false)
const selectedReservation = ref(null)
const reviewContent = ref('')
const reviewRatings = reactive({
  cleanliness: 5,
  service: 5,
  value: 5,
  location: 5,
  facilities: 5
})
const reviewFiles = ref([])
const reviewPreview = ref([])

const ratingLabels = {
  cleanliness: '숙소 청결 상태',
  service: '서비스',
  value: '가격 대비 만족도',
  location: '위치',
  facilities: '부대시설'
}

const statusMap = { COMPLETED:'예약 완료', CANCELLED:'취소됨', CONFIRMED:'예약 확정', PENDING:'예약 대기', HOLD:'보류' }
const formatDate = s => (s ? new Date(s).toLocaleDateString('ko-KR') : '')
const toggleReservation = r => { r.active = !r.active }
const goToReservationDetail = id => router.push(`/reservations/${id}`)
const onImageClick = () => fileInput.value?.click()
const onFileChange = e => { const f = e?.target?.files?.[0]; if (f) { /* 이미지 업로드 */ } }

// 탭에 따라 예약 내역 필터링
const filteredReservations = computed(() => {
  const today = new Date()
  today.setHours(0, 0, 0, 0)

  return reservations.value.filter(r => {
    const startDate = new Date(r.startDate)
    const endDate = new Date(r.endDate)
    startDate.setHours(0, 0, 0, 0)
    endDate.setHours(0, 0, 0, 0)

    if (currentTab.value === 'before') {
      // 이용 전: 예약 완료 상태이고 체크인 날짜가 오늘 이후
      return r.status === 'COMPLETED' && startDate >= today
    } else if (currentTab.value === 'after') {
      // 이용 후: 예약 완료 상태이고 체크아웃 날짜가 오늘 이전
      return r.status === 'COMPLETED' && endDate < today
    } else if (currentTab.value === 'cancelled') {
      // 취소됨
      return r.status === 'CANCELLED'
    }
    return false
  })
})

// 다시 예약 (호텔 상세 페이지로 이동)
const rebookHotel = (hotelId) => {
  router.push(`/hotels/${hotelId}`)
}

// 리뷰 제출 버튼 활성화 여부
const isReviewValid = computed(() => {
  return reviewContent.value.trim().length > 0
})

// 리뷰 작성 모달 열기
const openReviewModal = (reservation) => {
  selectedReservation.value = reservation
  reviewContent.value = ''
  reviewRatings.cleanliness = 5
  reviewRatings.service = 5
  reviewRatings.value = 5
  reviewRatings.location = 5
  reviewRatings.facilities = 5
  reviewFiles.value = []
  reviewPreview.value = []
  showReviewModal.value = true
}

// 리뷰 모달 닫기
const closeReviewModal = () => {
  showReviewModal.value = false
  selectedReservation.value = null
  reviewContent.value = ''
  reviewFiles.value = []
  reviewPreview.value = []
}

// 리뷰 파일 업로드
const handleReviewFile = (e) => {
  const files = Array.from(e.target.files)
  reviewFiles.value = files
  reviewPreview.value = files.map(f => URL.createObjectURL(f))
}

// 리뷰 평점 계산
const calcOverallRating = () => {
  const sum =
    reviewRatings.cleanliness +
    reviewRatings.service +
    reviewRatings.value +
    reviewRatings.location +
    reviewRatings.facilities
  return Math.round(sum / 5)
}

// 리뷰 제출
const submitReview = async () => {
  if (!selectedReservation.value) return
  if (!reviewContent.value.trim()) {
    alert('리뷰 내용을 입력해주세요.')
    return
  }

  try {
    const formData = new FormData()
    formData.append('userId', user.id)
    formData.append('content', reviewContent.value)
    formData.append('rating', calcOverallRating())
    formData.append('cleanliness', reviewRatings.cleanliness)
    formData.append('service', reviewRatings.service)
    formData.append('value', reviewRatings.value)
    formData.append('location', reviewRatings.location)
    formData.append('facilities', reviewRatings.facilities)
    reviewFiles.value.forEach(f => formData.append('files', f))

    // 백엔드 엔드포인트: POST /api/reviews/reservations/{reservationId}
    await http.post(`/reviews/reservations/${selectedReservation.value.id}`, formData, {
      headers: { 'Content-Type': 'multipart/form-data' }
    })

    alert('리뷰가 작성되었습니다.')
    closeReviewModal()
    // 예약 목록 새로고침하여 리뷰 작성 상태 업데이트
    await fetchReservations()
  } catch (err) {
    console.error('리뷰 작성 실패:', err)
    alert(err.response?.data?.message || '리뷰 작성에 실패했습니다.')
  }
}

const checkAuthStatus = () => {
  const userInfo = getAuthUser()
  if (userInfo) {
    isLoggedIn.value = true
    Object.assign(user, userInfo)
    user.passwordLength = user.provider === 'LOCAL' ? 8 : 0
  } else {
    router.push('/login')
  }
}
const fetchUserProfile = async () => {
  isLoading.user = true
  try {
    const data = await UserApi.getInfo()
    Object.assign(user, data)
    profileImage.value = data.profileImageUrl || ''
    setAuthUser(data)
    notifyAuthChanged()
    user.passwordLength = user.provider === 'LOCAL' ? 8 : 0
  } catch (error) {
    const status = error.response?.status
    if (status === 401 || error.message?.includes('401')) {
      // 401 오류는 무시
    } else {
      console.warn("사용자 정보를 불러올 수 없습니다.", error)
    }
  } finally { isLoading.user = false }
}
const fetchReservations = async () => {
  isLoading.history = true
  try {
    const base = await getMy(0, 20)

    // 중복 제거: 고유한 hotelId만 추출
    const uniqueHotelIds = [...new Set(base.map(r => r.hotelId))]

    // 고유한 호텔 정보만 한 번씩 요청
    const hotelDataPromises = uniqueHotelIds.map(async (hotelId) => {
      try {
        const res = await http.get(`/hotels/${hotelId}`)
        return { hotelId, name: res.data.hotel.name }
      } catch (err) {
        console.error(`호텔 ${hotelId} 정보 조회 실패:`, err)
        return { hotelId, name: `호텔 ID: ${hotelId}` }
      }
    })

    const hotelDataArray = await Promise.all(hotelDataPromises)

    // hotelId를 키로 하는 Map 생성 (빠른 조회)
    const hotelMap = new Map(hotelDataArray.map(h => [h.hotelId, h.name]))

    // 사용자의 모든 리뷰 가져오기
    let userReviews = []
    try {
      const reviewsRes = await http.get(`/reviews/user/${user.id}`)
      userReviews = reviewsRes.data || []
    } catch (e) {
      // 리뷰 조회 실패 (무시)
      userReviews = []
    }

    // 각 예약에 호텔 이름과 리뷰 존재 여부 매핑
    reservations.value = base.map((r) => {
      const foundReview = userReviews.find(review => {
        const reviewReservationId = review.reservationId ?? review.reservation?.id ?? null
        return reviewReservationId === r.id
      })
      return {
        ...r,
        hotelName: hotelMap.get(r.hotelId) || `호텔 ID: ${r.hotelId}`,
        roomType: r.roomType || '스탠다드',
        statusText: statusMap[r.status] || r.status,
        active: false,
        hasReview: !!foundReview,
        reviewId: foundReview?.id
      }
    })
  } catch (e) {
    const status = e?.response?.status
    if (status === 401) {
      // 401 무시
    } else {
      console.error('예약 내역 조회 실패', status, e?.response?.data || e)
    }
    reservations.value = []
  } finally { isLoading.history = false }
}

onMounted(() => {
  checkAuthStatus()
  if (isLoggedIn.value) {
    fetchUserProfile().then(fetchReservations)
  } else {
    isLoading.user = false
    isLoading.history = false
  }
})
</script>

<style scoped src="@/assets/css/mypage/myaccount.css"></style>
<style scoped src="@/assets/css/mypage/myhistory.css"></style>
