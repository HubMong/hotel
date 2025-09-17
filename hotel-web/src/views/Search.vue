<template>
  <div class="search-layout">
    <aside class="filter-sidebar">
      <h3>상세 조건</h3>
      <div class="filter-group">
        <h4>1박 요금</h4>
        <div class="price-range">
          <span>{{ minPrice.toLocaleString() }}원</span>
          <span>{{ maxPrice.toLocaleString() }}원</span>
        </div>
        <input 
          type="range" 
          min="0" 
          max="1000000" 
          step="10000" 
          v-model="maxPrice"
          class="price-slider"
        >
      </div>
      <div class="filter-group">
        <h4>편의 시설</h4>
        <div v-for="amenity in amenitiesList" :key="amenity" class="checkbox-item">
          <input type="checkbox" :id="amenity" :value="amenity" v-model="selectedAmenities">
          <label :for="amenity">{{ amenity }}</label>
        </div>
      </div>
      <div class="filter-group">
        <h4>숙박 시설 등급</h4>
        <div v-for="type in accommodationTypes" :key="type" class="checkbox-item">
          <input type="checkbox" :id="type" :value="type" v-model="selectedTypes">
          <label :for="type">{{ type }}</label>
        </div>
      </div>
    </aside>

    <main class="main-content">
      <div class="search-results-header">
        <h2 class="page-title">호텔 검색 결과</h2>
      </div>
      <div class="results-container">
        <div v-if="filteredResults.length > 0" class="hotel-list">
          <p class="results-count"><strong>{{ filteredResults.length }}개</strong>의 검색 결과</p>
          <RouterLink v-for="hotel in filteredResults" :key="hotel.id" :to="`/hotels/${hotel.id}`" class="hotel-card-link">
            <article class="hotel-card">
              <img :src="findHotel(hotel.id)?.image" :alt="hotel.name" class="hotel-image">
              
              <div class="hotel-details">
                <span class="hotel-rating">{{ findHotel(hotel.id)?.rating }}</span>
                <h4 class="hotel-name">{{ findHotel(hotel.id)?.name }}</h4>
                <p class="hotel-amenities">{{ hotel.amenities.join(' · ') }}</p>
              </div>
              
              <div class="hotel-price-block">
                <span class="price">{{ hotel.price.toLocaleString() }}원</span>
                <p class="per-night">{{ findHotel(hotel.id)?.capacity }}명 / 1박</p>
              </div>
            </article>
          </RouterLink>
        </div>
        <div v-else class="no-results">
          <p>조건에 맞는 호텔이 없습니다.</p>
        </div>
      </div>
    </main>
  </div>
</template>

<script setup>
import { ref, watch, computed } from 'vue';
import { RouterLink } from 'vue-router';

// 이미지 파일들을 직접 import 합니다.
import hotel1Image from '@/assets/hotels/hotel1.png';
import hotel2Image from '@/assets/hotels/hotel2.png';
import hotel3Image from '@/assets/hotels/hotel3.png';
import hotel4Image from '@/assets/hotels/hotel4.png';
import hotel5Image from '@/assets/hotels/hotel5.png';


const props = defineProps({
  initialResults: {
    type: Array,
    required: true
  }
});

const amenitiesList = ref(['무료 Wi-Fi', '주차 가능', '수영장', '피트니스 센터']);
const selectedAmenities = ref([]);
const accommodationTypes = ref(['5성급(*****)', '4성급(****)', '3성급(***)', '2성급(**)', '1성급(*)']);
const selectedTypes = ref([]);
const minPrice = ref(0);
const maxPrice = ref(500000);

const allHotels = ref([
  { id: 1, name: '서울 신라 호텔', price: 450000, rating: '5성급(*****)', amenities: ['무료 Wi-Fi', '수영장', '피트니스 센터'], capacity: 2, image: hotel1Image, description: '서울 강남에 위치한 아코르 프리미엄 브랜드.' },
  { id: 2, name: '롯데 호텔 월드', price: 320000, rating: '5성급(*****)', amenities: ['무료 Wi-Fi', '주차 가능', '피트니스 센터'], capacity: 4, image: hotel2Image, description: '서울 잠실에 위치한 랜드마크 호텔.' },
  { id: 4, name: '노보텔 앰배서더 동대문', price: 210000, rating: '4성급(****)', amenities: ['무료 Wi-Fi', '주차 가능', '수영장'], capacity: 3, image: hotel3Image, description: '동대문 패션가에 위치한 모던한 디자인 호텔.' },
  { id: 5, name: 'L7 홍대', price: 180000, rating: '4성급(****)', amenities: ['무료 Wi-Fi', '피트니스 센터'], capacity: 2, image: hotel4Image, description: '힙한 홍대의 분위기를 담은 라이프스타일 호텔.' },
  { id: 6, name: '신라스테이 역삼', price: 150000, rating: '3성급(***)', amenities: ['무료 Wi-Fi', '주차 가능'], capacity: 2, image: hotel5Image, description: '강남 비즈니스 중심가에 위치한 실용적인 호텔.' },
]);

const findHotel = (hotelId) => {
  return allHotels.value.find(h => h.id === hotelId);
};

const filteredResults = computed(() => {
  return props.initialResults.filter(hotel => {
    const priceMatch = hotel.price >= minPrice.value && hotel.price <= maxPrice.value;
    const hotelDetails = findHotel(hotel.id);
    const typeMatch = selectedTypes.value.length > 0 ? selectedTypes.value.includes(hotelDetails.rating) : true;
    const amenityMatch = selectedAmenities.value.length > 0
      ? selectedAmenities.value.every(amenity => hotelDetails.amenities.includes(amenity))
      : true;
    return priceMatch && typeMatch && amenityMatch;
  });
});
</script>

<style scoped>
.search-layout {
  display: flex;
  gap: 48px;
  padding: 0 120px;
}
.filter-sidebar {
  flex-basis: 250px;
  flex-shrink: 0;
  border-right: 1px solid var(--line);
  padding-right: 32px;
}
.main-content {
  flex-grow: 1;
  overflow-x: hidden;
  padding-right: 0;
}
.filter-group { margin-bottom: 2rem; }
.filter-group h4 {
  margin-bottom: 1rem;
  font-size: 16px;
  color: var(--ink-light);
}
.checkbox-item {
  display: flex;
  align-items: center;
  margin-bottom: 0.8rem;
}
.checkbox-item input {
  margin-right: 8px;
  width: 16px;
  height: 16px;
}
.checkbox-item label {
  cursor: pointer;
  color: var(--ink);
  font-size: 14px;
}
.price-range {
  display: flex;
  justify-content: space-between;
  margin-bottom: 8px;
  font-size: 14px;
  color: var(--ink-light);
}
.price-slider {
  width: 100%;
  cursor: pointer;
}
.search-results-header { margin-bottom: 24px; }
.page-title {
  font-size: 28px;
  margin-bottom: 24px;
}
.results-container { margin-top: 48px; }
.results-count { margin-bottom: 16px; color: var(--ink); }
.hotel-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}
.hotel-card-link { text-decoration: none; color: inherit; }
.hotel-card {
  display: flex;
  align-items: center;
  gap: 20px;
  border: 1px solid var(--line);
  border-radius: 8px;
  padding: 20px;
  background: var(--bg-white);
  box-shadow: 0 2px 8px rgba(0,0,0,0.04);
  transition: all 0.2s ease;
}
.hotel-card:hover {
  border-color: var(--brand-primary);
  box-shadow: 0 4px 16px rgba(0,0,0,0.06);
  transform: translateY(-2px);
}
.hotel-image {
  width: 200px;
  height: 200px;
  object-fit: cover;
  border-radius: 6px;
  flex-shrink: 0;
}
.hotel-details { flex-grow: 1; }
.hotel-rating {
  font-size: 12px;
  color: var(--ink-light);
  margin-bottom: 4px;
}
.hotel-name {
  font-size: 18px;
  font-weight: 600;
  margin: 0;
  color: var(--ink);
}
.hotel-amenities {
  font-size: 14px;
  color: var(--ink-light);
  margin-top: 8px;
}
.hotel-price-block {
  text-align: right;
  flex-shrink: 0;
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: flex-end;
  gap: 4px;
}
.hotel-price-block .price {
  font-size: 22px;
  font-weight: 700;
  color: var(--brand-primary);
}
.hotel-price-block .per-night {
  font-size: 14px;
  color: var(--ink-light);
}
@media (max-width: 992px) {
  .search-layout {
    flex-direction: column;
    padding-top: 24px;
  }
  .filter-sidebar {
    border-right: none;
    padding-right: 0;
    border-bottom: 1px solid var(--line);
    margin-bottom: 24px;
    padding-bottom: 24px;
  }
}
</style>