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
            @click="router.push({ name: 'HotelSupport' })"
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
            <button class="submit-btn full-width-btn" @click="openInquiryModal('hotel')">
                1:1 {{ categoryTitle }} 문의하기
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
                <div v-if="inquiryType === 'hotel'" class="form-group">
                    <label for="hotel-select">문의할 예약 선택</label>
                    <select v-if="bookedHotels.length > 0" id="hotel-select" v-model="selectedHotelId" required>
                      <option :value="null" disabled>-- 예약 내역을 선택해주세요 --</option>
                      <option v-for="hotel in bookedHotels" :key="hotel.id" :value="hotel.id">
                        {{ hotel.name }}
                      </option>
                    </select>
                    <p v-else class="no-data-message">조회된 예약 내역이 없습니다.</p>
                </div>
                
                <div v-if="inquiryType === 'website'" class="form-group">
                    <label for="inquiry-name">이름</label>
                    <input type="text" id="inquiry-name" v-model="inquiry.name" required />
                </div>
                <div class="form-group">
                    <label for="inquiry-email">이메일</label>
                    <input type="email" id="inquiry-email" v-model="inquiry.email" required />
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
                  :disabled="inquiryType === 'hotel' && !selectedHotelId"
                >
                  문의 접수
                </button>
            </form>
            
            <div v-else class="login-prompt">
              <p>1:1 문의는 로그인 후 이용 가능합니다.</p>
              <button @click="goToLogin" class="submit-btn full-width-btn">로그인 페이지로 이동</button>
            </div>

            <section class="modal-inquiry-history">
                <h4>나의 문의 내역 ({{ modalTitle }})</h4>
                <div class="inquiry-history">
                    <div v-if="filteredInquiries.length > 0" class="inquiry-list small horizontal-list">
                        <div v-for="item in filteredInquiries" :key="item.id" class="inquiry-item">
                            <span class="inquiry-status" :class="item.status">{{ item.status === 'answered' ? '답변 완료' : '처리중' }}</span>
                            <p class="inquiry-title">{{ item.title }}</p>
                            <span class="inquiry-date">{{ item.date }}</span>
                        </div>
                    </div>
                    <p v-else class="no-inquiries">선택하신 유형의 문의 내역이 없습니다.</p>
                </div>
            </section>
        </div>
    </div>
    <Footer />
  </div>
</template>

<script setup>
import { ref, reactive, computed, watch, onMounted } from 'vue';
import { useRouter, useRoute } from 'vue-router';
import Header from "@/components/user/main_page/Header.vue";
import Footer from "@/components/user/main_page/Footer.vue";
import ReservationApi from '@/api/ReservationApi';
import http from '@/api/http';

const router = useRouter();
const route = useRoute();

// 1. 상태 변수 및 인증 초기화
const isLoggedIn = ref(false); 
const user = reactive({});

const token = localStorage.getItem('token');
const userInfo = localStorage.getItem('user');

if (token && userInfo) {
    isLoggedIn.value = true;
    try {
        // User 객체에 id, name, email 등 필요한 정보가 있다고 가정
        Object.assign(user, JSON.parse(userInfo));
    } catch (e) {
        isLoggedIn.value = false;
    }
}
// ----------------------------------------------------

const selectedCategory = ref('hotel'); 
const bookedHotels = ref([]); 
const selectedHotelId = ref(null); // 예약 ID가 저장됩니다.
// inquiry 객체는 호텔 문의에서는 title, message만 사용합니다.
const inquiry = reactive({ name: user.name || "", email: user.email || "", title: "", message: "" }); 
const isModalVisible = ref(false);
const inquiryType = ref(null);

// 2. 데이터 (FAQ, 문의 내역)
const faqItems = ref([
    { category: "hotel", question: "예약 취소는 어떻게 하나요?", answer: "마이페이지 > 예약 내역에서 직접 취소하실 수 있습니다.", open: false },
    { category: "hotel", question: "호텔의 체크인/체크아웃 시간은 어떻게 되나요?", answer: "호텔 정책에 따라 다르며, 예약 상세 페이지에서 확인 가능합니다.", open: false },
    { category: "website", question: "회원 정보는 어떻게 수정하나요?", answer: "로그인 후, 마이페이지 > 회원 정보 수정 메뉴에서 변경할 수 있습니다.", open: false },
    { category: "website", question: "결제 수단에는 어떤 것들이 있나요?", answer: "신용카드, 카카오페이 등 다양한 결제 수단을 지원하고 있습니다.", open: false },
]);
// 실제 API에서 받아올 문의 내역으로 변경됩니다.
const inquiries = ref([]); 

// 3. Computed 속성
const filteredFaqItems = computed(() => {
    return faqItems.value.filter(item => item.category === 'hotel');
});

const filteredInquiries = computed(() => {
    // inquiryType이 'hotel'로 고정되므로, inquiries.value 전체를 반환합니다.
    return inquiries.value;
});

const categoryTitle = computed(() => '예약/결제');
const modalTitle = computed(() => '호텔');

// 4. Watcher
watch(selectedCategory, (newCategory) => {
    faqItems.value.forEach(item => item.open = false);
});


// 6. Methods/Functions
const handleLogout = () => { /* ... (로직 생략) ... */ };
const toggleFaq = (itemToToggle) => {
    const originalItem = faqItems.value.find(item => item.question === itemToToggle.question);
    if (originalItem) { originalItem.open = !originalItem.open; }
};
const goToLogin = () => { router.push('/login'); };

const openInquiryModal = (type) => {
    if (!isLoggedIn.value) { goToLogin(); return; }

    inquiryType.value = type;
    inquiry.title = '';
    inquiry.message = '';
    selectedHotelId.value = null; // 모달 열 때 예약 선택 초기화
    
    if (type === 'hotel') {
        fetchBookedHotels(); // 예약 내역 로드
        fetchInquiries();     // 문의 내역 로드
    } else {
        // 웹사이트 문의 로직 (현재는 호텔 문의 페이지이므로 실행되지 않음)
        inquiry.name = user.name || '';
        inquiry.email = user.email || '';
    }

    isModalVisible.value = true;
};

const closeModal = () => {
    isModalVisible.value = false;
    selectedHotelId.value = null;
    inquiryType.value = null;
};


// 🚩 문의 내역 조회 (API 호출)
const fetchInquiries = async () => {
    if (!isLoggedIn.value || !user.id) return;
    try {
        // 백엔드 API: 사용자 ID에 해당하는 호텔 문의 내역을 조회 (HotelInquiry)
        const response = await http.get(`/inquiries/my/hotel`); 
        
        // 데이터 포맷에 맞게 변환 (백엔드에서 date, status, title 등이 DTO로 왔다고 가정)
        inquiries.value = response.data.map(item => ({
            id: item.id,
            category: 'hotel',
            title: item.title,
            // 날짜 형식 변경 (예: 2025-09-20)
            date: new Date(item.createdAt).toLocaleDateString('ko-KR', { year: 'numeric', month: '2-digit', day: '2-digit'}),
            // status 필드가 'ANSWERED', 'PENDING'으로 온다고 가정
            status: item.status.toLowerCase(), 
        }));

    } catch (error) {
        console.error("나의 호텔 문의 내역 조회 실패:", error);
        inquiries.value = [];
    }
};


// 🚩 문의 전송 로직
const submitInquiry = async () => {
    if (!inquiry.title || !inquiry.message) {
        alert("제목과 내용을 입력해주세요.");
        return;
    }
    if (inquiryType.value === 'hotel' && !selectedHotelId.value) {
        alert("문의할 예약 내역을 선택해주세요.");
        return;
    }

    const submissionData = {
        // 백엔드 HotelInquiry 엔티티에 맞춰 필드를 구성
        reservationId: selectedHotelId.value, // 예약 ID를 호텔 문의에 연결
        title: inquiry.title,
        message: inquiry.message,
        // userId는 백엔드 토큰에서 추출한다고 가정
    };

    try {
        // 백엔드 API: 호텔 문의 전송
        await http.post('/inquiries/hotel', submissionData); 

        alert("문의가 성공적으로 접수되었습니다. 담당자가 빠르게 답변해 드리겠습니다.");
        closeModal();
        // 폼 초기화
        inquiry.title = "";
        inquiry.message = "";
        selectedHotelId.value = null; 
        
        // 문의 내역 새로고침
        fetchInquiries(); 

    } catch (error) {
        console.error("문의 접수 실패:", error);
        const errorMessage = error.response?.data?.message || "문의 접수에 실패했습니다.";
        alert(errorMessage);
    }
};


// 🔥🔥🔥 예약 호텔 목록 로딩 로직 (유지) 🔥🔥🔥
const fetchBookedHotels = async () => {
    bookedHotels.value = []; 
    if (!user.id) { console.error("사용자 ID를 찾을 수 없어 예약 내역을 조회할 수 없습니다."); return; }
    
    try {
        // 이 부분은 복잡한 로직이므로 최대한 유지
        const reservations = await ReservationApi.getByUserId(user.id); 
        
        if (!reservations || reservations.length === 0) { bookedHotels.value = []; return; }

        const promises = reservations.map(res =>
            http.get(`/hotels/${res.hotelId}`) 
                .then(hotelResponse => {
                    const hotelName = hotelResponse.data?.hotel?.name || hotelResponse.data?.name;
                    return { reservationId: res.id, name: hotelName, startDate: res.startDate };
                })
                .catch(error => {
                    console.warn(`Hotel name for ID ${res.hotelId} failed`, error);
                    return { reservationId: res.id, name: `호텔 ID: ${res.hotelId}`, startDate: res.startDate };
                })
        );

        const resolvedDetails = await Promise.all(promises);

        bookedHotels.value = resolvedDetails.map(detail => ({
            id: detail.reservationId, // 예약 ID를 값으로 사용
            name: `${detail.name || '이름을 찾을 수 없음'} (${new Date(detail.startDate).toLocaleDateString('ko-KR')} 체크인)`
        }));
        
        console.log("로딩된 예약 내역:", bookedHotels.value); 

    } catch (error) {
        console.error("예약 내역 조회 실패:", error);
        bookedHotels.value = [];
    }
};


// 7. Lifecycle Hooks
onMounted(() => {
    // 페이지 로드시 문의 내역을 가져옵니다.
    if (isLoggedIn.value) {
        fetchInquiries(); 
    }
});
</script>

<style scoped src="@/assets/css/support/hotelsupport.css">
</style>