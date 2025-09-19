<template>
  <div class="wishlist">
    <h2>내 위시리스트</h2>
    <ul>
      <li v-for="item in wishlist" :key="item.wishlistId" class="wishlist-item">
        <img :src="getImage(item.hotelImageUrl)" :alt="item.hotelName" class="hotel-image" />
        {{ item.hotelName }}
        <button @click="removeFromWishlist(item.wishlistId)">삭제</button>
      </li>
    </ul>
  </div>
</template>

<script setup>
import { ref, onMounted } from "vue";
import { useRouter } from "vue-router";
import api from "../api/api";

const wishlist = ref([]);
const router = useRouter();

const getImage = (fileName) => {
  try {
    return new URL(`../assets/hotels/${fileName}`, import.meta.url).href;
  } catch {
    return "/default.png";
  }
};

const fetchWishlist = async () => {
  const token = localStorage.getItem("token");
  if (!token) {
    alert("로그인이 필요합니다.");
    router.push("/login");
    return;
  }

  const res = await api.get("/wishlist/me", {
    headers: { Authorization: `Bearer ${token}` },
  });
  wishlist.value = res.data;
};

const removeFromWishlist = async (wishlistId) => {
  const token = localStorage.getItem("token");
  if (!token) return;

  await api.delete(`/wishlist/${wishlistId}`, {
    headers: { Authorization: `Bearer ${token}` },
  });
  wishlist.value = wishlist.value.filter((w) => w.wishlistId !== wishlistId);
  alert("위시리스트에서 제거되었습니다.");
};

onMounted(fetchWishlist);
</script>

<style scoped>
.hotel-image {
  width: 80px;
  height: 60px;
  object-fit: cover;
  border-radius: 6px;
  margin-right: 10px;
}
.wishlist-item {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 10px;
}
</style>
