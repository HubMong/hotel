<template>
  <div class="review-tab">
    <h2>리뷰 목록</h2>

    <!-- ✅ 리뷰 통계 -->
    <div class="review-stats" v-if="stats.reviewCount > 0">
      <p>⭐ 평균 평점: {{ Math.round(stats.averageRating * 10) / 10 }}</p>
      <p>총 리뷰 수: {{ stats.reviewCount }}</p>
    </div>

    <hr />

    <!-- ✅ 내가 쓴 리뷰 목록 -->
    <div v-if="reviews.length > 0" class="review-list">
      <div v-for="review in reviews" :key="review.id" class="review-card">
        <div class="review-header">
          <strong class="review-hotel">{{ review.hotelName || '알 수 없는 숙소' }}</strong>
          <span class="stars">
            {{ '★'.repeat(Math.round(review.rating)) }}{{ '☆'.repeat(5 - Math.round(review.rating)) }}
          </span>
          <span class="review-date">{{ formatDate(review.createdAt) }}</span>
        </div>

        <!-- ✅ 수정 모드 -->
        <div v-if="editId === review.id" class="edit-mode">
          <textarea v-model="editContent" rows="3" class="edit-box"></textarea>

          <!-- ✅ 평점 수정 -->
          <div class="rating-edit">
            <div v-for="(label, key) in ratingLabels" :key="key" class="rating-group">
              <label>{{ label }}:</label>
              <select v-model.number="editRatings[key]">
                <option v-for="n in 5" :key="key + n" :value="n">{{ n }}</option>
              </select>
            </div>
          </div>

          <!-- ✅ 기존 이미지 목록 -->
          <div v-if="editOldImages.length" class="old-images">
            <p>기존 이미지 (클릭 시 삭제)</p>
            <div class="image-list">
              <img
                v-for="(img, i) in editOldImages"
                :key="i"
                :src="`http://localhost:8888${img}`"
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
            <img v-for="(img, i) in editPreview" :key="i" :src="img" alt="미리보기" />
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
            <li>숙소 청결 상태: {{ Math.round(review.cleanliness) }}</li>
            <li>서비스: {{ Math.round(review.service) }}</li>
            <li>가격 대비 만족도: {{ Math.round(review.value) }}</li>
            <li>위치: {{ Math.round(review.location) }}</li>
            <li>부대시설: {{ Math.round(review.facilities) }}</li>
          </ul>

          <!-- ✅ 이미지 -->
          <div v-if="review.images && review.images.length" class="review-images">
            <img
              v-for="(img, i) in review.images"
              :key="i"
              :src="`http://localhost:8888${img}`"
              alt="리뷰 이미지"
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
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import http from '@/api/http'

const reviews = ref([])
const stats = reactive({ averageRating: 0, reviewCount: 0 })
const editId = ref(null)
const editContent = ref('')
const editRatings = reactive({
  cleanliness: 5,
  service: 5,
  value: 5,
  location: 5,
  facilities: 5,
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

const savedUser = JSON.parse(localStorage.getItem('user') || '{}')
const currentUserId = savedUser.id || null

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
  } catch (err) {
    console.error('리뷰 불러오기 실패:', err)
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

// ✅ 리뷰 수정 (세부 평점 추가 전송)
async function updateReview(reviewId) {
  try {
    const formData = new FormData()
    formData.append('userId', currentUserId)
    formData.append('content', editContent.value)
    formData.append('rating', calcOverallRating())

    // ✅ 세부 평점 추가
    formData.append('cleanliness', editRatings.cleanliness)
    formData.append('service', editRatings.service)
    formData.append('value', editRatings.value)
    formData.append('location', editRatings.location)
    formData.append('facilities', editRatings.facilities)

    editFiles.value.forEach(f => formData.append('files', f))
    formData.append('deleteImages', JSON.stringify(deleteImages.value))

    const res = await http.put(`/reviews/${reviewId}`, formData, {
      headers: { 'Content-Type': 'multipart/form-data' },
    })

    alert('리뷰가 수정되었습니다.')
    if (res.data?.hotelStats) {
      stats.averageRating = res.data.hotelStats.average
      stats.reviewCount = res.data.hotelStats.count
    }

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

<style scoped>
.review-tab {
  padding: 20px;
}
.review-images,
.image-list {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}
.review-images img,
.image-list img {
  width: 80px;
  height: 80px;
  object-fit: cover;
  border-radius: 8px;
  border: 1px solid #ddd;
  cursor: pointer;
  transition: all 0.2s ease;
}
.image-list img.to-delete {
  opacity: 0.4;
  border: 2px solid red;
}
.review-images img:hover,
.image-list img:hover {
  transform: scale(1.1);
}
.preview-container {
  display: flex;
  gap: 8px;
  margin-top: 10px;
}
.preview-container img {
  width: 80px;
  height: 80px;
  border-radius: 6px;
  object-fit: cover;
}
</style>
