<template>
  <div class="page-container">
    <header class="page-header">
      <div>
        <button @click="goBack" class="btn-back">←</button>
        <h1 class="page-title">{{ isEditMode ? '객실 수정' : '새 객실 등록' }}</h1>
      </div>
    </header>

    <main class="form-container">
      <form @submit.prevent="handleSubmit">
        <!-- 기본 정보 -->
        <div class="form-section">
          <h3>기본 정보</h3>
          <div class="form-grid">
            <div class="form-group span-2">
              <label for="roomName">객실명</label>
              <input type="text" id="roomName" v-model.trim="room.name" placeholder="예: 프리미어 디럭스 더블룸" required>
            </div>

            <div class="form-group">
              <label for="roomType">객실 타입</label>
              <select id="roomType" v-model="room.roomType" required>
                <option disabled value="">선택하세요</option>
                <option>스위트룸</option>
                <option>디럭스룸</option>
                <option>스탠다드룸</option>
                <option>싱글룸</option>
                <option>트윈룸</option>
              </select>
            </div>

            <div class="form-group">
              <label for="price">판매가 (1박)</label>
              <input type="text" id="price" v-model="formattedPrice" placeholder="숫자만 입력" required>
            </div>

            <div class="form-group">
              <label for="originalPrice">정가(선택)</label>
              <input type="text" id="originalPrice" v-model="formattedOriginalPrice" placeholder="숫자만 입력">
            </div>

            <div class="form-group">
              <label for="roomSize">방 크기</label>
              <div class="input-with-unit">
                <input type="number" id="roomSize" v-model.number="room.size" min="0" placeholder="숫자만 입력">
                <span>m²</span>
              </div>
            </div>

            <div class="form-group">
              <label for="roomCount">보유 객실 수</label>
              <input type="number" id="roomCount" v-model.number="room.roomCount" min="1" required>
            </div>

            <div class="form-group">
              <label for="capacityMin">기본 인원</label>
              <input type="number" id="capacityMin" v-model.number="room.capacityMin" min="1" required>
            </div>

            <div class="form-group">
              <label for="capacityMax">최대 인원</label>
              <input type="number" id="capacityMax" v-model.number="room.capacityMax" min="1" required>
            </div>

            <div class="form-group">
              <label for="checkInTime">체크인 시간</label>
              <input type="time" id="checkInTime" v-model="room.checkInTime" required>
            </div>

            <div class="form-group">
              <label for="checkOutTime">체크아웃 시간</label>
              <input type="time" id="checkOutTime" v-model="room.checkOutTime" required>
            </div>
          </div>
        </div>

        <!-- 상세 속성 -->
        <div class="form-section">
          <h3>상세 속성</h3>
          <div class="form-grid">
            <div class="form-group">
              <label for="bed">침대 구성</label>
              <input type="text" id="bed" v-model.trim="room.bed" placeholder="예: 더블 1개, 싱글 2개">
            </div>

            <div class="form-group">
              <label for="bath">욕실 수</label>
              <input type="number" id="bath" v-model.number="room.bath" min="0" placeholder="정수(예: 1)">
            </div>

            <div class="form-group">
              <label for="viewName">전망</label>
              <input type="text" id="viewName" v-model.trim="room.viewName" placeholder="예: 시티뷰, 오션뷰">
            </div>

            <div class="form-group">
              <label for="payment">결제 방식</label>
              <input type="text" id="payment" v-model.trim="room.payment" placeholder="예: 현장결제, 선결제">
            </div>

            <div class="form-group span-2">
              <label for="cancelPolicy">취소 정책</label>
              <input type="text" id="cancelPolicy" v-model.trim="room.cancelPolicy" placeholder="체크인 1일 전까지 무료 취소" />
            </div>
          </div>
        </div>

        <!-- 편의시설/특성 -->
        <div class="form-section">
          <h3>편의시설/특성</h3>
          <div class="amenities-grid">
            <label class="amenity-label">
              <input type="checkbox" v-model="room.aircon"> 에어컨
            </label>
            <label class="amenity-label">
              <input type="checkbox" v-model="room.wifi"> 무료 Wi-Fi
            </label>
            <label class="amenity-label">
              <input type="checkbox" v-model="room.freeWater"> 무료 생수
            </label>
            <label class="amenity-label">
              <input type="checkbox" v-model="room.hasWindow"> 창문 있음
            </label>
            <label class="amenity-label">
              <input type="checkbox" v-model="room.sharedBath"> 공용 욕실
            </label>
            <label class="amenity-label">
              <input type="checkbox" v-model="room.smoke"> 흡연 가능
            </label>
          </div>
          <small class="description">※ DB 스키마와 일치: aircon/wifi/free_water/has_window/shared_bath/smoke = BIT(1)</small>
        </div>

        <!-- 객실 사진 -->
        <div class="form-section">
          <h3>객실 사진</h3>
          <p class="description">첫 번째 사진이 대표 이미지로 사용됩니다. 드래그 앤 드롭으로 순서를 변경할 수 있습니다.</p>
          <div class="image-upload-container">
            <div class="image-preview-list">
              <div
                v-for="(image, index) in images"
                :key="image.id"
                class="image-preview-item"
                draggable="true"
                @dragstart="onDragStart(index)"
                @dragover.prevent
                @drop="onDrop(index)"
                @dragend="dragEnd"
              >
                <img :src="image.preview" :alt="image.file ? image.file.name : '기존 이미지'">
                <button type="button" class="remove-image-btn" @click="removeImage(index)">&times;</button>
                <span class="image-order-badge">{{ index + 1 }}</span>
              </div>
              <label class="image-upload-box">
                <span>+<br>사진 추가</span>
                <input type="file" multiple @change="handleImageUpload" accept="image/*">
              </label>
            </div>
          </div>
        </div>
        
        <div class="form-footer">
          <button type="submit" class="btn-primary" :disabled="isSubmitting">
            {{ isSubmitting ? '저장 중...' : (isEditMode ? '수정하기' : '저장하기') }}
          </button>
        </div>
      </form>
    </main>
  </div>
</template>

<script>
export default {
  name: 'OwnerRoomRegister',
  props: ['id'],
  data() {
    return {
      hotelId: null,
      isEditMode: false,
      isSubmitting: false,
      room: {
        name: '',
        roomType: '',
        price: null,
        originalPrice: null,
        size: null,                // UI용 숫자 → 전송시 문자열(roomSize)
        roomCount: 1,
        capacityMin: 2,
        capacityMax: 2,
        checkInTime: '15:00',
        checkOutTime: '11:00',
        bed: '',
        bath: 0,                   // INT(욕실 수)
        viewName: '',
        payment: '',
        cancelPolicy: '체크인 1일 전까지 무료 취소',
        aircon: true,              // BIT(1)
        wifi: true,                // BIT(1)
        freeWater: true,           // BIT(1)
        hasWindow: true,           // BIT(1)
        sharedBath: false,         // BIT(1)
        smoke: false,              // BIT(1) - 흡연 가능
        status: 'ACTIVE',          // 검색 집계 포함
      },
      images: [],
      deletedImages: [],
      dragIndex: null,
    };
  },
  computed: {
    formattedPrice: {
      get() {
        const v = this.room.price;
        return (v === null || v === undefined) ? '' : Number(v).toLocaleString('ko-KR');
      },
      set(value) {
        const n = Number(String(value ?? '').replace(/[^0-9]/g, ''));
        this.room.price = Number.isFinite(n) ? n : null;
      }
    },
    formattedOriginalPrice: {
      get() {
        const v = this.room.originalPrice;
        return (v === null || v === undefined) ? '' : Number(v).toLocaleString('ko-KR');
      },
      set(value) {
        const n = Number(String(value ?? '').replace(/[^0-9]/g, ''));
        this.room.originalPrice = Number.isFinite(n) ? n : null;
      }
    }
  },
  methods: {
    goBack() {
      this.$router.push({ name: 'OwnerRoom', query: { hotelId: this.hotelId } });
    },

    toBool(v) {
      return v === true || v === 1 || v === '1' || v === 'true' || v === 'TRUE';
    },

    async fetchRoomData() {
      try {
        const token = localStorage.getItem('token');
        const { data } = await this.$axios.get(`/owner/rooms/${this.id}`, { 
          headers: { 'Authorization': `Bearer ${token}` }
        });

        // 백엔드 키(스네이크/카멜) 모두 허용
        const g = (k, alt) => data[k] ?? data[alt];

        this.room = {
          name:            g('name'),
          roomType:        g('roomType', 'room_type') || '',
          price:           g('price') ?? null,
          originalPrice:   g('originalPrice', 'original_price') ?? null,
          size:            (() => {
                             const raw = g('roomSize', 'room_size');
                             if (raw == null) return null;
                             const n = Number(String(raw).replace(/[^0-9]/g, ''));
                             return Number.isFinite(n) ? n : null;
                           })(),
          roomCount:       g('roomCount', 'room_count') ?? 1,
          capacityMin:     g('capacityMin', 'capacity_min') ?? 2,
          capacityMax:     g('capacityMax', 'capacity_max') ?? 2,
          checkInTime:     g('checkInTime', 'check_in_time') ?? '15:00',
          checkOutTime:    g('checkOutTime', 'check_out_time') ?? '11:00',
          bed:             g('bed') ?? '',
          bath:            g('bath') ?? 0,
          viewName:        g('viewName', 'view_name') ?? '',
          payment:         g('payment') ?? '',
          cancelPolicy:    g('cancelPolicy', 'cancel_policy') ?? '체크인 1일 전까지 무료 취소',
          aircon:          this.toBool(g('aircon')),
          wifi:            this.toBool(g('wifi')),
          freeWater:       this.toBool(g('freeWater', 'free_water')),
          hasWindow:       this.toBool(g('hasWindow', 'has_window')),
          sharedBath:      this.toBool(g('sharedBath', 'shared_bath')),
          smoke:           this.toBool(g('smoke')), // 흡연 가능
          status:          g('status') ?? 'ACTIVE',
        };

        // 이미지 URL 처리(있다면)
        if (data.imageUrls && data.imageUrls.length > 0) {
          const baseURL = 'http://localhost:8888';
          this.images = data.imageUrls.map((url, index) => ({ 
            id: `existing-${index}`, 
            preview: String(url).startsWith('http') ? url : `${baseURL}${url}`,
            file: null,
            isExisting: true,
            originalUrl: url
          }));
        }
      } catch (error) {
        console.error("객실 정보 로딩 실패:", error);
        alert("객실 정보를 불러오는 데 실패했습니다.");
        this.goBack();
      }
    },
    
    handleImageUpload(event) {
      const files = Array.from(event.target.files || []);
      files.forEach(file => {
        this.images.push({ 
          id: `new-${Date.now()}-${Math.random()}`, 
          file, 
          preview: URL.createObjectURL(file),
          isExisting: false
        });
      });
      event.target.value = null;
    },
    
    removeImage(index) {
      const image = this.images[index];
      if (image.isExisting && image.originalUrl) {
        this.deletedImages.push(image.originalUrl);
      } else if (!image.isExisting && image.preview) {
        URL.revokeObjectURL(image.preview);
      }
      this.images.splice(index, 1);
    },
    
    onDragStart(index) { this.dragIndex = index; },
    onDrop(dropIndex) {
      if (this.dragIndex === null || this.dragIndex === dropIndex) return;
      const draggedItem = this.images.splice(this.dragIndex, 1)[0];
      this.images.splice(dropIndex, 0, draggedItem);
    },
    dragEnd() { this.dragIndex = null; },

    validateBeforeSubmit() {
      if (!this.room.name?.trim()) return '객실명을 입력해주세요.';
      if (!this.room.roomType) return '객실 타입을 선택해주세요.';
      if (!Number.isFinite(this.room.price) || this.room.price <= 0) return '판매가를 입력해주세요.';
      if (!Number.isFinite(this.room.roomCount) || this.room.roomCount < 1) return '보유 객실 수를 확인해주세요.';
      if (!Number.isFinite(this.room.capacityMin) || !Number.isFinite(this.room.capacityMax) || this.room.capacityMax < this.room.capacityMin) {
        return '기본/최대 인원을 확인해주세요.';
      }
      return null;
    },
    
    buildPayload() {
      // DB/백엔드 DTO에 맞춰 카멜케이스로 전달
      return {
        name: this.room.name,
        roomType: this.room.roomType,
        price: this.room.price,
        originalPrice: this.room.originalPrice ?? null,
        roomSize: this.room.size != null ? String(this.room.size) : null, // VARCHAR
        roomCount: this.room.roomCount,
        capacityMin: this.room.capacityMin,
        capacityMax: this.room.capacityMax,
        checkInTime: this.room.checkInTime,     // 'HH:mm' (백엔드에서 LocalTime으로 파싱)
        checkOutTime: this.room.checkOutTime,
        bed: this.room.bed || null,
        bath: Number.isFinite(this.room.bath) ? this.room.bath : null,
        viewName: this.room.viewName || null,
        payment: this.room.payment || null,
        cancelPolicy: this.room.cancelPolicy || null,
        aircon: !!this.room.aircon,
        wifi: !!this.room.wifi,
        freeWater: !!this.room.freeWater,
        hasWindow: !!this.room.hasWindow,
        sharedBath: !!this.room.sharedBath,
        smoke: !!this.room.smoke,              // 흡연 가능
        status: this.room.status || 'ACTIVE',
        // 이미지 수정 시 삭제 목록
        deletedImages: this.deletedImages.length ? this.deletedImages : undefined,
      };
    },
    
    async handleSubmit() {
      if (this.isSubmitting) return;
      this.isSubmitting = true;

      const token = localStorage.getItem('token');
      if (!token) {
        alert("로그인이 필요합니다.");
        this.isSubmitting = false;
        return this.$router.push('/login');
      }
      if (!this.hotelId) {
        alert("hotelId가 없습니다. 호텔 목록에서 다시 진입해주세요.");
        this.isSubmitting = false;
        return this.$router.push({ name: 'OwnerHotelList' });
      }

      const err = this.validateBeforeSubmit();
      if (err) {
        alert(err);
        this.isSubmitting = false;
        return;
      }

      const formData = new FormData();

      try {
        const body = this.buildPayload();
        formData.append('roomRequest', new Blob([JSON.stringify(body)], { type: 'application/json' }));

        // 이미지 파일 부착
        this.images.forEach(imageObj => {
          if (!imageObj.isExisting && imageObj.file) {
            // 신규 업로드: 등록은 'images', 수정은 'newImages'를 쓰는 백엔드도 있으니 둘 다 호환
            formData.append(this.isEditMode ? 'newImages' : 'images', imageObj.file);
          }
        });

        if (this.isEditMode) {
          await this.$axios.put(`/owner/rooms/${this.id}`, formData, {
            params: { hotelId: this.hotelId },
            headers: { 
              'Authorization': `Bearer ${token}`,
              'Content-Type': 'multipart/form-data'
            }
          });
          alert("객실 정보가 성공적으로 수정되었습니다.");
        } else {
          await this.$axios.post('/owner/rooms', formData, {
            params: { hotelId: this.hotelId },
            headers: { 
              'Authorization': `Bearer ${token}`,
              'Content-Type': 'multipart/form-data'
            }
          });
          alert("객실이 성공적으로 등록되었습니다.");
        }
        
        this.goBack();
        
      } catch (error) {
        console.error("저장 실패:", error.response || error);
        const action = this.isEditMode ? "수정" : "등록";
        const errorMsg = error.response?.data?.message || "입력 내용을 확인해주세요.";
        alert(`객실 ${action}에 실패했습니다. ${errorMsg}`);
      } finally {
        this.isSubmitting = false;
      }
    }
  },
  
  created() {
    this.hotelId = Number(this.$route.query.hotelId);
    if (!this.hotelId) {
      alert("hotelId가 없습니다. 호텔 목록에서 다시 진입해주세요.");
      this.$router.push({ name: 'OwnerHotelList' });
      return;
    }

    if (this.id) {
      this.isEditMode = true;
      this.fetchRoomData();
    }
  },
  
  beforeUnmount() {
    this.images.forEach(image => {
      if (!image.isExisting && image.preview) {
        URL.revokeObjectURL(image.preview);
      }
    });
  }
};
</script>

<style scoped>
.page-container {
  padding: 40px;
  background-color: #f8f9fa;
  min-height: 100vh;
}
.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 32px;
}
.page-header > div {
  display: flex;
  align-items: center;
  gap: 16px;
}
.page-title {
  font-size: 28px;
  font-weight: 800;
  color: #212529;
  margin: 0;
}
.btn-back {
  background: #fff;
  border: 1px solid #dee2e6;
  width: 40px;
  height: 40px;
  border-radius: 50%;
  font-size: 20px;
  font-weight: bold;
  cursor: pointer;
  transition: background-color 0.2s;
}
.btn-back:hover {
  background-color: #f1f3f5;
}
.form-container {
  background: #fff;
  border-radius: 12px;
  padding: 32px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.05);
}
.form-section {
  margin-bottom: 40px;
}
.form-section h3 {
  font-size: 18px;
  font-weight: 700;
  margin-bottom: 24px;
  padding-bottom: 12px;
  border-bottom: 1px solid #e9ecef;
}
.form-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 24px;
}
.form-group {
  display: flex;
  flex-direction: column;
}
.form-group.span-2 {
  grid-column: span 2;
}
label {
  margin-bottom: 8px;
  font-weight: 600;
  font-size: 14px;
}
input, select {
  padding: 0 16px;
  border: 1px solid #ced4da;
  border-radius: 8px;
  font-size: 16px;
  height: 48px;
}
input:focus, select:focus {
  outline: none;
  border-color: #4f46e5;
}
.input-with-unit {
  display: flex;
  align-items: center;
  border: 1px solid #ced4da;
  border-radius: 8px;
  padding-right: 16px;
}
.input-with-unit input {
  border: none;
  flex-grow: 1;
}
.input-with-unit span {
  color: #868e96;
}
.amenities-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(150px, 1fr));
  gap: 12px;
}
.amenity-label {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px;
  border-radius: 8px;
  cursor: pointer;
}
.amenity-label input {
  width: 18px;
  height: 18px;
  cursor: pointer;
}
.description {
  font-size: 13px;
  color: #868e96;
  margin-top: 6px;
}
.image-upload-container {
  background: #f8f9fa;
  padding: 16px;
  border-radius: 8px;
}
.image-preview-list {
  display: flex;
  flex-wrap: wrap;
  gap: 16px;
}
.image-upload-box {
  border: 2px dashed #ced4da;
  border-radius: 8px;
  width: 150px;
  height: 150px;
  display: flex;
  justify-content: center;
  align-items: center;
  text-align: center;
  cursor: pointer;
  color: #868e96;
  transition: border-color 0.2s;
}
.image-upload-box:hover {
  border-color: #4f46e5;
}
.image-upload-box input[type="file"] {
  display: none;
}
.image-preview-item {
  position: relative;
  width: 150px;
  height: 150px;
  border-radius: 8px;
  overflow: hidden;
  box-shadow: 0 2px 4px rgba(0,0,0,0.1);
  cursor: move;
}
.image-preview-item img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}
.remove-image-btn {
  position: absolute;
  top: 4px;
  right: 4px;
  width: 24px;
  height: 24px;
  border-radius: 50%;
  border: none;
  background: rgba(0,0,0,0.6);
  color: white;
  font-size: 16px;
  cursor: pointer;
  display: flex;
  justify-content: center;
  align-items: center;
  line-height: 1;
  transition: background 0.2s;
}
.remove-image-btn:hover {
  background: rgba(0,0,0,0.8);
}
.image-order-badge {
  position: absolute;
  bottom: 4px;
  left: 4px;
  background: rgba(0,0,0,0.6);
  color: white;
  border-radius: 50%;
  width: 24px;
  height: 24px;
  display: flex;
  justify-content: center;
  align-items: center;
  font-size: 12px;
  font-weight: bold;
}
.form-footer {
  margin-top: 40px;
  padding-top: 24px;
  border-top: 1px solid #e9ecef;
  display: flex;
  justify-content: flex-end;
}
.btn-primary {
  background-color: #4f46e5;
  color: white;
  border: none;
  padding: 14px 32px;
  border-radius: 8px;
  font-weight: 700;
  font-size: 16px;
  cursor: pointer;
  transition: background-color 0.2s;
}
.btn-primary:hover:not(:disabled) {
  background-color: #4338ca;
}
.btn-primary:disabled {
  background-color: #a5b4fc;
  cursor: not-allowed;
}
</style>
