<template>
  <div class="review-management">
    <div class="page-header">
      <h1>리뷰 관리</h1>
      <p class="page-description">전체 리뷰를 조회하고 신고된 리뷰를 숨김 처리합니다.</p>
    </div>

    <!-- 필터 및 검색 -->
    <div class="filters">
      <div class="filter-row">
        <div class="filter-group">
          <label>
            <input 
              v-model="showReportedOnly" 
              type="checkbox"
              @change="searchReviews"
            />
            신고된 리뷰만 보기
          </label>
        </div>
        <div class="filter-group">
          <label>
            <input 
              v-model="showHiddenOnly" 
              type="checkbox"
              @change="searchReviews"
            />
            숨김 처리된 리뷰만 보기
          </label>
        </div>
        <input 
          v-model="filters.hotelName" 
          type="text" 
          placeholder="호텔명 검색"
          class="filter-input"
        />
        <input 
          v-model="filters.userName" 
          type="text" 
          placeholder="작성자명 검색"
          class="filter-input"
        />
        <button @click="searchReviews" class="search-btn">검색</button>
        <button @click="resetFilters" class="reset-btn">초기화</button>
      </div>
    </div>

    <!-- 통계 카드 -->
    <div class="stats-grid">
      <div class="stat-card">
        <h3>전체 리뷰</h3>
        <p class="stat-number">{{ totalReviews }}</p>
      </div>
      <div class="stat-card reported">
        <h3>신고된 리뷰</h3>
        <p class="stat-number">{{ reportedReviews }}</p>
      </div>
      <div class="stat-card hidden">
        <h3>숨김 처리</h3>
        <p class="stat-number">{{ hiddenReviews }}</p>
      </div>
      <div class="stat-card rating">
        <h3>평균 평점</h3>
        <p class="stat-number">{{ averageRating }}</p>
      </div>
    </div>

    <!-- 리뷰 목록 테이블 -->
    <div class="table-container">
      <table class="review-table">
        <thead>
          <tr>
            <th>리뷰번호</th>
            <th>호텔명</th>
            <th>작성자</th>
            <th>평점</th>
            <th>제목</th>
            <th>작성일시</th>
            <th>상태</th>
            <th>액션</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="review in reviews" :key="review.reviewId" 
              :class="{ 'reported-row': review.isReported, 'hidden-row': review.isHidden }">
            <td>{{ review.reviewId }}</td>
            <td>{{ review.hotelName }}</td>
            <td>
              <div>
                <div>{{ review.userName }}</div>
                <small>{{ review.userEmail }}</small>
              </div>
            </td>
            <td>
              <div class="rating">
                <span class="stars">{{ getStarRating(review.starRating) }}</span>
                <small>({{ review.starRating }}/5)</small>
              </div>
            </td>
            <td>
              <div class="review-title" @click="viewReviewDetail(review)">
                <small v-if="review.content && review.content.length > 50">
                  {{ review.content.substring(0, 50) }}...
                </small>
                <small v-else-if="review.content">
                  {{ review.content }}
                </small>
                <small v-else>내용 없음</small>
              </div>
            </td>
            <td>{{ formatDateTime(review.wroteOn) }}</td>
            <td>
              <div class="status-badges">
                <span v-if="review.isReported" class="status-badge reported">신고됨</span>
                <span v-if="review.isHidden" class="status-badge hidden">숨김</span>
                <span v-if="!review.isReported && !review.isHidden" class="status-badge normal">정상</span>
              </div>
            </td>
            <td>
              <div class="action-buttons">
                <button 
                  @click="viewReviewDetail(review)" 
                  class="action-btn view-btn"
                >
                  상세보기
                </button>
                <button 
                  v-if="!review.isHidden"
                  @click="hideReview(review)" 
                  class="action-btn hide-btn"
                >
                  숨김
                </button>
                <button 
                  v-else
                  @click="showReview(review)" 
                  class="action-btn show-btn"
                >
                  숨김해제
                </button>
                <button 
                  v-if="!review.isReported"
                  @click="reportReview(review)" 
                  class="action-btn report-btn"
                >
                  신고처리
                </button>
              </div>
            </td>
          </tr>
        </tbody>
      </table>

      <!-- 빈 데이터 메시지 -->
      <div v-if="reviews.length === 0" class="empty-message">
        <p>조건에 맞는 리뷰가 없습니다.</p>
      </div>
    </div>

    <!-- 페이지네이션 -->
    <div class="pagination" v-if="pagination.totalPages > 1">
      <button 
        @click="changePage(pagination.currentPage - 1)"
        :disabled="pagination.currentPage === 0"
        class="page-btn"
      >
        이전
      </button>
      
      <span class="page-info">
        {{ pagination.currentPage + 1 }} / {{ pagination.totalPages }}
      </span>
      
      <button 
        @click="changePage(pagination.currentPage + 1)"
        :disabled="pagination.currentPage >= pagination.totalPages - 1"
        class="page-btn"
      >
        다음
      </button>
    </div>

    <!-- 리뷰 상세 모달 -->
    <div v-if="showDetailModal" class="modal-overlay" @click="closeDetailModal">
      <div class="modal-content" @click.stop>
        <div class="modal-header">
          <h2>리뷰 상세 정보</h2>
          <button @click="closeDetailModal" class="close-btn">×</button>
        </div>
        
        <div class="modal-body" v-if="selectedReview">
          <div class="detail-grid">
            <!-- 리뷰 정보 -->
            <div class="detail-section">
              <h3>리뷰 정보</h3>
              <div class="detail-item">
                <label>리뷰번호:</label>
                <span>{{ selectedReview.reviewId }}</span>
              </div>
              <div class="detail-item">
                <label>평점:</label>
                <span class="rating-detail">
                  {{ getStarRating(selectedReview.rating) }} ({{ selectedReview.rating }}/5)
                </span>
              </div>
              <div class="detail-item">
                <label>제목:</label>
                <span>{{ selectedReview.title }}</span>
              </div>
              <div class="detail-item full-width">
                <label>내용:</label>
                <div class="review-content">{{ selectedReview.content }}</div>
              </div>
              <div class="detail-item" v-if="selectedReview.imageUrls">
                <label>첨부 이미지:</label>
                <div class="image-list">
                  <img v-for="(url, index) in getImageUrls(selectedReview.imageUrls)" 
                       :key="index" :src="url" alt="리뷰 이미지" class="review-image" />
                </div>
              </div>
              <div class="detail-item">
                <label>작성일시:</label>
                <span>{{ formatDateTime(selectedReview.createdAt) }}</span>
              </div>
              <div class="detail-item">
                <label>상태:</label>
                <div class="status-badges">
                  <span v-if="selectedReview.isReported" class="status-badge reported">신고됨</span>
                  <span v-if="selectedReview.isHidden" class="status-badge hidden">숨김</span>
                  <span v-if="!selectedReview.isReported && !selectedReview.isHidden" class="status-badge normal">정상</span>
                </div>
              </div>
            </div>

            <!-- 작성자 정보 -->
            <div class="detail-section">
              <h3>작성자 정보</h3>
              <div class="detail-item">
                <label>사용자번호:</label>
                <span>{{ selectedReview.userId }}</span>
              </div>
              <div class="detail-item">
                <label>작성자명:</label>
                <span>{{ selectedReview.userName }}</span>
              </div>
              <div class="detail-item">
                <label>이메일:</label>
                <span>{{ selectedReview.userEmail }}</span>
              </div>
            </div>

            <!-- 호텔 정보 -->
            <div class="detail-section">
              <h3>호텔 정보</h3>
              <div class="detail-item">
                <label>호텔번호:</label>
                <span>{{ selectedReview.hotelId }}</span>
              </div>
              <div class="detail-item">
                <label>호텔명:</label>
                <span>{{ selectedReview.hotelName }}</span>
              </div>
              <div class="detail-item">
                <label>예약번호:</label>
                <span>{{ selectedReview.reservationNumber }}</span>
              </div>
            </div>

            <!-- 관리자 답변 -->
            <div class="detail-section">
              <h3>관리자 답변</h3>
              <div class="detail-item full-width">
                <label>답변 내용:</label>
                <div v-if="selectedReview.adminReply" class="admin-reply">
                  {{ selectedReview.adminReply }}
                  <small v-if="selectedReview.repliedAt">
                    ({{ formatDateTime(selectedReview.repliedAt) }})
                  </small>
                </div>
                <div v-else class="no-reply">
                  관리자 답변이 없습니다.
                </div>
              </div>
            </div>
          </div>

          <!-- 액션 버튼 -->
          <div class="modal-actions">
            <button 
              v-if="!selectedReview.isHidden"
              @click="hideReview(selectedReview)" 
              class="action-btn hide-btn"
            >
              리뷰 숨김
            </button>
            <button 
              v-else
              @click="showReview(selectedReview)" 
              class="action-btn show-btn"
            >
              숨김 해제
            </button>
            <button 
              v-if="!selectedReview.isReported"
              @click="reportReview(selectedReview)" 
              class="action-btn report-btn"
            >
              신고 처리
            </button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import { ref, reactive, onMounted } from 'vue'
import axios from '@/api/http.js'

export default {
  name: 'ReviewManagement',
  setup() {
    const reviews = ref([])
    const selectedReview = ref(null)
    const showDetailModal = ref(false)
    const showReportedOnly = ref(false)
    const showHiddenOnly = ref(false)
    
    // 필터 상태
    const filters = reactive({
      hotelName: '',
      userName: ''
    })

    // 페이지네이션 상태
    const pagination = reactive({
      currentPage: 0,
      totalPages: 0,
      totalElements: 0,
      size: 20
    })

    // 통계 데이터
    const totalReviews = ref(0)
    const reportedReviews = ref(0)
    const hiddenReviews = ref(0)
    const averageRating = ref(0)

    // 리뷰 목록 조회
    const searchReviews = async () => {
      try {
        const params = {
          page: pagination.currentPage,
          size: pagination.size
        }

        if (showReportedOnly.value) params.reported = true
        if (showHiddenOnly.value) params.hidden = true
        if (filters.hotelName) params.hotelName = filters.hotelName
        if (filters.userName) params.userName = filters.userName

        console.log('리뷰 목록 요청 파라미터:', params)

        const response = await axios.get('/admin/reviews', { params })
        console.log('리뷰 목록 응답:', response.data)

        const data = response.data?.data || { content: [], totalPages: 0, totalElements: 0 }
        reviews.value = data.content || []
        
        pagination.totalPages = data.totalPages || 0
        pagination.totalElements = data.totalElements || 0
        
        updateStatistics()
        
      } catch (error) {
        console.error('리뷰 목록 로딩 오류:', error)
        alert('리뷰 목록을 불러오는데 실패했습니다.')
      }
    }

    // 통계 업데이트
    const updateStatistics = () => {
      totalReviews.value = reviews.value.length
      reportedReviews.value = reviews.value.filter(r => r.isReported).length
      hiddenReviews.value = reviews.value.filter(r => r.isHidden).length
      
        if (reviews.value.length > 0) {
        const avgRating = reviews.value.reduce((sum, r) => sum + r.starRating, 0) / reviews.value.length
        averageRating.value = avgRating.toFixed(1)
      }
    }

    // 페이지 변경
    const changePage = (page) => {
      pagination.currentPage = page
      searchReviews()
    }

    // 필터 초기화
    const resetFilters = () => {
      showReportedOnly.value = false
      showHiddenOnly.value = false
      filters.hotelName = ''
      filters.userName = ''
      pagination.currentPage = 0
      searchReviews()
    }

    // 리뷰 상세 보기
    const viewReviewDetail = async (review) => {
      try {
        console.log('리뷰 상세 조회 시작:', review.reviewId)
        
        const response = await axios.get(`/admin/reviews/${review.reviewId}`)
        console.log('리뷰 상세 조회 응답:', response.data)

        const data = response.data?.data || {}
        selectedReview.value = {
          reviewId: data.reviewId || review.reviewId,
          rating: data.starRating || review.starRating,
          content: data.content || review.content || '',
          imageUrls: data.image || review.image,
          createdAt: data.wroteOn || review.wroteOn,
          isReported: data.isReported || review.isReported || false,
          isHidden: data.isHidden || review.isHidden || false,
          adminReply: data.adminReply || '',
          repliedAt: data.repliedAt,
          
          userId: data.userId || review.userId,
          userName: data.userName || review.userName,
          userEmail: data.userEmail || review.userEmail,
          
          hotelId: data.hotelId || review.hotelId,
          hotelName: data.hotelName || review.hotelName,
          reservationNumber: data.transactionId || review.transactionId
        }
        
        showDetailModal.value = true
        
      } catch (error) {
        console.error('리뷰 상세 조회 오류:', error)
        alert('리뷰 상세 정보를 불러오는데 실패했습니다.')
      }
    }

    // 상세 모달 닫기
    const closeDetailModal = () => {
      showDetailModal.value = false
      selectedReview.value = null
    }

    // 리뷰 숨김 처리
    const hideReview = async (review) => {
      if (!confirm('이 리뷰를 숨김 처리하시겠습니까?')) return

      try {
        await axios.put(`/admin/reviews/${review.reviewId}/hide`)

        alert('리뷰가 성공적으로 숨김 처리되었습니다.')
        searchReviews()
        
        if (selectedReview.value && selectedReview.value.reviewId === review.reviewId) {
          selectedReview.value.isHidden = true
        }
        
      } catch (error) {
        console.error('리뷰 숨김 처리 오류:', error)
        alert('리뷰 숨김 처리에 실패했습니다.')
      }
    }

    // 리뷰 숨김 해제
    const showReview = async (review) => {
      if (!confirm('이 리뷰의 숨김을 해제하시겠습니까?')) return

      try {
        await axios.put(`/admin/reviews/${review.reviewId}/show`)

        alert('리뷰 숨김이 성공적으로 해제되었습니다.')
        searchReviews()
        
        if (selectedReview.value && selectedReview.value.reviewId === review.reviewId) {
          selectedReview.value.isHidden = false
        }
        
      } catch (error) {
        console.error('리뷰 숨김 해제 오류:', error)
        alert('리뷰 숨김 해제에 실패했습니다.')
      }
    }

    // 리뷰 신고 처리
    const reportReview = async (review) => {
      if (!confirm('이 리뷰를 신고 처리하시겠습니까?')) return

      try {
        await axios.put(`/admin/reviews/${review.reviewId}/report`)

        alert('리뷰가 성공적으로 신고 처리되었습니다.')
        searchReviews()
        
        if (selectedReview.value && selectedReview.value.reviewId === review.reviewId) {
          selectedReview.value.isReported = true
        }
        
      } catch (error) {
        console.error('리뷰 신고 처리 오류:', error)
        alert('리뷰 신고 처리에 실패했습니다.')
      }
    }

    // 별점 표시
    const getStarRating = (rating) => {
      return '★'.repeat(rating) + '☆'.repeat(5 - rating)
    }

    // 이미지 URL 파싱
    const getImageUrls = (imageUrlString) => {
      if (!imageUrlString) return []
      try {
        return JSON.parse(imageUrlString)
      } catch {
        return imageUrlString.split(',').map(url => url.trim())
      }
    }

    // 날짜시간 포맷팅
    const formatDateTime = (dateString) => {
      if (!dateString) return '-'
      return new Date(dateString).toLocaleString('ko-KR')
    }

    // 컴포넌트 마운트 시 데이터 로드
    onMounted(() => {
      searchReviews()
    })

    return {
      reviews,
      selectedReview,
      showDetailModal,
      showReportedOnly,
      showHiddenOnly,
      filters,
      pagination,
      totalReviews,
      reportedReviews,
      hiddenReviews,
      averageRating,
      searchReviews,
      changePage,
      resetFilters,
      viewReviewDetail,
      closeDetailModal,
      hideReview,
      showReview,
      reportReview,
      getStarRating,
      getImageUrls,
      formatDateTime
    }
  }
}
</script>

<style scoped src="@/assets/css/admin/review-management.css"></style>