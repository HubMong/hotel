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
            :class="{ 'active': selectedCategory === 'hotel' }"
            @click="selectCategory('hotel')"
          >
            <h3>🏨 호텔 문의</h3>
            <p>예약, 결제, 시설 등 호텔 관련 문의</p>
          </div>
          <div 
            class="category-card"
            :class="{ 'active': selectedCategory === 'website' }"
            @click="selectCategory('website')"
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
            <div v-for="(item, index) in filteredFaqItems" :key="index" class="faq-item">
              <button @click="toggleFaq(index)" class="faq-question">
                <span>{{ item.question }}</span>
                <span>{{ item.open ? '▲' : '▼' }}</span>
              </button>
              <div v-show="item.open" class="faq-answer">
                <p>{{ item.answer }}</p>
              </div>
            </div>
          </div>
        </section>

        <div class="service-layout-wrapper">
          <section class="service-section inquiry-form-section">
            <h2 class="section-title">1:1 {{ categoryTitle }} 문의하기</h2>
            
            <form v-if="isLoggedIn" @submit.prevent="submitInquiry" class="inquiry-form">
              <div v-if="selectedCategory === 'hotel'" class="form-group">
                <label for="hotel-select">문의할 예약 선택</label>
                <select id="hotel-select" v-model="selectedHotelId" required>
                  <option :value="null" disabled>-- 예약 내역을 선택해주세요 --</option>
                  <option v-for="hotel in bookedHotels" :key="hotel.id" :value="hotel.id">
                    {{ hotel.name }}
                  </option>
                </select>
              </div>

              <div v-if="selectedCategory === 'website'" class="form-group">
                <label for="inquiry-name">이름</label>
                <input type="text" id="inquiry-name" v-model="inquiry.name" required />
              </div>
              <div v-if="selectedCategory === 'website'" class="form-group">
                <label for="inquiry-email">이메일</label>
                <input type="email" id="inquiry-email" v-model="inquiry.email" required />
              </div>

              <div class="form-group">
                <label for="inquiry-title">제목</label>
                <input type="text" id="inquiry-title" v-model="inquiry.title" required />
              </div>
              <div class="form-group">
                <label for="inquiry-message">문의 내용</label>
                <textarea id="inquiry-message" v-model="inquiry.message" rows="8" required></textarea>
              </div>
              <button 
                type="submit" 
                class="submit-btn" 
                :disabled="selectedCategory === 'hotel' && !selectedHotelId"
              >
                문의 접수
              </button>
            </form>

            <div v-else class="login-prompt">
              <p>1:1 문의는 로그인 후 이용 가능합니다.</p>
              <button @click="goToLogin" class="submit-btn">로그인 페이지로 이동</button>
            </div>
          </section>
          
          <section class="service-section inquiry-history-section">
            <h2 class="section-title">나의 문의 내역</h2>
            <div class="inquiry-history">
              <div v-if="filteredInquiries.length > 0" class="inquiry-list">
                <div v-for="item in filteredInquiries" :key="item.id" class="inquiry-item">
                  <span class="inquiry-status" :class="item.status">{{ item.status === 'answered' ? '답변 완료' : '처리중' }}</span>
                  <p class="inquiry-title">{{ item.title }}</p>
                  <span class="inquiry-date">{{ item.date }}</span>
                </div>
              </div>
              <div v-else class="no-inquiries">
                <p>선택하신 유형의 문의 내역이 없습니다.</p>
              </div>
            </div>
          </section>
        </div>
      </div>

    </main>

    <Footer />
  </div>
</template>

<script>
import Header from "@/components/user/main_page/Header.vue";
import Footer from "@/components/user/main_page/Footer.vue";
import ReservationApi from '@/api/ReservationApi';
import http from '@/api/http';

export default {
  name: "SupportPage",
  components: {
    Header, Footer,
  },
  data() {
    return {
      isLoggedIn: false,
      user: {}, 
      selectedCategory: null,
      faqItems: [
        { category: "hotel", question: "예약 취소는 어떻게 하나요?", answer: "마이페이지 > 예약 내역에서 직접 취소하실 수 있습니다.", open: false },
        { category: "hotel", question: "호텔의 체크인/체크아웃 시간은 어떻게 되나요?", answer: "호텔 정책에 따라 다르며, 예약 상세 페이지에서 확인 가능합니다.", open: false },
        { category: "website", question: "회원 정보는 어떻게 수정하나요?", answer: "로그인 후, 마이페이지 > 회원 정보 수정 메뉴에서 변경할 수 있습니다.", open: false },
        { category: "website", question: "결제 수단에는 어떤 것들이 있나요?", answer: "신용카드, 카카오페이 등 다양한 결제 수단을 지원하고 있습니다.", open: false },
      ],
      bookedHotels: [], 
      selectedHotelId: null, 
      inquiry: { name: "", email: "", title: "", message: "" },
      inquiries: [
        { id: 1, category: "hotel", title: "객실 내 흡연 가능한가요?", date: "2025-09-20", status: "answered" },
        { id: 2, category: "website", title: "비밀번호 찾기가 되지 않습니다.", date: "2025-09-21", status: "pending" },
        { id: 3, category: "hotel", title: "반려동물 동반 입실 정책 문의", date: "2025-09-19", status: "answered" },
      ]
    };
  },
  watch: {
    selectedCategory(newCategory) {
      if (newCategory !== 'hotel') {
        this.selectedHotelId = null;
      }
    }
  },
  computed: {
    filteredFaqItems() {
      if (!this.selectedCategory) return [];
      return this.faqItems.filter(item => item.category === this.selectedCategory);
    },
    filteredInquiries() {
      if (!this.selectedCategory) return [];
      return this.inquiries.filter(item => item.category === this.selectedCategory);
    },
    categoryTitle() {
      if (!this.selectedCategory) return '';
      return this.selectedCategory === 'hotel' ? '예약/결제' : '웹사이트';
    }
  },
  methods: {
    selectCategory(category) {
      if (this.selectedCategory === category) {
        this.selectedCategory = null;
      } else {
        this.selectedCategory = category;
        if (category === 'hotel' && this.isLoggedIn) {
          this.fetchBookedHotels();
        }
      }
    },
    async fetchBookedHotels() {
      if (!this.user.id) {
        console.error("사용자 ID를 찾을 수 없어 예약 내역을 조회할 수 없습니다.");
        return;
      }
      console.log("API를 통해 예약된 호텔 목록을 가져옵니다...");
      try {
        const reservations = await ReservationApi.getByUserId(this.user.id);
        if (!reservations || reservations.length === 0) {
            this.bookedHotels = [];
            return;
        }

        const promises = reservations.map(res =>
          http.get(`/hotels/${res.hotelId}`)
            .then(hotelResponse => {
              // 💡 만약 계속 undefined가 나온다면, 이 로그를 확인해주세요.
              console.log('서버로부터 받은 호텔 정보:', hotelResponse.data);
              
              return {
                reservationId: res.id,
                // 💡 hotel이나 name이 없을 경우를 대비해 안전한 코드로 수정 (Optional Chaining)
                hotelName: hotelResponse.data?.hotel?.name, 
                startDate: res.startDate
              };
            })
            .catch(error => {
              console.warn(`Hotel name for ID ${res.hotelId} failed`, error);
              return { 
                reservationId: res.id,
                hotelName: `호텔 ID: ${res.hotelId}`,
                startDate: res.startDate
              };
            })
        );

        const resolvedDetails = await Promise.all(promises);

        this.bookedHotels = resolvedDetails.map(detail => ({
          id: detail.reservationId,
          // 💡 hotelName이 없을 경우 '이름 없음'으로 표시되도록 수정
          name: `${detail.hotelName || '이름을 찾을 수 없음'} (${new Date(detail.startDate).toLocaleDateString('ko-KR')} 체크인)`
        }));

      } catch (error) {
        console.error("예약 내역 조회 실패:", error);
        alert("예약 내역을 불러오는 데 실패했습니다.");
        this.bookedHotels = [];
      }
    },
    toggleFaq(index) {
      const itemToToggle = this.filteredFaqItems[index];
      itemToToggle.open = !itemToToggle.open;
    },
    submitInquiry() {
      const submissionData = {
        category: this.selectedCategory,
        title: this.inquiry.title,
        message: this.inquiry.message,
      };
      
      if (this.selectedCategory === 'hotel') {
        if (!this.selectedHotelId) {
          alert("문의할 예약 내역을 선택해주세요.");
          return;
        }
        submissionData.reservationId = this.selectedHotelId;
      } else {
        submissionData.name = this.inquiry.name;
        submissionData.email = this.inquiry.email;
      }
      
      console.log("제출된 문의 데이터:", submissionData);
      alert("문의가 성공적으로 접수되었습니다.");
      
      this.inquiry = { name: "", email: "", title: "", message: "" };
      this.selectedHotelId = null;
    },
    goToLogin() {
      this.$router.push('/login');
    }
  },
  created() {
    const token = localStorage.getItem('token');
    const userInfo = localStorage.getItem('user');
    if (token && userInfo) {
      this.isLoggedIn = true;
      this.user = JSON.parse(userInfo);
    }
  }
};
</script>
<style scoped src="@/assets/css/support/customer_service.css">
</style>