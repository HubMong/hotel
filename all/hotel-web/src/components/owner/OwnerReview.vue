<template>
  <section class="reviews-section">
    <div class="header-actions">
      <h2>리뷰 관리</h2>
      <div class="user-actions">
        <span v-if="user" class="user-name">{{ user.name }}님</span>
        <button class="logout-btn" @click="$emit('logout')">로그아웃</button>
      </div>
    </div>

    <div class="review-controls">
      <select v-model="reviewFilter.hotel" class="filter-select">
        <option value="ALL">모든 호텔</option>
        <option v-for="hotel in myHotels" :key="hotel.id" :value="hotel.name">{{ hotel.name }}</option>
      </select>
      <select v-model="reviewFilter.rating" class="filter-select">
        <option value="ALL">모든 별점</option>
        <option value="5">★★★★★</option> <option value="4">★★★★☆</option> <option value="3">★★★☆☆</option> <option value="2">★★☆☆☆</option> <option value="1">★☆☆☆☆</option>
      </select>
      <select v-model="reviewFilter.replied" class="filter-select">
          <option value="ALL">전체 보기</option> <option value="REPLIED">답변 완료</option> <option value="NOT_REPLIED">미답변</option>
      </select>
      <select v-model="reviewFilter.status" class="filter-select">
        <option value="ALL">모든 리뷰</option> <option value="VISIBLE">노출 리뷰</option> <option value="REPORTED">신고 리뷰</option> <option value="HIDDEN">숨긴 리뷰</option>
      </select>
    </div>

    <div class="review-list">
      <div v-for="review in filteredReviews" :key="review.id" class="review-card" @click="showReviewDetails(review)" :class="`status-${review.status.toLowerCase()}`">
        <img :src="review.image || 'https://via.placeholder.com/150'" alt="리뷰 대표 이미지" class="review-image"/>
        <div class="review-content">
          <div class="review-header">
            <span class="review-hotel">{{ review.hotelName }}</span>
            <div class="star-rating">
              <span v-for="n in 5" :key="n" :class="{ 'filled': n <= review.star_rating }">★</span>
            </div>
          </div>
          <p class="review-text">{{ review.content }}</p>
          <div class="review-footer">
            <span class="review-author">{{ review.author }}</span>
            <span class="review-date">{{ review.wrote_on }}</span>
            <span v-if="review.reply" class="reply-badge">답변 완료</span>
            <span v-if="review.status === 'REPORTED'" class="status-badge reported">신고됨</span>
            <span v-if="review.status === 'HIDDEN'" class="status-badge hidden">숨김 처리됨</span>
          </div>
        </div>
      </div>
      <div v-if="filteredReviews.length === 0" class="no-reviews">
          해당 조건의 리뷰가 없습니다.
      </div>
    </div>

    <div v-if="selectedReview" class="modal-overlay" @click.self="closeReviewDetails">
      <div class="modal-content review-modal">
        <button class="modal-close-btn" @click="closeReviewDetails">✕</button>
        <h3>리뷰 상세 및 답변</h3>
        <div class="review-detail-content">
          <div class="review-detail-header">
            <div class="author-info">
              <strong>{{ selectedReview.author }}</strong>
              <span>{{ selectedReview.wrote_on }}</span>
            </div>
            <div class="star-rating large">
                <span v-for="n in 5" :key="n" :class="{ 'filled': n <= selectedReview.star_rating }">★</span>
            </div>
          </div>
          <p class="review-detail-text">{{ selectedReview.content }}</p>
          <img v-if="selectedReview.image" :src="selectedReview.image" alt="리뷰 이미지" class="review-detail-image"/>
        </div>

        <div class="reply-section">
          <h4>사장님 답변</h4>
          <textarea v-model="selectedReview.reply" placeholder="답변을 작성해주세요..."></textarea>
          <div class="modal-actions">
            <button v-if="selectedReview.status === 'VISIBLE'" @click="handleReportReview" class="btn-report"> 악성 리뷰로 신고 </button>
            <div v-else class="status-display"> {{ getStatusLabel(selectedReview.status) }} </div>
            <div class="reply-actions">
              <button v-if="selectedReview.replied" @click="handleReplySubmit" class="btn-primary">답변 수정</button>
              <button v-else @click="handleReplySubmit" class="btn-primary">답변 등록</button>
            </div>
          </div>
        </div> 
      </div>
    </div>
  </section>
</template>

<script>
import axios from "axios";

export default {

  
}
</script>

<style scoped>
.reviews-section { padding: 30px; display: flex; flex-direction: column; }
.header-actions { display: flex; justify-content: space-between; align-items: center; margin-bottom: 25px; }
h2 { margin: 0; font-size: 24px; color: #111827; }
.user-actions { display: flex; align-items: center; gap: 15px; }
.user-name { font-weight: 600; color: #374151; }
.logout-btn { padding: 10px 16px; background: #6b7280; color: #fff; border: none; border-radius: 6px; cursor: pointer; font-size: 14px; font-weight: 700; }
.logout-btn:hover { background: #4b5563; }
.review-controls { display: flex; gap: 15px; margin-bottom: 25px; background-color: #fff; padding: 15px; border-radius: 12px; box-shadow: 0 4px 12px rgba(0,0,0,0.08); }
.filter-select { padding: 10px; border: 1px solid #d1d5db; border-radius: 6px; font-size: 14px; }
.review-list { display: grid; grid-template-columns: 1fr; gap: 20px; }
.review-card { display: flex; background: #fff; border-radius: 12px; box-shadow: 0 4px 12px rgba(0,0,0,0.08); padding: 20px; gap: 20px; cursor: pointer; transition: transform .2s, box-shadow .2s; }
.review-card:hover { transform: translateY(-5px); box-shadow: 0 8px 20px #0000001f; }
.review-card.status-reported { border-left: 4px solid #ef4444; }
.review-card.status-hidden { opacity: 0.6; border-left: 4px solid #6b7280; }
.review-image { width: 120px; height: 120px; object-fit: cover; border-radius: 8px; flex-shrink: 0; }
.review-content { flex-grow: 1; display: flex; flex-direction: column; }
.review-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 10px; }
.review-hotel { font-weight: 700; font-size: 16px; }
.star-rating span { color: #d1d5db; font-size: 18px; }
.star-rating span.filled { color: #f59e0b; }
.review-text { margin: 0; color: #4b5563; line-height: 1.5; flex-grow: 1; display: -webkit-box; -webkit-line-clamp: 2; -webkit-box-orient: vertical; overflow: hidden; }
.review-footer { margin-top: 15px; font-size: 13px; color: #9ca3af; display: flex; align-items: center; gap: 15px; }
.reply-badge { background-color: #10b981; color: white; padding: 3px 8px; border-radius: 12px; font-size: 12px; font-weight: 700; }
.status-badge { padding: 3px 8px; border-radius: 12px; font-size: 12px; font-weight: 700; color: white; }
.status-badge.reported { background-color: #ef4444; }
.status-badge.hidden { background-color: #6b7280; }
.no-reviews { text-align: center; padding: 40px; color: #9ca3af; background: #fff; border-radius: 12px; }
.modal-overlay { position: fixed; top: 0; left: 0; right: 0; bottom: 0; background-color: rgba(0,0,0,0.6); display: flex; justify-content: center; align-items: center; z-index: 1000; }
.modal-content { background: white; padding: 30px; border-radius: 12px; width: 90%; max-width: 600px; position: relative; }
.modal-close-btn { position: absolute; top: 15px; right: 15px; background: none; border: none; font-size: 24px; cursor: pointer; color: #6b7280; }
.review-modal .review-detail-content { background: #f9fafb; padding: 20px; border-radius: 8px; margin-bottom: 25px; }
.review-detail-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 15px; }
.author-info { display: flex; flex-direction: column; }
.author-info strong { font-size: 16px; }
.author-info span { font-size: 13px; color: #6b7280; }
.star-rating.large span { font-size: 22px; }
.review-detail-text { line-height: 1.6; margin-bottom: 15px; white-space: pre-wrap; }
.review-detail-image { width: 100%; max-height: 300px; object-fit: cover; border-radius: 8px; }
.reply-section h4 { margin: 0 0 10px; }
.reply-section textarea { width: 100%; min-height: 100px; padding: 10px; border: 1px solid #d1d5db; border-radius: 6px; resize: vertical; }
.modal-actions { margin-top: 20px; padding-top: 20px; border-top: 1px solid #e5e7eb; display: flex; justify-content: space-between; align-items: center; }
.btn-report { padding: 10px 16px; border: none; border-radius: 6px; font-weight: 700; color: white; background-color: #ef4444; cursor: pointer; transition: background-color .2s; }
.btn-report:hover { background-color: #dc2626; }
.status-display { padding: 10px; color: white; background-color: #6b7280; border-radius: 6px; font-size: 14px; text-align: center; }
.reply-actions { display: flex; gap: 10px; }
.btn-primary { padding: 10px 16px; background: #3b82f6; color: #fff; border: none; border-radius: 6px; cursor: pointer; font-size: 14px; font-weight: 700; }
</style>