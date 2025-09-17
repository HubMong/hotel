<template>
  <header class="topbar" role="banner">
    <div class="container topbar__inner">
      <router-link class="logo" to="/hotels/1" aria-label="호텔 상세 홈">egoda</router-link>

      <nav class="actions" aria-label="주요 메뉴">
        <router-link class="btn btn--ghost" to="/me">마이 페이지</router-link>
        <router-link class="btn btn--ghost" to="/wishlist">찜 목록</router-link>
        <router-link class="btn btn--primary" to="/login">로그인</router-link>
      </nav>
    </div>
  </header>

  <section class="searchbar sticky-search">
    <div class="container">
      <form @submit.prevent="handleSearch" class="searchgrid">
        <div class="field">
          <label>어느 곳을 찾고 계신가요?</label>
          <input v-model="destination" placeholder="도시, 숙소명 등" />
        </div>

        <div class="field">
          <label>체크-인</label>
          <input v-model="checkInDate" type="date" />
        </div>

        <div class="field">
          <label>체크-아웃</label>
          <input v-model="checkOutDate" type="date" />
        </div>

        <div class="field guests-field" ref="guestsField">
          <label>인원</label>
          <div class="guest-display" @click="isGuestCounterVisible = !isGuestCounterVisible">
            <p>{{ totalGuestsDisplay }}</p>
          </div>
          <div class="guests-counter" v-show="isGuestCounterVisible">
            <div class="counter-item">
              <span>성인</span>
              <div class="counter-buttons">
                <button type="button" @click="decrementAdults" class="counter-btn">-</button>
                <span class="count">{{ adults }}</span>
                <button type="button" @click="incrementAdults" class="counter-btn">+</button>
              </div>
            </div>
            <div class="counter-item">
              <span>어린이</span>
              <div class="counter-buttons">
                <button type="button" @click="decrementChildren" class="counter-btn">-</button>
                <span class="count">{{ children }}</span>
                <button type="button" @click="incrementChildren" class="counter-btn">+</button>
              </div>
            </div>
          </div>
        </div>

        <button class="btn btn--primary" type="submit">검색</button>
      </form>
    </div>
  </section>

  <router-view class="page" :initialResults="searchResults" />
</template>

<script setup>
import { ref, computed, onMounted, onBeforeUnmount } from 'vue';
import { RouterLink } from 'vue-router';
import axios from 'axios';  

const destination = ref('');
const checkInDate = ref('');
const checkOutDate = ref('');
const adults = ref(0);
const children = ref(0);
const isGuestCounterVisible = ref(false);
const guestsField = ref(null);

const incrementAdults = () => { adults.value++; };
const decrementAdults = () => { if (adults.value > 0) adults.value--; };
const incrementChildren = () => { children.value++; };
const decrementChildren = () => { if (children.value > 0) children.value--; };

// 총 인원수 표시를 위한 computed 속성
const totalGuestsDisplay = computed(() => {
  const total = adults.value + children.value;
  return `총 ${total}명`;
});

const allHotels = ref([
  { id: 1, name: '서울 신라 호텔', price: 450000, rating: '5성급(*****)', amenities: ['무료 Wi-Fi', '수영장', '피트니스 센터'], capacity: 2, image: 'https://picsum.photos/id/10/200/200', description: '서울 강남에 위치한 아코르 프리미엄 브랜드.' },
  { id: 2, name: '롯데 호텔 월드', price: 320000, rating: '5성급(*****)', amenities: ['무료 Wi-Fi', '주차 가능', '피트니스 센터'], capacity: 4, image: 'https://picsum.photos/id/20/200/200', description: '서울 잠실에 위치한 랜드마크 호텔.' },
  { id: 4, name: '노보텔 앰배서더 동대문', price: 210000, rating: '4성급(****)', amenities: ['무료 Wi-Fi', '주차 가능', '수영장'], capacity: 3, image: 'https://picsum.photos/id/30/200/200', description: '동대문 패션가에 위치한 모던한 디자인 호텔.' },
  { id: 5, name: 'L7 홍대', price: 180000, rating: '4성급(****)', amenities: ['무료 Wi-Fi', '피트니스 센터'], capacity: 2, image: 'https://picsum.photos/id/40/200/200', description: '힙한 홍대의 분위기를 담은 라이프스타일 호텔.' },
  { id: 6, name: '신라스테이 역삼', price: 150000, rating: '3성급(***)', amenities: ['무료 Wi-Fi', '주차 가능'], capacity: 2, image: 'https://picsum.photos/id/50/200/200', description: '강남 비즈니스 중심가에 위치한 실용적인 호텔.' },
]);

// 페이지 로딩 시점에 모든 호텔을 보여줍니다.
const searchResults = ref(allHotels.value);
const handleSearch = () => {
  searchResults.value = allHotels.value.filter(hotel => {
    const destinationMatch = destination.value ? hotel.name.toLowerCase().includes(destination.value.toLowerCase()) : true;
    const guestsMatch = (adults.value + children.value) <= hotel.capacity;
    return destinationMatch && guestsMatch;
  });
};

// 드롭다운 외부 클릭 시 닫기
const closeOnOutsideClick = (event) => {
  if (guestsField.value && !guestsField.value.contains(event.target)) {
    isGuestCounterVisible.value = false;
  }
};
onMounted(() => {
  document.addEventListener('click', closeOnOutsideClick);
});
onBeforeUnmount(() => {
  document.removeEventListener('click', closeOnOutsideClick);
});
</script>

<style>
/* 전역 리셋/유틸 */
html, body, #app { margin: 0; padding: 0; }
#app { max-width: none; }
:root{
  --brand:#223454;
  --ink:#1d2939;
  --line:#e5e7eb;
  --bg:#fff;
}
body { background: var(--bg); color: var(--ink);
  font-family: system-ui, -apple-system, Segoe UI, Roboto, sans-serif; }

/* 가운데 정렬 공통 컨테이너 */
.container { max-width: 1200px; margin: 0 auto; padding: 0 16px; }

/* 공통 필드 스타일 */
.field { display: grid; gap: 4px; position: relative; } /* position: relative 추가 */
.field label { font-size: 12px; color: #777; }

/* 인원 선택 UI 스타일 */
.guests-field .guest-display {
  height: 40px;
  padding: 0 10px;
  border: 1px solid #ddd;
  border-radius: 10px;
  background: #fff;
  text-align: left;
  display: flex;
  align-items: center;
  cursor: pointer;
}
.guests-field .guest-display p {
  margin: 0;
  font-size: 14px;
  color: var(--ink);
}
.guests-counter {
  position: absolute;
  top: 100%;
  left: 0;
  z-index: 10;
  background-color: #fff;
  border: 1px solid #ddd;
  border-radius: 10px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
  padding: 16px;
  display: flex;
  flex-direction: column;
  gap: 16px;
  width: 250px;
  margin-top: 8px;
}
.counter-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.counter-buttons {
  display: flex;
  align-items: center;
  gap: 8px;
}
.counter-btn {
  width: 28px;
  height: 28px;
  border: 1px solid #ccc;
  border-radius: 50%;
  background-color: transparent;
  cursor: pointer;
  font-size: 16px;
  font-weight: bold;
  color: #555;
  display: flex;
  align-items: center;
  justify-content: center;
}
.count {
  min-width: 15px;
  text-align: center;
  font-size: 14px;
}

/* 헤더 */
.topbar{
  position: sticky; top: 0; z-index: 100;
  height: 64px; background: #fff; border-bottom: 1px solid var(--line);
  box-shadow: 0 .5px 0 rgba(0,0,0,.04);
}
.topbar__inner{ height: 100%; display: flex; align-items: center; gap: 16px; }
.logo{ font-weight:800; font-size:22px; color: var(--brand); text-decoration: none; }
.actions{ margin-left:auto; display:flex; gap:10px; }
.btn{
  display:inline-flex; align-items:center; justify-content:center;
  height:36px; padding:0 14px; border-radius:18px; font-weight:600; font-size:14px;
  border:1px solid transparent; text-decoration:none; transition:.15s ease;
}
.btn--ghost{ background:#fff; color:#344054; border-color: var(--line); }
.btn--ghost:hover{ background:#f8fafc; }
.btn--primary{ background:var(--brand); color:#fff; border-color:var(--brand); }
.btn--primary:hover{ filter:brightness(1.05); }

/* 검색바 (헤더 아래 고정) */
.sticky-search{ position: sticky; top: 64px; z-index: 90; }
.searchbar{ background:#fff; border-bottom:1px solid #eee; }
.searchgrid{
  display:grid; gap:10px; align-items:end;
  grid-template-columns: 1.6fr 1fr 1fr 1fr 120px;
  padding: 12px 0;
}
.field input{
  height:40px; padding:0 10px; border:1px solid #ddd; border-radius:10px; background:#fff;
  text-align:left;
}
.ghost{ cursor: default; }

/* 본문 여백 */
.page{ padding: 16px 0 40px; }

/* 반응형 */
@media (max-width: 960px){
  .searchgrid{ grid-template-columns: 1fr 1fr; }
  .btn--primary{ grid-column: 1 / -1; }
}
</style>