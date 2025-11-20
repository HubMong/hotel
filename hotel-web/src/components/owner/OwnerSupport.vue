<template>
  <div class="owner-inquiry-management">
    <div class="page-header">
      <h1>내 호텔 문의 관리</h1>
      <p class="page-description">투숙객의 예약 및 결제 관련 문의를 처리합니다.</p>
    </div>

    <div class="stats-grid">
      <div class="stat-card">
        <h3>총 문의 수</h3>
        <p class="stat-number">{{ totalInquiries }}</p>
      </div>
      <div class="stat-card pending">
        <h3>처리 대기</h3>
        <p class="stat-number">{{ pendingInquiries }}</p>
      </div>
      <div class="stat-card answered">
        <h3>답변 완료</h3>
        <p class="stat-number">{{ answeredInquiries }}</p>
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

    <div class="table-container">
      <table class="admin-table">
        <thead>
          <tr>
            <th>문의번호</th>
            <th>호텔/객실 명</th>
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
            
            <td>
              <div>{{ inquiry.hotelName }}</div>
              <small>{{ inquiry.roomName || '객실 정보 없음' }}</small>
            </td>
            
            <td class="title-col" @click="viewInquiryDetail(inquiry)">
              {{ inquiry.title }}
            </td>
            <td>
              <div>{{ inquiry.userName }}</div>
              <small>{{ inquiry.userEmail }}</small>
            </td>
            <td>{{ formatDateTime(inquiry.date) }}</td>
            <td>
              <span
                class="status"
                :class="inquiry.status === '답변 완료' || inquiry.status === 'ANSWERED' ? 'answered' : 'pending'"
              >
                {{ inquiry.status === '답변 완료' || inquiry.status === 'ANSWERED' ? '답변 완료' : '처리중' }}
              </span>
            </td>
            <td>
              <button @click="viewInquiryDetail(inquiry)" class="action-btn view-btn">
                상세/답변
              </button>
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
            <p><strong>제목:</strong> {{ selectedInquiry.title }}</p>
            <p><strong>호텔명:</strong> {{ selectedInquiry.hotelName }}</p>
            <p><strong>객실명:</strong> {{ selectedInquiry.roomName || '정보 없음' }}</p>
            <p><strong>내용:</strong> {{ selectedInquiry.message }}</p>
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
                <button type="submit" class="action-btn submit-reply-btn">
                  답변 등록
                </button>
              </div>
            </form>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue';
import http from '@/api/http';

// ======================= 상태 변수 =======================
const inquiries = ref([]);
const selectedInquiry = ref(null);
const showDetailModal = ref(false);
const replyContent = ref('');
const userHotels = ref([]);

// ======================= 필터 =======================
const filters = reactive({
  status: '',
  userName: ''
});

// ======================= 통계 계산 =======================
const totalInquiries = computed(() => inquiries.value.length);
const pendingInquiries = computed(() =>
  inquiries.value.filter(i =>
    i.status === '처리중' || i.status === '답변 대기' || i.status === 'PENDING'
  ).length
);
const answeredInquiries = computed(() =>
  inquiries.value.filter(i =>
    i.status === '답변 완료' || i.status === 'ANSWERED'
  ).length
);

// ======================= 소유 호텔 조회 =======================
const fetchOwnerHotels = async () => {
  try {
    const response = await http.get('/owner/hotels/my-hotels');
    userHotels.value = response.data || [];
  } catch (error) {
    userHotels.value = [];
  }
};

// ======================= 문의 검색 =======================
const searchInquiries = async () => {
  if (userHotels.value.length === 0) {
    inquiries.value = [];
    return;
  }

  try {
    const params = new URLSearchParams();
    userHotels.value.forEach(h => params.append('hotelIds', h.id));
    if (filters.status) params.append('status', filters.status);
    if (filters.userName) params.append('userName', filters.userName);

    const response = await http.get('/owner/inquiries', { params });

    // 🔑 [수정] 사용하지 않는 필드(createdAt, reservationId) 제거
    inquiries.value = response.data.map(i => ({
      id: i.id,
      hotelName: i.hotelName,
      title: i.title,
      message: i.message,
      date: i.date,
      status: i.status,
      adminReply: i.adminReply,
      userName: i.userName,
      // userEmail: i.userEmail || '',
      roomName: i.roomName || '정보 없음',
    }));
  } catch (error) {
    alert('문의 목록을 불러오는데 실패했습니다. 서버 연결을 확인하세요.');
  }
};

// ======================= 필터 초기화 =======================
const resetFilters = () => {
  filters.status = '';
  filters.userName = '';
  searchInquiries();
};

// ======================= 상세 보기 =======================
const viewInquiryDetail = (inquiry) => {
  selectedInquiry.value = { ...inquiry };
  replyContent.value = inquiry.adminReply || '';
  showDetailModal.value = true;
};

const closeDetailModal = () => {
  showDetailModal.value = false;
  selectedInquiry.value = null;
  replyContent.value = '';
};

// ======================= 답변 등록 =======================
const submitReply = async () => {
  if (!selectedInquiry.value || !replyContent.value.trim()) {
    alert('답변 내용을 입력해주세요.');
    return;
  }
  if (!confirm('답변을 등록하시겠습니까?')) return;

  try {
    const inquiryId = selectedInquiry.value.id;
    await http.post(`/owner/inquiries/${inquiryId}/reply`, {
      replyContent: replyContent.value
    });

    alert('답변이 성공적으로 등록되었습니다.');
    closeDetailModal();
    await searchInquiries();
  } catch (error) {
    alert('답변 등록에 실패했습니다. 서버 응답을 확인하세요.');
  }
};

// ======================= 날짜 포맷 =======================
const formatDateTime = (dateString) => {
  if (!dateString) return '-';
  return dateString.substring(0, 16);
};

// ======================= 마운트 시 실행 =======================
onMounted(async () => {
  await fetchOwnerHotels();
  await searchInquiries();
});
</script>

<style>
/* ======================================================================
  Owner/Admin Inquiry Management Styles
  (CSS는 그대로 유지됩니다.)
  ====================================================================== */

/* 전체 컨테이너 및 페이지 제목 */
.owner-inquiry-management {
    padding: 30px;
    background-color: #f7f8fa;
    min-height: 100vh;
}

.page-header h1 {
    font-size: 28px;
    font-weight: 700;
    color: #1f2937;
    margin-bottom: 5px;
}

.page-description {
    color: #6b7280;
    margin-bottom: 25px;
}

/* 통계 카드 그리드 */
.stats-grid {
    display: grid;
    grid-template-columns: repeat(3, 1fr);
    gap: 20px;
    margin-bottom: 30px;
}

.stat-card {
    background: #ffffff;
    padding: 20px;
    border-radius: 12px;
    box-shadow: 0 4px 6px rgba(0, 0, 0, 0.05);
    border-left: 5px solid #3b82f6; /* 기본 파란색 */
}

.stat-card.pending {
    border-left-color: #f59e0b; /* 주황색 */
}

.stat-card.answered {
    border-left-color: #10b981; /* 녹색 */
}

.stat-card h3 {
    font-size: 14px;
    color: #4b5563;
    margin-top: 0;
    margin-bottom: 10px;
    font-weight: 600;
}

.stat-number {
    font-size: 32px;
    font-weight: 800;
    color: #111827;
}

/* 필터 영역 */
.filters {
    background: #ffffff;
    padding: 20px;
    border-radius: 12px;
    box-shadow: 0 2px 4px rgba(0, 0, 0, 0.04);
    margin-bottom: 25px;
}

.filter-row {
    display: flex;
    gap: 10px;
    align-items: center;
}

.filter-select, .filter-input {
    padding: 10px 12px;
    border: 1px solid #d1d5db;
    border-radius: 6px;
    font-size: 14px;
    box-sizing: border-box;
}

.filter-select {
    min-width: 150px;
}

.filter-input {
    flex-grow: 1;
    max-width: 300px;
}

.search-btn, .reset-btn {
    padding: 10px 15px;
    border: none;
    border-radius: 6px;
    font-weight: 600;
    cursor: pointer;
    transition: background-color 0.2s;
}

.search-btn {
    background-color: #3b82f6;
    color: white;
}

.search-btn:hover {
    background-color: #2563eb;
}

.reset-btn {
    background-color: #e5e7eb;
    color: #374151;
}

.reset-btn:hover {
    background-color: #d1d5db;
}


/* 테이블 */
.table-container {
    background: #ffffff;
    border-radius: 12px;
    box-shadow: 0 2px 4px rgba(0, 0, 0, 0.04);
    overflow: hidden;
}

.admin-table {
    width: 100%;
    border-collapse: collapse;
    font-size: 14px;
    table-layout: fixed; /* 테이블 레이아웃 고정 */
}

.admin-table th, .admin-table td {
    padding: 15px 12px;
    text-align: left;
    border-bottom: 1px solid #e5e7eb;
}

.admin-table thead th {
    background-color: #f9fafb;
    color: #4b5563;
    font-weight: 700;
    text-transform: uppercase;
}

.admin-table tbody tr:hover {
    background-color: #f3f4f6;
    cursor: default;
}

.admin-table td.title-col {
    font-weight: 600;
    color: #1f2937;
    cursor: pointer;
    max-width: 250px;
    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;
}

.admin-table td small {
    display: block;
    color: #6b7280;
    font-size: 12px;
    margin-top: 2px;
}

/* 상태 태그 */
.status {
    padding: 4px 8px;
    border-radius: 4px;
    font-weight: 600;
    font-size: 12px;
    display: inline-block;
}

.status.pending {
    background-color: #fef3c7;
    color: #b45309;
}

.status.answered {
    background-color: #d1fae5;
    color: #065f46;
}

/* 액션 버튼 */
.action-btn {
    padding: 8px 12px;
    border: 1px solid #d1d5db;
    border-radius: 6px;
    background-color: white;
    font-weight: 600;
    cursor: pointer;
    transition: background-color 0.2s, border-color 0.2s;
}

.action-btn.view-btn:hover {
    background-color: #f3f4f6;
    border-color: #9ca3af;
}

.no-data {
    text-align: center;
    padding: 50px 0;
    color: #6b7280;
    font-size: 16px;
}

/* ======================================================================
  모달 (Modal) 스타일
  ====================================================================== */
.modal-overlay {
    position: fixed;
    top: 0;
    left: 0;
    right: 0;
    bottom: 0;
    background: rgba(0, 0, 0, 0.6);
    display: flex;
    justify-content: center;
    align-items: center;
    z-index: 1000;
}

.modal-content {
    background: white;
    padding: 25px;
    border-radius: 12px;
    width: 90%;
    max-width: 700px;
    box-shadow: 0 10px 25px rgba(0, 0, 0, 0.2);
    max-height: 90vh;
    overflow-y: auto;
}

.modal-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    border-bottom: 1px solid #e5e7eb;
    padding-bottom: 15px;
    margin-bottom: 20px;
}

.modal-header h2 {
    font-size: 22px;
    font-weight: 700;
    margin: 0;
}

.close-btn {
    background: none;
    border: none;
    font-size: 24px;
    cursor: pointer;
    color: #4b5563;
}

.modal-body {
    display: flex;
    flex-direction: column;
    gap: 20px;
}

.detail-section {
    border: 1px solid #e5e7eb;
    padding: 15px;
    border-radius: 8px;
}

.detail-section h3 {
    font-size: 16px;
    font-weight: 700;
    color: #1f2937;
    margin-top: 0;
    margin-bottom: 15px;
}

.detail-item {
    display: flex;
    margin-bottom: 8px;
    font-size: 14px;
}

.detail-item label {
    font-weight: 600;
    color: #4b5563;
    min-width: 80px;
    margin-right: 10px;
}

.inquiry-content {
    white-space: pre-wrap;
    background-color: #f9fafb;
    padding: 10px;
    border-radius: 6px;
    border: 1px solid #e5e7eb;
    margin-top: 5px;
}

/* 답변 폼 */
.reply-textarea {
    width: 100%;
    padding: 10px;
    border: 1px solid #d1d5db;
    border-radius: 6px;
    font-size: 14px;
    box-sizing: border-box;
    resize: vertical;
    margin-bottom: 15px;
}

.modal-actions {
    text-align: right;
}

.submit-reply-btn {
    background-color: #10b981;
    color: white;
    border: none;
}

.submit-reply-btn:hover {
    background-color: #059669;
}

.existing-reply {
    margin-top: 20px;
    padding-top: 15px;
    border-top: 1px dashed #d1d5db;
}

.existing-reply h4 {
    font-size: 14px;
    color: #4b5563;
    margin-top: 0;
    margin-bottom: 5px;
}

.existing-reply p {
    white-space: pre-wrap;
    color: #374151;
    font-style: italic;
    margin: 0;
}
</style>