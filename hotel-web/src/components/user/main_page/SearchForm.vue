<template>
  <div class="search-form-wrapper">
    <div class="search-form-card">
      <h2 class="search-form-title">어디로 갈까요 ?</h2>

      <div class="search-form-grid">
        <!-- 목적지 -->
        <div class="search-form-item destination">
          <label class="search-form-label">목적지</label>
          <div class="input-field">
            <input
              type="text"
              v-model="destination"
              class="input-text"
              placeholder="목적지 또는 숙소명"
            />
          </div>
        </div>

        <!-- 체크인 + 체크아웃 (범위 선택) -->
        <div class="search-form-item date-range">
          <label class="search-form-label">체크인 / 체크아웃</label>
          <div class="input-field" ref="datePickerField">
            <flat-pickr
              v-model="dateRange"
              :config="dateRangeConfig"
              placeholder="체크인 ~ 체크아웃 날짜 선택"
              class="input-text"
            />
          </div>
        </div>

        <!-- 여행자 수 -->
        <div class="search-form-item">
          <label class="search-form-label">여행자 수</label>
          <div class="input-field traveler-dropdown">
            <button type="button" class="traveler-button" @click="toggleTravelerMenu">
              {{ travelerLabel }}
            </button>

            <div v-if="showTravelerMenu" class="traveler-menu">
              <div class="traveler-item">
                <span>성인</span>
                <div class="counter">
                  <button @click="decrease('adults')" :disabled="adults <= 1">-</button>
                  <span>{{ adults }}</span>
                  <button @click="increase('adults')">+</button>
                </div>
              </div>
              <div class="traveler-item">
                <span>어린이</span>
                <div class="counter">
                  <button @click="decrease('children')" :disabled="children <= 0">-</button>
                  <span>{{ children }}</span>
                  <button @click="increase('children')">+</button>
                </div>
              </div>
              <!-- 확인 버튼 -->
              <div class="traveler-actions">
                <button class="confirm-btn" @click="closeTravelerMenu">확인</button>
              </div>
            </div>
          </div>
        </div>

        <!-- 검색 버튼 -->
        <div class="search-form-item search-button-container">
          <label class="search-form-label">&nbsp;</label>
          <button class="search-button" @click="goSearch">검색</button>
        </div>
      </div>
    </div>
  </div>
</template>

<style src="@/assets/css/homepage/searchForm.css"></style>

<script>
import FlatPickr from "vue-flatpickr-component";
import "flatpickr/dist/flatpickr.css";
import { Korean } from "flatpickr/dist/l10n/ko.js";

export default {
  name: "SearchForm",
  components: { FlatPickr },
  data() {
    return {
      destination: "",
      dateRange: [], // [Date, Date]
      adults: 1,
      children: 0,
      showTravelerMenu: false,
      dateRangeConfig: {
        mode: "range",
        showMonths: 2,
        altInput: true,
        altFormat: "m월 j일",
        dateFormat: "Y-m-d",
        minDate: "today",
        maxDate: new Date(2026, 11, 31),
        locale: Korean,
        static: true,
        disableMobile: true,
        clickOpens: true,
        allowInput: false,
        formatDate: (date, format, locale) => {
          // 범위 선택이 완료되었을 때 사용자 정의 표시
          if (this.dateRange && this.dateRange.length === 2) {
            const [checkIn, checkOut] = this.dateRange;
            const nights = Math.ceil((checkOut - checkIn) / (1000 * 3600 * 24));
            return `${checkIn.getMonth()+1}월 ${checkIn.getDate()}일 - ${checkOut.getMonth()+1}월 ${checkOut.getDate()}일 (${nights}박)`;
          }
          return `${date.getMonth()+1}월 ${date.getDate()}일`;
        },
        onReady: (selectedDates, dateStr, instance) => {
          const host = this.$refs.datePickerField;
          if (host && instance?.calendarContainer && instance.calendarContainer.parentElement !== host) {
            host.appendChild(instance.calendarContainer);
          }
          this.$nextTick(() => {
            this.updateCalendarHeaders(instance);
            setTimeout(() => { this.applySundayStyles(instance); }, 100);
          });
        },
        onChange: (selectedDates, dateStr, instance) => {
          this.updateDateDisplay(selectedDates, instance);
        },
        onMonthChange: (selectedDates, dateStr, instance) => {
          this.$nextTick(() => {
            this.updateCalendarHeaders(instance);
            setTimeout(() => { this.applySundayStyles(instance); }, 100);
          });
        },
        onOpen: (selectedDates, dateStr, instance) => {
          const today = new Date();
          instance.changeMonth(today.getMonth(), false);
          this.$nextTick(() => {
            this.updateCalendarHeaders(instance);
            setTimeout(() => { this.applySundayStyles(instance); }, 100);
          });
        },
        onClose: (selectedDates, dateStr, instance) => {
          const today = new Date();
          instance.changeMonth(today.getMonth(), false);
        },
        onDayCreate: (dObj, dStr, fp, dayElem) => {
          const date = dayElem.dateObj;
          if (date.getDay() === 0) {
            dayElem.style.color = '#ff4757';
            dayElem.classList.add('sunday-date');
          }
        }
      }
    };
  },
  computed: {
    travelerLabel() {
      const parts = [];
      if (this.adults > 0) parts.push(`성인 ${this.adults}명`);
      if (this.children > 0) parts.push(`어린이 ${this.children}명`);
      return parts.length ? parts.join(", ") : "여행자 선택";
    },
    checkIn()  { return this.dateRange?.[0] || null; },  // Date or null
    checkOut() { return this.dateRange?.[1] || null; },  // Date or null
    nights() {
      if (this.dateRange?.length === 2) {
        const [ci, co] = this.dateRange;
        return Math.ceil((co - ci) / (1000 * 3600 * 24));
      }
      return 0;
    }
  },
  methods: {
    toggleTravelerMenu() { this.showTravelerMenu = !this.showTravelerMenu; },
    closeTravelerMenu()  { this.showTravelerMenu = false; },
    increase(type) { this[type]++; },
    decrease(type) { if (this[type] > 0) this[type]--; },

    goSearch() {
      if (!this.destination) {
        alert("목적지를 입력해주세요.");
        return;
      }

      // YYYY-MM-DD
      const toYmd = (d) => {
        if (!d) return undefined;
        const dt = d instanceof Date ? d : new Date(d);
        const m = String(dt.getMonth() + 1).padStart(2, '0');
        const day = String(dt.getDate()).padStart(2, '0');
        return `${dt.getFullYear()}-${m}-${day}`;
      };

      // 원본 값
      let ci = this.checkIn ? new Date(this.checkIn) : null;
      let co = this.checkOut ? new Date(this.checkOut) : null;

      // ✅ 최소 1박 보정: co가 없거나 co <= ci면 다음날로 고정
      if (ci && (!co || co <= ci)) {
        const next = new Date(ci);
        next.setDate(next.getDate() + 1);
        co = next;
      }

      const checkInStr  = ci ? toYmd(ci) : undefined;
      const checkOutStr = co ? toYmd(co) : undefined;

      this.$router.push({
        name: 'Search',
        query: {
          q: this.destination || undefined,
          checkIn: checkInStr,
          checkOut: checkOutStr,
          adults: this.adults || undefined,
          children: this.children || undefined
        }
      });
    },

    updateDateDisplay(selectedDates, instance) {
      if (selectedDates.length === 2 && instance?.altInput) {
        const [ci, co] = selectedDates;
        const nights = Math.ceil((co - ci) / (1000 * 3600 * 24));
        instance.altInput.value = `${ci.getMonth()+1}월 ${ci.getDate()}일 - ${co.getMonth()+1}월 ${co.getDate()}일 (${nights}박)`;
      }
    },

    updateCalendarHeaders(instance) {
      const yearInputs = instance.calendarContainer.querySelectorAll('.numInputWrapper, .numInput, .arrowUp, .arrowDown');
      yearInputs.forEach(el => { el.style.display = 'none'; });

      const monthHeaders = instance.calendarContainer.querySelectorAll('.flatpickr-current-month');
      monthHeaders.forEach((header, index) => {
        header.innerHTML = '';

        const now = new Date();
        const baseMonth = (instance.currentMonth ?? now.getMonth()) + index;
        const baseYear  = (instance.currentYear  ?? now.getFullYear());
        const displayYear  = baseYear + Math.floor(baseMonth / 12);
        const displayMonth = baseMonth % 12;

        const monthNames = ['1월','2월','3월','4월','5월','6월','7월','8월','9월','10월','11월','12월'];
        const textSpan = document.createElement('span');
        textSpan.textContent = `${displayYear}년 ${monthNames[displayMonth]}`;
        textSpan.style.cssText = 'font-size:16px;font-weight:700;color:#333;pointer-events:none;user-select:none;';
        header.appendChild(textSpan);
      });
    },

    applySundayStyles(instance) {
      const dayElements = instance.calendarContainer.querySelectorAll('.flatpickr-day');
      dayElements.forEach(dayElem => {
        const d = dayElem.dateObj;
        if (d && d.getDay() === 0) {
          dayElem.style.setProperty('color', '#ff4757', 'important');
          dayElem.classList.add('sunday-date');
        }
      });
      if (!document.querySelector('#sunday-style')) {
        const style = document.createElement('style');
        style.id = 'sunday-style';
        style.textContent = `.flatpickr-calendar .flatpickr-day:nth-child(7n+1){color:#ff4757!important;}`;
        document.head.appendChild(style);
      }
    }
  }
};
</script>

<style scoped>
/* 입력 필드를 기준으로 팝업을 고정 */
.input-field { position: relative; }
::v-deep(.flatpickr-calendar) {
  position: absolute;
  top: 100%;
  left: 0;
  z-index: 9999;
}

/* 입력/버튼이 상위섹션에 눌리지 않도록 */
.search-form-card, .search-form-grid, .input-field, .input-text, .traveler-button {
  position: relative;
  z-index: 1;
}
</style>
