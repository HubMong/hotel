<!-- src/components/user/main_page/SearchForm.vue -->
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
            <FlatPickr
              ref="rangePicker"                
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
      dateRange: [], // [Date, Date] 또는 문자열이 섞일 수 있음 (그래서 fp.selectedDates 신뢰)
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

        onReady: (selected, _dateStr, instance) => {
          console.log('[SearchForm FP onReady] selected =', selected);
          const host = this.$refs.datePickerField;
          if (host && instance?.calendarContainer && instance.calendarContainer.parentElement !== host) {
            host.appendChild(instance.calendarContainer);
          }
          this.$nextTick(() => {
            this.updateCalendarHeaders(instance);
            setTimeout(() => { this.applySundayStyles(instance); }, 60);
          });

          const cal = instance.calendarContainer;
          if (cal) {
            cal.addEventListener('wheel', (e) => {
              if (e.target.closest('.cur-year') || e.target.closest('.numInputWrapper')) {
                e.preventDefault(); e.stopPropagation();
              }
            }, { passive:false, capture:true });
            cal.addEventListener('keydown', (e) => {
              if (e.target.closest('.cur-year') || e.target.closest('.numInputWrapper')) {
                e.preventDefault(); e.stopPropagation();
              }
            }, true);
          }
        },

        onOpen: (_sel, _dateStr, instance) => {
          const ci = this.checkIn;
          instance.jumpToDate(ci || new Date());
          this.$nextTick(() => {
            this.updateCalendarHeaders(instance);
            setTimeout(() => { this.applySundayStyles(instance); }, 60);
          });
        },

        onMonthChange: (_sel, _dateStr, instance) => {
          this.$nextTick(() => {
            this.updateCalendarHeaders(instance);
            setTimeout(() => { this.applySundayStyles(instance); }, 60);
          });
        },

        onClose: (_selected, _dateStr, _instance) => {},

        onChange: (selectedDates) => {
          console.log('[SearchForm FP onChange] selected =', selectedDates);
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
    checkIn()  { return this.dateRange?.[0] || null; },
    checkOut() { return this.dateRange?.[1] || null; },
    nights() {
      if (this.dateRange?.length === 2) {
        const [ci, co] = this.dateRange;
        return Math.ceil((co - ci) / (1000 * 3600 * 24));
      }
      return 0;
    }
  },
  methods: {
    // -------- 유틸
    toYmd(d) {
      if (!(d instanceof Date) || Number.isNaN(d.getTime())) return undefined;
      const y = d.getFullYear();
      const m = String(d.getMonth() + 1).padStart(2, '0');
      const day = String(d.getDate()).padStart(2, '0');
      return `${y}-${m}-${day}`;
    },

    // -------- 인원
    toggleTravelerMenu() { this.showTravelerMenu = !this.showTravelerMenu; },
    closeTravelerMenu()  { this.showTravelerMenu = false; },
    increase(type) { this[type]++; },
    decrease(type) { if (this[type] > 0) this[type]--; },

    // -------- 검색 (fp.selectedDates를 신뢰)
    goSearch() {
      if (!this.destination) {
        alert("목적지를 입력해주세요.");
        return;
      }

      // ✅ flatpickr 인스턴스에서 실제 선택값(Date[])을 읽음
      const fp = this.$refs.rangePicker?._flatpickr || this.$refs.rangePicker?.fp;
      const sel = fp?.selectedDates && fp.selectedDates.length ? fp.selectedDates : this.dateRange;

      let ci = sel?.[0] ? new Date(sel[0]) : null;
      let co = sel?.[1] ? new Date(sel[1]) : null;

      // 최소 1박 보정
      if (ci && (!co || co <= ci)) {
        const next = new Date(ci);
        next.setDate(next.getDate() + 1);
        co = next;
      }

      const query = {
        q: this.destination || undefined,
        checkIn: this.toYmd(ci),
        checkOut: this.toYmd(co),
        adults: this.adults || undefined,
        children: this.children || undefined
      };

      console.log('[SearchForm emit] v-model dateRange =', this.dateRange, 'fp.selectedDates =', (sel || []).map(this.toYmd));
      console.log('[SearchForm emit] pushing query =', query);

      this.$router.push({ path: '/search', query });
    },

    // -------- 달력 헤더/표시
    updateDateDisplay(selectedDates, instance) {
      if (selectedDates.length === 2 && instance?.altInput) {
        const [ci, co] = selectedDates;
        const nights = Math.ceil((co - ci) / (1000 * 3600 * 24));
        instance.altInput.value =
          `${ci.getMonth()+1}월 ${ci.getDate()}일 - ${co.getMonth()+1}월 ${co.getDate()}일 (${nights}박)`;
      }
    },

    updateCalendarHeaders(instance) {
      const yearInputs = instance.calendarContainer
        .querySelectorAll('.numInputWrapper, .numInput, .arrowUp, .arrowDown');
      yearInputs.forEach(el => { el.style.display = 'none'; });

      const monthHeaders = instance.calendarContainer.querySelectorAll('.flatpickr-current-month');
      monthHeaders.forEach((header, index) => {
        header.innerHTML = '';

        const now = new Date();
        const baseMonth = (instance.currentMonth ?? now.getMonth()) + index;
        const baseYear  = (instance.currentYear  ?? now.getFullYear());
        const displayYear  = baseYear + Math.floor(baseMonth / 12);
        const displayMonth = ((baseMonth % 12) + 12) % 12;

        const monthNames = ['1월','2월','3월','4월','5월','6월','7월','8월','9월','10월','11월','12월'];
        const textSpan = document.createElement('span');
        textSpan.textContent = `${displayYear}년 ${monthNames[displayMonth]}`;
        textSpan.style.cssText =
          'font-size:16px;font-weight:700;color:#333;pointer-events:none;user-select:none;';
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
/* 팝업을 입력 필드에 고정 */
.input-field { position: relative; }
::v-deep(.flatpickr-calendar) {
  position: absolute;
  top: 100%;
  left: 0;
  z-index: 9999;
}

/* 연도 인풋 숨김 (휠/키 이슈 차단) */
:deep(.flatpickr-current-month .numInputWrapper){ display:none !important; }

/* 겹침 방지 */
.search-form-card, .search-form-grid, .input-field, .input-text, .traveler-button {
  position: relative;
  z-index: 1;
}
</style>
