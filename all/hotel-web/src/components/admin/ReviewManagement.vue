<template>
  <div class="review-management">
    <div class="page-header">
      <h1>리뷰 관리</h1>
  <p class="page-description">전체 리뷰를 조회하고 숨김 처리를 관리합니다.</p>
    </div>

    <!-- 필터 및 검색 -->
    <div class="filters">
      <div class="filter-row">
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
            <th>
              리뷰번호
              <button class="sort-btn" @click.stop="sortBy('reviewId')" style="background:none;border:none;padding:0;margin-left:4px;vertical-align:middle;">
                <svg width="12" height="16" viewBox="0 0 12 24">
      hiddenReviews.value = reviews.value.filter(r => r.isHidden).length
                  <polygon points="6,22 2,16 10,16" :fill="sort.prop==='reviewId' && sort.order==='descending' ? '#222' : '#ccc'" />
                </svg>
              </button>
            </th>
            <th>
              호텔명
              <button class="sort-btn" @click.stop="sortBy('hotelName')" style="background:none;border:none;padding:0;margin-left:4px;vertical-align:middle;">
                <svg width="12" height="16" viewBox="0 0 12 24">
                  <polygon points="6,2 2,8 10,8" :fill="sort.prop==='hotelName' && sort.order==='ascending' ? '#222' : '#ccc'" />
                  <polygon points="6,22 2,16 10,16" :fill="sort.prop==='hotelName' && sort.order==='descending' ? '#222' : '#ccc'" />
                </svg>
              </button>
            </th>
            <th>
              작성자
              <button class="sort-btn" @click.stop="sortBy('userName')" style="background:none;border:none;padding:0;margin-left:4px;vertical-align:middle;">
                <svg width="12" height="16" viewBox="0 0 12 24">
                  <polygon points="6,2 2,8 10,8" :fill="sort.prop==='userName' && sort.order==='ascending' ? '#222' : '#ccc'" />
                  <polygon points="6,22 2,16 10,16" :fill="sort.prop==='userName' && sort.order==='descending' ? '#222' : '#ccc'" />
                </svg>
              </button>
            </th>
            <th>
              평점
              <button class="sort-btn" @click.stop="sortBy('starRating')" style="background:none;border:none;padding:0;margin-left:4px;vertical-align:middle;">
                <svg width="12" height="16" viewBox="0 0 12 24">
                  <polygon points="6,2 2,8 10,8" :fill="sort.prop==='starRating' && sort.order==='ascending' ? '#222' : '#ccc'" />
                  <polygon points="6,22 2,16 10,16" :fill="sort.prop==='starRating' && sort.order==='descending' ? '#222' : '#ccc'" />
                </svg>
              </button>
            </th>
            <th>제목</th>
            <th>
              작성일시
              <button class="sort-btn" @click.stop="sortBy('wroteOn')" style="background:none;border:none;padding:0;margin-left:4px;vertical-align:middle;">
                <svg width="12" height="16" viewBox="0 0 12 24">
                  <polygon points="6,2 2,8 10,8" :fill="sort.prop==='wroteOn' && sort.order==='ascending' ? '#222' : '#ccc'" />
                  <polygon points="6,22 2,16 10,16" :fill="sort.prop==='wroteOn' && sort.order==='descending' ? '#222' : '#ccc'" />
                </svg>
              </button>
            </th>
            <th>상태</th>
            <th>답변 상태</th>
            <th>액션</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="review in reviews" :key="review.reviewId" 
              :class="{ 'hidden-row': review.isHidden }">
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
                <span v-if="review.isHidden" class="status-badge hidden">숨김</span>
                <span v-if="!review.isHidden" class="status-badge normal">정상</span>
              </div>
            </td>
            <td>
              <!-- 답변 상태를 액션 버튼 디자인과 동일한 vertical stack으로 구성 -->
              <div class="action-buttons reply-action-buttons">
                <template v-if="review.adminReply">
                  <button class="action-btn show-btn" disabled style="cursor:default;">답변완료</button>
                  <button @click="startReplyEdit(review)" class="action-btn hide-btn">수정</button>
                  <button @click="deleteReplyFromList(review)" class="action-btn report-btn">삭제</button>
                </template>
                <template v-else>
                  <button @click="openQuickReplyForm(review)" class="action-btn reply-btn">답변 작성</button>
                </template>
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
                <!-- 답변 작성 버튼은 답변 상태 칼럼으로 이동 -->
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
                  @click="deleteReview(review)"
                  class="action-btn delete-btn"
                >
                  삭제
                </button>
              </div>
            </td>
          </tr>
        </tbody>
      </table>
    </div>

    <!-- 빈 데이터 메시지 -->
    <div v-if="reviews.length === 0" class="empty-message">
      <p>조건에 맞는 리뷰가 없습니다.</p>
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
                  <span v-if="selectedReview.isHidden" class="status-badge hidden">숨김</span>
                  <span v-if="!selectedReview.isHidden" class="status-badge normal">정상</span>
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
              
              <!-- 답변 수정/삭제 폼 (답변이 있을 때만) -->
              <div class="reply-form" v-if="showReplyForm && selectedReview.adminReply">
                <div class="form-header">
                  <h4>답변 수정</h4>
                  <p class="form-description">기존 답변을 수정하거나 삭제할 수 있습니다.</p>
                </div>
                <textarea 
                  v-model="replyText"
                  class="reply-textarea"
                  rows="4"
                ></textarea>
                <div class="reply-actions">
                  <button 
                    @click="deleteReply" 
                    class="btn btn-danger"
                  >
                     답변 삭제
                  </button>
                  <div class="action-group">
                    <button @click="cancelReply" class="btn btn-secondary">취소</button>
                    <button @click="submitReply" class="btn btn-primary" :disabled="!replyText.trim()">
                       답변 수정
                    </button>
                  </div>
                </div>
              </div>
              
              <!-- 답변 관리 버튼 (답변이 있을 때만 수정 가능) -->
              <div class="reply-management" v-if="!showReplyForm && selectedReview.adminReply">
                <button 
                  @click="startReply" 
                  class="btn btn-outline"
                >
                   답변 수정
                </button>
              </div>
              
              <!-- 답변이 없을 때 안내 메시지 -->
              <div class="no-reply-notice" v-if="!selectedReview.adminReply">
                <p>답변을 작성하려면 목록에서 '답변 작성' 버튼을 이용해주세요.</p>
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
              @click="deleteReview(selectedReview)"
              class="action-btn delete-btn"
            >
              리뷰 삭제
            </button>
          </div>
        </div>
      </div>
    </div>

    <!-- 답변 작성 모달 -->
    <div v-if="showReplyModal" class="modal-overlay" @click="closeReplyModal">
      <div class="modal-content reply-modal" @click.stop>
        <div class="modal-header">
          <h2>리뷰 답변 작성</h2>
          <button @click="closeReplyModal" class="close-btn">×</button>
        </div>

        <div class="modal-body" v-if="selectedReplyReview">
          <div class="reply-form-container">
            <div class="review-info">
              <h3>{{ selectedReplyReview.hotelName }}</h3>
              <div class="reviewer-info">
                <span class="reviewer-name">{{ selectedReplyReview.userName }}</span>
                <span class="review-rating">{{ getStarRating(selectedReplyReview.starRating) }} ({{ selectedReplyReview.starRating }}/5)</span>
              </div>
              <div class="review-content-preview">
                "{{ selectedReplyReview.content?.substring(0, 200) }}{{ selectedReplyReview.content?.length > 200 ? '...' : '' }}"
              </div>
            </div>

            <div class="reply-form">
              <label for="reply-textarea">답변 내용</label>
              <textarea
                id="reply-textarea"
                v-model="quickReplyText"
                placeholder="고객님의 소중한 리뷰에 정중하고 도움이 되는 답변을 작성해주세요..."
                class="reply-textarea"
                rows="6"
              ></textarea>
            </div>
          </div>

          <div class="modal-actions">
            <button @click="closeReplyModal" class="btn btn-secondary">취소</button>
            <button @click="submitQuickReply" class="btn btn-primary" :disabled="!quickReplyText.trim()">
               답변 등록
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
  // 신고 필터 제거
    const showHiddenOnly = ref(false)
    
    // 답변 관련 상태
    const showReplyForm = ref(false)
    const replyText = ref('')
    
    // 빠른 답변 관련 상태
    const quickReplyRowId = ref(null)
    const quickReplyText = ref('')
    const showReplyModal = ref(false)
    const selectedReplyReview = ref(null)
    
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

    // 정렬 상태
    const sort = ref({ prop: 'wroteOn', order: 'descending' })

    // 통계 데이터
    const totalReviews = ref(0)
  // 신고 통계 제거
    const hiddenReviews = ref(0)
    const averageRating = ref(0)

    // 리뷰 목록 조회
    const searchReviews = async () => {
      try {
        const params = {
          page: pagination.currentPage,
          size: pagination.size
        }

        if (showHiddenOnly.value) params.hidden = true
        if (filters.hotelName) params.hotelName = filters.hotelName
        if (filters.userName) params.userName = filters.userName
        const sortParam = toApiSort(sort.value)
        if (sortParam) params.sort = sortParam

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
  // 신고 통계 제거됨
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

    // 정렬 핸들러들
    const sortBy = (prop) => {
      if (sort.value.prop === prop) {
        sort.value.order = sort.value.order === 'ascending' ? 'descending' : 'ascending'
      } else {
        sort.value = { prop, order: 'ascending' }
      }
      pagination.currentPage = 0
      searchReviews()
    }
    const sortIcon = (prop) => {
      if (sort.value.prop !== prop) return ''
      return sort.value.order === 'ascending' ? 'asc' : 'desc'
    }
    const toApiSort = ({ prop, order }) => {
      if (!prop || !order) return null
      const dir = order === 'ascending' ? 'asc' : 'desc'
      // 백엔드가 지원하는 필드만 허용
      const allowed = new Set(['reviewId','hotelName','userName','starRating','wroteOn'])
      if (!allowed.has(prop)) return null
      return `${prop},${dir}`
    }

    // 필터 초기화
    const resetFilters = () => {
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
      showReplyForm.value = false
      replyText.value = ''
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

    // 신고 처리 기능 제거됨

    // 리뷰 삭제
    const deleteReview = async (review) => {
      const id = review?.reviewId
      if (!id) return
      if (!confirm('이 리뷰를 삭제하시겠습니까? 삭제 후에는 복구할 수 없습니다.')) return

      try {
        await axios.delete(`/admin/reviews/${id}`)
        alert('리뷰가 성공적으로 삭제되었습니다.')

        // 상세 모달에서 삭제한 경우 닫기
        if (selectedReview.value && selectedReview.value.reviewId === id) {
          closeDetailModal()
        }

        // 목록 새로고침 및 통계 반영
        // 빠른 체감 반영을 위해 로컬에서도 제거 후 통계 업데이트, 이어서 서버 동기화
        reviews.value = reviews.value.filter(r => r.reviewId !== id)
        updateStatistics()
        // 서버 기준 재조회로 페이지네이션 등 갱신
        searchReviews()
      } catch (error) {
        console.error('리뷰 삭제 오류:', error)
        let msg = '리뷰 삭제에 실패했습니다.'
        try {
          msg = error?.response?.data?.message || msg
        } catch {}
        alert(msg)
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

    // 답변 작성 시작
    const startReply = () => {
      replyText.value = selectedReview.value.adminReply || ''
      showReplyForm.value = true
    }

    // 목록에서 답변 수정 시작
    const startReplyEdit = (review) => {
      selectedReplyReview.value = review
      quickReplyText.value = review.adminReply || ''
      showReplyModal.value = true
    }

    // 목록에서 답변 삭제
    const deleteReplyFromList = async (review) => {
      if (!confirm('정말로 이 답변을 삭제하시겠습니까?')) {
        return
      }

      try {
        await axios.delete(`/admin/reviews/${review.reviewId}/reply`)
        
        alert('답변이 성공적으로 삭제되었습니다.')
        
        // 목록에서 답변 정보 제거
        review.adminReply = null
        review.repliedAt = null
        
        // 목록 새로고침
        searchReviews()
        
      } catch (error) {
        console.error('답변 삭제 오류:', error)
        alert('답변 삭제에 실패했습니다.')
      }
    }

    // 답변 작성/수정 제출
    const submitReply = async () => {
      if (!replyText.value.trim()) {
        alert('답변 내용을 입력해주세요.')
        return
      }

      try {
        const reviewId = selectedReview.value.reviewId
        await axios.post(`/admin/reviews/${reviewId}/reply`, {
          reply: replyText.value.trim()
        })

        alert('답변이 성공적으로 등록되었습니다.')
        
        // 선택된 리뷰 정보 업데이트
        selectedReview.value.adminReply = replyText.value.trim()
        selectedReview.value.repliedAt = new Date().toISOString()
        
        // 폼 닫기
        showReplyForm.value = false
        replyText.value = ''
        
        // 목록 새로고침
        searchReviews()
        
      } catch (error) {
        console.error('답변 등록 오류:', error)
        alert('답변 등록에 실패했습니다.')
      }
    }

    // 답변 작성 취소
    const cancelReply = () => {
      showReplyForm.value = false
      replyText.value = ''
    }

    // 답변 삭제
    const deleteReply = async () => {
      if (!confirm('답변을 삭제하시겠습니까?')) return

      try {
        const reviewId = selectedReview.value.reviewId
        await axios.delete(`/admin/reviews/${reviewId}/reply`)

        alert('답변이 성공적으로 삭제되었습니다.')
        
        // 선택된 리뷰 정보 업데이트
        selectedReview.value.adminReply = null
        selectedReview.value.repliedAt = null
        
        // 폼 닫기
        showReplyForm.value = false
        replyText.value = ''
        
        // 목록 새로고침
        searchReviews()
        
      } catch (error) {
        console.error('답변 삭제 오류:', error)
        alert('답변 삭제에 실패했습니다.')
      }
    }

    // 빠른 답변 작성 폼 열기
    const openQuickReplyForm = (review) => {
      selectedReplyReview.value = review
      quickReplyText.value = ''
      showReplyModal.value = true
    }

    // 빠른 답변 취소
    const cancelQuickReply = () => {
      closeReplyModal()
    }

    // 답변 모달 닫기
    const closeReplyModal = () => {
      showReplyModal.value = false
      selectedReplyReview.value = null
      quickReplyText.value = ''
    }

    // 빠른 답변 제출
    const submitQuickReply = async () => {
      if (!quickReplyText.value.trim()) {
        alert('답변 내용을 입력해주세요.')
        return
      }

      try {
        await axios.post(`/admin/reviews/${selectedReplyReview.value.reviewId}/reply`, {
          reply: quickReplyText.value.trim()
        })

        alert('답변이 성공적으로 등록되었습니다.')
        
        // 답변 모달 닫기
        closeReplyModal()
        
        // 목록 새로고침
        searchReviews()
        
      } catch (error) {
        console.error('빠른 답변 등록 오류:', error)
        alert('답변 등록에 실패했습니다.')
      }
    }

    // 컴포넌트 마운트 시 데이터 로드
    onMounted(() => {
      searchReviews()
    })

    return {
      reviews,
      selectedReview,
      showDetailModal,
      
      showHiddenOnly,
      filters,
      pagination,
      sort,
      totalReviews,
      
      hiddenReviews,
      averageRating,
      searchReviews,
      changePage,
      resetFilters,
      sortBy,
      sortIcon,
      viewReviewDetail,
      closeDetailModal,
      hideReview,
      showReview,
      
      deleteReview,
      getStarRating,
      getImageUrls,
      formatDateTime,
      
      // 답변 관련
      showReplyForm,
      replyText,
      startReply,
      submitReply,
      cancelReply,
      deleteReply,
      
      // 목록에서 답변 관리
      startReplyEdit,
      deleteReplyFromList,
      
      // 빠른 답변 관련
      showReplyModal,
      selectedReplyReview,
      quickReplyText,
      openQuickReplyForm,
      cancelQuickReply,
      submitQuickReply,
      closeReplyModal
    }
  }
}
</script>

<style scoped src="@/assets/css/admin/review-management.css"></style>