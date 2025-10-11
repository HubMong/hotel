<template>
  <div class="page-container">
    <header class="page-header">
      <h1 class="page-title">예약 관리</h1>
    </header>

    <div class="filter-bar">
      <select v-model="filters.roomType">
        <option value="">모든 객실 타입</option>
        <option v-for="rt in roomTypes" :key="rt" :value="rt">{{ rt }}</option>
      </select>

      <input type="text" v-model="filters.guestName" placeholder="방문자 이름 검색" />

      <select v-model="filters.reservationStatus">
        <option value="전체">전체 상태</option>
        <option value="COMPLETED">예약 완료</option>
        <option value="CANCELLED">예약 취소</option>
      </select>

      <select v-model="filters.checkinoutStatus">
        <option value="전체">전체</option>
        <option value="오늘 체크인">오늘 체크인</option>
        <option value="오늘 체크아웃">오늘 체크아웃</option>
      </select>
      
      <button @click="resetFilters" class="btn-secondary">초기화</button>
    </div>

    <main class="calendar-card">
      <FullCalendar ref="fullCalendar" :options="calendarOptions" />
    </main>

    <div v-if="listModal.visible" class="modal-overlay" @click.self="closeListModal">
      <div class="modal-content list-modal">
        <header class="modal-header">
          <h3>{{ listModal.date }} 예약 목록</h3>
          <button @click="closeListModal" class="close-button">&times;</button>
        </header>
        <ul class="reservation-list">
          <li v-for="event in listModal.events" :key="event.id" @click="openDetailModal(event.id)">
            <span class="list-item-color" :style="{ backgroundColor: event.color }"></span>
            {{ event.title }}
          </li>
        </ul>
      </div>
    </div>

    <div v-if="detailModal.visible" class="modal-overlay" @click.self="closeDetailModal">
      <div class="modal-content detail-modal">
        <header class="modal-header">
          <h3>예약 상세 정보</h3>
          <button @click="closeDetailModal" class="close-button">&times;</button>
        </header>
        <div v-if="detailModal.loading" class="loading-spinner"></div>
        <div v-if="detailModal.data" class="detail-grid">
          <div><label>호텔명</label><span>{{ detailModal.data.hotelName }}</span></div>
          <div><label>객실타입</label><span>{{ detailModal.data.roomType }}</span></div>
          <div><label>방문자 이름</label><span>{{ detailModal.data.guestName }}</span></div>
          <div><label>방문자 전화번호</label><span>{{ detailModal.data.guestPhone }}</span></div>
          <div><label>체크인</label><span>{{ detailModal.data.checkInDate }}</span></div>
          <div><label>체크아웃</label><span>{{ detailModal.data.checkOutDate }}</span></div>
          <div><label>인원</label><span>성인 {{ detailModal.data.numAdult }}명, 아동 {{ detailModal.data.numKid }}명</span></div>
          <div><label>예약 상태</label><span :class="detailModal.data.reservationStatus.toLowerCase()">{{ detailModal.data.reservationStatus }}</span></div>
          <div class="full-width"><label>체크인/아웃 상태</label><span :class="detailModal.data.resStatus.toLowerCase()">{{ detailModal.data.resStatus }}</span></div>
        </div>
        <div v-if="detailModal.data" class="modal-actions">
          <button v-if="canCheckIn" @click="updateResStatus('check-in')" class="btn-primary">체크인</button>
          <button v-if="canCancelCheckIn" @click="updateResStatus('cancel-checkin')" class="btn-secondary">체크인 취소</button>
          <button v-if="canCheckOut" @click="updateResStatus('check-out')" class="btn-success">체크아웃</button>
          <button v-if="canCancelCheckOut" @click="updateResStatus('cancel-checkout')" class="btn-secondary">체크아웃 취소</button>
          <button v-if="canCancelReservation" @click="updateResStatus('cancel')" class="btn-danger">예약 취소</button>
        </div>
      </div>
    </div>
  </div>
</template>



<script>
import { useRouter } from 'vue-router';
import FullCalendar from '@fullcalendar/vue3';
import dayGridPlugin from '@fullcalendar/daygrid';
import timeGridPlugin from '@fullcalendar/timegrid';
import listPlugin from '@fullcalendar/list';
import interactionPlugin from '@fullcalendar/interaction';

export default {
  name: 'OwnerReservation',
  components: { FullCalendar },
  data() {
    return {
      fullCalendar: null,
      allEvents: [],
      roomTypes: ['스위트룸', '디럭스룸', '스탠다드룸', '싱글룸', '트윈룸'],
      today: new Date().toISOString().split('T')[0],
      filters: {
        roomType: '',
        guestName: '',
        reservationStatus: 'COMPLETED',
        checkinoutStatus: '전체',
      },
      listModal: {
        visible: false,
        date: '',
        events: [],
      },
      detailModal: {
        visible: false,
        loading: false,
        data: null,
        id: null,
      },
      calendarOptions: {
        plugins: [dayGridPlugin, timeGridPlugin, listPlugin, interactionPlugin],
        initialView: 'dayGridMonth',
        headerToolbar: { left: 'prev,next today', center: 'title', right: 'dayGridMonth,dayGridWeek' },
        locale: 'ko',
        events: [],
        dayMaxEvents: true,
        eventClick: (info) => this.openDetailModal(info.event.id),
        dateClick: (info) => {
          const calendarApi = this.$refs.fullCalendar.getApi();
          const eventsOnDate = calendarApi.getEvents().filter(e => {
            const start = e.startStr.split('T')[0];
            const end = new Date(e.end - 1).toISOString().split('T')[0];
            return info.dateStr >= start && info.dateStr <= end;
          });

          if (eventsOnDate.length > 0) {
            this.listModal.date = info.dateStr;
            this.listModal.events = eventsOnDate.map(e => ({ id: e.id, title: e.title, color: e.backgroundColor }));
            this.listModal.visible = true;
          }
        },
      },
    };
  },
  computed: {
    filteredEvents() {
      if (!this.allEvents) return [];
      return this.allEvents.filter(event => {
        const detail = event.extendedProps;
        if (!detail) return false;

        const roomTypeMatch = this.filters.roomType === '' || (detail.roomType && detail.roomType === this.filters.roomType);
        const guestNameMatch = this.filters.guestName === '' || (event.title && event.title.toLowerCase().includes(this.filters.guestName.toLowerCase()));

        let checkinoutMatch = true;
        if (this.filters.checkinoutStatus === '오늘 체크인') {
          checkinoutMatch = event.start === this.today;
        } else if (this.filters.checkinoutStatus === '오늘 체크아웃') {
          const endDate = new Date(event.end);
          endDate.setDate(endDate.getDate() - 1);
          checkinoutMatch = endDate.toISOString().split('T')[0] === this.today;
        }
        
        const reservationStatusMatch = this.filters.reservationStatus === '전체' || (detail.status && detail.status === this.filters.reservationStatus);

        return roomTypeMatch && guestNameMatch && checkinoutMatch && reservationStatusMatch;
      });
    },
    canCheckIn() {
      if (!this.detailModal.data) return false;
      return this.detailModal.data.resStatus === 'NOT_VISITED' && this.isCheckinPeriod(this.detailModal.data.checkInDate, this.detailModal.data.checkOutDate);
    },
    canCancelCheckIn() {
      if (!this.detailModal.data) return false;
      return this.detailModal.data.resStatus === 'CHECKED_IN' && !this.isCheckoutDay(this.detailModal.data.checkOutDate);
    },
    canCheckOut() {
      if (!this.detailModal.data) return false;
      return this.detailModal.data.resStatus === 'CHECKED_IN' && this.isCheckoutDay(this.detailModal.data.checkOutDate);
    },
    canCancelCheckOut() {
      if (!this.detailModal.data) return false;
      return this.detailModal.data.resStatus === 'CHECKED_OUT';
    },
    canCancelReservation() {
      if (!this.detailModal.data) return false;
      const status = this.detailModal.data.reservationStatus;
      return (status === 'COMPLETED' || this.detailModal.data.resStatus === 'NOT_VISITED') && new Date(this.detailModal.data.checkInDate) >= new Date(this.today);
    },
  },
  methods: {
    isCheckinPeriod(checkInStr, checkOutStr) {
        const today = new Date(this.today);
        today.setHours(0,0,0,0);
        const checkIn = new Date(checkInStr);
        checkIn.setHours(0,0,0,0);
        const checkOut = new Date(checkOutStr);
        checkOut.setHours(0,0,0,0);
        return today >= checkIn && today < checkOut;
    },
    isCheckoutDay(checkOutStr) {
        const today = new Date(this.today);
        today.setHours(0,0,0,0);
        const checkOut = new Date(checkOutStr);
        checkOut.setHours(0,0,0,0);
        return today.getTime() === checkOut.getTime();
    },
    async fetchAllReservations() {
      try {
        const token = localStorage.getItem('token');
        if (!token) throw new Error("Token not found");

        const { data } = await this.$axios.get('/owner/reservations', {
          headers: { 'Authorization': `Bearer ${token}` }
        });

        this.allEvents = data
          .filter(res => res.start && res.end)
          .map(res => {
            return {
              id: res.id,
              title: res.title,
              start: res.start,
              end: res.end,
              allDay: true,
              extendedProps: {
                  ...res.extendedProps,
                  status: res.status 
              },
              backgroundColor: res.color,
              borderColor: res.color
            };
        });

        this.applyFilters();
      } catch (error) {
        console.error("예약 로딩 실패:", error);
        alert("예약 정보를 불러오는 데 실패했습니다.");
      }
    },
    applyFilters() {
      this.calendarOptions.events = this.filteredEvents;
    },
    closeListModal() {
      this.listModal.visible = false;
    },
    closeDetailModal() {
      this.detailModal.visible = false;
      this.detailModal.data = null;
    },
    async openDetailModal(id) {
      if (this.listModal.visible) this.listModal.visible = false;
      this.detailModal.id = id;
      this.detailModal.visible = true;
      this.detailModal.loading = true;
      try {
        const token = localStorage.getItem('token');
        const { data } = await this.$axios.get(`/owner/reservations/${id}`, { headers: { 'Authorization': `Bearer ${token}` } });
        this.detailModal.data = data;
      } catch (e) {
        alert('상세 정보를 불러오지 못했습니다.');
        this.closeDetailModal();
      } finally {
        this.detailModal.loading = false;
      }
    },
    resetFilters() {
      this.filters.roomType = '';
      this.filters.guestName = '';
      this.filters.checkinoutStatus = '전체';
      this.filters.reservationStatus = 'COMPLETED';
    },
    async updateResStatus(action) {
        let confirmMessage = '정말로 이 작업을 실행하시겠습니까?';
        if (action === 'cancel-checkin') confirmMessage = '체크인을 취소하고 미방문 상태로 변경하시겠습니까?';
        if (action === 'cancel-checkout') confirmMessage = '체크아웃을 취소하고 체크인 상태로 변경하시겠습니까?';

        if (!confirm(confirmMessage)) return;

        try {
            const token = localStorage.getItem('token');
            await this.$axios.put(`/owner/reservations/${this.detailModal.id}/${action}`, null, { headers: { 'Authorization': `Bearer ${token}` } });
            
            alert('상태가 성공적으로 변경되었습니다.');

        } catch(e) {
            alert(e.response?.data?.message || '상태 변경에 실패했습니다.');
        } finally {
            await this.fetchAllReservations(); 
            
            if (this.detailModal.visible && this.detailModal.id) {
                await this.openDetailModal(this.detailModal.id);
            }
        }
    },
  },
  watch: {
    filters: {
      handler() {
        this.applyFilters();
      },
      deep: true,
    },
  },
  mounted() {
    this.fetchAllReservations();
  },
  setup() {
    const router = useRouter();
    return { router };
  }
};
</script>

<style scoped>
.page-container { padding: 40px; background-color: #f8f9fa; height: 100%; }
.page-header { margin-bottom: 24px; }
.page-title { font-size: 28px; font-weight: 800; color: #212529; }
.filter-bar { display: flex; gap: 12px; margin-bottom: 24px; align-items: center; flex-wrap: wrap; }
.filter-bar input, .filter-bar select { padding: 10px 14px; border-radius: 8px; border: 1px solid #d1d5db; font-size: 14px; }
.calendar-card { background: #fff; border-radius: 12px; padding: 32px; box-shadow: 0 4px 12px rgba(0, 0, 0, 0.05); }
:deep(.fc-button-primary) { background-color: #4f46e5 !important; border-color: #4f46e5 !important; }
:deep(.fc-daygrid-day.fc-day-today) { background-color: #eef2ff; }
:deep(.fc-event) { cursor: pointer; }

/* 모달 공통 */
.modal-overlay { position: fixed; top: 0; left: 0; right: 0; bottom: 0; background-color: rgba(0,0,0,0.6); display: flex; justify-content: center; align-items: center; z-index: 1000; }
.modal-content { background: white; border-radius: 12px; width: 90%; box-shadow: 0 10px 25px rgba(0,0,0,0.1); }
.modal-header { display: flex; justify-content: space-between; align-items: center; padding: 20px 24px; border-bottom: 1px solid #e5e7eb; }
.modal-header h3 { margin: 0; font-size: 20px; }
.close-button { background: none; border: none; font-size: 28px; cursor: pointer; color: #9ca3af; }

/* 리스트 모달 */
.list-modal { max-width: 400px; }
.reservation-list { list-style: none; padding: 16px; margin: 0; max-height: 60vh; overflow-y: auto; }
.reservation-list li { cursor: pointer; padding: 12px 16px; border-radius: 6px; display: flex; align-items: center; gap: 12px; font-weight: 500; }
.reservation-list li:hover { background-color: #f3f4f6; }
.list-item-color { width: 10px; height: 10px; border-radius: 50%; }

/* 상세 모달 */
.detail-modal { max-width: 600px; }
.detail-grid { padding: 24px; display: grid; grid-template-columns: 1fr 1fr; gap: 20px; }
.detail-grid > div { display: flex; flex-direction: column; }
.detail-grid label { font-size: 13px; color: #6b7280; margin-bottom: 4px; }
.detail-grid span { font-weight: 500; }
.detail-grid .full-width { grid-column: 1 / -1; }
.modal-actions { display: flex; justify-content: flex-end; gap: 12px; padding: 20px 24px; border-top: 1px solid #e5e7eb; background-color: #f9fafb; border-bottom-left-radius: 12px; border-bottom-right-radius: 12px;}
.btn-primary, .btn-secondary, .btn-danger, .btn-success { padding: 10px 20px; border: none; border-radius: 8px; font-weight: 600; cursor: pointer; }
.btn-primary { background-color: #4f46e5; color: white; }
.btn-secondary { background-color: #e5e7eb; color: #374151; }
.btn-danger { background-color: #ef4444; color: white; }
.btn-success { background-color: #22c55e; color: white; }
span.completed { color: #2563eb; font-weight: bold; }
span.cancelled { color: #9ca3af; font-weight: bold; }
span.reserved { color: #f59e0b; font-weight: bold; }
span.checked_in { color: #22c55e; font-weight: bold; }
span.checked_out { color: #1e40af; font-weight: bold; }
</style>