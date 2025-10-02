<template>
  <div class="customer-service-page">
    
    <Header 
      :isLoggedIn="isLoggedIn" 
      :user="user" 
      @logout="handleLogout" 
    />

    <main class="service-container">
      <div class="page-title-section">
        <h1 class="page-title">고객센터</h1>
        <p class="page-subtitle">무엇을 도와드릴까요?</p>
      </div>

      <section class="category-selector-section">
        <h2 class="section-title">문의 유형 선택</h2>
        <div class="category-list">
          <div 
            class="category-card" 
            @click="router.push({ name: 'HotelSupport' })"
          >
            <h3>🏨 호텔 문의</h3>
            <p>예약, 결제, 시설 등 호텔 관련 문의</p>
          </div>
          <div 
            class="category-card active"
            @click="router.push({ name: 'WebsiteSupport' })"
          >
            <h3>💻 웹사이트 문의</h3>
            <p>회원가입, 오류 등 웹사이트 이용 관련 문의</p>
          </div>
        </div>
      </section>

      <div v-show="selectedCategory" class="content-area">
        <section class="service-section">
          <h2 class="section-title">자주 묻는 질문</h2>
          <div class="faq-list">
            <div v-for="item in filteredFaqItems" :key="item.question" class="faq-item">
              <button @click="toggleFaq(item)" class="faq-question">
                <span>{{ item.question }}</span>
                <span>{{ item.open ? '▲' : '▼' }}</span>
              </button>
              <div v-show="item.open" class="faq-answer">
                <p>{{ item.answer }}</p>
              </div>
            </div>
          </div>

          <div class="inquiry-action-area">
              <button class="submit-btn" @click="openInquiryModal('website')">
                1:1 문의하기
              </button>
              <button class="submit-btn" @click="openMyInquiriesModal"> 나의 문의 내역
              </button>
            </div>
            </section>
          </div>
        </main>
        
        <div v-if="isModalVisible" class="modal-overlay" @click.self="closeModal">
          <div class="modal-content">
            <div class="modal-header">
              <h3>1:1 문의하기</h3>
              <button @click="closeModal" class="close-btn">&times;</button>
            </div>
            
            <form v-if="isLoggedIn" @submit.prevent="submitInquiry" class="inquiry-form">
              <div class="form-group">
                <label for="inquiry-title">제목</label>
                <input type="text" id="inquiry-title" v-model="inquiry.title" required />
              </div>
              <div class="form-group">
                <label for="inquiry-message">문의 내용</label>
                <textarea id="inquiry-message" v-model="inquiry.message" rows="5" required></textarea>
              </div>
              
              <button type="submit" class="submit-btn full-width-btn">
                문의 접수
              </button>
            </form>
          </div>
        </div>

        <div v-if="isMyInquiriesModalVisible" class="modal-overlay" @click.self="closeMyInquiriesModal">
      <div class="modal-content">
        <div class="modal-header">
          <h3>나의 문의 내역</h3>
          <button @click="closeMyInquiriesModal" class="close-btn">&times;</button>
        </div>
        <div class="modal-body">
          <div v-if="inquiries.length === 0" class="no-inquiries-text">문의 내역이 없습니다.</div>
          
          <div v-else class="inquiry-grid"> 
            <div 
              v-for="item in inquiries" 
              :key="item.id" 
              class="inquiry-card-item" :class="{ answered: item.status === 'ANSWERED' }" 
              @click="viewInquiryDetail(item)"
            >
              <div class="item-status">
                {{ item.status === 'ANSWERED' ? '답변 완료' : '처리중' }}
              </div>
              <div class="item-title">{{ item.title }}</div>
              <div class="item-date">{{ item.date }}</div>
            </div>
          </div>
          </div>
        </div>
      </div>
    </div>

    <div v-if="selectedInquiry" class="modal-overlay" @click.self="closeInquiryDetail">
      <div class="modal-content" @click.stop>
        <div class="modal-header">
          <h3>문의 상세 내용</h3>
          <button @click="closeInquiryDetail" class="close-btn">&times;</button>
        </div>
        <div class="modal-body" v-if="selectedInquiry.id">
          <div class="detail-section">
            <h3>나의 문의</h3>
            <p><strong>제목:</strong> {{ selectedInquiry.title }}</p>
            <div class="inquiry-content">{{ selectedInquiry.message }}</div>
          </div>
          <div v-if="selectedInquiry.adminReply" class="detail-section">
            <h3>관리자 답변</h3>
            <div class="admin-reply">{{ selectedInquiry.adminReply }}</div>
            <small v-if="selectedInquiry.repliedAt">답변 일시: {{ formatDateTime(selectedInquiry.repliedAt) }}</small>
          </div>
          <div v-else class="detail-section">
            <h3>관리자 답변</h3>
            <p class="no-reply">아직 답변이 등록되지 않았습니다.</p>
          </div>
        </div>
      </div>

    <Footer v-if="!isMyInquiriesModalVisible && !selectedInquiry" />
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import Header from "@/components/user/main_page/Header.vue";
import Footer from "@/components/user/main_page/Footer.vue";
import http from '@/api/http';

const router = useRouter();
const selectedCategory = ref('website');

const isLoggedIn = ref(false); 
const user = reactive({});
const token = localStorage.getItem('token');
const userInfo = localStorage.getItem('user');

if (token && userInfo) {
  isLoggedIn.value = true;
  try { Object.assign(user, JSON.parse(userInfo)); } 
  catch (e) { isLoggedIn.value = false; }
}

const inquiry = reactive({ title: "", message: "" });
const isModalVisible = ref(false); // 문의 작성 모달
const isMyInquiriesModalVisible = ref(false); // 나의 문의 내역 모달
const selectedInquiry = ref(null); // 상세 보기할 문의 객체

const faqItems = ref([
  { category: "website", question: "회원 정보는 어떻게 수정하나요?", answer: "로그인 후, 마이페이지 > 회원 정보 수정 메뉴에서 변경할 수 있습니다.", open: false },
  { category: "website", question: "결제 수단에는 어떤 것들이 있나요?", answer: "신용카드, 카카오페이 등 다양한 결제 수단을 지원하고 있습니다.", open: false },
]);
const inquiries = ref([]);

const filteredFaqItems = computed(() => {
  return faqItems.value.filter(item => item.category === 'website');
});

const categoryTitle = computed(() => '웹사이트');
const modalTitle = computed(() => '웹사이트');


const handleLogout = () => { console.log("로그아웃 처리 로직 필요"); };
const toggleFaq = (itemToToggle) => { itemToToggle.open = !itemToToggle.open; };

const openInquiryModal = () => {
  if (!isLoggedIn.value) { router.push('/login'); return; }
  isModalVisible.value = true;
};
const closeModal = () => { isModalVisible.value = false; };

const openMyInquiriesModal = () => {
  if (!isLoggedIn.value) { router.push('/login'); return; }
  fetchInquiries(); 
  isMyInquiriesModalVisible.value = true;
};
const closeMyInquiriesModal = () => { isMyInquiriesModalVisible.value = false; };

const viewInquiryDetail = async (inquiry) => {
  try {
    // [수정 1] 경로에 /api/ 를 명시적으로 추가했습니다.
    const response = await http.get(`/inquiries/${inquiry.id}`); 
    
    // [선택적 수정] 만약 서버 응답이 { data: {...} } 형태라면 .data를 추가합니다.
    // 일반적인 Spring/SpringBoot 응답이라면 response.data를 사용합니다.
    selectedInquiry.value = response.data; // 또는 response.data.data;

  } catch (error) {
    console.error("문의 상세 정보를 불러오는 데 실패했습니다:", error);
    
    // [수정 2] 에러 코드를 포함한 상세 메시지 처리 로직 추가
    const status = error.response?.status;
    let errorMessage = "상세 정보를 불러오는데 실패했습니다.";
    if (status === 404) {
        errorMessage = "해당 문의를 찾을 수 없거나 접근 권한이 없습니다.";
    } else if (status === 500) {
        // 서버 내부 오류(500)는 백엔드 팀이 확인해야 함을 암시
        errorMessage = "서버 내부 오류(500)가 발생했습니다. 잠시 후 다시 시도해 주세요.";
    } else if (error.message.includes('Network Error')) {
        errorMessage = "네트워크 연결 상태를 확인해 주세요.";
    }

    alert(errorMessage);
  }
};
const closeInquiryDetail = () => { selectedInquiry.value = null; };

const formatDateTime = (dateString) => {
  if (!dateString) return '';
  return new Date(dateString).toLocaleString('ko-KR');
};

const fetchInquiries = async () => {
  if (!isLoggedIn.value) return;
  try {
    const response = await http.get('/inquiries/my');
    inquiries.value = response.data;
  } catch (error) {
    console.error("문의 내역을 불러오는 데 실패했습니다:", error);
  }
};

const submitInquiry = async () => {
  if (!inquiry.title || !inquiry.message) {
    alert("제목과 내용을 입력해주세요.");
    return;
  }
  
  const submissionData = {
    category: 'website',
    title: inquiry.title,
    message: inquiry.message,
  };

  try {
    await http.post('/inquiries', submissionData);
    alert("문의가 성공적으로 접수되었습니다.");
    closeModal();
    inquiry.title = "";
    inquiry.message = "";
    fetchInquiries();
  } catch (error) {
    console.error("문의 접수 실패:", error);
    const errorMessage = error.response?.data?.message || "문의 접수에 실패했습니다.";
    alert(errorMessage);
  }
};

onMounted(() => {
  fetchInquiries();
});
</script>

<style scoped src="@/assets/css/support/websitesupport.css">
</style>