<template>
  <div class="main-page">
    <!-- 헤더 -->
    <Header :isLoggedIn="isLoggedIn" :user="user" @logout="handleLogout" />

    <!-- Hero 배너 -->
    <div class="hero-section hero-bg">
      <div class="hero-overlay"></div>
      <div class="hero-text-container">
        <div class="hero-text">
          <p class="hero-subtitle">
            검색을 통해 요금을 비교하고 무료 취소 포함한 특가도 확인하세요!
          </p>
        </div>
      </div>
    </div>

    <!-- 검색 폼 컴포넌트 -->
    <SearchForm />

    <!-- 추천 국내 여행지 슬라이드 -->
    <section class="recommended-section">
      <h2 class="section-title">추천 국내 여행지</h2>
      <swiper
        :modules="modules"
        :slides-per-view="4"
        :space-between="20"
        :loop="true"
        :navigation="true"
        :pagination="{ clickable: true }"
        class="destination-swiper"
      >
        <swiper-slide
          v-for="(place, index) in destinationsDomestic"
          :key="index"
          class="destination-card"
        >
          <img :src="place.image" :alt="place.name" class="destination-image" />
          <div class="destination-info">
            <h3>{{ place.name }}</h3>
            <p>{{ place.description }}</p>
          </div>
        </swiper-slide>
      </swiper>
    </section>

    <!-- 추천 해외 여행지 슬라이드 -->
    <section class="recommended-section">
      <h2 class="section-title">추천 해외 여행지</h2>
      <swiper
        :modules="modules"
        :slides-per-view="4"
        :space-between="20"
        :loop="true"
        :navigation="true"
        :pagination="{ clickable: true }"
        class="destination-swiper"
      >
        <swiper-slide
          v-for="(place, index) in destinationsOverseas"
          :key="index"
          class="destination-card"
        >
          <img :src="place.image" :alt="place.name" class="destination-image" />
          <div class="destination-info">
            <h3>{{ place.name }}</h3>
            <p>{{ place.description }}</p>
          </div>
        </swiper-slide>
      </swiper>
    </section>

    <!-- 인기 호텔 추천 -->
    <section class="hotel-section">
      <h2 class="section-title">인기 호텔 추천</h2>
      <div class="hotel-list">
        <div v-for="(hotel, index) in popularHotels" :key="index" class="hotel-card">
          <img :src="hotel.image" :alt="hotel.name" class="hotel-image" />  
          <div class="hotel-info">
            <h3>{{ hotel.name }}</h3>
            <p>{{ hotel.city }}</p>
            <p class="hotel-price">{{ hotel.price }}</p>
            <p class="hotel-rating">⭐ {{ hotel.rating }}</p>
          </div>
        </div>
      </div>
    </section>

    <!-- 여행 팁 -->
    <section class="tips-section">
      <h2 class="section-title">여행 꿀팁</h2>
      <div class="tips-list">
        <div v-for="(tip, index) in travelTips" :key="index" class="tip-card">
          <h3>{{ tip.title }}</h3>
          <p>{{ tip.description }}</p>
        </div>
      </div>
    </section>  

    <!-- 푸터 -->
    <Footer />
  </div>
</template>

<style scoped src="@/assets/css/homepage/mainpage.css"></style> 

<script>
import Header from "@/components/user/main_page/Header.vue";
import Footer from "@/components/user/main_page/Footer.vue";
import SearchForm from "@/components/user/main_page/SearchForm.vue";
import { Swiper, SwiperSlide } from "swiper/vue";
import { Navigation, Pagination } from "swiper/modules";

import "swiper/css";
import "swiper/css/navigation";
import "swiper/css/pagination";

export default {
  name: "MainPage",
  components: {
    Header,
    SearchForm,
    Swiper,
    SwiperSlide,
    Footer
  },
  data() {
    return {
      isLoggedIn: false,
      user: { name: "홍길동" },
      // 국내 여행지
      destinationsDomestic: [
        { name: "서울", description: "한국의 수도, 쇼핑과 문화의 중심지", image: "src/images/gangreung.jpg" },
        { name: "부산", description: "바다와 해운대, 맛있는 해산물", image: "src/images/gangreung.jpg" },
        { name: "제주", description: "힐링 여행지, 한라산과 올레길", image: "src/images/gangreung.jpg" },
        { name: "강릉", description: "동해안의 바다와 커피 거리", image: "src/images/gangreung.jpg" },
        { name: "여수", description: "밤바다로 유명한 낭만 여행지", image: "src/images/gangreung.jpg" },
        { name: "전주", description: "한옥마을과 맛의 고장", image: "src/images/gangreung.jpg" },
        { name: "경주", description: "천년 고도의 역사 여행", image: "src/images/gangreung.jpg" },
        { name: "인천", description: "국제공항과 차이나타운", image: "src/images/gangreung.jpg" }
      ],
      // 해외 여행지
      destinationsOverseas: [
        { name: "도쿄", description: "일본의 수도, 전통과 현대의 조화", image: "/src/images/gangreung.jpg" },
        { name: "파리", description: "낭만의 도시, 에펠탑과 루브르", image: "src/images/gangreung.jpg" },
        { name: "방콕", description: "태국의 활기찬 수도", image: "src/images/gangreung.jpg" },
        { name: "뉴욕", description: "세계의 중심, 자유의 여신상", image: "src/images/gangreung.jpg" },
        { name: "런던", description: "영국의 수도, 빅벤과 타워브리지", image: "https://source.unsplash.com/400x250/?london" },
        { name: "로마", description: "고대 유적과 이탈리아 감성", image: "https://source.unsplash.com/400x250/?rome" },
        { name: "시드니", description: "오페라하우스와 아름다운 항구", image: "https://source.unsplash.com/400x250/?sydney" },
        { name: "하와이", description: "천국 같은 휴양지", image: "https://source.unsplash.com/400x250/?hawaii" }
      ],
      // 인기 호텔
      popularHotels: [
        { name: "롯데호텔 서울", city: "서울", price: "₩250,000 / 1박", rating: 4.7, image: "src/images/paradiseHotel.jpg" },
        { name: "파라다이스 호텔", city: "부산", price: "₩200,000 / 1박", rating: 4.5, image: "src/images/paradiseHotel.jpg" },
        { name: "신라호텔", city: "제주", price: "₩300,000 / 1박", rating: 4.9, image: "src/images/paradiseHotel.jpg" },
        { name: "라마다 프라자", city: "인천", price: "₩180,000 / 1박", rating: 4.4, image: "src/images/paradiseHotel.jpg" }
      ],

      // 여행 팁
      travelTips: [
        { title: "제주도 여행 전 필수 체크리스트", description: "렌트카 예약, 숙소, 맛집 예약까지! 미리 준비하면 편리해요." },
        { title: "유럽 여행 시 꿀팁", description: "유레일 패스로 교통비 절약하고 인기 명소는 사전 예약 필수!" },
        { title: "해외여행 짐 싸기 노하우", description: "짐은 최소화! 멀티어댑터, 보조배터리는 필수 아이템." }
      ]
    };
  },
  mounted() {
    // 로그인 상태 확인 (로컬스토리지나 쿠키에서 토큰/사용자 정보 확인)
    this.checkAuthStatus();
  },
  methods: {
    checkAuthStatus() {
      // JWT 토큰이나 사용자 정보를 확인하여 로그인 상태 설정
      const token = localStorage.getItem('token');
      const userInfo = localStorage.getItem('user');
      
      if (token && userInfo) {
        this.isLoggedIn = true;
        this.user = JSON.parse(userInfo);
      }
    },
    handleLogout() {
      // 로그아웃 시 상태 초기화
      this.isLoggedIn = false;
      this.user = { name: "홍길동" };
    }
  },
  setup() {
    return {
      modules: [Navigation, Pagination], 
    };
  }
};
</script>
