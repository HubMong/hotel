<template>
  <div class="hotels">
    <h2>호텔 목록</h2>
    <div v-if="hotels.length === 0">등록된 호텔이 없습니다.</div>
    <ul>
      <li v-for="hotel in hotels" :key="hotel.id" class="hotel-item">
        <div class="hotel-info">
          <img :src="getImage(hotel.imageUrl)" :alt="hotel.name" class="hotel-image" />
          <span class="hotel-name">{{ hotel.name }}</span>
        </div>
        <button @click="addToWishlist(hotel.id)">🤍 위시리스트 추가</button>
      </li>
    </ul>
  </div>
</template>

<script setup>
import { ref, onMounted } from "vue";
import { useRouter } from "vue-router";
import api from "../api/api";

const hotels = ref([]);
const router = useRouter();

const getImage = (path) => {
  try {
    return new URL(`../assets/hotels/${path}`, import.meta.url).href;
  } catch {
    return "/default.png"; // 기본 이미지
  }
};

const fetchHotels = async () => {
  try {
    const res = await api.get("/hotel/lists");
    hotels.value = res.data;
  } catch (err) {
    console.error("호텔 목록 불러오기 실패:", err);
  }
};

const addToWishlist = async (hotelId) => {
  try {
    const token = localStorage.getItem("token");
    if (!token) {
      alert("로그인이 필요합니다.");
      router.push("/login");
      return;
    }

    await api.post(
      "/wishlist/add",
      { hotelId },
      { headers: { Authorization: `Bearer ${token}` } }
    );

    alert("위시리스트에 추가되었습니다!");
  } catch (err) {
    console.error("위시리스트 추가 실패:", err);
    alert("이미 추가되었습니다");
  }
};

onMounted(fetchHotels);
</script>

<style scoped>
.hotels {
  max-width: 600px;
  margin: 20px auto;
}
.hotel-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px;
  border-bottom: 1px solid #ddd;
}
.hotel-info {
  display: flex;
  align-items: center;
  gap: 12px;
}
.hotel-image {
  width: 100px;
  height: 80px;
  object-fit: cover;
  border-radius: 6px;
}
.hotel-name {
  font-size: 16px;
  font-weight: bold;
}
button {
  background: #28a745;
  color: white;
  border: none;
  border-radius: 4px;
  padding: 6px 10px;
  cursor: pointer;
}
button:hover {
  background: #218838;
}
</style>
