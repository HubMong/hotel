<template>
  <div class="review-tab">
    <!-- ✅ 리뷰 통계 -->
    <div class="review-stats" v-if="stats.reviewCount > 0">
      <p>⭐ 평균 평점: {{ Number(stats.averageRating).toFixed(1) }}</p>
      <p>총 리뷰 수: {{ stats.reviewCount }}</p>
    </div>

    <hr />

    <!-- ✅ 내가 쓴 리뷰 목록 -->
    <div v-if="reviews.length > 0" class="review-list">
      <div 
        v-for="review in reviews" 
        :key="review.id" 
        :id="'review-' + review.id"
        class="review-card"
        :class="{ 'highlight-review': highlightId == review.id }"
      >
        <div class="review-header">
          <router-link
            :to="`/hotels/${review.hotelId}/reviews?reviewId=${review.id}`"
            class="review-hotel"
          >
            {{ review.hotelName || '알 수 없는 숙소' }}
          </router-link>

          <span class="stars">
            ⭐ {{ Number(review.rating).toFixed(1) }} / 5
          </span>
          <span class="review-date">{{ formatDate(review.createdAt) }}</span>
        </div>

        <!-- ✅ 수정 모드 -->
        <div v-if="editId === review.id" class="edit-mode">
          <textarea v-model="editContent" rows="3" class="edit-box"></textarea>

          <!-- ✅ 세부 별점 수정 -->
          <div class="rating-edit">
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
                  :class="{ active: n <= (editRatings[key] || 0) }"
                  @mouseover="editRatings[key + '_hover'] = n"
                  @mouseleave="editRatings[key + '_hover'] = 0"
                  @click="editRatings[key] = n"
                >
                  ★
                </span>
              </div>
            </div>
          </div>

          <!-- ✅ 기존 이미지 목록 -->
          <div v-if="editOldImages.length" class="old-images">
            <p>기존 이미지 (클릭 시 삭제)</p>
            <div class="image-list">
              <img
                v-for="(img, i) in editOldImages"
                :key="i"
                :src="toImageUrl(img)"
                @click="toggleDelete(img)"
                :class="{ 'to-delete': deleteImages.includes(img) }"
                alt="기존 이미지"
              />
            </div>
          </div>

          <!-- ✅ 새 이미지 업로드 -->
          <div class="file-upload">
            <label>이미지 추가:</label>
            <input type="file" multiple @change="handleEditFile" />
          </div>

          <!-- ✅ 새 이미지 미리보기 -->
          <div v-if="editPreview.length" class="preview-container">
            <img
              v-for="(img, i) in editPreview"
              :key="i"
              :src="img"
              alt="미리보기"
            />
          </div>

          <div class="edit-actions">
            <button @click="updateReview(review.id)">저장</button>
            <button class="cancel" @click="cancelEdit">취소</button>
          </div>
        </div>

        <!-- ✅ 보기 모드 -->
        <div v-else>
          <p class="review-content">{{ review.content }}</p>

          <ul class="detail-ratings">
            <li>숙소 청결 상태: ⭐ {{ Number(review.cleanliness).toFixed(1) }}</li>
            <li>서비스: ⭐ {{ Number(review.service).toFixed(1) }}</li>
            <li>가격 대비 만족도: ⭐ {{ Number(review.value).toFixed(1) }}</li>
            <li>위치: ⭐ {{ Number(review.location).toFixed(1) }}</li>
            <li>부대시설: ⭐ {{ Number(review.facilities).toFixed(1) }}</li>
          </ul>

          <!-- ✅ 이미지 클릭 시 확대 -->
          <div v-if="review.images && review.images.length" class="review-images">
            <img
              v-for="(img, i) in review.images"
              :key="i"
              :src="toImageUrl(img)"
              alt="리뷰 이미지"
              @click="openImage(toImageUrl(img))"
            />
          </div>

          <!-- ✅ 사장님 답글 -->
          <div v-if="review.adminReply" class="review-reply">
            <p><strong>사장님 답글:</strong> {{ review.adminReply }}</p>
          </div>

          <!-- ✅ 버튼 -->
          <div class="review-actions">
            <button @click="startEdit(review)">수정</button>
            <button class="delete" @click="deleteReview(review.id)">삭제</button>
          </div>
        </div>
      </div>
    </div>

    <p v-else class="empty">아직 작성한 리뷰가 없습니다.</p>

    <!-- ✅ 이미지 확대 모달 -->
    <div v-if="showModal" class="image-modal" @click="closeModal">
      <img :src="modalImage" alt="확대 이미지" @click.stop />
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, nextTick } from 'vue'
import { useRoute } from 'vue-router'
import http, { resolveBackendUrl } from '@/api/http'
import { getAuthUser } from '@/utils/auth-storage'

const route = useRoute()
const reviews = ref([])
const stats = reactive({ averageRating: 0, reviewCount: 0 })
const highlightId = ref(null)

const editId = ref(null)
const editContent = ref('')
const editRatings = reactive({
  cleanliness: 5,
  service: 5,
  value: 5,
  location: 5,
  facilities: 5,
  cleanliness_hover: 0,
  service_hover: 0,
  value_hover: 0,
  location_hover: 0,
  facilities_hover: 0,
})

const ratingLabels = {
  cleanliness: '숙소 청결 상태',
  service: '서비스',
  value: '가격 대비 만족도',
  location: '위치',
  facilities: '부대시설',
}

const editFiles = ref([])
const editPreview = ref([])
const editOldImages = ref([])
const deleteImages = ref([])

const savedUser = getAuthUser() || {}
const currentUserId = savedUser.id || null

const toImageUrl = (path) => {
  if (!path) return ''
  if (/^https?:\/\//i.test(path)) return path
  const normalized = path.startsWith('/') ? path : `/${path}`
  return resolveBackendUrl(normalized)
}

// ✅ 이미지 모달
const showModal = ref(false)
const modalImage = ref('')

function openImage(url) {
  modalImage.value = url
  showModal.value = true
}
function closeModal() {
  showModal.value = false
  modalImage.value = ''
}

// ✅ 리뷰 불러오기
async function fetchReviews() {
  if (!currentUserId) return
  try {
    const res = await http.get(`/reviews/user/${currentUserId}`)
    reviews.value = res.data

    if (res.data.length > 0) {
      stats.averageRating =
        res.data.reduce((sum, r) => sum + r.rating, 0) / res.data.length
      stats.reviewCount = res.data.length
    } else {
      stats.averageRating = 0
      stats.reviewCount = 0
    }

    // ✅ 하이라이트 처리
    if (route.query.highlight) {
      const targetId = Number(route.query.highlight)
      highlightId.value = targetId
      await nextTick()
      const el = document.getElementById(`review-${targetId}`)
      if (el) {
        el.scrollIntoView({ behavior: 'smooth', block: 'center' })
      }
    }
  } catch (err) {
    const status = err.response?.status
    if (status === 401 || err.message?.includes('401')) {
      // 401 오류는 무시
    } else {
      console.error('리뷰 불러오기 실패:', err)
    }
  }
}

// ✅ 수정 모드 진입
function startEdit(review) {
  editId.value = review.id
  editContent.value = review.content
  editOldImages.value = [...(review.images || [])]
  deleteImages.value = []
  editPreview.value = []
  editFiles.value = []

  editRatings.cleanliness = Math.round(review.cleanliness || 5)
  editRatings.service = Math.round(review.service || 5)
  editRatings.value = Math.round(review.value || 5)
  editRatings.location = Math.round(review.location || 5)
  editRatings.facilities = Math.round(review.facilities || 5)
}

// ✅ 삭제할 이미지 토글
function toggleDelete(img) {
  if (deleteImages.value.includes(img)) {
    deleteImages.value = deleteImages.value.filter(i => i !== img)
  } else {
    deleteImages.value.push(img)
  }
}

// ✅ 수정 취소
function cancelEdit() {
  editId.value = null
  editContent.value = ''
  editFiles.value = []
  editPreview.value = []
  editOldImages.value = []
  deleteImages.value = []
}

// ✅ 새 파일 업로드
function handleEditFile(e) {
  const files = Array.from(e.target.files)
  editFiles.value = files
  editPreview.value = files.map(f => URL.createObjectURL(f))
}

// ✅ 평균 평점 계산
function calcOverallRating() {
  const sum =
    editRatings.cleanliness +
    editRatings.service +
    editRatings.value +
    editRatings.location +
    editRatings.facilities
  return Math.round(sum / 5)
}

// ✅ 리뷰 수정
async function updateReview(reviewId) {
  try {
    const formData = new FormData()
    formData.append('userId', currentUserId)
    formData.append('content', editContent.value)
    formData.append('rating', calcOverallRating())
    formData.append('cleanliness', editRatings.cleanliness)
    formData.append('service', editRatings.service)
    formData.append('value', editRatings.value)
    formData.append('location', editRatings.location)
    formData.append('facilities', editRatings.facilities)
    editFiles.value.forEach(f => formData.append('files', f))
    formData.append('deleteImages', JSON.stringify(deleteImages.value))

    await http.put(`/reviews/${reviewId}`, formData, {
      headers: { 'Content-Type': 'multipart/form-data' },
    })

    alert('리뷰가 수정되었습니다.')
    cancelEdit()
    fetchReviews()
  } catch (err) {
    console.error('리뷰 수정 실패:', err)
    alert(err.response?.data?.message || '리뷰 수정 실패')
  }
}

// ✅ 리뷰 삭제
async function deleteReview(reviewId) {
  if (!confirm('정말 삭제하시겠습니까?')) return
  try {
    await http.delete(`/reviews/${reviewId}`, { params: { userId: currentUserId } })
    alert('리뷰가 삭제되었습니다.')
    fetchReviews()
  } catch (err) {
    console.error('리뷰 삭제 실패:', err)
  }
}

// ✅ 날짜 포맷
function formatDate(dateStr) {
  return new Date(dateStr).toLocaleDateString('ko-KR', {
    year: 'numeric',
    month: 'short',
    day: 'numeric',
  })
}

onMounted(fetchReviews)
</script>

<style src="@/assets/css/mypage/myreview.css"></style>