<template>
  <div class="inquiry-management">
    <div class="page-header">
      <div class="page-header-left">
        <h1>문의 관리</h1>
        <p class="page-description">전체 문의를 조회하고 답변을 처리합니다.</p>
      </div>
      <div v-if="pendingInquiries > 0" class="alert-banner pending-alert">
        🚨 처리 대기 중인 문의가 **{{ pendingInquiries }}**건 있습니다. 즉시 확인해주세요!
      </div>
    </div>

    <div class="filters">
      <div class="filter-row">
        <select v-model="filters.status" @change="searchInquiries" class="filter-select">
          <option value="">전체 상태</option>
          <option value="PENDING">처리중</option>
          <option value="ANSWERED">답변 완료</option>
        </select>
        <input 
          v-model="filters.userName" 
          type="text" 
          placeholder="작성자명 검색"
          class="filter-input"
        />
        <button @click="searchInquiries" class="search-btn">검색</button>
        <button @click="resetFilters" class="reset-btn">초기화</button>
      </div>
    </div>

    <div class="stats-grid">
      <div class="stat-card">
        <h3>전체 문의</h3>
        <p class="stat-number">{{ totalInquiries }}</p>
      </div>
      <div class="stat-card reported">
        <h3>처리중 문의</h3>
        <p class="stat-number">{{ pendingInquiries }}</p>
      </div>
      <div class="stat-card hidden">
        <h3>답변 완료</h3>
        <p class="stat-number">{{ answeredInquiries }}</p>
      </div>
    </div>

    <div class="table-container">
      <table class="admin-table">
        <thead>
          <tr>
            <th>문의번호</th>
            <th>카테고리</th>
            <th>제목</th>
            <th>작성자</th>
            <th>작성일시</th>
            <th>상태</th>
            <th>액션</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="inquiry in inquiries" :key="inquiry.id">
            <td>{{ inquiry.id }}</td>
            <td>{{ inquiry.category === 'HOTEL' ? '호텔 문의' : '웹사이트 문의' }}</td> 
            <td class="title-col" @click="viewInquiryDetail(inquiry)">
              {{ inquiry.title }}
            </td>
            <td>
              <div>{{ inquiry.userName }}</div>
              <small>{{ inquiry.userEmail }}</small>
            </td>
            <td>{{ formatDateTime(inquiry.date) }}</td>
            <td>
              <span class="status" :class="inquiry.status.toLowerCase()">
                {{ inquiry.status === 'ANSWERED' ? '답변 완료' : '처리중' }}
              </span>
            </td>
            <td>
              <button @click="viewInquiryDetail(inquiry)" class="action-btn view-btn">상세/답변</button>
            </td>
          </tr>
        </tbody>
      </table>
      <div v-if="!totalInquiries" class="no-data">
        해당 조건에 맞는 문의 내역이 없습니다.
      </div>
    </div>

    <div v-if="showDetailModal" class="modal-overlay" @click="closeDetailModal">
      <div class="modal-content" @click.stop>
        <div class="modal-header">
          <h2>문의 상세 및 답변</h2>
          <button @click="closeDetailModal" class="close-btn">×</button>
        </div>

        <div class="modal-body" v-if="selectedInquiry">
          <div class="detail-section">
            <h3>문의 정보</h3>
            <div class="detail-item"><label>문의번호:</label><span>{{ selectedInquiry.id }}</span></div>
            <div class="detail-item"><label>작성자:</label><span>{{ selectedInquiry.userName }} ({{ selectedInquiry.userEmail }})</span></div>
            <div class="detail-item"><label>작성일시:</label><span>{{ formatDateTime(selectedInquiry.date) }}</span></div>
            <div class="detail-item"><label>제목:</label><span>{{ selectedInquiry.title }}</span></div>
            <div class="detail-item full-width">
              <label>내용:</label>
              <div class="inquiry-content">{{ selectedInquiry.message }}</div>
            </div>
          </div>

          <div class="detail-section">
            <h3>관리자 답변</h3>
            <form @submit.prevent="submitReply">
              <textarea 
                v-model="replyContent"
                class="reply-textarea"
                rows="6"
                placeholder="답변을 입력하세요..."
                required
              ></textarea>
              <div class="modal-actions">
                <button type="submit" class="action-btn submit-reply-btn">답변 등록</button>
              </div>
            </form>

            <div v-if="selectedInquiry.adminReply" class="existing-reply">
              <h4>기존 답변 내용:</h4>
              <p>{{ selectedInquiry.adminReply }}</p>
              <small v-if="selectedInquiry.repliedAt">({{ formatDateTime(selectedInquiry.repliedAt) }})</small>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, computed } from 'vue';
import http from '@/api/http';

// 문의 목록
const inquiries = ref([]);
const selectedInquiry = ref(null);
const showDetailModal = ref(false);
const replyContent = ref('');

const filters = reactive({ status: '', userName: '' });
// ❌ [제거] 정렬 기능 제거: sortState 변수 제거

// 통계
const totalInquiries = computed(() => inquiries.value.length);
const pendingInquiries = computed(() => inquiries.value.filter(i => i.status === 'PENDING').length);
const answeredInquiries = computed(() => inquiries.value.filter(i => i.status === 'ANSWERED').length);

// ✅ 문의 목록 불러오기 (정렬 파라미터 제거)
const searchInquiries = async () => {
  try {
    // 🔑 [수정] 정렬 파라미터(sort) 제거
    const params = {};
    
    if (filters.status) params.status = filters.status;
    if (filters.userName) params.userName = filters.userName;

    const response = await http.get('/admin/inquiries', { params });
    
    // 서버 응답 데이터 처리
    inquiries.value = (response.data || []).map(i => ({
        ...i,
        category: (i.category || '').toUpperCase(),
        status: (i.status || 'PENDING').toUpperCase(), // 상태를 대문자로 통일
    })) || [];
  } catch (error) {
    console.error('문의 목록 로딩 오류:', error);
    alert('문의 목록을 불러오는데 실패했습니다.');
  }
};

// ❌ [제거] 정렬 토글 함수 제거
// const toggleSort = (field) => { ... };

// 필터 초기화
const resetFilters = () => {
  filters.status = '';
  filters.userName = '';
  searchInquiries();
};

// 상세 모달
const viewInquiryDetail = (inquiry) => {
  selectedInquiry.value = inquiry;
  replyContent.value = inquiry.adminReply || ''; // 기존 답변이 있으면 채워넣기
  showDetailModal.value = true;
};

const closeDetailModal = () => {
  showDetailModal.value = false;
  selectedInquiry.value = null;
  replyContent.value = '';
};

// 답변 등록
const submitReply = async () => {
  if (!selectedInquiry.value || !replyContent.value.trim()) {
    alert('답변 내용을 입력해주세요.');
    return;
  }
  if (!confirm('답변을 등록하시겠습니까?')) return;

  try {
    await http.post(`/admin/inquiries/${selectedInquiry.value.id}/reply`, {
      replyContent: replyContent.value
    });
    alert('답변이 성공적으로 등록되었습니다.');
    closeDetailModal();
    searchInquiries(); // 목록 새로고침
  } catch (error) {
    console.error('답변 등록 오류:', error);
    alert('답변 등록에 실패했습니다.');
  }
};

// 날짜 포맷
const formatDateTime = (date) => {
  if (!date) return '-';
  return new Date(date).toLocaleString('ko-KR');
};

onMounted(() => {
  searchInquiries();
});
</script>

<style scoped src="@/assets/css/admin/inquiry-management.css"></style>