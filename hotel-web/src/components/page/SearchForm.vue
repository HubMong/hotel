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

        <!-- 검색 버튼 (grid 안에 포함) -->
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
import { Korean } from "flatpickr/dist/l10n/ko.js"; // 한국어 locale

export default {
  name: "SearchForm",
  components: { FlatPickr },
  data() {
    return {
      destination: "",
      dateRange: [], // [체크인, 체크아웃]
      adults: 1,
      children: 0,
      showTravelerMenu: false,
      dateRangeConfig: {
        mode: "range",
        showMonths: 2, // 두 개의 달을 나란히 표시
        altInput: true,
        altFormat: "m월 j일",
        dateFormat: "Y-m-d",
        minDate: "today",
        maxDate: new Date(2026, 11, 31), // 2026년 12월 31일까지 선택 가능
        locale: Korean, //  한국어 달력 적용
        static: true,
        disableMobile: true, // 모바일에서도 데스크톱 스타일 사용
        clickOpens: true,
        allowInput: false,
        formatDate: (date, format, locale) => {
          // 범위 선택이 완료되었을 때 사용자 정의 형식으로 표시
          if (this.dateRange && this.dateRange.length === 2) {
            const checkIn = this.dateRange[0];
            const checkOut = this.dateRange[1];
            const checkInMonth = checkIn.getMonth() + 1;
            const checkInDate = checkIn.getDate();
            const checkOutMonth = checkOut.getMonth() + 1;
            const checkOutDate = checkOut.getDate();
            const timeDiff = checkOut.getTime() - checkIn.getTime();
            const nights = Math.ceil(timeDiff / (1000 * 3600 * 24));
            return `${checkInMonth}월 ${checkInDate}일 - ${checkOutMonth}월 ${checkOutDate}일 (${nights}박)`;
          }
          // 기본 형식
          const month = date.getMonth() + 1;
          const day = date.getDate();
          return `${month}월 ${day}일`;
        },
        onReady: (selectedDates, dateStr, instance) => {
          // 캘린더 컨테이너를 입력 필드 컨테이너에 부착
          const host = this.$refs.datePickerField;
          if (host && instance && instance.calendarContainer && instance.calendarContainer.parentElement !== host) {
            host.appendChild(instance.calendarContainer);
          }
          
          // 헤더 업데이트 및 일요일 스타일 적용
          this.$nextTick(() => {
            this.updateCalendarHeaders(instance);
            // 약간의 지연 후 스타일 적용
            setTimeout(() => {
              this.applySundayStyles(instance);
            }, 100);
          });
        },
        onChange: (selectedDates, dateStr, instance) => {
          // 날짜가 선택될 때마다 호출되는 콜백
          this.updateDateDisplay(selectedDates, instance);
        },
        onMonthChange: (selectedDates, dateStr, instance) => {
          // 달이 변경될 때마다 헤더 업데이트
          this.$nextTick(() => {
            this.updateCalendarHeaders(instance);
            // 약간의 지연 후 스타일 적용
            setTimeout(() => {
              this.applySundayStyles(instance);
            }, 100);
          });
        },
        onOpen: (selectedDates, dateStr, instance) => {
          // 캘린더가 열릴 때마다 현재 달로 리셋
          const today = new Date();
          instance.changeMonth(today.getMonth(), false);
          
          this.$nextTick(() => {
            this.updateCalendarHeaders(instance);
            // 약간의 지연 후 스타일 적용
            setTimeout(() => {
              this.applySundayStyles(instance);
            }, 100);
          });
        },
        onClose: (selectedDates, dateStr, instance) => {
          // 캘린더가 닫힐 때 현재 달로 리셋 (다음에 열 때를 위해)
          const today = new Date();
          instance.changeMonth(today.getMonth(), false);
        },
        onDayCreate: (dObj, dStr, fp, dayElem) => {
          // 날짜 객체에서 요일 정보 추출
          const date = dayElem.dateObj;
          
          // 일요일인지 확인 (0 = 일요일)
          const isSunday = date.getDay() === 0;
          
          // 일요일이면 빨간색으로 표시
          if (isSunday) {
            dayElem.style.color = '#ff4757';
            dayElem.classList.add('sunday-date');
          }
        }
      }
    };
  },
  computed: {
    travelerLabel() {
      let parts = [];
      if (this.adults > 0) parts.push(`성인 ${this.adults}명`);
      if (this.children > 0) parts.push(`어린이 ${this.children}명`);
      return parts.length > 0 ? parts.join(", ") : "여행자 선택";
    },
    checkIn() {
      return this.dateRange && this.dateRange.length > 0 ? this.dateRange[0] : "";
    },
    checkOut() {
      return this.dateRange && this.dateRange.length > 1 ? this.dateRange[1] : "";
    },
    // 박수 계산
    nights() {
      if (this.dateRange && this.dateRange.length === 2) {
        const checkIn = new Date(this.dateRange[0]);
        const checkOut = new Date(this.dateRange[1]);
        const timeDiff = checkOut.getTime() - checkIn.getTime();
        return Math.ceil(timeDiff / (1000 * 3600 * 24));
      }
      return 0;
    }
  },
  methods: {
    toggleTravelerMenu() {
      this.showTravelerMenu = !this.showTravelerMenu;
    },
    closeTravelerMenu() {
      this.showTravelerMenu = false;
    },
    increase(type) {
      this[type]++;
    },
    decrease(type) {
      if (this[type] > 0) this[type]--;
    },
    goSearch() {
      if (!this.destination) {
        alert("목적지를 입력해주세요.");
        return;
      }

      this.$router.push({
        path: "/search",
        query: {
          destination: this.destination,
          checkIn: this.checkIn || "",   // 선택 안 하면 빈 값
          checkOut: this.checkOut || "",
          adults: this.adults,
          children: this.children
        }
      });
    },
    updateDateDisplay(selectedDates, instance) {
      if (selectedDates.length === 2) {
        const checkIn = selectedDates[0];
        const checkOut = selectedDates[1];
        
        // 날짜 포맷팅
        const checkInMonth = checkIn.getMonth() + 1;
        const checkInDate = checkIn.getDate();
        const checkOutMonth = checkOut.getMonth() + 1;
        const checkOutDate = checkOut.getDate();
        
        // 박수 계산
        const timeDiff = checkOut.getTime() - checkIn.getTime();
        const nights = Math.ceil(timeDiff / (1000 * 3600 * 24));
        
        // 사용자 정의 표시 형식
        const displayText = `${checkInMonth}월 ${checkInDate}일 - ${checkOutMonth}월 ${checkOutDate}일 (${nights}박)`;
        
        // flatpickr 인스턴스의 altInput 직접 업데이트
        if (instance && instance.altInput) {
          instance.altInput.value = displayText;
        }
      }
    },
    updateCalendarHeaders(instance) {
      // 연도 관련 요소들 숨기기
      const yearInputs = instance.calendarContainer.querySelectorAll('.numInputWrapper, .numInput, .arrowUp, .arrowDown');
      yearInputs.forEach(el => {
        el.style.display = 'none';
      });
      
      // 월 헤더 업데이트
      const monthHeaders = instance.calendarContainer.querySelectorAll('.flatpickr-current-month');
      monthHeaders.forEach((header, index) => {
        // 기존 내용 모두 제거
        header.innerHTML = '';
        
        // 현재 표시되는 월 계산 (연도가 바뀌는 경우도 고려)
        let displayYear, displayMonth;
        if (instance.currentMonth !== undefined && instance.currentYear !== undefined) {
          const baseMonth = instance.currentMonth + index;
          displayYear = instance.currentYear;
          displayMonth = baseMonth;
          
          // 12월을 넘어가면 다음 년도로
          if (baseMonth > 11) {
            displayYear = instance.currentYear + Math.floor(baseMonth / 12);
            displayMonth = baseMonth % 12;
          }
        } else {
          const currentDate = new Date();
          const baseMonth = currentDate.getMonth() + index;
          displayYear = currentDate.getFullYear();
          displayMonth = baseMonth;
          
          // 12월을 넘어가면 다음 년도로
          if (baseMonth > 11) {
            displayYear = currentDate.getFullYear() + Math.floor(baseMonth / 12);
            displayMonth = baseMonth % 12;
          }
        }
        
        const monthNames = ['1월', '2월', '3월', '4월', '5월', '6월', '7월', '8월', '9월', '10월', '11월', '12월'];
        
        // "2025년 9월" 또는 "2026년 1월" 형태로 텍스트 생성
        const fullText = `${displayYear}년 ${monthNames[displayMonth]}`;
        
        // 텍스트 요소 생성
        const textSpan = document.createElement('span');
        textSpan.textContent = fullText;
        textSpan.style.cssText = 'font-size: 16px; font-weight: 700; color: #333; pointer-events: none; user-select: none;';
        header.appendChild(textSpan);
      });
    },
    applySundayStyles(instance) {
      // 모든 날짜 요소에서 일요일 찾기 - 여러 방법으로 시도
      const dayElements = instance.calendarContainer.querySelectorAll('.flatpickr-day');
      
      dayElements.forEach((dayElem, index) => {
        // 방법 1: dateObj 사용
        if (dayElem.dateObj) {
          const isSunday = dayElem.dateObj.getDay() === 0;
          if (isSunday) {
            dayElem.style.setProperty('color', '#ff4757', 'important');
            dayElem.classList.add('sunday-date');
          }
        }
        
        // 방법 2: aria-label 사용 (fallback)
        if (!dayElem.dateObj && dayElem.getAttribute('aria-label')) {
          const ariaLabel = dayElem.getAttribute('aria-label');
          const dateMatch = ariaLabel.match(/(\d{4})-(\d{2})-(\d{2})/);
          if (dateMatch) {
            const [, year, month, day] = dateMatch;
            const date = new Date(parseInt(year), parseInt(month) - 1, parseInt(day));
            const isSunday = date.getDay() === 0;
            if (isSunday) {
              dayElem.style.setProperty('color', '#ff4757', 'important');
              dayElem.classList.add('sunday-date');
            }
          }
        }
        
        // 방법 3: 텍스트 내용과 위치 기반으로 계산
        if (!dayElem.dateObj && !dayElem.getAttribute('aria-label')) {
          const dayNumber = parseInt(dayElem.textContent);
          if (dayNumber && !isNaN(dayNumber)) {
            // 현재 월의 첫 번째 날의 요일을 계산
            const currentMonth = instance.currentMonth;
            const currentYear = instance.currentYear;
            const firstDayOfMonth = new Date(currentYear, currentMonth, 1);
            const firstDayWeekday = firstDayOfMonth.getDay();
            
            // 해당 날짜의 요일 계산
            const dayWeekday = (firstDayWeekday + dayNumber - 1) % 7;
            if (dayWeekday === 0) { // 일요일
              dayElem.style.setProperty('color', '#ff4757', 'important');
              dayElem.classList.add('sunday-date');
            }
          }
        }
      });
      
      // 추가로 CSS로도 적용
      const style = document.createElement('style');
      style.textContent = `
        .flatpickr-calendar .flatpickr-day:nth-child(7n+1) {
          color: #ff4757 !important;
        }
      `;
      if (!document.querySelector('#sunday-style')) {
        style.id = 'sunday-style';
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
</style>
