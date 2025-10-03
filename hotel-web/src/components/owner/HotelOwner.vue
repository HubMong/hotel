<template>
  <div class="owner-page">
    <aside class="sidebar">
      <div class="logo">🏨 Owner</div>
      <nav>
        <ul>
          <li :class="{ active: activeMenu === 'dashboard' }" @click="activeMenu = 'dashboard'">대시보드</li>
          <li :class="{ active: activeMenu === 'hotels' }" @click="activeMenu = 'hotels'">호텔/객실 관리</li>
          <li :class="{ active: activeMenu === 'reservations' }" @click="activeMenu = 'reservations'">예약 관리</li>
          <li :class="{ active: activeMenu === 'reviews' }" @click="activeMenu = 'reviews'">리뷰 관리</li>
        </ul>
      </nav>

      <div class="sidebar-footer">
        <button class="btn-homepage" @click="$router.push('/')">홈페이지</button>
        <button class="btn-logout-sidebar" @click="logoutAndGoHome">로그아웃</button>
      </div>
    </aside>

    <main class="main-content">
      
      <section v-if="activeMenu === 'dashboard'">
        <div class="header-actions">
          <h2>대시보드</h2>
          <div class="user-actions">
            <span v-if="user" class="user-name">{{ user.name }}님</span>
            <button class="logout-btn" @click="logoutAndGoHome">로그아웃</button>
          </div>
        </div>

        <div class="dashboard-grid">
          <div class="stat-card">
            <h4>오늘 매출</h4>
            <p>₩ {{ formatNumber(dashboardSummary.todaySales) }}</p>
            <span :class="['comparison', getComparisonClass(dashboardSummary.salesChangeVsYesterday)]">
              {{ getComparisonText(dashboardSummary.salesChangeVsYesterday) }} vs 어제
            </span>
          </div>
          <div class="stat-card">
            <h4>이번 주 매출</h4>
            <p>₩ {{ formatNumber(dashboardSummary.thisWeekSales) }}</p>
            <span :class="['comparison', getComparisonClass(dashboardSummary.salesChangeVsLastWeek)]">
              {{ getComparisonText(dashboardSummary.salesChangeVsLastWeek) }} vs 지난주
            </span>
          </div>
          <div class="stat-card">
            <h4>이번 달 매출</h4>
            <p>₩ {{ formatNumber(dashboardSummary.thisMonthSales) }}</p>
            <span :class="['comparison', getComparisonClass(dashboardSummary.salesChangeVsLastMonth)]">
              {{ getComparisonText(dashboardSummary.salesChangeVsLastMonth) }} vs 지난달
            </span>
          </div>
        </div>

        <div class="chart-container">
          <div class="chart-header">
            <div class="chart-title-group">
              <h3>매출 분석</h3>
              <div class="chart-main-filters">
                <select v-model="chartFilters.hotelId" class="filter-select small">
                  <option :value="null">모든 호텔</option>
                  <option v-for="hotel in myHotels" :key="hotel.id" :value="hotel.id">{{ hotel.name }}</option>
                </select>

                <select v-model="chartFilters.roomType" class="filter-select small">
                  <option :value="null">모든 객실</option>
                  <option v-for="roomType in allRoomTypes" :key="roomType" :value="roomType">{{ roomType }}</option>
                </select>
                
                <flat-pickr
                  :config="chartDateConfig"
                  placeholder="날짜 범위 선택"
                  class="date-picker-placeholder small"
                  :value="chartFilters.dateRange" 
                />
              </div>
            </div>
            
            <div class="chart-period-filters">
              <button class="filter-btn" :class="{ active: activePeriod === '7days' }" @click="setPeriod('7days')">최근 7일</button>
              <button class="filter-btn" :class="{ active: activePeriod === '30days' }" @click="setPeriod('30days')">최근 30일</button>
              <button class="filter-btn" :class="{ active: activePeriod === '1year' }" @click="setPeriod('1year')">최근 1년</button>
              <button class="filter-btn reset-btn" @click="clearChartFilters">초기화</button>
            </div>
          </div>
          
          <div class="chart-placeholder">
            <SalesChart v-if="chartData.length > 0" :sales-data="chartData" />
            <p v-else>해당 기간에 표시할 데이터가 없습니다.</p>
          </div>
        </div>

        <div class="dashboard-grid secondary">
          <div class="info-card">
            <h4>오늘의 현황</h4>
            <div class="check-in-out-tabs">
              <button :class="{ active: activeTab === 'check-in' }" @click="activeTab = 'check-in'">체크인 ({{ todaysCheckIns.length }})</button>
              <button :class="{ active: activeTab === 'check-out' }" @click="activeTab = 'check-out'">체크아웃 ({{ todaysCheckOuts.length }})</button>
            </div>

            <ul v-if="activeTab === 'check-in'" class="guest-list">
              <li v-for="reservation in todaysCheckIns" :key="reservation.id" @click="showReservationDetails(reservation)">
                <span>{{ reservation.guestName }}</span>
                <span class="room-type">{{ reservation.roomType }}</span>
              </li>
            </ul>
            <ul v-if="activeTab === 'check-out'" class="guest-list">
              <li v-for="reservation in todaysCheckOuts" :key="reservation.id" @click="showReservationDetails(reservation)">
                <span>{{ reservation.guestName }}</span>
                <span class="room-type">{{ reservation.roomType }}</span>
              </li>
            </ul>
          </div>

          <div class="info-card">
            <h4>최근 예약</h4>
            <ul class="activity-list">
              <li v-for="reservation in recentReservations" :key="reservation.id" @click="showReservationDetails(reservation)">
                <p><strong>{{ reservation.guestName }}</strong>님이 <strong>{{ reservation.roomType }}</strong> 예약을 완료했습니다.</p>
                <span class="time-ago">{{ formatTimeAgo(reservation.createdAt) }}</span>
              </li>
            </ul>
          </div>

          <div class="info-card">
            <h4>최근 리뷰</h4>
            <ul class="activity-list">
              <li v-for="review in recentReviews" :key="review.id">
                <p><strong>{{ review.name }}</strong> 님이 <strong>"{{ review.comment }}"</strong> 리뷰를 남겼습니다.</p>
                <div class="star-rating small">
                  <span v-for="n in 5" :key="n" :class="{ 'filled': n <= review.rating }">★</span>
                </div>
              </li>
            </ul>
          </div>
        </div>
      </section>

      <div v-if="activeMenu === 'hotels'">
        <section v-if="currentView === 'list'">
          <div class="header-actions">
            <h2>내 호텔 목록</h2>
            <div class="user-actions">
              <span v-if="user" class="user-name">{{ user.name }}님</span>
              <button class="add-btn" @click="openCreateForm">호텔 등록</button>
              <button class="logout-btn" @click="logout">로그아웃</button>
            </div>
          </div>
          <div class="hotel-grid">
            <div v-for="h in myHotels" :key="h.id" class="hotel-card" @click="showHotelDetails(h)">
              <img :src="h.imageUrls && h.imageUrls.length > 0 ? h.imageUrls[0] : 'https://via.placeholder.com/300'" alt="호텔 대표 이미지" class="hotel-card-image"/>
              <div class="hotel-card-info">
                <strong>{{ h.name }}</strong>
                <span>{{ h.address }}</span>
              </div>
            </div>
          </div>
        </section>

        <section v-if="currentView === 'details' && selectedHotel">
           <div class="header-actions">
             <button class="back-btn" @click="goToList">← 호텔 목록으로</button>
             <div class="user-actions">
              <span v-if="user" class="user-name">{{ user.name }}님</span>
              <button class="logout-btn" @click="logout">로그아웃</button>
            </div>
           </div>
           <div class="hotel-details-view">
              <img :src="selectedHotel.imageUrls && selectedHotel.imageUrls.length > 0 ? selectedHotel.imageUrls[0] : 'https://via.placeholder.com/400'" alt="호텔 대표 이미지" class="details-image"/>
              <div class="details-info">
                <h2>{{ selectedHotel.name }}</h2>
                <p><strong>주소:</strong> {{ selectedHotel.address }}, {{ selectedHotel.country }}</p>
                <p><strong>성급:</strong> {{ selectedHotel.starRating }}성</p>
                <p><strong>설명:</strong> {{ selectedHotel.description || '등록된 설명이 없습니다.' }}</p>
                <div class="details-actions">
                  <button class="btn-edit" @click="editHotel(selectedHotel)">수정</button>
                  <button class="btn-delete" @click="deleteHotel(selectedHotel.id)">삭제</button>
                  <button class="btn-rooms" @click="showRoomList(selectedHotel)">객실 보기</button>
                </div>
              </div>
           </div>
        </section>
        
        <section v-if="currentView === 'rooms' && selectedHotel">
          <div class="header-actions">
            <button class="back-btn" @click="showHotelDetails(selectedHotel)">← 호텔 정보로</button>
            <div class="user-actions">
               <span v-if="user" class="user-name">{{ user.name }}님</span>
               <button class="logout-btn" @click="logout">로그아웃</button>
            </div>
          </div>
          <h3>{{ selectedHotel.name }} - 객실 관리</h3>
           <div class="header-actions secondary">
            <p>등록된 객실 수: {{ rooms.length }}</p>
            <button class="add-btn" @click="openRoomCreateForm">객실 추가</button>
          </div>
          
          <ul class="room-list">
             <li v-for="room in rooms" :key="room.id" class="room-item">
                <img :src="room.imageUrls && room.imageUrls.length > 0 ? room.imageUrls[0] : 'https://via.placeholder.com/150'" alt="객실 대표 이미지" class="room-image" />
                <div class="room-info">
                  <strong>{{ room.roomType }}</strong>
                  <span>- 크기: {{ room.roomSize }}</span>
                  <span>- 인원: {{ room.capacityMin }}~{{ room.capacityMax }}명</span>
                  <span>- 가격: {{ room.price.toLocaleString() }}원</span>
                </div>
                <div class="actions">
                  <button @click="editRoom(room)">수정</button>
                  <button @click="deleteRoom(room.id)">삭제</button>
                </div>
             </li>
          </ul>
        </section>

        <section v-if="currentView === 'hotelForm'">
          <div class="form-wrapper">
          <div class="header-actions">
              <button class="back-btn" @click="cancelHotelForm">← 뒤로가기</button>
              <div class="user-actions">
               <span v-if="user" class="user-name">{{ user.name }}님</span>
               <button class="logout-btn" @click="logout">로그아웃</button>
              </div>
          </div>

          <div class="form-container">
            <h2>{{ editingHotel ? '호텔 수정' : '새 호텔 등록' }}</h2>
            <form @submit.prevent="handleHotelSubmit">
              <div class="form-group"><label>호텔명</label><input v-model="hotelForm.name" required /></div>
              <div class="form-group"><label>사업자번호 (선택)</label><input v-model.number="hotelForm.businessId" type="number" /></div>
              <div class="form-group"><label>주소</label><input v-model="hotelForm.address" required /></div>
              <div class="form-group"><label>국가</label><input v-model="hotelForm.country" required /></div>
              <div class="form-group"><label>성급 (1~5)</label><input v-model.number="hotelForm.starRating" type="number" min="1" max="5" required /></div>
              <div class="form-group"><label>호텔 설명</label><textarea v-model="hotelForm.description"></textarea></div>

              <div class="form-group">
                <label>이미지 (첫 번째 이미지가 대표 이미지)</label>
                <input type="file" @change="handleHotelFileChange" multiple accept="image/*" class="file-input">

                <draggable 
                  v-model="hotelEditableImages" item-key="id" 
                  class="image-preview-grid draggable-area" 
                  ghost-class="ghost">
                  <template #item="{ element, index }">
                      <div class="image-preview-item">
                          <img :src="element.src" alt="이미지 프리뷰"/>
                          <span v-if="index === 0" class="main-photo-badge">대표</span>
                          <button type="button" class="btn-remove-img" @click="removeHotelImage(element, index)">X</button>
                      </div>
                  </template>
              </draggable>
              </div>

              <div class="form-group">
                <label>편의시설</label>
                <div class="amenities-grid">
                  <div v-for="amenity in allAmenities" :key="amenity.id" class="amenity-item">
                    <input 
                      type="checkbox" 
                      :id="'amenity-' + amenity.id" 
                      :value="amenity.id"
                      v-model="hotelForm.amenityIds" 
                    />
                    <label :for="'amenity-' + amenity.id">{{ amenity.name }}</label>
                  </div>
                </div>
              </div>

              <div class="form-actions">
                <button type="submit" class="btn-primary">저장</button>
                <button type="button" class="btn-secondary" @click="cancelHotelForm">취소</button>
              </div>
            </form>
          </div>
        </div>
        </section>
        
        <section v-if="currentView === 'roomForm'">
          <div class="form-wrapper">
          <div class="header-actions">
              <button class="back-btn" @click="showRoomList(selectedHotel)">← 객실 목록으로</button>
              <div class="user-actions">
                  <span v-if="user" class="user-name">{{ user.name }}님</span>
                  <button class="logout-btn" @click="logout">로그아웃</button>
              </div>
          </div>
          <div class="form-container">
            <h2>{{ editingRoom ? '객실 수정' : '새 객실 등록' }}</h2>
              <form @submit.prevent="handleRoomSubmit">
                <div class="form-group">
                  <label>객실 타입</label>
                  <select v-model="roomForm.roomType" required>
                      <option disabled value="">객실 타입을 선택하세요</option>
                      <option>스위트룸</option>
                      <option>디럭스룸</option>
                      <option>스탠다드룸</option>
                      <option>싱글룸</option>
                      <option>트윈룸</option>
                  </select>
                </div>
                <div class="form-group"><label>객실 크기</label><input v-model="roomForm.roomSize" required /></div>
                <div class="form-group"><label>최소/최대 인원</label><div class="inline-group"><input v-model.number="roomForm.capacityMin" type="number" required /><input v-model.number="roomForm.capacityMax" type="number" required /></div></div>
                <div class="form-group"><label>1박 가격</label><input v-model.number="roomForm.price" type="number" required /></div>
                <div class="form-group"><label>객실 수</label><input v-model.number="roomForm.roomCount" type="number" required /></div>
                <div class="form-group"><label>체크인/체크아웃 시간</label><div class="inline-group"><input v-model="roomForm.checkInTime" type="time" required /><input v-model="roomForm.checkOutTime" type="time" required /></div></div>
                
                <div class="form-group">
                  <label>이미지 (첫 번째 이미지가 대표 이미지)</label>
                  <input type="file" @change="handleRoomFileChange" multiple accept="image/*" class="file-input">
  
                  <draggable 
                    v-model="roomEditableImages" item-key="id" 
                    class="image-preview-grid draggable-area" 
                    ghost-class="ghost">
                    <template #item="{ element, index }">
                        <div class="image-preview-item">
                            <img :src="element.src" alt="이미지 프리뷰"/>
                            <span v-if="index === 0" class="main-photo-badge">대표</span>
                            <button type="button" class="btn-remove-img" @click="removeRoomImage(element, index)">X</button>
                        </div>
                    </template>
                </draggable>
                </div>

                <div class="form-actions">
                  <button type="submit" class="btn-primary">저장</button>
                  <button type="button" class="btn-secondary" @click="showRoomList(selectedHotel)">취소</button>
                </div>
              </form>
          </div>
          </div>
        </section>
      </div>
      
      <section v-if="activeMenu === 'reservations'" class="reservations-section compact">
        <div class="header-actions">
          <h2>예약 관리</h2>
          <div class="user-actions">
            <span v-if="user" class="user-name">{{ user.name }}님</span>
            <button class="logout-btn" @click="logout">로그아웃</button>
          </div>
        </div>

        <div class="top-filter-container">
            <select id="hotel-filter" v-model="filterHotel" class="filter-select">
                <option value="ALL">모든 호텔</option>
                <option v-for="hotel in myHotels" :key="hotel.id" :value="hotel.name">{{ hotel.name }}</option>
            </select>

            <select id="room-type-filter" v-model="filterRoomType" class="filter-select">
                <option value="ALL">모든 객실</option>
                <option v-for="roomType in allRoomTypes" :key="roomType" :value="roomType">{{ roomType }}</option>
            </select>
        </div>

        <div class="reservations-content-compact">
          
          <div class="calendar-container">
            <FullCalendar ref="fullCalendar" :options="calendarOptions" />
          </div>

          <div class="reservation-sidebar">
            <div class="sidebar-header">
              <h3>{{ selectedDate ? `${selectedDate} 예약` : '최근 예약' }}</h3>
              <button v-if="selectedDate" @click="clearDateFilter" class="btn-clear-filter">초기화</button>
            </div>

            <div class="list-controls">
              <input type="text" v-model="searchKeyword" placeholder="예약자명 검색" class="search-input"/>
              
              <select v-model="filterStatus" class="filter-select">
                  <option value="COMPLETED">예약 완료</option>
                  <option value="CANCELLED">예약 취소</option>
                  <option value="ALL">모든 상태</option>
              </select>
            </div>

            <ul class="reservation-list">
              <li v-for="reservation in filteredReservations" :key="reservation.id" class="reservation-card" @click="showReservationDetails(reservation)">
                <div class="card-header">
                  <strong>{{ reservation.guestName }}</strong>
                  <span :class="`status-badge ${reservation.status.toLowerCase()}`">{{ reservation.statusLabel }}</span>
                </div>
                <div class="card-body">
                  <p>{{ reservation.roomType }}</p>
                  <p>{{ reservation.checkInDate }} ~ {{ reservation.checkOutDate }}</p>
                </div>
              </li>
              <li v-if="filteredReservations.length === 0" class="no-reservations">
                해당 예약이 없습니다.
              </li>
            </ul>
          </div>

        </div>
      </section>

      <div v-if="selectedReservation" class="modal-overlay" @click.self="closeReservationDetails">
        <div class="modal-content">
          <button class="modal-close-btn" @click="closeReservationDetails">✕</button>
          <h3>예약 상세 정보</h3>
          <div class="modal-grid">
            <div class="modal-item"><strong>예약 번호:</strong><span>{{ selectedReservation.id }}</span></div>
            <div class="modal-item"><strong>예약 상태:</strong><span :class="`status-badge ${selectedReservation.status.toLowerCase()}`">{{ selectedReservation.statusLabel }}</span></div>
            <div class="modal-item"><strong>예약자명:</strong><span>{{ selectedReservation.guestName }}</span></div>
            
            <div class="modal-item"><strong>연락처:</strong><span>{{ selectedReservation.guestPhone }}</span></div>
            <div class="modal-item"><strong>호텔:</strong><span>{{ selectedReservation.hotelName }}</span></div>
            
            <div class="modal-item"><strong>객실 타입:</strong><span>{{ selectedReservation.roomType }}</span></div>

            <div class="modal-item full-width"><strong>체크인/아웃:</strong><span>{{ selectedReservation.checkInDate }} ~ {{ selectedReservation.checkOutDate }} ({{ selectedReservation.nights }}박)</span></div>
            
            <div class="modal-item"><strong>성인:</strong><span>{{ selectedReservation.adults }}명</span></div>
            <div class="modal-item"><strong>어린이:</strong><span>{{ selectedReservation.children }}명</span></div>
            <div class="modal-item full-width"><strong>요청사항:</strong><span>{{ selectedReservation.requests || '없음' }}</span></div>
          </div>
          <div class="modal-actions">
            <button 
              class="btn-danger" 
              @click="cancelReservation(selectedReservation.id)"
              :disabled="!isCancellable(selectedReservation)"
              :class="{ 'disabled': !isCancellable(selectedReservation) }">
              예약 취소
            </button>
          </div>
        </div>
      </div>

      <section v-if="activeMenu === 'reviews'" class="reviews-section">
        <div class="header-actions">
          <h2>리뷰 관리</h2>
          <div class="user-actions">
            <span v-if="user" class="user-name">{{ user.name }}님</span>
            <button class="logout-btn" @click="logoutAndGoHome">로그아웃</button>
          </div>
        </div>

        <div class="review-controls">
          <select v-model="reviewFilter.hotel" class="filter-select">
            <option value="ALL">모든 호텔</option>
            <option>강릉 씨마크 호텔</option>
            <option>파라다이스 호텔 부산</option>
          </select>
          <select v-model="reviewFilter.rating" class="filter-select">
            <option value="ALL">모든 별점</option>
            <option value="5">★★★★★</option>
            <option value="4">★★★★☆</option>
            <option value="3">★★★☆☆</option>
            <option value="2">★★☆☆☆</option>
            <option value="1">★☆☆☆☆</option>
          </select>
          <select v-model="reviewFilter.replied" class="filter-select">
              <option value="ALL">전체 보기</option>
              <option value="REPLIED">답변 완료</option>
              <option value="NOT_REPLIED">미답변</option>
          </select>
        </div>

        <div class="review-list">
          <div v-for="review in filteredReviews" :key="review.id" class="review-card" @click="showReviewDetails(review)">
            <img :src="review.image || 'https://via.placeholder.com/150'" alt="리뷰 대표 이미지" class="review-image"/>
            <div class="review-content">
              <div class="review-header">
                <span class="review-hotel">{{ review.hotelName }}</span>
                <div class="star-rating">
                  <span v-for="n in 5" :key="n" :class="{ 'filled': n <= review.star_rating }">★</span>
                </div>
              </div>
              <p class="review-text">{{ review.content }}</p>
              <div class="review-footer">
                <span class="review-author">{{ review.author }}</span>
                <span class="review-date">{{ review.wrote_on }}</span>
                <span v-if="review.reply" class="reply-badge">답변 완료</span>
              </div>
            </div>
          </div>
          <div v-if="filteredReviews.length === 0" class="no-reviews">
              해당 조건의 리뷰가 없습니다.
          </div>
        </div>
      </section>

      <div v-if="selectedReview" class="modal-overlay" @click.self="closeReviewDetails">
        <div class="modal-content review-modal">
          <button class="modal-close-btn" @click="closeReviewDetails">✕</button>
          <h3>리뷰 상세 및 답변</h3>
          <div class="review-detail-content">
            <div class="review-detail-header">
              <div class="author-info">
                <strong>{{ selectedReview.author }}</strong>
                <span>{{ selectedReview.wrote_on }}</span>
              </div>
              <div class="star-rating large">
                  <span v-for="n in 5" :key="n" :class="{ 'filled': n <= selectedReview.star_rating }">★</span>
              </div>
            </div>
            <p class="review-detail-text">{{ selectedReview.content }}</p>
            <img v-if="selectedReview.image" :src="selectedReview.image" alt="리뷰 이미지" class="review-detail-image"/>
          </div>
          <div class="reply-section">
            <h4>사장님 답변</h4>
            <textarea v-model="selectedReview.reply" placeholder="답변을 작성해주세요..."></textarea>
            <div class="reply-actions">
              <button class="btn-primary">답변 등록</button>
            </div>
          </div>
        </div>
      </div>

    </main>
  </div>
</template>

<script>
import flatPickr from 'vue-flatpickr-component';
import 'flatpickr/dist/flatpickr.css';
import { Korean } from "flatpickr/dist/l10n/ko.js";

import axios from "axios";
import FullCalendar from '@fullcalendar/vue3';
import dayGridPlugin from '@fullcalendar/daygrid';
import interactionPlugin from '@fullcalendar/interaction';
import draggable from 'vuedraggable';
import flatpickr from 'flatpickr';
import SalesChart from './SalesChart.vue'; 

// ✅ 오너 전용 API 베이스
const OWNER_BASE = '/api/owner/hotels';

export default {
  components: {
    FullCalendar,
    draggable,
    flatPickr,
    SalesChart
  },
  data() {
    return {
      activeMenu: 'dashboard',
      dashboardSummary: {
        todaySales: 0,
        thisWeekSales: 0,
        thisMonthSales: 0,
        salesChangeVsYesterday: 0,
        salesChangeVsLastWeek: 0,
        salesChangeVsLastMonth: 0,
      },

      chartDateRange: [], // 선택된 날짜 범위를 저장할 배열
      chartDateConfig: {
        showMonths: 2,
        mode: "range",        // 범위 선택 모드
        dateFormat: "Y-m-d",  // 데이터 형식
        altInput: true,       // 사용자에게 보여줄 대체 입력란 사용
        altFormat: "Y년 m월 d일", // 보여줄 날짜 형식
        locale: Korean,       // 한국어 설정
        onClose: (selectedDates) => {
          // 사용자가 날짜 선택을 마치고 창을 닫았을 때만 값을 업데이트합니다.
          if (selectedDates.length === 2) {
            this.chartFilters.dateRange = [
              new Date(selectedDates[0]),
              new Date(selectedDates[1]),
            ];
          }
        },
        // onClose 콜백 등을 필요에 따라 추가할 수 있습니다.
        onReady: (_, __, instance) => {
          this.updateChartCalendarHeaders(instance);
        },
        onMonthChange: (_, __, instance) => {
          this.$nextTick(() => {
            this.updateChartCalendarHeaders(instance);
          });
        },
      },

      chartFilters: {
        hotelId: null,
        roomType: null,
        dateRange: [],
      },
      chartData: [], // 그래프에 표시될 최종 데이터
      activePeriod: '7days',


      user: null,
      myHotels: [],
      selectedHotel: null,
      rooms: [],
      editingHotel: null,
      editingRoom: null,
      hotelForm: {},
      roomForm: {},

      hotelEditableImages: [],
      roomEditableImages: [], 

      newImageFiles: [],
      deletedImageUrls: [],

      allAmenities: [],
      currentView: 'list', 
      
      // 예약 관련 상태
      allReservations: [],
      selectedReservation: null,
      selectedDate: null,
      searchKeyword: '',
      filterStatus: 'COMPLETED',
      filterHotel: 'ALL',
      filterRoomType: 'ALL',
      
      allRoomTypes: ['스위트룸', '디럭스룸', '스탠다드룸', '싱글룸', '트윈룸'],


      isWheelScrolling: false,
      wheelScrollTimer: null,

      // 캘린더 옵션
      calendarOptions: {
        plugins: [dayGridPlugin, interactionPlugin],
        initialView: 'dayGridMonth',
        headerToolbar: {
          left: 'prev,next today',
          center: 'title',
          right: 'dayGridMonth,dayGridWeek'
        },
        locale: 'ko',
        events: [], // 이벤트는 watch를 통해 동적으로 채워집니다.
        dateClick: this.handleDateClick,
        dayMaxEvents: 3,
        views: {
          dayGridWeek: {
            dayMaxEvents: 10
          }
        },
      },

      allReviews: [],
      selectedReview: null,
      reviewFilter: {
        hotel: 'ALL',
        rating: 'ALL',
        replied: 'ALL',
      },

      activeTab: 'check-in',
      todaysCheckIns: [],
      todaysCheckOuts: [],
      recentReservations: [],
      recentReviews: [ { id: 1, name: '조하윤', rating: 5, comment: '정말 최고의 경험이었어요!' } /* ... */ ],
    };
  },



  computed: {
    filteredReservations() {
      let reservations = this.allReservations;

      if (this.selectedDate) {
        const selected = new Date(this.selectedDate);
        selected.setHours(0,0,0,0);
        reservations = reservations.filter(r => {
          const checkIn = new Date(r.checkInDate);
          checkIn.setHours(0,0,0,0);
          const checkOut = new Date(r.checkOutDate);
          checkOut.setHours(0,0,0,0);
          return selected >= checkIn && selected <= checkOut;
        });
      }
      
      if (this.filterStatus !== 'ALL') {
        reservations = reservations.filter(r => r.status === this.filterStatus);
      }
      if (this.filterHotel !== 'ALL') {
          reservations = reservations.filter(r => r.hotelName === this.filterHotel);
      }
      if (this.filterRoomType !== 'ALL') {
          reservations = reservations.filter(r => r.roomType === this.filterRoomType);
      }
      if (this.searchKeyword.trim() !== '') {
        const keyword = this.searchKeyword.toLowerCase();
        reservations = reservations.filter(r => 
          r.guestName.toLowerCase().includes(keyword)
        );
      }
      return reservations;
    },
    isCancellable() {
      return (reservation) => {
        if (!reservation) return false;

        // 이미 취소된 예약은 취소 불가
        if (reservation.status === 'CANCELLED') {
          return false;
        }

        // 오늘 날짜와 체크인 날짜를 시간 정보 없이 비교
        const today = new Date();
        today.setHours(0, 0, 0, 0); 
        const checkInDate = new Date(reservation.checkInDate);
        checkInDate.setHours(0, 0, 0, 0);

        // 체크인 날짜가 지났거나 오늘이면 취소 불가
        if (checkInDate <= today) {
          return false;
        }

        return true;
      };
    },
    filteredReviews() {
        let reviews = this.allReviews;

        if (this.reviewFilter.hotel !== 'ALL') {
            reviews = reviews.filter(r => r.hotelName === this.reviewFilter.hotel);
        }
        if (this.reviewFilter.rating !== 'ALL') {
            reviews = reviews.filter(r => r.star_rating == this.reviewFilter.rating);
        }
        if (this.reviewFilter.replied !== 'ALL') {
            if (this.reviewFilter.replied === 'REPLIED') {
                reviews = reviews.filter(r => r.reply && r.reply.trim() !== '');
            } else {
                reviews = reviews.filter(r => !r.reply || r.reply.trim() === '');
            }
        }
        return reviews;
    },
    filteredCalendarEvents() {
      console.log("[Computed] 최근 예약 목록 필터링 결과를 캘린더에 반영합니다.");
      
      // 1. 이미 모든 필터링이 완료된 'filteredReservations' 결과를 가져옵니다.
      const filteredList = this.filteredReservations;
      
      // 2. 이 결과를 FullCalendar 이벤트 형식으로 변환하기만 하면 됩니다.
      return filteredList.map(r => {
        // 체크아웃 날짜에 하루를 더해 FullCalendar가 마지막 날까지 포함하도록 함
        const endDate = new Date(r.checkOutDate);
        endDate.setDate(endDate.getDate() + 1);

        return {
          title: `${r.guestName} (${r.roomType})`,
          start: r.checkInDate,
          end: endDate.toISOString().split('T')[0], // 'YYYY-MM-DD' 형식으로 변환
          color: r.status === 'COMPLETED' ? '#10b981' : '#6b7280',
          extendedProps: { reservation: r }
        };
      });
    },
  },





  methods: {
    async fetchDashboardSummary() {
      const headers = this.getAuthHeaders();
      if (!headers) return;
      try {
        const response = await axios.get(`${OWNER_BASE}/dashboard/sales-summary`, { headers });
        this.dashboardSummary = response.data;
      } catch (error) {
        console.error("대시보드 요약 정보 조회 실패:", error);
      }
    },
    formatNumber(num, fractionDigits = 0) {
      if (typeof num !== 'number') return num;
      return num.toLocaleString('ko-KR', {
        minimumFractionDigits: fractionDigits,
        maximumFractionDigits: fractionDigits,
      });
    },
    getComparisonClass(change) {
      if (change > 0) return 'positive';
      if (change < 0) return 'negative';
      return '';
    },
    getComparisonText(change) {
      if (change === 0 || !isFinite(change)) return '-';
      const sign = change > 0 ? '+' : '';
      return `${sign}${this.formatNumber(change, 1)}%`;
    },
    clearChartFilters() {
      this.chartDateRange = []; // 날짜 선택 배열을 비웁니다.
      // 필요하다면 호텔, 객실 필터도 여기서 초기화할 수 있습니다.
      // this.chartHotelFilter = 'ALL';
      // this.chartRoomFilter = 'ALL';
    },
    updateChartCalendarHeaders(instance) {
      if (!instance.calendarContainer) return;
      
      // 기본 연/월 선택 UI 숨기기
      const yearInputs = instance.calendarContainer.querySelectorAll('.numInputWrapper, .arrowUp, .arrowDown');
      yearInputs.forEach(el => { el.style.display = 'none'; });

      const monthHeaders = instance.calendarContainer.querySelectorAll('.flatpickr-current-month');
      monthHeaders.forEach((header, index) => {
        header.innerHTML = ''; // 기존 내용 삭제

        const now = new Date();
        const baseMonth = (instance.currentMonth ?? now.getMonth()) + index;
        const baseYear  = (instance.currentYear  ?? now.getFullYear());
        const displayYear  = baseYear + Math.floor(baseMonth / 12);
        const displayMonth = ((baseMonth % 12) + 12) % 12;

        const monthNames = ['1월','2월','3월','4월','5월','6월','7월','8월','9월','10월','11월','12월'];
        
        const textSpan = document.createElement('span');
        textSpan.textContent = `${displayYear}년 ${monthNames[displayMonth]}`;
        textSpan.style.cssText = 'font-size:16px; font-weight:600; color:#333;';
        header.appendChild(textSpan);
      });
    },

    async fetchChartData() {
      const headers = this.getAuthHeaders();
      if (!headers) return;

      let startDate, endDate;
      const today = new Date();
      today.setHours(0, 0, 0, 0);

      // 1. 캘린더에서 날짜 범위를 직접 선택했는지 먼저 확인합니다.
      if (this.chartFilters.dateRange && this.chartFilters.dateRange.length === 2) {
        this.activePeriod = 'custom'; // 버튼 상태를 'custom'으로 변경
        [startDate, endDate] = this.chartFilters.dateRange.map(d => new Date(d));
      } else {
        // 2. 직접 선택하지 않았다면, '최근 7일' 등 버튼 상태에 따라 날짜를 계산합니다.
        endDate = new Date(today);
        startDate = new Date(today);
        if (this.activePeriod === '7days') {
          startDate.setDate(today.getDate() - 6);
        } else if (this.activePeriod === '30days') {
          startDate.setDate(today.getDate() - 29);
        } else if (this.activePeriod === '1year') {
          startDate.setFullYear(today.getFullYear() - 1);
        }
      }
      
      // YYYY-MM-DD 형식으로 변환
      const formatDate = (date) => {
        const d = new Date(date);
        const year = d.getFullYear();
        const month = String(d.getMonth() + 1).padStart(2, '0');
        const day = String(d.getDate()).padStart(2, '0');
        return `${year}-${month}-${day}`;
      };

      const requestBody = {
        startDate: formatDate(startDate),
        endDate: formatDate(endDate),
        hotelId: this.chartFilters.hotelId,
        roomType: this.chartFilters.roomType,
      };

      try {
        const response = await axios.post(`${OWNER_BASE}/dashboard/daily-sales`, requestBody, { headers });
        this.chartData = this.fillMissingDates(response.data, startDate, endDate);
      } catch (error) {
        console.error("차트 데이터 조회 실패:", error);
        this.chartData = [];
      }
    },
    
    // ✅ [추가] 데이터가 없는 날짜를 0으로 채워주는 헬퍼 함수
    fillMissingDates(data, startDate, endDate) {
      const salesMap = new Map(data.map(item => [item.date, item.totalSales]));
      const filledData = [];
      let currentDate = new Date(startDate);

      while (currentDate <= endDate) {
        const y = currentDate.getFullYear();
        const m = String(currentDate.getMonth() + 1).padStart(2, '0');
        const d = String(currentDate.getDate()).padStart(2, '0');
        const dateStr = `${y}-${m}-${d}`;

        filledData.push({
          date: dateStr,
          totalSales: salesMap.get(dateStr) || 0
        });
        
        currentDate.setDate(currentDate.getDate() + 1);
      }
      return filledData;
    },

    // 기간 버튼 클릭 핸들러
    setPeriod(period) {
      this.activePeriod = period;
      this.chartFilters.dateRange = []; // 기간 버튼 선택 시 캘린더 선택은 초기화
      // this.fetchChartData();
    },
    clearChartFilters() {
      this.chartFilters.hotelId = null;
      this.chartFilters.roomType = null;
      this.setPeriod('7days');
    },
    async fetchDashboardActivity() {
      const headers = this.getAuthHeaders();
      if (!headers) return;
      try {
        const response = await axios.get(`${OWNER_BASE}/dashboard/activity`, { headers });
        const data = response.data;
        this.todaysCheckIns = data.checkIns;
        this.todaysCheckOuts = data.checkOuts;
        this.recentReservations = data.recentReservations;
      } catch (error) {
        console.error("대시보드 활동 정보 조회 실패:", error);
      }
    },
    
    formatTimeAgo(dateString) {
      if (!dateString) return '';
      const now = new Date();
      const past = new Date(dateString);
      const diffInSeconds = Math.floor((now - past) / 1000);
      
      const minutes = Math.floor(diffInSeconds / 60);
      if (minutes < 1) return '방금 전';
      if (minutes < 60) return `${minutes}분 전`;
      
      const hours = Math.floor(minutes / 60);
      if (hours < 24) return `${hours}시간 전`;

      const days = Math.floor(hours / 24);
      return `${days}일 전`;
    },

    // --- 공통 메소드 ---
    getAuthHeaders() {
      const token = localStorage.getItem('token');
      if (!token) {
        this.$router.push("/login");
        return null;
      }
      return { 'Authorization': `Bearer ${token}` };
    },
    logout() {
        localStorage.removeItem('token');
        localStorage.removeItem('user');
        alert("로그아웃 되었습니다.");
        this.$router.push('/login');
    },
    logoutAndGoHome() {
        localStorage.removeItem('token');
        localStorage.removeItem('user');
        alert("로그아웃 되었습니다.");
        this.$router.push('/'); // 로그인 페이지 대신 홈페이지로 이동
    },
    goToList() {
      this.selectedHotel = null;
      this.currentView = 'list';
      this.fetchHotels();
    },

    // --- 데이터 조회 메소드 ---
    async fetchHotels() {
      // 1. 로그인된 사용자 정보 확인
      console.log("1. fetchHotels: 현재 사용자 정보", this.user);
      if (!this.user) {
        console.error("사용자 정보가 없어 호텔 목록을 조회할 수 없습니다.");
        return;
      }

      const headers = this.getAuthHeaders();
      if (!headers) {
        console.error("인증 헤더가 없어 API를 호출할 수 없습니다.");
        return;
      }

      // 2. API 호출 직전
      console.log("2. fetchHotels:", `${OWNER_BASE}/my-hotels`, "API 호출 시작");

      try {
        const res = await axios.get(`${OWNER_BASE}/my-hotels`, { headers });
        // 3. API 응답 데이터 확인
        console.log("3. fetchHotels: API 응답 데이터", res.data);
        this.myHotels = res.data;
      } catch (err) {
        // 4. 에러 발생 시
        console.error("4. fetchHotels: 호텔 조회 실패:", err.response?.data || err.message);
      }
    },
    async fetchAmenities() {
      const headers = this.getAuthHeaders();
      if (!headers) return;
      try {
        const response = await axios.get(`${OWNER_BASE}/amenities`, { headers });
        this.allAmenities = response.data;
        console.log("전체 편의시설 목록:", this.allAmenities);
      } catch (err) {
        console.error("편의시설 목록 조회 실패:", err);

        // this.allAmenities = [
        //     { id: 1, name: '무료 Wi-Fi' }, { id: 2, name: '수영장' },
        //     { id: 3, name: '헬스장' }, { id: 4, name: '주차장' }
        // ];
      }
    },
    async fetchRooms(hotelId) {
      const headers = this.getAuthHeaders();
      if (!headers) return;
      console.log("1. [객실 조회] API 호출 시작:", `${OWNER_BASE}/${hotelId}/rooms`);
      try {
        const res = await axios.get(`${OWNER_BASE}/${hotelId}/rooms`, { headers });
        console.log("2. [객실 조회] API 응답 데이터:", res.data);
        this.rooms = res.data;
      } catch (err) {
        console.error("3. [객실 조회] API 호출 실패:", err.response?.data || err.message);
        alert("객실 정보 조회에 실패했습니다.");
      }
    },
    
    // --- 뷰 전환 메소드 ---
    showHotelDetails(hotel) {
      this.selectedHotel = hotel;
      this.currentView = 'details';
    },
    async showRoomList(hotel) {
      this.selectedHotel = hotel;
      this.currentView = 'loading';
      await this.fetchRooms(hotel.id);
      this.currentView = 'rooms';
    },
    cancelHotelForm() {
      if (this.editingHotel) {
        this.currentView = 'details';
      } else {
        this.currentView = 'list';
      }
      this.editingHotel = null;
    },

    // --- 호텔 관리 ---
    openCreateForm() {
      this.editingHotel = null; // 수정 모드가 아님을 명확히 함
      // 새 호텔 정보를 입력받을 빈 객체로 초기화
      this.hotelForm = {
        name: '',
        businessId: null,
        address: '',
        country: "대한민국",
        starRating: 5,
        description: '',
        amenityIds: []
      };
      // 이미지 관련 배열들도 모두 비워줌
      this.hotelEditableImages = [];
      this.newImageFiles = [];
      this.deletedImageUrls = [];
      this.currentView = 'hotelForm'; 
    },
    editHotel(hotel) {
      this.editingHotel = hotel;
      this.hotelForm = JSON.parse(JSON.stringify(hotel)); // ✅ [추천] 깊은 복사로 변경
      this.hotelEditableImages = (hotel.imageUrls || []).map(url => ({ type: 'url', src: url, id: url }));
      this.newImageFiles = []; // 수정 시에는 새로 추가된 파일 목록 초기화
      this.deletedImageUrls = []; // 수정 시에는 삭제할 URL 목록 초기화
      this.currentView = 'hotelForm';
    },
    handleHotelFileChange(event) {
      const files = Array.from(event.target.files);
      files.forEach(file => {
        // 미리보기를 위한 URL 생성
        const previewUrl = URL.createObjectURL(file);
        // vuedraggable에 표시될 객체 생성
        const imageObject = {
          type: 'file',
          src: previewUrl,
          fileObject: file,
          id: previewUrl // 고유 key로 사용
        };
        this.hotelEditableImages.push(imageObject);
        this.newImageFiles.push(file); // 새로 추가된 파일 목록에 저장
      });
      event.target.value = ''; // 같은 파일 다시 선택 가능하도록 초기화
    },
    removeHotelImage(imageToRemove, index) {
      // 미리보기 목록에서 제거
      this.hotelEditableImages.splice(index, 1);
      
      if (imageToRemove.type === 'url') {
        // 기존에 있던 이미지(URL)라면 삭제 목록에 추가
        this.deletedImageUrls.push(imageToRemove.src);
      } else if (imageToRemove.type === 'file') {
        // 새로 추가했던 파일이라면 newImageFiles 목록에서 제거
        this.newImageFiles = this.newImageFiles.filter(
          f => f !== imageToRemove.fileObject
        );
        // 메모리 관리를 위해 생성했던 미리보기 URL 해제
        URL.revokeObjectURL(imageToRemove.src);
      }
    },
    handleHotelSubmit() {
      const formData = new FormData();

      // 1. 순서가 변경된 최종 이미지 URL 목록을 생성
      const finalImageUrls = this.hotelEditableImages
        .filter(img => img.type === 'url')
        .map(img => img.src);
      
      // hotelForm 데이터에 최종 URL 목록과 삭제할 URL 목록을 추가
      const hotelData = {
        ...this.hotelForm,
        imageUrls: finalImageUrls, // 정렬된 기존 URL 목록
        deletedUrls: this.deletedImageUrls // 삭제할 URL 목록
      };
      
      // hotelForm 데이터를 JSON으로 변환하여 formData에 추가
      formData.append('hotel', new Blob([JSON.stringify(hotelData)], { type: 'application/json' }));
      
      // 2. 새로 추가된 파일들을 순서대로 formData에 추가
      const newFilesInOrder = this.hotelEditableImages
        .filter(img => img.type === 'file')
        .map(img => img.fileObject);
        
      newFilesInOrder.forEach(file => {
        formData.append('files', file);
      });

      // 디버깅을 위한 콘솔 출력
      console.log("✅ 전송될 호텔 데이터:", hotelData);
      console.log("✅ 전송될 새 파일:", newFilesInOrder);

      if (this.editingHotel) {
        this.updateHotel(formData);
      } else {
        this.createHotel(formData);
      }
    },

    async createHotel(formData) {
      const headers = this.getAuthHeaders();
      if (!headers) return;
      try {
        await axios.post(`${OWNER_BASE}`, formData, { headers });
        alert("호텔이 성공적으로 등록되었습니다.");
        this.goToList();
      } catch (err) {
        console.error("호텔 등록 실패:", err.response?.data || err.message);
        alert("호텔 등록에 실패했습니다.");
      }
    },
    async updateHotel(formData) {
      const headers = this.getAuthHeaders();
      if (!headers) return;

      // 1. 수정 API 호출 직전 데이터 확인
      console.log("1. [수정] API 호출 시작:", `${OWNER_BASE}/${this.editingHotel.id}`);
      console.log("   [수정] 전송할 데이터 (FormData):", formData);
      // FormData의 내용을 확인하려면 아래와 같이 각 key를 직접 로깅해야 합니다.
      for (let [key, value] of formData.entries()) {
        console.log(`   [수정] FormData ${key}:`, value);
      }

      try {
        await axios.post(`${OWNER_BASE}/${this.editingHotel.id}`, formData, { headers });
        // 2. 수정 성공 시
        console.log("2. [수정] API 호출 성공");
        alert("호텔 정보가 성공적으로 수정되었습니다.");
        this.goToList();
      } catch (err) {
        // 3. 수정 실패 시
        console.error("3. [수정] API 호출 실패:", err.response?.data || err.message);
        alert("호텔 수정에 실패했습니다.");
      }
    },
    async deleteHotel(id) {
      if (!confirm("정말로 이 호텔을 삭제하시겠습니까? 연관된 모든 객실 정보도 함께 삭제됩니다.")) return;
      const headers = this.getAuthHeaders();
      if (!headers) return;

      // 1. 삭제 API 호출 직전 ID 확인
      console.log("1. [삭제] API 호출 시작:", `${OWNER_BASE}/${id}`);

      try {
        await axios.delete(`${OWNER_BASE}/${id}`, { headers });
        // 2. 삭제 성공 시
        console.log("2. [삭제] API 호출 성공");
        alert("호텔이 삭제되었습니다.");
        this.goToList();
      } catch (err) {
        // 3. 삭제 실패 시
        console.error("3. [삭제] API 호출 실패:", err.response?.data || err.message);
        alert("호텔 삭제에 실패했습니다.");
      }
    },

    // --- 객실 관리 ---
    openRoomCreateForm() {
        this.editingRoom = null;
        this.roomForm = { roomType: '스탠다드룸', checkInTime: '15:00', checkOutTime: '11:00' };
        this.roomEditableImages = [];
        this.newImageFiles = [];
        this.deletedImageUrls = [];
        this.currentView = 'roomForm';
    },
    editRoom(room) {
        this.editingRoom = room;
        this.roomForm = JSON.parse(JSON.stringify(room));
        this.roomEditableImages = (room.imageUrls || []).map(url => ({ type: 'url', src: url, id: url }));
        this.newImageFiles = [];
        this.deletedImageUrls = [];
        this.currentView = 'roomForm';
    },
    handleRoomFileChange(event) {
      const files = Array.from(event.target.files);
      files.forEach(file => {
        const previewUrl = URL.createObjectURL(file);
        this.roomEditableImages.push({ type: 'file', src: previewUrl, fileObject: file, id: previewUrl });
        this.newImageFiles.push(file);
      });
      event.target.value = '';
    },
    removeRoomImage(imageToRemove, index) {
      this.roomEditableImages.splice(index, 1);
      if (imageToRemove.type === 'url') {
        this.deletedImageUrls.push(imageToRemove.src);
      } else {
        this.newImageFiles = this.newImageFiles.filter(f => f !== imageToRemove.fileObject);
        URL.revokeObjectURL(imageToRemove.src);
      }
    },
    handleRoomSubmit() {
      const formData = new FormData();
      const finalImageUrls = this.roomEditableImages.filter(img => img.type === 'url').map(img => img.src);
      const roomData = {
        ...this.roomForm,
        imageUrls: finalImageUrls,
        deletedUrls: this.deletedImageUrls
      };
      
      formData.append('room', new Blob([JSON.stringify(roomData)], { type: 'application/json' }));
      
      const newFilesInOrder = this.roomEditableImages.filter(img => img.type === 'file').map(img => img.fileObject);
      newFilesInOrder.forEach(file => { formData.append('files', file); });
      
      console.log("✅ 전송될 객실 데이터:", roomData);
      console.log("✅ 전송될 새 파일:", newFilesInOrder);

      if (this.editingRoom) this.updateRoom(formData);
      else this.createRoom(formData);
    },

    async createRoom(formData) {
      const headers = this.getAuthHeaders();
      if (!headers) return;
      try {
        await axios.post(`${OWNER_BASE}/${this.selectedHotel.id}/rooms`, formData, { headers});
        alert("객실이 등록되었습니다.");
        this.showRoomList(this.selectedHotel);
      } catch(err) {
        console.error("객실 등록 실패:", err.response?.data || err.message);
        alert("객실 등록에 실패했습니다.");
      }
    },

    async updateRoom(formData) {
      const headers = this.getAuthHeaders();
      if (!headers) return;
      try {
        await axios.put(`${OWNER_BASE}/rooms/${this.editingRoom.id}`, formData, { headers });
        alert("객실 정보가 수정되었습니다.");
        this.showRoomList(this.selectedHotel);
      } catch(err) {
        console.error("객실 수정 실패:", err.response?.data || err.message);
        alert("객실 수정에 실패했습니다.");
      }
    },
     async deleteRoom(roomId) {
      if (!confirm("객실을 삭제하시겠습니까?")) return;
      const headers = this.getAuthHeaders();
      if (!headers) return;
      try {
        await axios.delete(`${OWNER_BASE}/rooms/${roomId}`, { headers });
        alert("객실이 삭제되었습니다.");
        this.fetchRooms(this.selectedHotel.id);
      } catch(err) {
        console.error("객실 삭제 실패:", err.response?.data || err.message);
        alert("객실 삭제에 실패했습니다.");
      }
    },
    
    checkLoginStatus() {
      const userInfo = localStorage.getItem('user');
      if (userInfo) {
        this.user = JSON.parse(userInfo);
        this.fetchHotels();
      } else {
        this.$router.push("/login");
      }
    },
    handleDateClick(arg) {
      this.selectedDate = arg.dateStr;
      this.searchKeyword = '';
      this.filterStatus = 'COMPLETED';
      this.filterRoomType = 'ALL';
    },
    showReservationDetails(reservation) {
      this.selectedReservation = reservation;
    },
    closeReservationDetails() {
      this.selectedReservation = null;
    },
    handleWheelScroll(event) {
      event.preventDefault();
      if (this.isWheelScrolling) return;

      this.isWheelScrolling = true;
      if (this.$refs.fullCalendar) {
        const calendarApi = this.$refs.fullCalendar.getApi();
        if (event.deltaY < 0) {
          calendarApi.prev();
        } else {
          calendarApi.next();
        }
      }
      this.wheelScrollTimer = setTimeout(() => {
        this.isWheelScrolling = false
      }, 300);
    },
    clearDateFilter() {
      this.selectedDate = null;
    },
    
    // 임시 데이터 생성 및 캘린더 이벤트 업데이트
    loadMockReservations() {
      console.log("A. [Method] loadMockReservations 메서드가 호출되었습니다.");
      // 백엔드 API 대신 사용할 하드코딩된 데이터
      const mockData = [
        { id: 'R1001', hotelName: '강릉 씨마크 호텔', guestName: '김철수', guestPhone: '010-1234-5678', roomType: '디럭스룸', checkIn: '2025-09-22', checkOut: '2025-09-24', nights: 2, adults: 2, children: 0, status: 'COMPLETED', statusLabel: '예약 완료', requests: '바다 전망 객실로 부탁드립니다.' },
        { id: 'R1002', hotelName: '강릉 씨마크 호텔', guestName: '박영희', guestPhone: '010-2222-3333', roomType: '스위트룸', checkIn: '2025-09-23', checkOut: '2025-09-26', nights: 3, adults: 2, children: 1, status: 'PENDING', statusLabel: '예약 대기', requests: '아기 침대가 필요해요.' },
        { id: 'R1003', hotelName: '파라다이스 호텔 부산', guestName: '이민준', guestPhone: '010-4567-8901', roomType: '스탠다드룸', checkIn: '2025-09-25', checkOut: '2025-09-26', nights: 1, adults: 1, children: 0, status: 'CANCELLED', statusLabel: '예약 취소' },
        { id: 'R1004', hotelName: '강릉 씨마크 호텔', guestName: '최유나', roomType: '디럭스룸', checkIn: '2025-10-03', checkOut: '2025-10-05', nights: 2, adults: 2, children: 0, status: 'COMPLETED', statusLabel: '예약 완료' },
      ];

      this.allReservations = mockData;
      console.log("B. [Method] this.allReservations에 임시 데이터가 할당되었습니다:", this.allReservations);

      // 캘린더에 표시할 이벤트 데이터로 변환
      this.calendarOptions.events = this.allReservations.map(r => ({
        title: `${r.guestName} (${r.roomType})`,
        start: r.checkIn,
        end: r.checkOut,
        color: r.status === 'COMPLETED' ? '#10b981' : (r.status === 'PENDING' ? '#f59e0b' : '#6b7280'),
      }));
      console.log("C. [Method] 캘린더 이벤트가 업데이트되었습니다:", this.calendarOptions.events);
    },
    async cancelReservation(reservationId) {
      if (!confirm("정말로 이 예약을 취소하시겠습니까?")) return;

      const headers = this.getAuthHeaders();
      if (!headers) return;

      try {
        await axios.post(`${OWNER_BASE}/reservations/${reservationId}/owner-cancel`, {}, { headers });
        
        alert("예약이 성공적으로 취소되었습니다.");

        this.closeReservationDetails();
        await this.fetchReservations();
        await this.fetchDashboardActivity();

      } catch (error) {
        console.error("예약 취소 실패:", error);
        alert("예약 취소 중 오류가 발생했습니다.");
      }
    },
    

    showReviewDetails(review) {
      // 원본 데이터를 수정하지 않기 위해 객체를 복사해서 사용
      this.selectedReview = { ...review };
    },
    closeReviewDetails() {
      this.selectedReview = null;
    },

    loadMockReviews() {
      this.allReviews = [
        { id: 1, reservation_id: 101, wrote_on: '2025-09-21', star_rating: 5, content: '객실이 정말 깨끗하고 바다 전망이 환상적이었어요! 직원분들도 모두 친절하셔서 편안하게 쉬다 갑니다. 다음에 또 방문할게요!', image: 'https://source.unsplash.com/random/800x600?hotel,view', hotelName: '강릉 씨마크 호텔', author: '김철수', reply: '소중한 후기 감사드립니다! 다음에도 최고의 경험을 선물해 드릴 수 있도록 노력하겠습니다.' },
        { id: 2, reservation_id: 102, wrote_on: '2025-09-20', star_rating: 4, content: '수영장이 넓고 좋아서 아이들이 정말 좋아했어요. 다만 조식 종류가 조금 더 다양했으면 하는 아쉬움이 남네요.', image: 'https://source.unsplash.com/random/800x600?hotel,pool', hotelName: '파라다이스 호텔 부산', author: '박영희', reply: '' },
        { id: 3, reservation_id: 103, wrote_on: '2025-09-19', star_rating: 3, content: '위치는 좋았지만 방음이 잘 안돼서 조금 시끄러웠습니다. 시설은 전반적으로 만족합니다.', image: null, hotelName: '강릉 씨마크 호텔', author: '최유나', reply: '' },
      ];
    },
    async fetchReservations() {
        if (!this.user) return;
        const headers = this.getAuthHeaders();  
        if (!headers) return;

        try {
            const response = await axios.get(`${OWNER_BASE}/owner/${this.user.id}/reservations`, { headers });
            
            this.allReservations = response.data
                .filter(r => r.status !== 'PENDING'); // PENDING 상태 제외

        } catch (error) {
            console.error("[예약 조회] API 호출 실패:", error.response || error);
            alert("예약 정보를 불러오는 데 실패했습니다.");
        }
    },
  },


  watch: {
    chartFilters: {
      handler(newFilters, oldFilters) {
        const newDateRange = JSON.stringify(newFilters.dateRange);
        const oldDateRange = oldFilters ? JSON.stringify(oldFilters.dateRange) : null;

        if (newDateRange !== oldDateRange && newFilters.dateRange && newFilters.dateRange.length > 0) {
            this.activePeriod = 'custom';
        }
        
        this.fetchChartData();
      },
      deep: true
    },

    // 캘린더 이벤트 목록을 감시하는 부분은 그대로 유지합니다.
    filteredCalendarEvents: {
      handler(newEvents) {
        this.calendarOptions.events = newEvents;
      },
      immediate: true
    }
  },


  mounted() {
    this.checkLoginStatus();
    this.fetchAmenities();
    this.fetchReservations();

    this.$nextTick(() => {
        const calendarEl = this.$refs.fullCalendar?.$el;
        if (calendarEl) {
            // passive: false 옵션을 추가하여 preventDefault가 작동하도록 합니다.
            calendarEl.addEventListener('wheel', this.handleWheelScroll, { passive: false });
        }
    });

    this.fetchDashboardSummary();
    this.fetchChartData();
    this.fetchDashboardActivity();

    this.loadMockReviews(); //리뷰 임시데이터
  },
  beforeUnmount() {
    // 컴포넌트가 사라질 때 이벤트 리스너를 제거하여 메모리 누수 방지
    clearTimeout(this.wheelScrollTimer);
    const calendarEl = this.$refs.fullCalendar?.$el;
    if (calendarEl) {
        calendarEl.removeEventListener('wheel', this.handleWheelScroll);
    }
  }
};
</script>

<style scoped>
/* 전체 레이아웃 */
.owner-page {
  display: flex;
  height: 100vh;
  width: 100vw;
  margin: 0;
  background: #f3f4f6;
}
.sidebar {
  width: 220px;
  background: #111827;
  color: #fff;
  padding: 20px 10px;
  box-sizing: border-box;
  position: fixed;
  top: 0;
  left: 0;
  bottom: 0;
  overflow-y: auto;
  z-index: 10;
}
.sidebar .logo {
  font-weight: 700;
  font-size: 20px;
  margin-bottom: 25px;
  text-align: center;
}
.sidebar ul {
  list-style: none;
  padding: 0;
  margin: 0;
}
.sidebar li {
  padding: 12px 15px;
  cursor: pointer;
  border-radius: 6px;
  margin: 4px 0;
  transition: background-color .2s;
}
.sidebar li.active,
.sidebar li:hover {
  background: #374151;
}
.main-content {
  margin-left: 130px;
  width: calc(100% - 130px);
  height: 100vh;
  padding: 0;
  box-sizing: border-box;
  overflow-y: auto;
}
.main-content > section {
  padding: 30px;
}
.main-content h2 {
  margin: 0;
  font-size: 24px;
  color: #111827;
}
.main-content h3 {
  margin-top: 20px;
  font-size: 20px;
  color: #111827;
}

/* 헤더 & 버튼 */
.header-actions {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 25px;
  margin-top: 25px;
}
.header-actions.secondary {
  margin-top: 20px;
  margin-bottom: 20px;
  padding-bottom: 10px;
  border-bottom: 1px solid #e5e7eb;
}
.user-actions {
  display: flex;
  align-items: center;
  gap: 15px;
}
.user-name {
  font-weight: 600;
  color: #374151;
}
.add-btn {
  padding: 10px 16px;
  background: #3b82f6;
  color: #fff;
  border: none;
  border-radius: 6px;
  cursor: pointer;
  font-size: 14px;
  font-weight: 700;
}
.add-btn:hover {
  background: #2563eb;
}
.logout-btn {
  padding: 10px 16px;
  background: #6b7280;
  color: #fff;
  border: none;
  border-radius: 6px;
  cursor: pointer;
  font-size: 14px;
  font-weight: 700;
}
.logout-btn:hover {
  background: #4b5563;
}
.back-btn {
  margin: 0;
  padding: 10px 16px;
  background: #6b7280;
  color: #fff;
  border: none;
  border-radius: 6px;
  cursor: pointer;
}
.back-btn:hover {
  background: #4b5563;
}

/* 호텔 목록 & 상세 */
.hotel-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: 25px;
}
.hotel-card {
  aspect-ratio: 1/1;
  background: #fff;
  border-radius: 12px;
  box-shadow: 0 4px 12px #00000014;
  cursor: pointer;
  overflow: hidden;
  display: flex;
  flex-direction: column;
  transition: transform .2s, box-shadow .2s;
}
.hotel-card:hover {
  transform: translateY(-5px);
  box-shadow: 0 8px 20px #0000001f;
}
.hotel-card-image {
  width: 100%;
  height: 70%;
  object-fit: cover;
}
.hotel-card-info {
  padding: 15px;
  flex-grow: 1;
  display: flex;
  flex-direction: column;
}
.hotel-card-info strong {
  font-size: 18px;
  margin-bottom: 5px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.hotel-card-info span {
  font-size: 14px;
  color: #6b7280;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.hotel-details-view {
  background: #fff;
  border-radius: 12px;
  padding: 30px;
  display: flex;
  gap: 30px;
  border: 1px solid #e5e7eb;
}
.details-image {
  width: 400px;
  height: 400px;
  object-fit: cover;
  border-radius: 10px;
  flex-shrink: 0;
}
.details-info {
  display: flex;
  flex-direction: column;
}
.details-info h2 {
  margin-top: 0;
}
.details-info p {
  font-size: 16px;
  color: #374151;
  line-height: 1.6;
}
.details-actions {
  margin-top: auto;
  padding-top: 20px;
  display: flex;
  gap: 15px;
}
.details-actions button {
  padding: 12px 24px;
  font-size: 16px;
  font-weight: 700;
  border-radius: 8px;
  border: none;
  cursor: pointer;
  transition: background-color .2s;
}
.btn-edit {
  background-color: #3b82f6;
  color: #fff;
}
.btn-edit:hover {
  background-color: #2563eb;
}
.btn-delete {
  background-color: #ef4444;
  color: #fff;
}
.btn-delete:hover {
  background-color: #dc2626;
}
.btn-rooms {
  background-color: #10b981;
  color: #fff;
}
.btn-rooms:hover {
  background-color: #059669;
}

/* 객실 목록 */
.room-list {
  list-style: none;
  padding: 0;
  margin: 0;
}
.room-item {
  background: #fff;
  padding: 15px;
  margin-bottom: 10px;
  border-radius: 8px;
  border: 1px solid #e5e7eb;
  display: flex;
  align-items: center;
  gap: 15px;
}
.room-image {
  width: 120px;
  height: 90px;
  border-radius: 6px;
  object-fit: cover;
}
.room-info {
  flex: 1;
  display: flex;
  flex-direction: column;
}
.room-info strong {
  font-size: 16px;
}
.room-info span {
  font-size: 14px;
  color: #6b7280;
}
.actions button {
  margin-left: 5px;
  padding: 6px 10px;
  border: none;
  border-radius: 6px;
  cursor: pointer;
  font-size: 14px;
}
.actions button:first-child {
  background: #3b82f6;
  color: #fff;
}
.actions button:last-child {
  background: #ef4444;
  color: #fff;
}

/* 등록/수정 폼 */
.form-wrapper {
  max-width: 800px;
  margin: 0 auto;
}
.form-container {
  background: #fff;
  padding: 30px;
  border-radius: 12px;
  border: 1px solid #e5e7eb;
}
.form-group {
  margin-bottom: 20px;
}
.form-group label {
  display: block;
  font-size: 14px;
  font-weight: 600;
  margin-bottom: 8px;
  color: #374151;
}
.form-group input,
.form-group select,
.form-group textarea {
  width: 100%;
  padding: 12px;
  border: 1px solid #d1d5db;
  border-radius: 6px;
  font-size: 14px;
  box-sizing: border-box;
}
.form-group textarea {
  resize: vertical;
  min-height: 120px;
}
.inline-group {
  display: flex;
  gap: 10px;
}
.form-actions {
  margin-top: 30px;
  display: flex;
  justify-content: flex-end;
  gap: 12px;
}
.form-actions button {
  padding: 12px 20px;
  border-radius: 6px;
  border: none;
  cursor: pointer;
  font-size: 14px;
  font-weight: 700;
}
.btn-secondary {
  background: #e5e7eb;
  color: #374151;
}
.btn-secondary:hover {
  background: #d1d5db;
}

/* 이미지 및 편의시설 */
.file-input {
  width: 100%;
  padding: 8px;
  border: 1px solid #d1d5db;
  border-radius: 6px;
  background-color: white;
}
.image-preview-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(120px, 1fr));
  gap: 10px;
  margin-top: 15px;
}
.image-preview-item {
  position: relative;
  aspect-ratio: 4 / 3;
}
.image-preview-item img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  border-radius: 6px;
  border: 1px solid #e5e7eb;
}
.btn-remove-img {
  position: absolute;
  top: 5px;
  right: 5px;
  width: 24px;
  height: 24px;
  border-radius: 50%;
  border: none;
  background-color: rgba(0, 0, 0, 0.6);
  color: white;
  cursor: pointer;
  font-weight: bold;
  display: flex;
  align-items: center;
  justify-content: center;
  line-height: 1;
}
.amenities-grid {
  display: grid;
  grid-template-columns: repeat(5, 1fr);
  gap: 12px;
  background-color: #f9fafb;
  padding: 15px;
  border-radius: 8px;
  border: 1px solid #e5e7eb;
}
.amenity-item {
  display: flex;
  align-items: center;
  gap: 8px;
}
.amenity-item input[type="checkbox"] {
  width: 16px;
  height: 16px;
  cursor: pointer;
}
.amenity-item label {
  font-size: 14px;
  color: #374151;
  margin-bottom: 0;
  cursor: pointer;
}

/* 대시보드 */
.dashboard-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(250px, 1fr));
  gap: 25px;
  margin-bottom: 30px;
}
.stat-card {
  background: #fff;
  padding: 20px;
  border-radius: 12px;
  box-shadow: 0 4px 12px rgba(0,0,0,0.08);
}
.stat-card h4 {
  margin: 0 0 10px;
  font-size: 16px;
  color: #6b7280;
}
.stat-card p {
  margin: 0 0 10px;
  font-size: 28px;
  font-weight: 700;
  color: #111827;
}
.stat-card .comparison {
  font-size: 14px;
}
.comparison.positive { color: #10b981; }
.comparison.negative { color: #ef4444; }
.placeholder-chart {
    background: #fff;
    border-radius: 12px;
    padding: 20px;
    height: 300px;
    display: flex;
    align-items: center;
    justify-content: center;
    color: #9ca3af;
    font-size: 18px;
    border: 2px dashed #e5e7eb;
}

/* hotel-web/src/components/Owner/Ownerpage.vue */

/* ... (기존의 다른 스타일은 그대로 둡니다) ... */

/* ✅ [기존 예약관리 CSS를 지우고 아래 코드로 교체하세요] */
.reservations-section.compact {
  display: flex;
  flex-direction: column;
  height: 100%;
  padding: 30px 15px;
}
.top-filter-container {
  display: flex;
  align-items: center;
  gap: 15px; /* 필터 간 간격 */
  margin-bottom: 20px;
  background-color: #fff;
  padding: 15px;
  border-radius: 12px;
  box-shadow: 0 4px 12px rgba(0,0,0,0.08);
}
.top-filter-container label {
  font-weight: 600;
  font-size: 14px;
  margin-right: -5px; /* 라벨과 select 박스 간격 줄임 */
}
.top-filter-container .filter-select {
  width: auto; /* 자동으로 너비 조절 */
  min-width: 150px; /* 최소 너비 */
}
.reservation-sidebar .sidebar-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin: 0 0 20px;
  /* border-bottom: 1px solid #e5e7eb; */
}

.reservation-sidebar .sidebar-header h3 {
  margin: 0; /* h3의 기본 마진 제거 */
}
.btn-clear-filter {
  background: #e5e7eb;
  color: #374151;
  border: none;
  padding: 8px 14px;
  font-size: 12px;
  font-weight: 700;
  border-radius: 6px;
  cursor: pointer;
  transition: background-color .2s;
  margin-bottom: 10px;
}
.btn-clear-filter:hover {
  background: #d1d5db;
}
.reservations-content-compact {
  display: flex;
  gap: 15px;
  flex-grow: 1;
  overflow: hidden;
  height: 100%; /* 추가 */
}

.calendar-container {
  flex: 1;
  min-width: 0;
  background: #fff;
  padding: 20px;
  border-radius: 12px;
  box-shadow: 0 4px 12px rgba(0,0,0,0.08);
  display: flex;       /* 추가 */
  flex-direction: column; /* 추가 */
}
.calendar-container :deep(.fc) {
  height: 100%;
}

/* ✅ 캘린더 헤더 레이아웃 깨짐 방지 */
.calendar-container :deep(.fc-header-toolbar) {
    display: flex;
    justify-content: space-between;
}
.calendar-container :deep(.fc-toolbar-chunk) {
    display: flex;
    align-items: center;
}
.calendar-container :deep(.fc-toolbar-title) {
    flex-shrink: 1; /* 공간이 부족하면 타이틀 너비가 줄어들도록 설정 */
    margin: 0 1em; /* 좌우 여백 */
    font-size: 1.5em;
}
.reservation-sidebar {
  width: 320px;
  flex-shrink: 0;
  background: #fff;
  padding: 20px;
  border-radius: 12px;
  box-shadow: 0 4px 12px rgba(0,0,0,0.08);
  display: flex;
  flex-direction: column;
}
.reservation-sidebar h3 {
  margin: 0 0 20px;
  padding-bottom: 15px;
  border-bottom: 1px solid #e5e7eb;
}

.list-controls {
  display: flex;
  flex-direction: column;
  gap: 10px;
  margin-bottom: 20px;
}
.search-input, .filter-select {
  width: 100%;
  padding: 10px;
  border: 1px solid #d1d5db;
  border-radius: 6px;
  font-size: 14px;
}

.reservation-list {
  list-style: none;
  padding: 0;
  margin: 0;
  overflow-y: auto;
  flex-grow: 1;
} 

/* ❗️ .reservation-card, .status-badge 등 나머지 스타일은 이전과 동일하므로 그대로 사용하시면 됩니다. */
.reservation-card {
  background: #f9fafb;
  padding: 15px;
  border-radius: 8px;
  margin-bottom: 10px;
  cursor: pointer;
  transition: background-color .2s, box-shadow .2s;
  border: 1px solid #e5e7eb;
}
.reservation-card:hover {
  background-color: #f3f4f6;
  box-shadow: 0 2px 4px rgba(0,0,0,0.05);
}
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 10px;
}
.card-header strong {
  font-size: 16px;
}
.status-badge {
  padding: 4px 10px;
  border-radius: 12px;
  font-size: 12px;
  font-weight: 700;
  color: #fff;
}
.status-badge.completed { background-color: #10b981; }
.status-badge.pending { background-color: #f59e0b; }
.status-badge.cancelled { background-color: #6b7280; }
.card-body p {
  margin: 0;
  font-size: 14px;
  color: #4b5563;
}
.no-reservations {
  text-align: center;
  padding: 40px;
  color: #9ca3af;
}

/* Modal Styles */
.modal-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background-color: rgba(0,0,0,0.6);
  display: flex;
  justify-content: center;
  align-items: center;
  z-index: 1000;
}
.modal-content {
  background: white;
  padding: 30px;
  border-radius: 12px;
  width: 90%;
  max-width: 600px;
  position: relative;
}
.modal-close-btn {
  position: absolute;
  top: 15px;
  right: 15px;
  background: none;
  border: none;
  font-size: 24px;
  cursor: pointer;
  color: #6b7280;
}
.modal-content h3 {
  margin: 0 0 20px;
}
.modal-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 15px;
}
.modal-item {
  display: flex;
  flex-direction: column;
  background-color: #f9fafb;
  padding: 10px;
  border-radius: 6px;
}
.modal-item strong {
  font-size: 12px;
  color: #6b7280;
  margin-bottom: 4px;
}
.modal-item span {
  font-size: 15px;
}
.modal-item.full-width {
  grid-column: 1 / -1;
}
.modal-actions {
  margin-top: 30px;
  display: flex;
  justify-content: flex-end;
  gap: 12px;
}
.btn-danger { 
  background-color: #ef4444; color: #fff;
  border-radius: 6px;
  padding: 8px 14px;
 }

.btn-danger.disabled {
  background-color: #9ca3af; /* 회색 */
  cursor: not-allowed;
}

.btn-danger.disabled:hover {
  background-color: #9ca3af; /* 호버 시에도 색상 유지 */
}

.sidebar {
  display: flex;
  flex-direction: column; /* 아이템을 세로로 배치 */
}
.sidebar nav {
  flex-grow: 1; /* nav가 남는 공간을 모두 차지하여 footer를 아래로 밀어냄 */
}

/* 사이드바 하단 버튼 영역 스타일 */
.sidebar-footer {
  padding: 10px;
  display: flex;
  flex-direction: column;
  gap: 10px; /* 버튼 사이 간격 */
}

/* 하단 버튼 공통 스타일 */
.sidebar-footer button {
  width: 100%;
  padding: 12px;
  border: none;
  border-radius: 6px;
  color: white;
  font-size: 15px;
  font-weight: 700;
  cursor: pointer;
  transition: background-color 0.2s;
}

/* 홈페이지 버튼 스타일 */
.btn-homepage {
  background-color: #4B5563; /* 회색 계열 */
}
.btn-homepage:hover {
  background-color: #374151;
}

/* 로그아웃 버튼 스타일 */
.btn-logout-sidebar {
  background-color: #a92a2a; /* 어두운 빨간색 */
}

.btn-logout-sidebar:hover {
  background-color: #8a2020; /* 더 어두운 빨간색 */
}
.reviews-section {
  display: flex;
  flex-direction: column;
}
.review-controls {
  display: flex;
  gap: 15px;
  margin-bottom: 25px;
  background-color: #fff;
  padding: 15px;
  border-radius: 12px;
  box-shadow: 0 4px 12px rgba(0,0,0,0.08);
}
.review-list {
  display: grid;
  grid-template-columns: 1fr;
  gap: 20px;
}
.review-card {
  display: flex;
  background: #fff;
  border-radius: 12px;
  box-shadow: 0 4px 12px rgba(0,0,0,0.08);
  padding: 20px;
  gap: 20px;
  cursor: pointer;
  transition: transform .2s, box-shadow .2s;
}
.review-card:hover {
  transform: translateY(-5px);
  box-shadow: 0 8px 20px #0000001f;
}
.review-image {
  width: 120px;
  height: 120px;
  object-fit: cover;
  border-radius: 8px;
  flex-shrink: 0;
}
.review-content {
  flex-grow: 1;
  display: flex;
  flex-direction: column;
}
.review-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 10px;
}
.review-hotel {
  font-weight: 700;
  font-size: 16px;
}
.star-rating span {
  color: #d1d5db;
  font-size: 18px;
}
.star-rating span.filled {
  color: #f59e0b;
}
.review-text {
  margin: 0;
  color: #4b5563;
  line-height: 1.5;
  flex-grow: 1;
  /* 여러 줄 말줄임표 처리 */
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;  
  overflow: hidden;
}
.review-footer {
  margin-top: 15px;
  font-size: 13px;
  color: #9ca3af;
  display: flex;
  align-items: center;
  gap: 15px;
}
.reply-badge {
  background-color: #10b981;
  color: white;
  padding: 3px 8px;
  border-radius: 12px;
  font-size: 12px;
  font-weight: 700;
}
.no-reviews {
  text-align: center;
  padding: 40px;
  color: #9ca3af;
  background: #fff;
  border-radius: 12px;
}


/* 리뷰 상세 모달 */
.review-modal .review-detail-content {
  background: #f9fafb;
  padding: 20px;
  border-radius: 8px;
  margin-bottom: 25px;
}
.review-detail-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 15px;
}
.author-info {
  display: flex;
  flex-direction: column;
}
.author-info strong { font-size: 16px; }
.author-info span { font-size: 13px; color: #6b7280; }
.star-rating.large span { font-size: 22px; }

.review-detail-text {
  line-height: 1.6;
  margin-bottom: 15px;
}
.review-detail-image {
  width: 100%;
  max-height: 300px;
  object-fit: cover;
  border-radius: 8px;
}
.reply-section h4 {
  margin: 0 0 10px;
}
.reply-section textarea {
  width: 100%;
  min-height: 100px;
  padding: 10px;
  border: 1px solid #d1d5db;
  border-radius: 6px;
  resize: vertical;
}
.reply-actions {
  margin-top: 15px;
  text-align: right;
}
.chart-container {
  background: #fff;
  padding: 25px;
  border-radius: 12px;
  box-shadow: 0 4px 12px rgba(0,0,0,0.08);
  margin-bottom: 30px;
}
.chart-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
  gap: 20px;
}
.chart-title-group {
  display: flex;
  align-items: center;
  gap: 15px; /* 제목과 필터 그룹 사이 간격 */
  flex-grow: 1; /* 남는 공간 차지 */
}
.chart-header h3 {
  margin: 0;
  font-size: 18px;
  white-space: nowrap; /* 제목이 줄바꿈되지 않도록 */
}
.chart-main-filters {
  display: flex;
  align-items: center;
  gap: 10px;
}

.chart-period-filters {
  display: flex;
  gap: 10px;
  flex-shrink: 0; 
}

.filter-select.small {
  padding: 8px 12px;
  font-size: 14px;
  height: 38px; /* 다른 버튼들과 높이를 맞춤 */
  border: 1px solid #d1d5db;
  border-radius: 6px;
  background-color: #fff;
}

/* flat-pickr 컴포넌트의 내부 input에 스타일을 적용하기 위해 :deep() 사용 */
:deep(.date-picker-placeholder.small) {
  padding: 8px 12px;
  font-size: 14px;
  height: 38px;
  border: 1px solid #d1d5db;
  border-radius: 6px;
  background-color: #fff;
  min-width: 260px;
}
.filter-btn.reset-btn {
  background: #6b7280; /* 회색 계열 */
  color: white;
  border-color: #6b7280;
}
.filter-btn.reset-btn:hover {
  background: #4b5563; /* 더 진한 회색 */
  border-color: #4b5563;
}

.filter-btn {
  background: #f3f4f6;
  border: 1px solid #e5e7eb;
  padding: 8px 14px;
  border-radius: 6px;
  cursor: pointer;
  font-size: 14px;
}
.filter-btn:hover {
  background: #3b82f6;
  color: white;
  border-color: #3b82f6;
}
.chart-placeholder {
  background: #f9fafb;
  border-radius: 8px;
  height: 300px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #9ca3af;
  font-size: 16px;
}

/* 대시보드 하단 그리드 */
.dashboard-grid.secondary {
  grid-template-columns: repeat(auto-fit, minmax(300px, 1fr));
}

/* 정보 카드 (체크인/아웃, 최근활동 등) */
.info-card {
  background: #fff;
  padding: 25px;
  border-radius: 12px;
  box-shadow: 0 4px 12px rgba(0,0,0,0.08);
}
.info-card h4 {
  margin: 0 0 20px;
  padding-bottom: 15px;
  border-bottom: 1px solid #e5e7eb;
  font-size: 16px;
}
.check-in-out-tabs {
  display: flex;
  margin-bottom: 15px;
  border-radius: 8px;
  background: #f3f4f6;
  padding: 5px;
}
.check-in-out-tabs button {
  flex: 1;
  padding: 8px;
  border: none;
  background: transparent;
  border-radius: 6px;
  cursor: pointer;
  font-weight: 600;
  transition: background-color .2s, color .2s;
}
.check-in-out-tabs button.active {
  background: #fff;
  color: #3b82f6;
  box-shadow: 0 2px 4px rgba(0,0,0,0.1);
}
.guest-list, .activity-list {
  list-style: none;
  padding: 0;
  margin: 0;
}
.guest-list {
  max-height: 225px; /* 4명 분량의 높이 (li 1개당 약 45px) */
  overflow-y: auto;
}

.activity-list {
  max-height: 280px; /* 5명 분량의 높이 (li 1개당 약 56px) */
  overflow-y: auto;
}
.guest-list li, .activity-list li {
  display: flex;
  justify-content: space-between;
  padding: 10px 5px;
  border-bottom: 1px solid #f3f4f6;
}
.guest-list li:last-child, .activity-list li:last-child {
  border-bottom: none;
}
.guest-list .room-type {
  color: #6b7280;
  font-size: 14px;
}
.activity-list {
  display: flex;
  flex-direction: column;
  gap: 15px;
}
.activity-list li {
  flex-direction: column;
  align-items: flex-start;
  padding: 0;
  border: none;
}
.activity-list p {
  margin: 0 0 5px;
  font-size: 14px;
}
.activity-list .time-ago, .activity-list .star-rating {
  font-size: 12px;
  color: #9ca3af;
}
.star-rating.small span { font-size: 14px; }
.star-rating.small span.filled { color: #f59e0b; }
.star-rating.small span:not(.filled) { color: #d1d5db; }
/* 드래그 앤 드롭 영역 스타일 */
.draggable-area {
  cursor: grab;
}

/* 드래그 중인 아이템의 원래 자리를 표시하는 스타일 */
.ghost {
  opacity: 0.5;
  background: #c8ebfb;
}

/* 이미지 아이템 위에 '대표' 배지 스타일 */
.main-photo-badge {
  position: absolute;
  top: 5px;
  left: 5px;
  background-color: #3b82f6;
  color: white;
  padding: 3px 8px;
  border-radius: 12px;
  font-size: 12px;
  font-weight: 700;
  z-index: 2;
}
</style>
