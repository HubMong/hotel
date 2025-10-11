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
            class="category-card active" 
            @click="changeCategory('hotel')"
          >
            <h3>🏨 호텔 문의</h3>
            <p>예약, 결제, 시설 등 호텔 관련 문의</p>
          </div>
          <div 
            class="category-card"
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
            <button class="submit-btn" @click="openInquiryModal()">
              1:1 {{ categoryTitle }} 문의하기
            </button>
            <button class="submit-btn" @click="openMyInquiriesModal"> 
              나의 문의 내역
            </button>
          </div>
        </section>
      </div>
    </main>
    
    <div v-if="isModalVisible" class="modal-overlay" @click.self="closeModal">
      <div class="modal-content">
        <div class="modal-header">
          <h3>1:1 {{ modalTitle }} 문의하기</h3>
          <button @click="closeModal" class="close-btn">&times;</button>
        </div>
        
        <form v-if="isLoggedIn" @submit.prevent="submitInquiry" class="inquiry-form">
          <div v-if="selectedCategory === 'hotel'" class="form-group">
            <label for="hotel-select">문의할 예약 선택</label>
            <select v-if="bookedHotels.length > 0" id="hotel-select" v-model="selectedHotelId" required>
              <option :value="null" disabled>-- 예약 내역을 선택해주세요 --</option>
              <option v-for="hotel in bookedHotels" :key="hotel.id" :value="hotel.id">
                {{ hotel.name }} - 객실: {{ hotel.rooms }}
              </option>
            </select>

            <p v-else class="no-data-message">조회된 예약 내역이 없습니다.</p>
          </div>
          
          <div class="form-group">
            <label for="inquiry-title">제목</label>
            <input type="text" id="inquiry-title" v-model="inquiry.title" required />
          </div>
          <div class="form-group">
            <label for="inquiry-message">문의 내용</label>
            <textarea id="inquiry-message" v-model="inquiry.message" rows="5" required></textarea>
          </div>
          
          <button 
            type="submit" 
            class="submit-btn full-width-btn" 
            :disabled="selectedCategory === 'hotel' && !selectedHotelId"
          >
            문의 접수
          </button>
        </form>
        
        <div v-else class="login-prompt">
          <p>1:1 문의는 로그인 후 이용 가능합니다.</p>
          <button @click="goToLogin" class="submit-btn full-width-btn">로그인 페이지로 이동</button>
        </div>
      </div>
    </div>

    <div v-if="isMyInquiriesModalVisible" class="modal-overlay" @click.self="closeMyInquiriesModal">
      <div class="modal-content">
        <div class="modal-header">
          <h3>나의 문의 내역 ({{ modalTitle }})</h3>
          <button @click="closeMyInquiriesModal" class="close-btn">&times;</button>
        </div>
        <div class="modal-body">
          <div v-if="inquiries.length === 0" class="no-inquiries-text">문의 내역이 없습니다.</div>
          
          <div v-else class="inquiry-grid"> 
            <div 
              v-for="item in inquiries" 
              :key="item.id" 
              class="inquiry-card-item" 
              :class="{ answered: item.status === 'ANSWERED' || item.status === '답변 완료' }" 
              @click="viewInquiryDetail(item)"
            >
              <div class="item-status">
                {{ item.status === 'ANSWERED' || item.status === '답변 완료' ? '답변 완료' : '처리중' }}
              </div>
              <div class="item-title">{{ item.title }}</div>
              <div class="item-date">{{ item.date || item.createdAt }}</div> 
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
    </div>

    <Footer />
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import Header from "@/components/user/main_page/Header.vue";
import Footer from "@/components/user/main_page/Footer.vue";
import http from '@/api/http'; 
import ReservationApi from '@/api/ReservationApi';
import HotelApi from '@/api/HotelApi';

const router = useRouter();

// --- 상태 변수 및 인증 초기화 ---
const isLoggedIn = ref(false); 
const user = reactive({});
const token = localStorage.getItem('token');
const userInfo = localStorage.getItem('user');

if (token && userInfo) {
  isLoggedIn.value = true;
  try { Object.assign(user, JSON.parse(userInfo)); } 
  catch (e) { isLoggedIn.value = false; }
}

const selectedCategory = ref('hotel'); 
const bookedHotels = ref([]); 
const selectedHotelId = ref(null);
const inquiry = reactive({ title: "", message: "" }); 
const isModalVisible = ref(false); 
const isMyInquiriesModalVisible = ref(false); 
const selectedInquiry = ref(null); 

const faqItems = ref([
    { category: "hotel", question: "예약 취소는 어떻게 하나요?", answer: "마이페이지 > 예약 내역에서 직접 취소하실 수 있습니다.", open: false },
    { category: "hotel", question: "호텔의 체크인/체크아웃 시간은 어떻게 되나요?", answer: "호텔 정책에 따라 다르며, 예약 상세 페이지에서 확인 가능합니다.", open: false },
    { category: "website", question: "비밀번호를 잊어버렸어요.", answer: "로그인 페이지에서 '비밀번호 찾기'를 통해 본인 인증 후 재설정할 수 있습니다.", open: false },
    { category: "website", question: "회원 탈퇴는 어떻게 하나요?", answer: "마이페이지 > 회원 정보 수정에서 탈퇴가 가능합니다. (단, 예약 내역이 없어야 합니다)", open: false },
]);
const inquiries = ref([]); 

// --- Computed 속성 및 일반 함수 ---
const filteredFaqItems = computed(() => faqItems.value.filter(item => item.category === selectedCategory.value));
const filteredInquiries = computed(() => inquiries.value);
const categoryTitle = computed(() => selectedCategory.value === 'hotel' ? '예약/결제' : '회원/오류');
const modalTitle = computed(() => selectedCategory.value === 'hotel' ? '호텔' : '웹사이트');

const handleLogout = () => { router.push('/'); };
const toggleFaq = (itemToToggle) => { itemToToggle.open = !itemToToggle.open; };
const goToLogin = () => { router.push('/login'); };

const changeCategory = (categoryName) => {
    // 웹사이트 카테고리 클릭 시 라우터 푸시
    if (categoryName === 'website') {
        router.push({ name: 'WebsiteSupport' });
        return;
    }
    
    selectedCategory.value = categoryName;
    if (isLoggedIn.value) {
        fetchInquiries();
    }
};

const openInquiryModal = () => {
    if (!isLoggedIn.value) { goToLogin(); return; }
    inquiry.title = '';
    inquiry.message = '';
    selectedHotelId.value = null; 
    
    if (selectedCategory.value === 'hotel') {
        fetchBookedHotels();
    }
    isModalVisible.value = true;
};
const closeModal = () => {
    isModalVisible.value = false;
    selectedHotelId.value = null;
};

const openMyInquiriesModal = () => {
  if (!isLoggedIn.value) { goToLogin(); return; }
  fetchInquiries(); 
  isMyInquiriesModalVisible.value = true;
};
const closeMyInquiriesModal = () => { isMyInquiriesModalVisible.value = false; };


/**
 * 🔑 [수정 1] 별도 API 호출 없이 로컬 객체를 복사하여 상세 모달 표시
 */
const viewInquiryDetail = (inquiry) => {
  selectedInquiry.value = { ...inquiry }; // 로컬 객체를 복사하여 바인딩
  isMyInquiriesModalVisible.value = false; // 목록 모달 닫기
};
const closeInquiryDetail = () => { 
    selectedInquiry.value = null; 
    openMyInquiriesModal(); // 상세 모달 닫고 목록 모달 다시 열기
};


const formatDateTime = (dateString) => {
  if (!dateString) return '';
  return new Date(dateString).toLocaleString('ko-KR');
};

// ==========================================================
// API 호출 함수
// ==========================================================

/**
 * 나의 문의 내역을 불러옵니다.
 */
const fetchInquiries = async () => {
    if (!isLoggedIn.value) return;
    try {
        // API 경로: 호텔 문의는 /hotel-inquiries/my 사용
        const endpoint = '/hotel-inquiries/my'; 
        
        const response = await http.get(endpoint); 
        
        // 🔑 [수정 2] 문의 내역 데이터 가공: 상세 보기에 필요한 모든 필드 저장
        inquiries.value = response.data.map(item => ({
            id: item.id,
            title: item.title,
            message: item.message, // 상세 보기 내용
            adminReply: item.adminReply, // 관리자 답변
            repliedAt: item.repliedAt, // 답변 일시
            // 🔑 [수정] 상태: 대문자/한글 모두 대응 가능하도록 대문자로 통일 (ANSWERED/PENDING)
            status: (item.status || 'PENDING').toUpperCase(), 
            date: item.date || item.createdAt, // 날짜는 서버에서 오는 필드 사용
            // 기타 필드는 필요하다면 여기에 추가
        }));
    } catch (error) {
        console.error("나의 문의 내역 조회 실패:", error);
    }
};

/**
 * 새로운 문의를 제출합니다.
 */
const submitInquiry = async () => {
    if (!inquiry.title || !inquiry.message || 
        (selectedCategory.value === 'hotel' && !selectedHotelId.value)) { 
        alert("모든 필수 필드를 입력해주세요.");
        return;
    }

    const submissionData = {
        reservationId: selectedCategory.value === 'hotel' ? selectedHotelId.value : null,
        title: inquiry.title,
        message: inquiry.message,
        category: selectedCategory.value, 
    };
    
    const endpoint = '/hotel-inquiries'; // 호텔 문의 엔드포인트

    try {
        await http.post(endpoint, submissionData); 

        alert("문의가 성공적으로 접수되었습니다.");
        closeModal();
        // 문의 접수 후 목록이 열려있다면 새로고침
        if (isMyInquiriesModalVisible.value) {
            fetchInquiries(); 
        }
        inquiry.title = "";
        inquiry.message = "";
    } catch (error) {
        console.error("문의 접수 실패:", error);
        alert(error.response?.data?.message || "문의 접수에 실패했습니다.");
    }
};

/**
 * 🔑 [수정 3] 예약 목록 로직 (객실 이름 문제 해결)
 */
const fetchBookedHotels = async () => {
    bookedHotels.value = []; 
    if (!user.id) return; 
    
    try {
        const reservations = await ReservationApi.getByUserId(user.id); 
        if (!reservations || reservations.length === 0) return;

        const promises = reservations.map(async (res) => {
            // 1. 호텔 정보 가져오기
            const hotelData = await HotelApi.getDetail(res.hotelId); 

            const hotelName = hotelData?.hotel?.name || hotelData?.name || `호텔 ID: ${res.hotelId}`;
            
            // 2. 🔑 [핵심 로직] room.name 컬럼을 사용하여 객실 이름 추출
            const rooms = hotelData?.hotel?.rooms || hotelData?.rooms || []; // 호텔에 속한 모든 객실 목록
            
            const reservedRoom = rooms.find(room => room.id === res.roomId);
            
            // 'room' 테이블의 'name' 컬럼을 사용합니다.
            const roomName = reservedRoom ? reservedRoom.name : '객실 이름 찾을 수 없음'; 

            return { 
                reservationId: res.id, 
                name: hotelName, 
                rooms: roomName, // 객실 이름을 정확하게 할당
                startDate: res.startDate 
            };
        });

        const resolvedDetails = await Promise.all(promises);

        bookedHotels.value = resolvedDetails.map(detail => ({
            id: detail.reservationId,
            name: `${detail.name} (${new Date(detail.startDate).toLocaleDateString('ko-KR')} 체크인)`,
            rooms: detail.rooms // 객실 정보
        }));
    } catch (error) {
        console.error("예약 내역 조회 실패:", error);
    }
};


// --- Lifecycle Hooks ---
onMounted(() => {
    if (isLoggedIn.value) {
        fetchInquiries(); 
    }
});
</script>

<style scoped src="@/assets/css/support/hotelsupport.css">
</style>