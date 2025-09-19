<template>
  <div class="search-layout">
    <aside class="filter-sidebar">
      <h3>상세 조건</h3>

      <!-- 가격 필터 -->
      <div class="filter-group">
        <h4>1박 요금</h4>
        <div class="price-range">
          <span>{{ minPrice.toLocaleString() }}원</span>
          <span>{{ maxPrice.toLocaleString() }}원</span>
        </div>
        <div class="price-sliders">
          <input type="range" min="0" max="1000000" step="10000" v-model="minPrice" class="price-slider" />
          <input type="range" min="0" max="1000000" step="10000" v-model="maxPrice" class="price-slider" />
        </div>
      </div>

      <!-- 편의 시설 필터 -->
      <div class="filter-group">
        <h4>편의 시설</h4>
        <div v-for="amenity in amenitiesList" :key="amenity" class="checkbox-item">
          <input type="checkbox" :id="amenity" :value="amenity" v-model="selectedAmenities" />
          <label :for="amenity">{{ amenity }}</label>
        </div>
      </div>

      <!-- 숙박 시설 등급 필터 -->
      <div class="filter-group">
        <h4>숙박 시설 등급</h4>
        <div v-for="type in accommodationTypes" :key="type" class="checkbox-item">
          <input type="checkbox" :id="type" :value="type" v-model="selectedTypes" />
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

          <article v-for="hotel in filteredResults" :key="hotel.id" class="hotel-card">
            <img :src="getImage(hotel.imageUrl)" :alt="hotel.name" class="hotel-image" />

            <div class="hotel-details">
              <span class="hotel-rating">{{ hotel.rating }}</span>
              <h4 class="hotel-name">{{ hotel.name }}</h4>
              <p class="hotel-amenities">{{ hotel.amenities?.join(' · ') }}</p>
            </div>

            <div class="hotel-price-block">
              <span class="price">{{ hotel.price.toLocaleString() }}원</span>
              <p class="per-night">{{ hotel.capacity }}명 / 1박</p>

              <!-- 찜 버튼 -->
              <button class="wishlist-btn" @click="addToWishlist(hotel)">
                찜하기
              </button>
            </div>
          </article>
        </div>
        <div v-else class="no-results">
          <p>조건에 맞는 호텔이 없습니다.</p>
        </div>
      </div>
    </main>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from "vue";
import { useRouter } from "vue-router";
import api from "../api/api";

const router = useRouter();

const amenitiesList = ref(["무료 Wi-Fi", "주차 가능", "수영장", "피트니스 센터"]);
const selectedAmenities = ref([]);
const accommodationTypes = ref(["5성급(*****)", "4성급(****)", "3성급(***)", "2성급(**)", "1성급(*)"]);
const selectedTypes = ref([]);
const minPrice = ref(0);
const maxPrice = ref(500000);

const hotels = ref([]);

// ✅ 이미지 로더
const getImage = (path) => {
  try {
    return new URL(`../assets/hotels/${path}`, import.meta.url).href;
  } catch {
    return "/default.png"; // 기본 이미지
  }
};

// ✅ DB에서 호텔 목록 불러오기
const fetchHotels = async () => {
  try {
    const res = await api.get("/hotel/lists");
    hotels.value = res.data.map(h => ({
      ...h,
      amenities: h.amenities ? h.amenities.split(",") : [],
    }));
  } catch (err) {
    console.error("호텔 불러오기 실패:", err);
  }
};

// ✅ 위시리스트 추가
const addToWishlist = async (hotel) => {
  try {
    const token = localStorage.getItem("token");
    if (!token) {
      alert("로그인이 필요합니다.");
      router.push("/login");
      return;
    }

    await api.post(
      "/wishlist/add",
      { hotelId: hotel.id },
      { headers: { Authorization: `Bearer ${token}` } }
    );

    alert("위시리스트에 추가되었습니다!");
  } catch (err) {
    console.error("위시리스트 추가 실패:", err);
    alert("이미 추가되었습니다.");
  }
};

// ✅ 필터링
const filteredResults = computed(() => {
  return hotels.value.filter((hotel) => {
    const priceMatch = hotel.price >= minPrice.value && hotel.price <= maxPrice.value;
    const typeMatch = selectedTypes.value.length > 0 ? selectedTypes.value.includes(hotel.rating) : true;
    const amenityMatch =
      selectedAmenities.value.length > 0
        ? selectedAmenities.value.every((a) => hotel.amenities.includes(a))
        : true;
    return priceMatch && typeMatch && amenityMatch;
  });
});

onMounted(fetchHotels);
</script>


<style scoped>
/* 🔹 기존 스타일 + 찜 버튼 스타일 포함 */
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
.price-sliders {
  display: flex;
  flex-direction: column;
  gap: 8px;
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
.wishlist-btn {
  margin-top: 8px;
  padding: 6px 12px;
  font-size: 14px;
  border: 1px solid var(--brand-primary);
  border-radius: 6px;
  background: #fff;
  color: var(--brand-primary);
  cursor: pointer;
  transition: all 0.2s ease;
}
.wishlist-btn:hover {
  background: var(--brand-primary);
  color: #fff;
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
  .hotel-card {
    flex-direction: column;
    align-items: flex-start;
  }
  .hotel-image {
    width: 100%;
    height: 180px;
  }
  .hotel-price-block {
    align-items: flex-start;
  }
}
</style>
