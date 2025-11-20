<template>
  <div class="owner-review-container">
    <h1>리뷰 관리</h1>

    <div v-if="filterData.hotels && filterData.hotels.length > 0" class="filter-wrapper">
      <div class="filter-controls">
        <div class="filter-group">
          <label for="hotel-select">호텔 선택</label>
          <select id="hotel-select" v-model="selectedHotelId" @change="onHotelChange">
            <option :value="null">전체 호텔</option>
            <option v-for="hotel in filterData.hotels" :key="hotel.hotelId" :value="hotel.hotelId">
              {{ hotel.hotelName }}
            </option>
          </select>
        </div>
        <div class="filter-group">
          <label for="room-type-select">객실 타입</label>
          <select id="room-type-select" v-model="selectedRoomType" @change="onFilterChange" :disabled="selectedHotelId === null">
            <option :value="null">전체 객실</option>
            <option v-for="roomType in currentRoomTypes" :key="roomType" :value="roomType">
              {{ roomType }}
            </option>
          </select>
        </div>
        <div class="filter-group">
          <label for="answer-status-select">답변 여부</label>
          <select id="answer-status-select" v-model="answerStatus" @change="onFilterChange">
            <option value="UNREPLIED">미답변</option>
            <option value="">전체 답변</option>
            <option value="REPLIED">답변 완료</option>
          </select>
        </div>
        <div class="filter-group">
          <label for="exposure-status-select">리뷰 상태</label>
          <select id="exposure-status-select" v-model="exposureStatus" @change="onFilterChange">
            <option value="ALL">전체 리뷰</option>
            <option value="EXPOSED">노출 리뷰</option>
            <option value="REPORTED">신고된 리뷰</option>
            <option value="HIDDEN">숨긴 리뷰</option>
          </select>
        </div>
      </div>
    </div>
    <div v-else class="no-data-message">
      <p>관리할 호텔이 없습니다. 호텔을 먼저 등록해주세요.</p>
    </div>

    <div v-if="reviews.length > 0" class="review-list">
      <div v-for="review in reviews" :key="review.reviewId" class="review-card" :class="{ 'hidden-review': review.isHidden }">
        <div class="review-header">
          <div class="user-info">
            <span class="author-nickname">{{ review.authorNickname }}</span>
            <span class="rating">⭐ {{ review.rating.toFixed(1) }}</span>
          </div>
          <div class="status-tags">
            <span v-if="review.isHidden" class="status-tag hidden">숨김 처리</span>
            <span v-else-if="review.isReported" class="status-tag reported">신고됨</span>
          </div>
        </div>
        
        <p v-if="!review.isHidden" class="review-content">{{ review.content }}</p>
        <p v-else class="review-content hidden-message">관리자에 의해 숨김 처리된 리뷰입니다.</p>

        <div class="review-footer">
          <span class="created-date">{{ formatDate(review.createdAt) }}</span>
          <button @click="reportReview(review)" class="report-btn" :disabled="review.isReported || review.isHidden">
            신고
          </button>
        </div>

        <div class="reply-section">
          <div v-if="review.reply" class="reply-view">
            <p class="reply-content"><strong>사장님 답변:</strong> {{ review.reply.content }}</p>
            <div class="reply-actions">
              <button @click="openReplyModal(review, true)" class="btn-edit">답변 수정</button>
              <button @click="deleteReply(review)" class="btn-delete">답변 삭제</button>
            </div>
          </div>
          <div v-else class="reply-prompt">
            <button @click="openReplyModal(review, false)" class="btn-add-reply">답변 달기</button>
          </div>
        </div>
      </div>
    </div>
    <div v-else-if="!isLoading && ownerId" class="no-data-message">
      <p>현재 필터 조건에 맞는 리뷰가 없습니다.</p>
    </div>

    <div class="pagination" v-if="totalPages > 1">
      <button @click="changePage(page - 1)" :disabled="page === 0">이전</button>
      <span>{{ page + 1 }} / {{ totalPages }}</span>
      <button @click="changePage(page + 1)" :disabled="page >= totalPages - 1">다음</button>
    </div>

    <div v-if="showReplyModal" class="modal-overlay" @click.self="closeReplyModal">
      <div class="modal-content">
        <h3>답변 {{ isEditingReply ? '수정' : '작성' }}</h3>
        <textarea v-model="replyContent" placeholder="답변 내용을 입력하세요..."></textarea>
        <div class="modal-actions">
          <button @click="submitReply" class="btn-primary">저장</button>
          <button @click="closeReplyModal" class="btn-secondary">취소</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, computed } from 'vue';
import http from '@/api/http';
import { getOwnerId } from '@/utils/auth-storage';

const BASE_URL = '/owner';
const OwnerApi = {
  getFilterData: (ownerId) => http.get(`${BASE_URL}/filter-data`, { params: { ownerId } }),
  getReviews: (params) => http.get(`${BASE_URL}/reviews`, { params }),
  addReply: (reviewId, content) => http.post(`${BASE_URL}/reviews/${reviewId}/reply`, content),
  updateReply: (reviewId, content) => http.put(`${BASE_URL}/reviews/${reviewId}/reply`, content),
  deleteReply: (reviewId) => http.delete(`${BASE_URL}/reviews/${reviewId}/reply`),
  reportReview: (reviewId) => http.post(`${BASE_URL}/reviews/${reviewId}/report`),
};

const ownerId = ref(null);
const filterData = ref({ hotels: [] });
const selectedHotelId = ref(null);
const selectedRoomType = ref(null);
const answerStatus = ref(''); // 기본값을 '전체 답변'으로 변경
const exposureStatus = ref('ALL');
const reviews = ref([]);
const page = ref(0);
const totalPages = ref(0);
const isLoading = ref(true);
const showReplyModal = ref(false);
const selectedReview = ref(null);
const replyContent = ref('');
const isEditingReply = ref(false);

const currentRoomTypes = computed(() => {
  if (!selectedHotelId.value || !filterData.value.hotels) return [];
  const selectedHotel = filterData.value.hotels.find(h => h.hotelId === selectedHotelId.value);
  return selectedHotel ? selectedHotel.roomTypes : [];
});

const fetchFilterData = async () => {
  if (!ownerId.value) {
    isLoading.value = false;
    return;
  }
  try {
    const response = await OwnerApi.getFilterData(ownerId.value);
    filterData.value = response.data;
  } catch (error) {
    console.error("필터 데이터 로딩 실패:", error);
  } finally {
    await fetchReviews(); 
    isLoading.value = false;
  }
};

const fetchReviews = async () => {
  if (!ownerId.value) return;
  const params = {
    ownerId: ownerId.value,
    hotelId: selectedHotelId.value,
    roomType: selectedRoomType.value,
    status: answerStatus.value,
    exposureStatus: exposureStatus.value,
    page: page.value,
    size: 10,
    sort: 'createdAt,desc'
  };
  try {
    const response = await OwnerApi.getReviews(params);
    reviews.value = response.data.content;
    totalPages.value = response.data.totalPages;
  } catch (error) {
    console.error("리뷰 로딩 실패:", error);
    reviews.value = [];
    totalPages.value = 0;
  }
};

const onHotelChange = () => {
  selectedRoomType.value = null;
  onFilterChange();
}

const onFilterChange = () => {
  page.value = 0;
  fetchReviews();
};

const changePage = (newPage) => {
  if (newPage >= 0 && newPage < totalPages.value) {
    page.value = newPage;
    fetchReviews();
  }
};

const formatDate = (dateString) => {
  if (!dateString) return '';
  const date = new Date(dateString);
  return `${date.getFullYear()}.${String(date.getMonth() + 1).padStart(2, '0')}.${String(date.getDate()).padStart(2, '0')}`;
};

const openReplyModal = (review, isEditing) => {
  selectedReview.value = review;
  isEditingReply.value = isEditing;
  replyContent.value = isEditing && review.reply ? review.reply.content : '';
  showReplyModal.value = true;
};

const closeReplyModal = () => {
  showReplyModal.value = false;
  selectedReview.value = null;
  replyContent.value = '';
};

const submitReply = async () => {
  if (!replyContent.value.trim()) {
    alert("답변 내용을 입력해주세요.");
    return;
  }
  try {
    if (isEditingReply.value) {
      await OwnerApi.updateReply(selectedReview.value.reviewId, { content: replyContent.value });
    } else {
      await OwnerApi.addReply(selectedReview.value.reviewId, { content: replyContent.value });
    }
    alert(`답변이 성공적으로 ${isEditingReply.value ? '수정' : '등록'}되었습니다.`);
    closeReplyModal();
    await fetchReviews();
  } catch (error) {
    console.error("답변 처리 오류:", error);
    alert("답변 처리 중 오류가 발생했습니다.");
  }
};

const deleteReply = async (review) => {
  if (confirm("정말로 답변을 삭제하시겠습니까?")) {
    try {
      await OwnerApi.deleteReply(review.reviewId);
      alert("답변이 삭제되었습니다.");
      await fetchReviews();
    } catch (error) {
      console.error("답변 삭제 오류:", error);
      alert("답변 삭제 중 오류가 발생했습니다.");
    }
  }
};

// ✅ 신고 기능 수정
const reportReview = async (review) => {
  if (confirm("이 리뷰를 신고하시겠습니까? 신고 처리 후에는 취소할 수 없습니다.")) {
    try {
      await OwnerApi.reportReview(review.reviewId);
      alert("리뷰가 신고 접수되었습니다.");
      // UI만 임시로 바꾸는 대신, 서버에서 최신 데이터를 다시 불러와서
      // 신고 상태가 영구적으로 반영되도록 합니다.
      await fetchReviews();
    } catch (error) {
      console.error("리뷰 신고 오류:", error);
      alert("리뷰 신고 중 오류가 발생했습니다.");
    }
  }
};

onMounted(() => {
  const storedOwnerId = getOwnerId();
  if (storedOwnerId) {
    ownerId.value = parseInt(storedOwnerId, 10);
    fetchFilterData();
  } else {
    console.error("로그인된 업주 정보를 찾을 수 없습니다.");
    isLoading.value = false;
  }
});
</script>

<style scoped>
/* ✅ 요청하신 스타일 적용 */
.owner-review-container {
  padding: 2rem 3rem;
  font-family: 'Noto Sans KR', sans-serif;
  /* width: 100%; */
  background-color: #f7f9fc;
  min-height: 100vh;
}

h1 {
  font-size: 2rem;
  font-weight: 700;
  margin-bottom: 2rem;
  text-align: left;
  color: #2c3e50;
}

/* --- 이하 고급스러운 디자인 적용 --- */
.filter-wrapper {
  background-color: #fff;
  padding: 1.5rem;
  border-radius: 12px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.05);
  margin-bottom: 2rem;
}
.filter-controls {
  display: flex;
  flex-wrap: wrap;
  gap: 1.5rem;
  align-items: flex-end;
}
.filter-group {
  display: flex;
  flex-direction: column;
}
.filter-group label {
  font-size: 0.85rem;
  color: #555;
  margin-bottom: 0.5rem;
  font-weight: 500;
}
.filter-group select {
  padding: 0.6rem 1rem;
  border: 1px solid #dcdfe6;
  border-radius: 8px;
  min-width: 180px;
  background-color: #fff;
  transition: border-color 0.2s;
}
.filter-group select:focus {
  outline: none;
  border-color: #409eff;
}

.review-list {
  display: flex;
  flex-direction: column;
  gap: 1.5rem;
}
.review-card {
  border-radius: 12px;
  background-color: #fff;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.05);
  transition: transform 0.2s, box-shadow 0.2s;
  overflow: hidden; /* 자식 요소가 부모를 벗어나지 않도록 */
}
.review-card:hover {
  transform: translateY(-5px);
  box-shadow: 0 8px 20px rgba(0, 0, 0, 0.08);
}
.review-card.hidden-review {
  background-color: #f8f8f8;
  opacity: 0.7;
}

.review-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  padding: 1.5rem 1.5rem 1rem;
}
.user-info {
  display: flex;
  align-items: center;
  gap: 0.75rem;
}
.author-nickname {
  font-weight: 600;
  font-size: 1.1rem;
  color: #333;
}
.rating { 
  font-size: 1rem; 
  color: #f5a623; 
  font-weight: 700;
}
.status-tags {
  display: flex;
  gap: 0.5rem;
}
.status-tag {
  font-size: 0.75rem;
  font-weight: 700;
  padding: 0.25rem 0.6rem;
  border-radius: 12px;
  color: #fff;
}
.status-tag.reported {
  background-color: #e6a23c; /* 주황색 */
}
.status-tag.hidden {
  background-color: #f56c6c; /* 빨간색 */
}

.review-content { 
  line-height: 1.7; 
  color: #606266; 
  padding: 0 1.5rem 1rem;
  word-break: break-word;
}
.review-content.hidden-message {
  font-style: italic;
  color: #909399;
}

.review-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 0 1.5rem 1.5rem;
  font-size: 0.85rem;
  color: #909399;
}
.report-btn {
  background: none;
  border: none;
  color: #909399;
  cursor: pointer;
  font-size: 0.8rem;
  transition: color 0.2s;
}
.report-btn:hover:not(:disabled) { color: #f56c6c; }
.report-btn:disabled {
  cursor: not-allowed;
  color: #c0c4cc;
  text-decoration: line-through;
}

.reply-section {
  background-color: #f7f9fc;
  padding: 1.5rem;
  border-top: 1px solid #f0f2f5;
}
/* ✅ 버튼 오른쪽 정렬 및 크기 고정 */
.reply-view, .reply-prompt {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 1rem;
}
.reply-prompt {
    justify-content: flex-end; /* 답변 달기 버튼을 오른쪽으로 */
}
.reply-content { 
  margin: 0; 
  font-size: 0.95rem; 
  flex-grow: 1; /* 텍스트가 남은 공간을 모두 차지 */
  word-break: break-word;
}
.reply-content strong { color: #333; }
.reply-actions {
    display: flex;
    gap: 0.75rem;
    flex-shrink: 0; /* 버튼 컨테이너가 줄어들지 않도록 고정 */
}
.btn-edit, .btn-add-reply, .btn-delete {
  padding: 0.5rem 1.2rem;
  border-radius: 8px;
  cursor: pointer;
  border: none;
  color: white;
  font-weight: 500;
  transition: background-color 0.2s;
  white-space: nowrap; /* 버튼 텍스트 줄바꿈 방지 */
}
.btn-edit { background-color: #67c23a; }
.btn-edit:hover { background-color: #85ce61; }
.btn-add-reply { background-color: #409eff; }
.btn-add-reply:hover { background-color: #66b1ff; }
.btn-delete { background-color: #f56c6c; }
.btn-delete:hover { background-color: #f78989; }
.no-data-message {
  text-align: center;
  padding: 4rem 2rem;
  color: #909399;
  background-color: #fff;
  border-radius: 12px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.05);
}
.pagination {
  display: flex;
  justify-content: center;
  align-items: center;
  margin-top: 2.5rem;
  gap: 0.5rem;
}
.pagination span {
  font-size: 0.9rem;
  color: #606266;
  margin: 0 0.5rem;
}
.pagination button {
  padding: 0.5rem 1rem;
  border: 1px solid #dcdfe6;
  background-color: #fff;
  cursor: pointer;
  border-radius: 6px;
  transition: all 0.2s;
}
.pagination button:hover:not(:disabled) {
  border-color: #409eff;
  color: #409eff;
}
.pagination button:disabled {
  cursor: not-allowed;
  opacity: 0.6;
}
.modal-overlay {
  position: fixed;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background-color: rgba(0, 0, 0, 0.6);
  display: flex;
  justify-content: center;
  align-items: center;
  z-index: 1000;
}
.modal-content {
  background-color: white;
  padding: 2.5rem;
  border-radius: 12px;
  width: 90%;
  max-width: 500px;
  box-shadow: 0 10px 30px rgba(0,0,0,0.1);
}
.modal-content h3 { margin-top: 0; color: #333; }
.modal-content textarea {
  width: 100%;
  min-height: 150px;
  margin-bottom: 1.5rem;
  padding: 0.75rem;
  border: 1px solid #dcdfe6;
  border-radius: 8px;
  transition: border-color 0.2s;
}
.modal-content textarea:focus {
  outline: none;
  border-color: #409eff;
}
.modal-actions {
  display: flex;
  justify-content: flex-end;
  gap: 1rem;
}
.btn-primary, .btn-secondary {
    padding: 0.6rem 1.5rem;
    border-radius: 8px;
    border: none;
    color: white;
    cursor: pointer;
    font-weight: 500;
    transition: background-color 0.2s;
}
.btn-primary { background-color: #409eff; }
.btn-primary:hover { background-color: #66b1ff; }
.btn-secondary { background-color: #909399; }
.btn-secondary:hover { background-color: #a6a9ad; }
</style>