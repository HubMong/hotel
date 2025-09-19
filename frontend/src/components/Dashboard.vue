<template>
  <div class="container py-4">
    <h2 class="mb-4">호텔 목록</h2>
    <table class="table table-bordered table-hover">
      <thead class="table-light">
        <tr>
          <th>ID</th>
          <th>이름</th>
          <th></th>
        </tr>
      </thead>
      <tbody>
        <tr v-for="hotel in hotels" :key="hotel.id">
          <td>{{ hotel.id }}</td>
          <td>{{ hotel.name }}</td>
          <td>
            <button class="btn btn-success btn-sm" @click="addToWishlist(hotel)">
              찜하기
            </button>
          </td>
        </tr>
      </tbody>
    </table>

    <div class="mb-4 d-flex gap-2">
      <button class="btn btn-success btn-sm" @click="fetchMyWishlist">내 찜 목록</button>
      <button 
        class="btn btn-sm"
        :class="isLoggedIn ? 'btn-danger' : 'btn-primary'"
        @click="isLoggedIn ? logout() : goLogin()">
        {{ isLoggedIn ? '로그아웃' : '로그인' }}
      </button>
      
    </div>

    <!-- 내 찜목록 표출 -->
    <div v-if="wishlist.length">
      <h3 class="mb-2">내 찜목록</h3>
      <table class="table table-bordered table-hover">
        <thead class="table-light">
          <tr>
            <th>찜ID</th>
            <th>호텔 이름</th>
            <th></th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="item in wishlist" :key="item.wishlistId">
            <td>{{ item.wishlistId }}</td>
            <td>{{ item.hotelName }}</td>
            <td>
              <button class="btn btn-danger btn-sm" @click="removeFromWishlist(item.wishlistId)">
                찜 제거
              </button>
            </td>
          </tr>
        </tbody>
      </table>
    </div>
  </div>
</template>

<script>
import axios from "axios";

export default {
  name: "HotelList",
  data() {
    return {
      hotels: [],
      wishlist: [],
    };
  },
  computed: {
    isLoggedIn() {
      return !!localStorage.getItem("Authorization"); // ✅ computed로 관리
    },
  },
  mounted() {
    this.fetchHotels();
  },
  methods: {
    async fetchHotels() {
      try {
        const res = await axios.get("http://localhost:8888/hotel/lists");
        this.hotels = res.data;
      } catch (err) {
        console.error("호텔 목록 불러오기 실패:", err);
      }
    },

    async addToWishlist(hotel) {
      try {
        const token = localStorage.getItem("Authorization");
        if (!token) {
          alert("로그인이 필요합니다.");
          return;
        }

        await axios.post(
          "http://localhost:8888/wishlist/add",
          { hotelId: hotel.id },
          { headers: { Authorization: token } }
        );

        alert(`호텔 '${hotel.name}'이(가) 찜목록에 추가되었습니다!`);
        this.fetchMyWishlist();
      } catch (err) {
        if (err.response && err.response.data === "이미 찜한 호텔입니다.") {
          alert("이미 찜한 호텔이에요!");
        } else {
          alert("찜하기 실패!");
        }
      }
    },

    async fetchMyWishlist() {
      try {
        const token = localStorage.getItem("Authorization");
        const res = await axios.get("http://localhost:8888/wishlist/me", {
          headers: { Authorization: token },
        });

        this.wishlist = res.data;
      } catch (err) {
        console.error("내 찜 목록 조회 실패:", err);
      }
    },

    async removeFromWishlist(wishlistId) {
      try {
        const token = localStorage.getItem("Authorization");
        if (!token) {
          alert("로그인이 필요합니다.");
          return;
        }

        await axios.delete(`http://localhost:8888/wishlist/${wishlistId}`, {
          headers: { Authorization: token },
        });

        alert("찜목록에서 제거되었습니다!");
        this.fetchMyWishlist();
      } catch (err) {
        console.error("찜 제거 실패:", err);
        alert("찜 제거 실패!");
      }
    },

    logout() {
      localStorage.removeItem("Authorization"); // ✅ 토큰 제거
      this.$router.push("/login");
    },
    goLogin() {
      this.$router.push("/login");
    },
    goCheckout() {
      this.$router.push("/checkout");
    },
  },
};
</script>
