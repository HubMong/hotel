<template>
  <div class="room-management-container">
    <header class="page-header">
      <h1 class="page-title">객실 관리</h1>
      <button @click="goToRegisterPage" class="btn-primary">＋ 새 객실 등록</button>
    </header>
    <div class="room-list-card">
      <table class="room-table">
        <thead>
          <tr>
            <th>객실명</th>
            <th>객실 타입</th>
            <th>보유 객실</th> <th>기본/최대 인원</th>
            <th>판매가 (1박)</th>
            <th>관리</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="room in rooms" :key="room.id">
            <td>{{ room.name }}</td>
            <td>{{ room.roomType }}</td>
            <td>{{ room.roomCount }}개</td> <td>{{ room.capacity }}</td>
            <td>{{ room.price }}</td>
            <td>
              <button @click="goToUpdatePage(room.id)" class="btn-secondary btn-sm">수정</button>
              <button @click="deleteRoom(room.id)" class="btn-danger btn-sm">삭제</button>
            </td>
          </tr>
          <tr v-if="!rooms.length">
            <td colspan="6" class="no-data">등록된 객실이 없습니다.</td>
          </tr>
        </tbody>
      </table>
    </div>
  </div>
</template>


<script>
export default {
  name: 'OwnerRoom',
  data() {
    return {
      rooms: [],
    };
  },
  methods: {
    goToRegisterPage() {
      // 🔹 등록 페이지로 갈 때도 hotelId 넘겨주기
      const hotelId = Number(this.$route.query.hotelId);
      this.$router.push({ name: 'OwnerRoomRegister', query: { hotelId } });
    },
    goToUpdatePage(roomId) {
      // 수정 페이지 라우팅은 기존대로 (필요하면 hotelId 같이 넘겨도 됨)
      this.$router.push({ name: 'OwnerRoomUpdate', params: { id: roomId }, query: { hotelId: this.$route.query.hotelId } });
    },
    async deleteRoom(roomId) {
      if (!confirm("정말로 이 객실을 삭제하시겠습니까?")) return;
      try {
        const token = localStorage.getItem('token');
        await this.$axios.delete(`/owner/rooms/${roomId}`, {
          headers: { 'Authorization': `Bearer ${token}` }
        });
        alert("객실이 삭제되었습니다.");
        this.fetchRooms(); // 목록 새로고침
      } catch (error) {
        console.error("객실 삭제 실패:", error);
        alert("객실 삭제에 실패했습니다.");
      }
    },
    async fetchRooms() {
      try {
        const token = localStorage.getItem('token');
        const hotelId = Number(this.$route.query.hotelId);

        if (!token) {
          alert("로그인이 필요합니다.");
          this.$router.push('/login');
          return;
        }
        if (!hotelId) {
          alert("hotelId가 없습니다. 호텔 목록에서 다시 진입해주세요.");
          this.$router.push({ name: 'OwnerHotelList' });
          return;
        }

        // 🔹 여기! hotelId를 쿼리로 전달
        const response = await this.$axios.get('/owner/rooms', {
          params: { hotelId },
          headers: { 'Authorization': `Bearer ${token}` }
        });
        this.rooms = response.data;

      } catch (error) {
        console.error("객실 목록을 불러오는 데 실패했습니다:", error);
        alert("객실 목록을 불러오는 중 오류가 발생했습니다.");
      }
    }
  },
  mounted() {
    this.fetchRooms();
  }
};
</script>


<style scoped>
/* 기존 스타일은 그대로 유지합니다. */
.room-management-container {
  padding: 40px;
  background-color: #f8f9fa;
  height: 100%;
}
.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 32px;
}
.page-title {
  font-size: 28px;
  font-weight: 800;
  color: #212529;
}
.btn-primary {
  background-color: #4f46e5;
  color: white;
  border: none;
  padding: 12px 20px;
  border-radius: 8px;
  font-weight: 700;
  font-size: 16px;
  cursor: pointer;
  transition: background-color 0.2s;
}
.btn-primary:hover {
  background-color: #4338ca;
}
.room-list-card {
  background: #fff;
  border-radius: 12px;
  padding: 32px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.05);
  border: 1px solid #e9ecef;
}
.room-table {
  width: 100%;
  border-collapse: collapse;
  text-align: left;
}
.room-table th {
  padding: 16px;
  border-bottom: 2px solid #dee2e6;
  font-size: 14px;
  font-weight: 700;
  color: #495057;
  background-color: #f8f9fa;
}
.room-table td {
  padding: 16px;
  border-bottom: 1px solid #e9ecef;
  font-size: 15px;
  vertical-align: middle;
}
.room-table tbody tr:last-child td {
  border-bottom: none;
}
.status-badge {
  padding: 4px 10px;
  border-radius: 12px;
  font-size: 12px;
  font-weight: 700;
}
.status-active {
  background-color: #d1fae5;
  color: #065f46;
}
.status-inactive {
  background-color: #fee2e2;
  color: #991b1b;
}
.btn-secondary, .btn-danger {
  border: 1px solid #dee2e6;
  background-color: #fff;
  padding: 6px 12px;
  border-radius: 6px;
  cursor: pointer;
  font-weight: 600;
  margin-right: 8px;
  transition: background-color 0.2s, color 0.2s;
}
.btn-danger {
  color: #dc3545;
  border-color: #dc3545;
}
.btn-secondary:hover {
  background-color: #f1f3f5;
}
.btn-danger:hover {
  background-color: #dc3545;
  color: white;
}
.no-data {
    text-align: center;
    padding: 60px;
    color: #868e96;
}
</style>