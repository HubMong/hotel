<template>
  <div class="hotel-management">
    <div class="page-header">
      <h1>호텔 및 객실 관리</h1>
      <div class="header-actions">
        <button class="btn btn-outline" @click="toggleInventoryCalendar">
          재고 달력
        </button>
      </div>
    </div>


    <!-- 마스터-디테일 메인 영역 -->
    <div class="master-detail-container"> 
      <!-- 왼쪽: 호텔 목록 (마스터) -->
      <div class="master-panel">
        <div class="master-header">
          <h3>호텔 목록</h3>
          <div class="master-controls">
            <input
              v-model="filters.name"
              type="text"
              placeholder="호텔명 검색..."
              class="search-input-compact"
              @input="debounceSearch"
            />
            <select v-model="filters.status" @change="loadHotels" class="status-select-compact">
              <option value="">전체</option>
              <option value="APPROVED">운영중</option>
              <option value="PENDING">승인대기</option>
              <option value="SUSPENDED">정지</option>
            </select>
          </div>
        </div>

        <div class="hotel-list">
          <div v-if="loading" class="loading-master">호텔 목록 로딩 중...</div>
          <div v-else-if="!hotels.content || hotels.content.length === 0" class="no-data-master">
            등록된 호텔이 없습니다.
          </div>
          <div v-else>
            <div 
              v-for="hotel in hotels.content" 
              :key="hotel.id" 
              class="hotel-item"
              :class="{ 
                active: selectedHotel?.id === hotel.id,
                pending: hotel.status === 'PENDING',
                suspended: hotel.status === 'SUSPENDED'
              }"
              @click="selectHotel(hotel)"
            >
              <div class="hotel-item-header">
                <div class="hotel-basic-info">
                  <h4 class="hotel-name">{{ hotel.name }}</h4>
                  <p class="hotel-location">{{ hotel.city }}</p>
                </div>
                <div class="hotel-status-badge">
                  <span :class="['status-indicator', `status-${hotel.status?.toLowerCase()}`]">
                    {{ getStatusLabel(hotel.status) }}
                  </span>
                </div>
              </div>
              
              <div class="hotel-item-meta">
                <div class="meta-stats">
                  <span class="stat-item">{{ hotel.roomCount || 0 }}개 객실</span>
                  <span class="stat-item">{{ hotel.reservationCount || 0 }}건 예약</span>
                  <span class="stat-item" v-if="hotel.averageRating > 0">{{ hotel.averageRating.toFixed(1) }}점</span>
                </div>
                <div class="hotel-revenue">
                  {{ formatCurrency(hotel.totalRevenue || 0) }}
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- 오른쪽: 객실 상세 (디테일) -->
      <div class="detail-panel" v-if="selectedHotel">
        <div class="detail-header">
          <div class="detail-title">
            <h3>{{ selectedHotel.name }} - 객실 관리</h3>
            <p>{{ selectedHotel.address }}</p>
          </div>
          <div class="detail-actions">
            <button class="btn btn-sm btn-outline" @click="addNewRoom">객실 추가</button>
            <button class="btn btn-sm btn-primary" @click="loadRooms">새로고침</button>
          </div>
        </div>

        <!-- 객실 상태 요약 -->
        <div class="room-status-summary">
          <div class="status-item available">
            <div class="status-count">{{ getRoomStatusCount('available') }}</div>
            <div class="status-label">예약 가능</div>
          </div>
          <div class="status-item occupied">
            <div class="status-count">{{ getRoomStatusCount('occupied') }}</div>
            <div class="status-label">예약 중</div>
          </div>
          <div class="status-item maintenance">
            <div class="status-count">{{ getRoomStatusCount('maintenance') }}</div>
            <div class="status-label">점검 중</div>
          </div>
          <div class="status-item cleaning">
            <div class="status-count">{{ getRoomStatusCount('cleaning') }}</div>
            <div class="status-label">청소 중</div>
          </div>
        </div>

        <!-- 객실 목록 -->
        <div class="rooms-section">
          <div class="rooms-header">
            <h4>객실 목록</h4>
            <div class="rooms-view-toggle">
              <button 
                class="btn btn-sm" 
                :class="{ active: roomViewMode === 'grid' }" 
                @click="roomViewMode = 'grid'"
              >
                그리드
              </button>
              <button 
                class="btn btn-sm" 
                :class="{ active: roomViewMode === 'list' }" 
                @click="roomViewMode = 'list'"
              >
                목록
              </button>
            </div>
          </div>

          <div v-if="roomsError" class="no-data-master" style="margin: 8px 0; color: #c0392b; font-weight: 500;">
            {{ roomsError }}
          </div>

          <!-- 그리드 뷰 -->
          <div v-if="roomViewMode === 'grid'" class="rooms-grid">
            <div 
              v-for="room in selectedHotelRooms" 
              :key="room.id"
              class="room-card"
              :class="`status-${roomDisplayStatus(room)}`"
            >
              <div class="room-card-image" v-if="room.imageUrl">
                <img :src="room.imageUrl" :alt="`${room.roomNumber} 이미지`" />
              </div>
              <div class="room-card-image no-image-grid" v-else>
                <span>📷</span>
              </div>
              <div class="room-card-header">
                <span class="room-number">{{ room.roomNumber }}</span>
                <span class="room-type">{{ room.roomType }}</span>
              </div>
              <div class="room-card-size" v-if="room.roomSize">
                <span class="size-text">{{ room.roomSize }}</span>
              </div>
              <div class="room-card-status">
                <div :class="['status-dot', `status-${roomDisplayStatus(room)}`]"></div>
                <span class="status-text">{{ getRoomStatusLabel(roomDisplayStatus(room)) }}</span>
              </div>
              <div class="room-card-info">
                <div class="room-price">{{ formatCurrency(room.price) }}/박</div>
                <div class="room-capacity">{{ room.capacity }}명</div>
              </div>
              <div class="room-card-actions">
                <button class="action-btn-small" @click="editRoom(room)" title="편집">편집</button>
                <select 
                  class="status-select-small" 
                  :value="roomDisplayStatus(room)" 
                  @change="updateRoomStatus(room, $event.target.value)"
                >
                  <option value="available">가능</option>
                  <option value="occupied">예약중</option>
                  <option value="maintenance">점검</option>
                  <option value="cleaning">청소</option>
                </select>
              </div>
            </div>
          </div>

          <!-- 리스트 뷰 -->
          <table v-else class="rooms-table">
            <thead>
              <tr>
                <th>이미지</th>
                <th>객실번호</th>
                <th>타입</th>
                <th>객실크기</th>
                <th>상태</th>
                <th>수용인원</th>
                <th>가격</th>
                <th>정가</th>
                <th>특징</th>
                <th>편의시설</th>
                <th>관리</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="room in selectedHotelRooms" :key="room.id" :class="`status-${roomDisplayStatus(room)}`">
                <td class="image-cell">
                  <img v-if="room.imageUrl" :src="room.imageUrl" alt="객실 이미지" class="room-thumbnail" />
                  <span v-else class="no-image">📷</span>
                </td>
                <td class="room-number-cell">{{ room.roomNumber }}</td>
                <td>{{ room.roomType }}</td>
                <td>{{ room.roomSize }}</td>
                <td>
                  <div class="status-cell">
                    <div :class="['status-dot', `status-${roomDisplayStatus(room)}`]"></div>
                    {{ getRoomStatusLabel(roomDisplayStatus(room)) }}
                  </div>
                </td>
                <td>{{ room.capacity }}명</td>
                <td class="price-cell">{{ formatCurrency(room.price) }}</td>
                <td class="price-cell">{{ formatCurrency(room.originalPrice || room.price) }}</td>
                <td class="amenities-cell">
                  <div class="amenities-list">
                    <span class="amenity-tag" :class="{ positive: room.hasWindow, negative: !room.hasWindow }">창문 {{ room.hasWindow ? '있음' : '없음' }}</span>
                    <span class="amenity-tag" :class="{ positive: room.aircon, negative: !room.aircon }">에어컨</span>
                    <span class="amenity-tag" :class="{ positive: room.wifi, negative: !room.wifi }">와이파이</span>
                    <span class="amenity-tag" :class="{ positive: room.freeWater, negative: !room.freeWater }">무료 생수</span>
                    <span class="amenity-tag" :class="{ positive: !room.sharedBath, negative: room.sharedBath }">개인 욕실</span>
                    <span class="amenity-tag" :class="{ positive: !room.smoke, negative: room.smoke }">금연</span>
                    <span v-if="room.bed" class="amenity-tag">침대: {{ room.bed }}</span>
                    <span v-if="room.bath" class="amenity-tag">욕실: {{ room.bath }}</span>
                  </div>
                </td>
                <td class="amenities-cell">
                  <div class="amenities-list">
                    <span v-for="amenity in room.amenities" :key="amenity" class="amenity-tag">{{ amenity }}</span>
                    <span v-if="!room.amenities || room.amenities.length === 0" class="amenity-tag negative">추가 편의 없음</span>
                  </div>
                </td>
                <td class="actions-cell">
                  <div class="room-actions">
                    <button class="action-btn-small" @click="editRoom(room)" title="편집">편집</button>
                    <select 
                      class="status-select-small" 
                      :value="roomDisplayStatus(room)" 
                      @change="updateRoomStatus(room, $event.target.value)"
                    >
                      <option value="available">가능</option>
                      <option value="occupied">예약중</option>
                      <option value="maintenance">점검</option>
                      <option value="cleaning">청소</option>
                    </select>
                  </div>
                </td>
              </tr>
            </tbody>
          </table>
        </div>

        <!-- 재고 달력 -->
        <div v-if="showInventoryCalendar" class="inventory-section">
          <div class="inventory-header">
            <div class="left">
              <button class="btn btn-sm" @click="prevMonth" :disabled="invLoading">이전 달</button>
              <strong class="month-label">{{ formatMonth(invMonth) }}</strong>
              <button class="btn btn-sm" @click="nextMonth" :disabled="invLoading">다음 달</button>
            </div>
            <div class="right">
              <button class="btn btn-sm btn-outline" @click="openRoomFilterModal">설정</button>
              <button class="btn btn-sm btn-primary" @click="reloadInventory" :disabled="invLoading">새로고침</button>
              <span v-if="roomIdFilter" class="filter-chip">객실 필터: #{{ roomIdFilter }}</span>
              <!-- 객실 상태 범례 (오른쪽 상단) -->
              <div class="calendar-legend" aria-label="객실 상태 설명">
                <span class="legend-item"><span class="inv-dot inv-dot-available" aria-hidden="true"></span> 영업</span>
                <span class="legend-item"><span class="inv-dot inv-dot-maintenance" aria-hidden="true"></span> 점검</span>
                <span class="legend-item"><span class="inv-dot inv-dot-cleaning" aria-hidden="true"></span> 청소</span>
                <span class="legend-item"><span class="inv-dot inv-dot-closed" aria-hidden="true"></span> 예약</span>
              </div>
            </div>
          </div>

          <div class="inventory-legend">
            <span class="legend-item"><span class="legend-dot ok"></span> 여유</span>
            <span class="legend-item"><span class="legend-dot tight"></span> 한계 근접</span>
            <span class="legend-item"><span class="legend-dot full"></span> 만실</span>
            <span class="legend-item"><span class="legend-dot over"></span> 초과</span>
          </div>

          <div v-if="invLoading" class="inventory-loading">
            <span>달력 데이터를 불러오는 중...</span>
            <small v-if="invProgress.totalPages"> ({{ invProgress.page + 1 }} / {{ invProgress.totalPages }})</small>
          </div>
          <div v-else class="calendar-grid">
            <div class="calendar-header">
              <div class="cell">일</div>
              <div class="cell">월</div>
              <div class="cell">화</div>
              <div class="cell">수</div>
              <div class="cell">목</div>
              <div class="cell">금</div>
              <div class="cell">토</div>
            </div>
            <div class="calendar-body">
              <div v-for="d in invDays" :key="d.key" class="day-cell" :class="[d.outside ? 'outside' : '', dayStatusClass(d.date)]" @click="openDayRoomModal(d.date)" title="객실 상태 조정">
                <div class="day-number">{{ d.date.getDate() }}</div>
                <div class="day-meta">
                  <div class="count">{{ getOccupancy(d.date) }} / {{ getEffectiveCapacity(d.date) }}</div>
                  <div class="status-text">{{ dayStatusText(d.date) }}</div>
                </div>
                <!-- 객실 상태 요약 표시 -->
                <div class="status-mini" style="margin-top:4px; display:flex; gap:6px; flex-wrap:wrap; align-items:center;">
                  <template v-if="roomIdFilter">
                    <span class="mini-chip"
                          :class="'status-' + overrideToRoomStatusClass(daySingleRoomOverride[formatDateYMD(d.date)] || selectedRoomStatus || 'available')"
                          style="display:inline-flex;align-items:center;gap:4px;font-size:11px;opacity:.9;">
                      <span class="status-dot"
                            :class="'status-' + overrideToRoomStatusClass(daySingleRoomOverride[formatDateYMD(d.date)] || selectedRoomStatus || 'available')"></span>
                      {{ daySingleRoomOverride[formatDateYMD(d.date)]
                        ? getOverrideStatusLabel(daySingleRoomOverride[formatDateYMD(d.date)])
                        : getRoomStatusLabel(selectedRoomStatus || 'available')
                      }}
                    </span>
                  </template>
                  <template v-else>
                    <template v-if="dayOverrideCounts(d.date).closed">
                      <span class="mini-chip status-occupied" style="display:inline-flex;align-items:center;gap:4px;font-size:11px;opacity:.9;">
                        <span class="status-dot status-occupied"></span> {{ dayOverrideCounts(d.date).closed }} 예약
                      </span>
                    </template>
                    <template v-if="dayOverrideCounts(d.date).maintenance">
                      <span class="mini-chip status-maintenance" style="display:inline-flex;align-items:center;gap:4px;font-size:11px;opacity:.9;">
                        <span class="status-dot status-maintenance"></span> {{ dayOverrideCounts(d.date).maintenance }} 점검
                      </span>
                    </template>
                    <template v-if="dayOverrideCounts(d.date).cleaning">
                      <span class="mini-chip status-cleaning" style="display:inline-flex;align-items:center;gap:4px;font-size:11px;opacity:.9;">
                        <span class="status-dot status-cleaning"></span> {{ dayOverrideCounts(d.date).cleaning }} 청소
                      </span>
                    </template>
                    <template v-if="dayOverrideCounts(d.date).open && dayOverrideCounts(d.date).open > 0">
                      <span class="mini-chip status-available" style="display:inline-flex;align-items:center;gap:4px;font-size:11px;opacity:.9;">
                        <span class="status-dot status-available"></span> {{ dayOverrideCounts(d.date).open }} 영업
                      </span>
                    </template>
                  </template>
                </div>
              </div>
            </div>
          </div>
          <div v-if="invError" class="inventory-error">{{ invError }}</div>
        </div>
      </div>

      <!-- 선택된 호텔이 없을 때 -->
      <div v-else class="detail-empty">
        <div class="empty-state">
          <div class="empty-icon">🏨</div>
          <h3>호텔을 선택해주세요</h3>
          <p>왼쪽 목록에서 호텔을 클릭하면<br>객실 정보를 확인할 수 있습니다.</p>
        </div>
      </div>

      <!-- 객실 필터 모달 -->
      <div v-if="showRoomFilterModal" class="modal-overlay" @click="closeRoomFilterModal">
        <div class="modal-content" @click.stop>
          <div class="modal-header">
            <h3>객실 필터 설정</h3>
            <button class="modal-close" @click="closeRoomFilterModal">&times;</button>
          </div>
          <div class="modal-body">
            <div class="filter-options">
              <div class="option-group">
                <label class="option-label">
                  <input type="radio" :value="null" v-model="tempRoomFilter" />
                  <span class="option-text">전체 객실</span>
                </label>
              </div>
              <div class="option-group">
                <label class="option-label">
                  <input type="radio" :value="'selected'" v-model="tempRoomFilter" />
                  <span class="option-text">선택한 객실만</span>
                </label>
              </div>
            </div>
            
            <div v-if="tempRoomFilter === 'selected'" class="room-selection">
              <h4>객실 선택</h4>
              <div class="room-list">
                <div v-if="selectedHotelRooms.length === 0" class="no-rooms">
                  <p>선택된 호텔의 객실이 없습니다.</p>
                </div>
                <div v-else class="room-grid">
                  <label v-for="room in selectedHotelRooms" :key="room.id" class="room-option">
                    <input type="radio" :value="room.id" v-model="selectedRoomId" name="roomSelection" />
                    <div class="room-card-small">
                      <div class="room-number">#{{ room.roomNumber }}</div>
                      <div class="room-type">{{ room.roomType }}</div>
                      <div class="room-status" :class="'status-' + room.status">
                        {{ getStatusText(room.status) }}
                      </div>
                    </div>
                  </label>
                </div>
              </div>
            </div>
          </div>
          <div class="modal-footer">
            <button class="btn btn-outline" @click="closeRoomFilterModal">취소</button>
            <button class="btn btn-primary" @click="applyRoomFilter" :disabled="tempRoomFilter === 'selected' && !selectedRoomId">
              적용
            </button>
          </div>
        </div>
      </div>

      <div v-if="showRoomEditor" class="modal-overlay" @click="closeRoomEditor">
        <div class="modal-content room-editor" @click.stop>
          <div class="modal-header">
            <h3>{{ editingRoom?.id ? '객실 편집' : '새 객실 추가' }}</h3>
            <button class="modal-close" @click="closeRoomEditor">&times;</button>
          </div>
          <div class="modal-body">
            <div class="form-grid">
              <label>
                객실 번호
                <input type="text" v-model="editingRoom.roomNumber" placeholder="예: 101" />
              </label>
              <label>
                객실 타입
                <select v-model="editingRoom.roomType">
                  <option value="스위트룸">스위트룸</option>
                  <option value="디럭스룸">디럭스룸</option>
                  <option value="스탠다드룸">스탠다드룸</option>
                  <option value="싱글룸">싱글룸</option>
                  <option value="트윈룸">트윈룸</option>
                </select>
              </label>
              <label class="input-with-unit-label">
                객실 크기
                <div class="input-with-unit">
                  <input
                    type="text"
                    v-model="editingRoom.roomSizeValue"
                    placeholder="예: 25"
                    inputmode="decimal"
                  />
                  <span class="unit-label">㎡</span>
                </div>
              </label>
              <label>
                상태
                <select v-model="editingRoom.status">
                  <option value="available">가능</option>
                  <option value="occupied">예약중</option>
                  <option value="maintenance">점검</option>
                  <option value="cleaning">청소</option>
                </select>
              </label>
              <label>
                수용 인원
                <input type="number" min="1" v-model.number="editingRoom.capacity" />
              </label>
              <label>
                가격 (1박)
                <input type="number" min="0" v-model.number="editingRoom.price" />
              </label>
              <label>
                정가
                <input type="number" min="0" v-model.number="editingRoom.originalPrice" />
              </label>
              <label>
                침대 정보
                <input type="text" v-model="editingRoom.bed" />
              </label>
              <label>
                욕실 수
                <input type="number" min="0" v-model.number="editingRoom.bath" />
              </label>
              <label>
                흡연 가능
                <select v-model="editingRoom.smoke">
                  <option :value="false">불가</option>
                  <option :value="true">가능</option>
                </select>
              </label>
              <label>
                창문 여부
                <select v-model="editingRoom.hasWindow">
                  <option :value="false">없음</option>
                  <option :value="true">있음</option>
                </select>
              </label>
              <label>
                에어컨
                <select v-model="editingRoom.aircon">
                  <option :value="false">없음</option>
                  <option :value="true">있음</option>
                </select>
              </label>
              <label>
                와이파이
                <select v-model="editingRoom.wifi">
                  <option :value="false">없음</option>
                  <option :value="true">있음</option>
                </select>
              </label>
              <label>
                무료 생수
                <select v-model="editingRoom.freeWater">
                  <option :value="false">없음</option>
                  <option :value="true">있음</option>
                </select>
              </label>
              <label>
                공용 욕실
                <select v-model="editingRoom.sharedBath">
                  <option :value="false">아니오</option>
                  <option :value="true">예</option>
                </select>
              </label>
              <label class="span-2">
                편의시설 (쉼표 구분)
                <textarea rows="3" v-model="editingRoom.amenitiesText" placeholder="예: TV, 미니바, 커피머신"></textarea>
              </label>
              <label class="span-2">
                객실 이미지
                <div class="image-upload-container">
                  <div class="image-input-wrapper">
                    <input 
                      type="file" 
                      accept="image/*" 
                      @change="handleImageChange" 
                      ref="imageInput"
                      class="image-input-file"
                      id="roomImageInput"
                    />
                    <label for="roomImageInput" class="image-input-label">
                      📁 파일 선택
                    </label>
                    <span class="image-filename" v-if="editingRoom.imageFile">
                      {{ editingRoom.imageFile.name }}
                    </span>
                  </div>
                  <div v-if="editingRoom.imageUrl" class="image-preview">
                    <img :src="editingRoom.imageUrl" alt="미리보기" @error="handleImageError" />
                    <button type="button" class="remove-image" @click="removeImage">×</button>
                  </div>
                  <input 
                    type="text" 
                    v-model="editingRoom.imageUrlInput"
                    @input="handleImageUrlChange"
                    placeholder="또는 이미지 URL을 직접 입력하세요"
                    class="image-url-input"
                    style="margin-top: 8px;"
                  />
                  <small class="image-help-text">
                    💡 이미지 파일을 선택하거나 URL을 직접 입력할 수 있습니다. (최대 5MB)
                  </small>
                </div>
              </label>
            </div>
          </div>
          <div class="modal-footer">
            <div class="footer-left">
              <button 
                v-if="editingRoom?.id" 
                class="btn btn-danger" 
                @click="deleteRoom" 
                :disabled="roomSaving"
              >
                삭제
              </button>
            </div>
            <div class="footer-right">
              <button class="btn btn-outline" @click="closeRoomEditor">취소</button>
              <button class="btn btn-primary" @click="saveRoom" :disabled="roomSaving">
                {{ roomSaving ? '저장 중...' : '저장' }}
              </button>
            </div>
          </div>
        </div>
      </div>

      <!-- 재고 달력 일 클릭: 객실 상태 조정 모달 -->
      <div v-if="showDayRoomModal" class="modal-overlay" @click="closeDayRoomModal">
        <div class="modal-content" @click.stop>
          <div class="modal-header">
            <h3>
              {{ selectedHotel?.name }} - 객실 상태 조정
              <small style="font-weight:normal;opacity:.8">{{ formatDateYMD(selectedInvDate) }}</small>
            </h3>
            <button class="modal-close" @click="closeDayRoomModal">&times;</button>
          </div>
          <div class="modal-body">
            <div v-if="!selectedHotelRooms || selectedHotelRooms.length === 0" class="no-data-small">
              선택된 호텔의 객실이 없습니다.
            </div>
            <table v-else class="rooms-table compact">
              <thead>
                <tr>
                  <th style="width:90px">객실번호</th>
                  <th style="width:120px">타입</th>
                  <th style="width:160px">상태</th>
                  <th>가격</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="room in selectedHotelRooms" :key="room.id">
                  <td class="room-number-cell">{{ room.roomNumber }}</td>
                  <td>{{ room.roomType }}</td>
                  <td>
                    <div class="status-cell">
                      <div :class="['status-dot', `status-${roomStatusFor(room)}`]"></div>
                      <select class="status-select-small" :value="roomStatusFor(room)" @change="onEditRoomStatus(room, $event.target.value)">
                        <option value="available">가능</option>
                        <option value="occupied">예약중</option>
                        <option value="maintenance">점검</option>
                        <option value="cleaning">청소</option>
                      </select>
                    </div>
                  </td>
                  <td class="price-cell">{{ formatCurrency(room.price) }}</td>
                </tr>
              </tbody>
            </table>
          </div>
          <div class="modal-footer">
            <div class="footer-left">
              <button class="btn btn-outline" @click="resetRoomStatusEdits" :disabled="savingStatuses">변경 취소</button>
            </div>
            <div class="footer-right">
              <button class="btn btn-secondary" @click="closeDayRoomModal" :disabled="savingStatuses">닫기</button>
              <button class="btn btn-primary" @click="saveDayRoomStatuses" :disabled="savingStatuses || pendingChangesCount === 0">
                {{ savingStatuses ? '저장 중...' : `변경사항 저장 (${pendingChangesCount})` }}
              </button>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import api from '../../api/http'

export default {
  name: 'AdminHotelManagement',
  data() {
    return {
      // 기존 데이터
      loading: false,
      hotels: { content: [], totalPages: 0, number: 0 },
      filters: {
        name: '',
        status: '',
        city: ''
      },
      stats: {
        totalHotels: 0,
        totalRooms: 0,
        availableRooms: 0,
        occupiedRooms: 0,
        maintenanceRooms: 0,
        pendingHotels: 0,
        approvedHotels: 0,
        totalRevenue: 0
      },
      
      // 새로운 마스터-디테일 기능
      selectedHotel: null,
      selectedHotelRooms: [],
  roomsError: '',
      roomViewMode: 'grid', // 'grid' 또는 'list'
      editingRoom: null,
      showRoomEditor: false,
      roomSaving: false,
      
      // UI 상태
      showInventoryCalendar: false,
  // 재고 달력 상태
      invLoading: false,
      invError: '',
      invMonth: new Date(),
      invDays: [],
      invCapacity: 0,
      occupancyMap: {}, // key: yyyy-mm-dd, value: number
  // per-date overrides state
  dayBlockedCount: {}, // key: yyyy-mm-dd -> number of rooms blocked (closed/maintenance/cleaning)
  dayOverrideCountsMap: {}, // key: yyyy-mm-dd -> { open, closed, maintenance, cleaning }
  daySingleRoomOverride: {}, // key: yyyy-mm-dd -> override status for selected single room
  dayRoomOverrides: {}, // key: yyyy-mm-dd -> { [roomId]: overrideStatus }
      roomIdFilter: null,
      invProgress: { page: 0, totalPages: 0 },
      
      // 객실 필터 모달
      showRoomFilterModal: false,
      tempRoomFilter: null,
      selectedRoomId: null,

      // 달력 일-클릭 모달 상태
      showDayRoomModal: false,
      selectedInvDate: null,
      roomStatusEdits: {}, // { roomId: newStatus }
      savingStatuses: false
    }
  },
  
  mounted() {
    this.loadHotels()
    this.loadStats()
  },
  computed: {
    // 전체 객실 기준 상태 카운트 (roomIdFilter가 없을 때 사용)
    statusCounts() {
      const counts = { available: 0, occupied: 0, maintenance: 0, cleaning: 0 }
      for (const r of this.selectedHotelRooms || []) {
        const s = (r.status || 'available').toLowerCase()
        if (counts[s] != null) counts[s]++
      }
      return counts
    },
    // 선택된 단일 객실의 상태 (roomIdFilter 있을 때 사용)
    selectedRoomStatus() {
      if (!this.roomIdFilter) return null
      const r = (this.selectedHotelRooms || []).find(x => String(x.id) === String(this.roomIdFilter))
      return r ? (r.status || 'available') : null
    },
    // Count only true diffs vs the effective (override-aware) original value
    pendingChangesCount() {
      if (!this.selectedHotel || !this.showDayRoomModal) return 0
      let n = 0
      for (const room of this.selectedHotelRooms || []) {
        const original = String(this.baselineRoomStatusFor(room) || 'available').toLowerCase()
        const edit = this.roomStatusEdits[room.id]
        if (edit != null && String(edit).toLowerCase() !== original) n++
      }
      return n
    }
  },
  
  methods: {
    toggleInventoryCalendar() {
      this.showInventoryCalendar = !this.showInventoryCalendar;
      if (this.showInventoryCalendar) {
        this.reloadInventory();
      }
    },
    // Return display status for a room: if a per-day override exists for the last selected date, use it; otherwise base status
    roomDisplayStatus(room) {
      const base = String(room?.status || 'available').toLowerCase()
      // If no date is selected, fall back to the room's base status
      if (!this.selectedInvDate) return base
      const key = this.formatDateYMD(this.selectedInvDate)
      const ov = this.dayRoomOverrides?.[key]?.[room?.id]
      if (!ov) {
        // In per-date mode without explicit override, default to 'available' (영업)
        return 'available'
      }
      // map override to room status class
      return this.overrideToRoomStatusClass(ov)
    },
    // Baseline (override-aware) status without considering in-progress edits
    baselineRoomStatusFor(room) {
      if (this.selectedInvDate) {
        const key = this.formatDateYMD(this.selectedInvDate)
        const ov = this.dayRoomOverrides?.[key]?.[room?.id]
        if (ov) return this.overrideToRoomStatusClass(ov)
        return 'available'
      }
      return String(room?.status || 'available').toLowerCase()
    },
    // 내부 유틸: 백엔드 Room -> 편의시설 텍스트 배열
    buildAmenities(room) {
      const amenities = []
      if (room?.wifi) amenities.push('Wi-Fi')
      if (room?.aircon) amenities.push('에어컨')
      if (room?.freeWater) amenities.push('무료 생수')
      if (room?.hasWindow) amenities.push('창문')
      if (room?.sharedBath) amenities.push('공용 욕실')
      if (room?.bath) amenities.push(`욕실 ${room.bath}`)
      if (room?.bed) amenities.push(room.bed)
      return amenities
    },

    // 객실 필터 모달 관련 메서드
    openRoomFilterModal() {
      if (!this.selectedHotel) {
        alert('먼저 호텔을 선택해주세요.')
        return
      }
      this.tempRoomFilter = this.roomIdFilter ? 'selected' : null
      this.selectedRoomId = this.roomIdFilter || null
      this.showRoomFilterModal = true
    },

    closeRoomFilterModal() {
      this.showRoomFilterModal = false
      this.tempRoomFilter = null
      this.selectedRoomId = null
    },

    applyRoomFilter() {
      if (this.tempRoomFilter === 'selected' && !this.selectedRoomId) {
        alert('객실을 선택해주세요.')
        return
      }
      
      if (this.tempRoomFilter === 'selected') {
        this.roomIdFilter = this.selectedRoomId
      } else {
        this.roomIdFilter = null
      }
      
      this.closeRoomFilterModal()
      this.reloadInventory()
    },

    getStatusText(status) {
      const statusMap = {
        'AVAILABLE': '이용가능',
        'available': '이용가능',
        'OCCUPIED': '사용중',
        'occupied': '사용중',
        'MAINTENANCE': '점검중',
        'maintenance': '점검중',
        'CLEANING': '청소중',
        'cleaning': '청소중'
      }
      return statusMap[status] || status
    },
    async loadHotels() {
      this.loading = true
      try {
        const params = {
          page: this.hotels.number || 0,
          size: 20
        }
        
        if (this.filters.name) params.name = this.filters.name
        if (this.filters.status) params.status = this.filters.status  
        if (this.filters.city) params.city = this.filters.city

        const response = await api.get('/admin/hotels', { params })
        this.hotels = response.data?.data || { content: [], totalPages: 0, number: 0 }
      } catch (error) {
        console.error('호텔 목록 로드 실패:', error)
        alert('호텔 목록을 불러오는데 실패했습니다.')
      } finally {
        this.loading = false
      }
    },

    async loadStats() {
      try {
        // 관리 통계 대시보드 API 사용
        const res = await api.get('/admin/stats/dashboard')
        const data = res?.data || {}
        const d = data?.data || {}
        this.stats = {
          totalHotels: Number(d.totalHotels ?? 0),
          totalRooms: Number(d.totalRooms ?? 0),
          availableRooms: Number(d.availableRooms ?? 0),
          occupiedRooms: Number(d.occupiedRooms ?? 0),
          maintenanceRooms: Number(d.maintenanceRooms ?? 0),
          pendingHotels: Number(d.pendingHotels ?? 0),
          approvedHotels: Number(d.approvedHotels ?? 0),
          totalRevenue: Number(d.totalRevenue ?? 0)
        }
      } catch (error) {
        console.error('통계 로드 실패:', error)
      }
    },

    async selectHotel(hotel) {
      this.selectedHotel = hotel
      await this.loadRooms()
    },

    async loadRooms() {
      if (!this.selectedHotel) return
      
      try {
        this.roomsError = ''
        // 관리자 객실 목록 API (호텔 기준) - 단일 엔드포인트
        const resp = await api.get(`/admin/hotels/${this.selectedHotel.id}/rooms`)
        const dataItems = Array.isArray(resp?.data?.data) ? resp.data.data : (Array.isArray(resp?.data) ? resp.data : [])
        // BE Room -> UI 필드 매핑
        this.selectedHotelRooms = (dataItems || []).map(r => {
          console.log('=== [FE] Room data:', r)
          console.log('=== [FE] primaryImageUrl:', r.primaryImageUrl)
          console.log('=== [FE] imageUrls:', r.imageUrls)
          
          return {
          id: r.id,
          roomNumber: r.name || '-',
          roomType: r.roomType || '스탠다드룸',
          roomSize: r.roomSize && r.roomSize !== '-' ? r.roomSize : '기본크기',
          // Normalize to lowercase for UI bindings and comparisons
          status: String(r.status || 'available').toLowerCase(),
          capacity: r.capacityMax ?? r.capacityMin ?? 0,
          capacityMax: r.capacityMax ?? r.capacityMin ?? 0,
          capacityMin: r.capacityMin ?? r.capacityMax ?? 0,
          price: r.price ?? r.originalPrice ?? 0,
          originalPrice: r.originalPrice ?? r.price ?? 0,
          wifi: !!r.wifi,
          aircon: !!r.aircon,
          freeWater: !!r.freeWater,
          hasWindow: !!r.hasWindow,
          sharedBath: !!r.sharedBath,
          smoke: !!r.smoke,
          bed: r.bed || '',
          bath: r.bath ?? 0,
          imageUrl: r.primaryImageUrl || r.imageUrl || '',
          imageUrls: Array.isArray(r.imageUrls) ? r.imageUrls : [],
          amenities: this.buildAmenities(r)
        }
        })
      } catch (error) {
        console.error('객실 목록 로드 실패:', error)
        this.roomsError = '객실 목록을 불러오는 데 실패했습니다. 새로고침하거나 잠시 후 다시 시도해주세요.'
      }
    },

    // 통계 계산 함수들
    getAvailabilityRate() {
      if (this.stats.totalRooms === 0) return 0
      return Math.round((this.stats.availableRooms / this.stats.totalRooms) * 100)
    },

    getRoomStatusCount(status) {
      return this.selectedHotelRooms.filter(room => room.status === status).length
    },

    // 상태 관련 함수들
    getStatusLabel(status) {
      const labels = {
        'PENDING': '승인대기',
        'APPROVED': '운영중', 
        'REJECTED': '승인거부',
        'SUSPENDED': '정지'
      }
      return labels[status] || status
    },

    getRoomStatusLabel(status) {
      const labels = {
        'available': '예약 가능',
        'AVAILABLE': '예약 가능',
        'occupied': '예약 중',
        'OCCUPIED': '예약 중',
        'maintenance': '점검 중',
        'MAINTENANCE': '점검 중',
        'cleaning': '청소 중',
        'CLEANING': '청소 중'
      }
      return labels[status] || status
    },

    // 액션 함수들
    addNewRoom() {
      this.editingRoom = {
        id: null,
        roomNumber: '',
        roomType: '스탠다드룸',
        roomSize: '기본크기',
        roomSizeValue: '',
        status: 'available',
        capacity: 1,
        price: 0,
        originalPrice: null,
        amenitiesText: '',
        wifi: false,
        aircon: false,
        freeWater: false,
        hasWindow: false,
        sharedBath: false,
        smoke: false,
        bed: '',
        bath: 0,
        imageUrl: '',
        imageUrlInput: '',
        imageFile: null
      }
      this.showRoomEditor = true
    },

    editRoom(room) {
      this.editingRoom = {
        ...room,
        roomType: (room.roomType && room.roomType !== '-') ? room.roomType : '스탠다드룸',
        roomSize: (room.roomSize && room.roomSize !== '-') ? room.roomSize : '기본크기',
        roomSizeValue: this.parseRoomSize(room.roomSize),
        status: room.status || 'available',
        capacity: room.capacity ?? room.capacityMax ?? 1,
        price: room.price ?? 0,
        originalPrice: room.originalPrice ?? room.price ?? 0,
        wifi: !!room.wifi,
        aircon: !!room.aircon,
        freeWater: !!room.freeWater,
        hasWindow: !!room.hasWindow,
        sharedBath: !!room.sharedBath,
        smoke: !!room.smoke,
        bed: room.bed || '',
        bath: room.bath ?? 0,
        imageUrl: room.imageUrl || '',
        imageUrlInput: room.imageUrl || '',
        imageFile: null,
        amenitiesText: room.amenities?.join(', ') || ''
      }
      this.showRoomEditor = true
    },

    closeRoomEditor() {
      this.showRoomEditor = false
      this.editingRoom = null
    },

    async saveRoom() {
      if (!this.editingRoom || !this.selectedHotel) return
      
      this.roomSaving = true
      try {
        let imageUrl = this.editingRoom.imageUrlInput || this.editingRoom.imageUrl || ''
        
        // 이미지 파일이 선택된 경우 업로드
        if (this.editingRoom.imageFile) {
          const formData = new FormData()
          formData.append('file', this.editingRoom.imageFile)
          
          try {
            const uploadRes = await api.post('/admin/upload/room-image', formData, {
              headers: { 'Content-Type': 'multipart/form-data' }
            })
            imageUrl = uploadRes.data?.data?.url || uploadRes.data?.url || imageUrl
            this.editingRoom.imageUrl = imageUrl
            this.editingRoom.imageUrlInput = imageUrl
          } catch (uploadErr) {
            console.error('이미지 업로드 실패:', uploadErr)
            alert('이미지 업로드에 실패했습니다. 객실 정보만 저장됩니다.')
          }
        }
        
        const formattedRoomSize = this.formatRoomSize(this.editingRoom.roomSizeValue, this.editingRoom.roomSize)
        this.editingRoom.roomSize = formattedRoomSize

        const payload = {
          hotelId: this.selectedHotel.id,
          name: this.editingRoom.roomNumber,
          roomType: (this.editingRoom.roomType && this.editingRoom.roomType !== '-') ? this.editingRoom.roomType : '스탠다드룸',
          roomSize: formattedRoomSize,
          status: this.editingRoom.status,
          capacityMax: this.editingRoom.capacity,
          capacityMin: this.editingRoom.capacity,
          price: this.editingRoom.price,
          originalPrice: this.editingRoom.originalPrice || this.editingRoom.price,
          roomCount: 1,
          wifi: this.editingRoom.wifi,
          aircon: this.editingRoom.aircon,
          freeWater: this.editingRoom.freeWater,
          hasWindow: this.editingRoom.hasWindow,
          sharedBath: this.editingRoom.sharedBath,
          smoke: this.editingRoom.smoke,
          bed: this.editingRoom.bed,
          bath: this.editingRoom.bath,
          imageUrl: imageUrl
        }

        if (this.editingRoom.id) {
          await api.put(`/admin/rooms/${this.editingRoom.id}`, payload)
        } else {
          await api.post('/admin/rooms', payload)
        }
        this.closeRoomEditor()
        await this.loadRooms()
      } catch (e) {
        console.error('객실 저장 실패:', e)
        const errorMsg = e?.response?.data?.message || e?.response?.data?.error || e?.message || '객실 저장에 실패했습니다.'
        alert(`객실 저장 실패: ${errorMsg}`)
      } finally {
        this.roomSaving = false
      }
    },

    async deleteRoom() {
      if (!this.editingRoom?.id) return
      
      const roomNumber = this.editingRoom.roomNumber || '이 객실'
      const confirmMessage = `${roomNumber}을(를) 정말 삭제하시겠습니까?\n\n삭제된 객실은 복구할 수 없습니다.`
      
      if (!confirm(confirmMessage)) {
        return
      }

      this.roomSaving = true
      try {
        console.log('객실 삭제 요청:', this.editingRoom.id)
        const response = await api.delete(`/admin/rooms/${this.editingRoom.id}`)
        console.log('객실 삭제 응답:', response)
        alert('객실이 성공적으로 삭제되었습니다.')
        this.closeRoomEditor()
        await this.loadRooms()
      } catch (error) {
        console.error('객실 삭제 에러:', error)
        console.error('에러 응답:', error?.response)
        console.error('에러 데이터:', error?.response?.data)
        
        let errorMsg = '객실 삭제에 실패했습니다.'
        if (error?.response?.data?.error) {
          errorMsg = error.response.data.error
        } else if (error?.response?.data?.message) {
          errorMsg = error.response.data.message
        } else if (error?.message) {
          errorMsg = error.message
        }
        
        // 예약이 있는 경우 특별한 메시지 표시
        if (errorMsg.includes('예약이 있는')) {
          alert(`❌ ${errorMsg}\n\n예약 관리 페이지에서 해당 객실의 예약을 먼저 취소해주세요.`)
        } else {
          alert(`❌ 객실 삭제 실패: ${errorMsg}`)
        }
      } finally {
        this.roomSaving = false
      }
    },


    async updateRoomStatus(room, newStatus) {
      try {
        // 모달로 여는 경우(selectedInvDate가 존재)엔 해당 날짜에만 override 생성
        if (this.showDayRoomModal && this.selectedInvDate) {
          const from = this.formatDateYMD(this.selectedInvDate)
          // override upsert: POST 먼저, 실패(이미 존재 등) 시 PUT로 갱신
          await this.saveRoomOverride(room.id, from, this.mapToOverride(String(newStatus || '').toLowerCase()))
        } else {
          // 전역 상태 변경 경로는 기존대로 유지
          const status = String(newStatus || '').toLowerCase()
          await api.put(`/admin/rooms/${room.id}/status`, { status })
          room.status = status
        }
      } catch (error) {
        console.error('상태 변경 실패', error)
        const msg = error?.response?.data?.error || error?.message || '상태 변경에 실패했습니다.'
        alert(msg)
        throw error
      }
    },

    // 일괄 편집 함수들

    // 기타 함수들
    formatCurrency(amount) {
      if (!amount) return '0원'
      return amount.toLocaleString('ko-KR') + '원'
    },

    refreshData() {
      this.loadHotels()
      this.loadStats()
      if (this.selectedHotel) {
        this.loadRooms()
      }
    },

    debounceSearch() {
      clearTimeout(this.searchTimeout)
      this.searchTimeout = setTimeout(() => {
        this.loadHotels()
      }, 500)
    },

    // ---------- 재고 달력 로직 ----------
    reloadInventory() {
      if (!this.selectedHotel) return
      this.invError = ''
      
      // 객실 수 계산: 실제 선택된 호텔의 객실 수를 사용
      if (this.roomIdFilter) {
        // 특정 객실만 선택된 경우
        this.invCapacity = 1
      } else {
        // 전체 객실인 경우 - 실제 객실 목록의 길이를 사용
        this.invCapacity = this.selectedHotelRooms.length
      }
      
      this.invDays = this.generateMonthDays(this.invMonth)
      this.occupancyMap = {}
      this.dayBlockedCount = {}
      this.dayOverrideCountsMap = {}
      this.daySingleRoomOverride = {}
      this.dayRoomOverrides = {}
      this.fetchInventoryForMonth()
      this.fetchOverridesForMonth()
    },
    prevMonth() {
      const m = new Date(this.invMonth)
      m.setMonth(m.getMonth() - 1)
      this.invMonth = m
      this.selectedInvDate = null
      this.reloadInventory()
    },
    nextMonth() {
      const m = new Date(this.invMonth)
      m.setMonth(m.getMonth() + 1)
      this.invMonth = m
      this.selectedInvDate = null
      this.reloadInventory()
    },
    formatMonth(d) {
      return `${d.getFullYear()}년 ${String(d.getMonth() + 1).padStart(2,'0')}월`
    },
    generateMonthDays(baseDate) {
      const year = baseDate.getFullYear()
      const month = baseDate.getMonth()
      const first = new Date(year, month, 1)
      const startDay = first.getDay()
      const start = new Date(year, month, 1 - startDay)
      const days = []
      for (let i = 0; i < 42; i++) {
        const d = new Date(start)
        d.setDate(start.getDate() + i)
        const outside = d.getMonth() !== month
        const key = this.formatDateYMD(d)
        days.push({ key, date: d, outside })
      }
      return days
    },
    async fetchInventoryForMonth() {
      if (!this.selectedHotel) return
      const from = new Date(this.invMonth.getFullYear(), this.invMonth.getMonth(), 1)
      const to = new Date(this.invMonth.getFullYear(), this.invMonth.getMonth() + 1, 0, 23, 59, 59, 999)
      const stayFrom = from.toISOString()
      const stayTo = to.toISOString()
      this.invLoading = true
      this.invProgress = { page: 0, totalPages: 0 }
      try {
        let page = 0
        const size = 200
        let totalPages = 1
        do {
          const params = { page, size, stayFrom, stayTo, hotelName: this.selectedHotel.name }
          const res = await api.get('/admin/reservations', { params })
          const envelope = res?.data?.data || { content: [], totalPages: 0 }
          totalPages = Number(envelope.totalPages || 0)
          this.invProgress = { page, totalPages }
          const items = Array.isArray(envelope.content) ? envelope.content : []
          for (const r of items) {
            if (this.roomIdFilter && r.roomId && Number(r.roomId) !== Number(this.roomIdFilter)) continue
            const s = r.startDate ? new Date(r.startDate) : null
            const e = r.endDate ? new Date(r.endDate) : null
            if (!s || !e) continue
            // 각 숙박일(체크아웃 전날까지) 차감/증가
            const cursor = new Date(s)
            while (cursor <= e) {
              const key = this.formatDateYMD(cursor)
              if (!this.occupancyMap[key]) this.occupancyMap[key] = 0
              this.occupancyMap[key] += 1
              cursor.setDate(cursor.getDate() + 1)
            }
          }
          page += 1
        } while (page < totalPages)
      } catch (e) {
        console.error(e)
        this.invError = '달력 데이터를 불러오는데 실패했습니다.'
      } finally {
        this.invLoading = false
      }
    },
    async fetchOverridesForMonth() {
      try {
        const year = this.invMonth.getFullYear()
        const month = this.invMonth.getMonth() + 1
        const from = `${year}-${String(month).padStart(2,'0')}-01`
        const lastDay = new Date(year, month, 0)
        const to = `${year}-${String(month).padStart(2,'0')}-${String(lastDay.getDate()).padStart(2,'0')}`
        const roomIds = this.roomIdFilter
          ? [this.roomIdFilter]
          : (this.selectedHotelRooms || []).map(r => r.id)
        if (!roomIds || roomIds.length === 0) return
        const params = { roomIds: roomIds.join(','), from, to }
        const res = await api.get('/admin/rooms/overrides', { params })
        const list = Array.isArray(res?.data?.data) ? res.data.data : (Array.isArray(res?.data) ? res.data : [])
        const blockedByDate = {}
        const countsMap = {}
        const singleId = this.roomIdFilter ? Number(this.roomIdFilter) : null
        const singleMap = {}
        const byDateRoom = {}
        for (const o of list) {
          const date = (o.date || o.day || o.overrideDate || '').slice(0,10)
          const status = String(o.status || 'open').toLowerCase()
          const rid = Number(o.roomId || o.roomID || o.room_id || o.room?.id)
          if (!date) continue
          if (!countsMap[date]) countsMap[date] = { open: 0, closed: 0, maintenance: 0, cleaning: 0 }
          if (countsMap[date][status] == null) countsMap[date][status] = 0
          countsMap[date][status] += 1
          if (!byDateRoom[date]) byDateRoom[date] = {}
          if (rid) byDateRoom[date][rid] = status
          if (status === 'closed' || status === 'maintenance' || status === 'cleaning') {
            if (!blockedByDate[date]) blockedByDate[date] = new Set()
            if (rid) blockedByDate[date].add(rid)
          }
          if (singleId && rid === singleId) {
            singleMap[date] = status
          }
        }
        const blockedCounts = {}
        for (const [d, set] of Object.entries(blockedByDate)) blockedCounts[d] = set.size
        // Compute default open counts per day as: total rooms - (closed+maintenance+cleaning)
        const baseCapacity = (this.roomIdFilter ? 1 : (this.selectedHotelRooms?.length || 0))
        const normalizedCounts = {}
        for (const d of Object.keys(countsMap)) {
          const c = countsMap[d] || {}
          const closed = Number(c.closed || 0)
          const maintenance = Number(c.maintenance || 0)
          const cleaning = Number(c.cleaning || 0)
          const explicitOpen = Number(c.open || 0)
          const inferredOpen = Math.max(0, baseCapacity - (closed + maintenance + cleaning))
          normalizedCounts[d] = {
            open: explicitOpen > 0 ? explicitOpen : inferredOpen,
            closed, maintenance, cleaning
          }
        }
        this.dayBlockedCount = blockedCounts
        this.dayOverrideCountsMap = normalizedCounts
        this.daySingleRoomOverride = singleMap
        this.dayRoomOverrides = byDateRoom
        // Do not auto-select a date; the user click determines the date context
      } catch (e) {
        console.warn('fetchOverridesForMonth 실패', e)
      }
    },
    getOccupancy(dateObj) {
      const key = this.formatDateYMD(dateObj)
      return Number(this.occupancyMap[key] || 0)
    },
    getEffectiveCapacity(dateObj) {
      const key = this.formatDateYMD(dateObj)
      const base = this.roomIdFilter ? 1 : (this.selectedHotelRooms?.length || 0)
      const blocked = Number(this.dayBlockedCount[key] || 0)
      return Math.max(0, base - blocked)
    },
    dayStatusClass(dateObj) {
      const occ = this.getOccupancy(dateObj)
      const cap = this.getEffectiveCapacity(dateObj)
      if (!cap) return 'status-unknown'
      if (occ < cap * 0.7) return 'status-ok'
      if (occ < cap) return 'status-tight'
      if (occ === cap) return 'status-full'
      return 'status-over'
    },
    dayStatusText(dateObj) {
      const c = this.dayStatusClass(dateObj)
      const map = { 'status-ok': '여유', 'status-tight': '한계 근접', 'status-full': '만실', 'status-over': '초과', 'status-unknown': '-' }
      return map[c] || '-'
    },
    dayOverrideCounts(dateObj) {
      const key = this.formatDateYMD(dateObj)
      const c = this.dayOverrideCountsMap[key]
      if (c) {
        // Ensure open count is present even if not explicitly provided
        const base = (this.roomIdFilter ? 1 : (this.selectedHotelRooms?.length || 0))
        const closed = Number(c.closed || 0)
        const maintenance = Number(c.maintenance || 0)
        const cleaning = Number(c.cleaning || 0)
        const open = (c.open != null && c.open > 0) ? c.open : Math.max(0, base - (closed + maintenance + cleaning))
        return { open, closed, maintenance, cleaning }
      }
      // No overrides at all: all rooms are open by default
      const base = (this.roomIdFilter ? 1 : (this.selectedHotelRooms?.length || 0))
      return { open: base, closed: 0, maintenance: 0, cleaning: 0 }
    },
    getOverrideStatusLabel(s) {
      const m = { open: '영업', closed: '예약', maintenance: '점검', cleaning: '청소' }
      return m[s] || s
    },
    overrideToRoomStatusClass(s) {
      // Map override status to existing room status classes to reuse colors
      const m = { open: 'available', closed: 'occupied', maintenance: 'maintenance', cleaning: 'cleaning' }
      const v = m[String(s || '').toLowerCase()] || 'available'
      return v
    },

    // ---------- 일-클릭 모달 로직 ----------
    openDayRoomModal(date) {
      if (!this.selectedHotel) {
        alert('먼저 호텔을 선택해주세요.')
        return
      }
      this.selectedInvDate = new Date(date)
      this.showDayRoomModal = true
      // Initialize edit map to baseline (override-aware) values for all rooms
      const init = {}
      for (const room of this.selectedHotelRooms || []) {
        init[room.id] = String(this.baselineRoomStatusFor(room) || 'available').toLowerCase()
      }
      this.roomStatusEdits = init
    },
    closeDayRoomModal() {
      this.showDayRoomModal = false
      // Clear selected date so that another date click starts fresh
      this.selectedInvDate = null
      this.roomStatusEdits = {}
    },
    roomStatusFor(room) {
      // Prefer the in-progress edit first
      const edited = this.roomStatusEdits[room.id]
      if (edited) return String(edited).toLowerCase()
      // Then, if a date-specific override exists, reflect that
      if (this.selectedInvDate) {
        const key = this.formatDateYMD(this.selectedInvDate)
        const ov = this.dayRoomOverrides?.[key]?.[room.id]
        if (ov) return this.overrideToRoomStatusClass(ov)
        // No override for this date: default per-day view to 'available'
        return 'available'
      }
      // Fallback to base room status
      return String(room.status || 'available').toLowerCase()
    },
    onEditRoomStatus(room, value) {
      // 기준값은 'baseline'(override 반영, 편집 제외)
      const original = String(this.baselineRoomStatusFor(room) || 'available').toLowerCase()
      const next = String(value || '').toLowerCase()
      if (next === original) {
        // revert edit if exists
        if (this.roomStatusEdits[room.id]) {
          const copy = { ...this.roomStatusEdits }
          delete copy[room.id]
          this.roomStatusEdits = copy
        }
      } else {
        this.roomStatusEdits = { ...this.roomStatusEdits, [room.id]: next }
      }
    },
    resetRoomStatusEdits() {
      this.roomStatusEdits = {}
    },
  async saveDayRoomStatuses() {
      if (!this.selectedHotel) return
      // 현재 선택값과 원본을 비교하여 실제 변경된 항목만 업데이트
      const changes = []
      for (const room of this.selectedHotelRooms || []) {
        // Compare against baseline (override-aware) without edits
        const original = String(this.baselineRoomStatusFor(room) || 'available').toLowerCase()
        const desired = String(this.roomStatusEdits[room.id] ?? original).toLowerCase()
        if (desired !== original) {
          changes.push({ room, status: desired })
        }
      }
      if (changes.length === 0) return
      this.savingStatuses = true
      try {
        for (const ch of changes) {
          await this.updateRoomStatus(ch.room, ch.status)
          // Optimistically reflect change in local overrides map for selected date
          if (this.selectedInvDate) {
            const key = this.formatDateYMD(this.selectedInvDate)
            if (!this.dayRoomOverrides[key]) this.dayRoomOverrides[key] = {}
            // store override in override terms (open/closed/maintenance/cleaning)
            this.dayRoomOverrides[key][ch.room.id] = this.mapToOverride(ch.status)
          }
        }
        // 저장 후 재조회: 객실 목록 + override만 새로고침하여 카운트 갱신
        await this.loadRooms()
        await this.fetchOverridesForMonth()
        this.closeDayRoomModal()
      } catch (e) {
        // 일부 실패하더라도 나머지는 진행됨
      } finally {
        this.savingStatuses = false
      }
    },
    async saveRoomOverride(roomId, from, overrideStatus) {
      try {
        await api.post(`/admin/rooms/${roomId}/overrides`, { from, to: from, status: overrideStatus, note: 'calendar-change' })
      } catch (e) {
        // 이미 존재 등의 사유로 실패한 경우 PUT으로 갱신 시도
        const code = e?.response?.status
        const msg = String(e?.response?.data?.error || e?.response?.data?.message || '').toLowerCase()
        if (code === 409 || code === 400 || msg.includes('exist') || msg.includes('중복') || msg.includes('already')) {
          try {
            await api.put(`/admin/rooms/${roomId}/overrides`, { from, to: from, status: overrideStatus, note: 'calendar-change' })
            return
          } catch (e2) {
            // 일부 API는 날짜를 path param으로 받기도 함: /overrides/{date}
            try {
              await api.put(`/admin/rooms/${roomId}/overrides/${from}`, { status: overrideStatus, note: 'calendar-change' })
              return
            } catch (e3) {
              // 마지막으로 원래 에러 throw
              const emsg = e3?.response?.data?.error || e3?.response?.data?.message || e2?.response?.data?.error || e2?.response?.data?.message || e?.message || 'Override 저장 실패'
              throw new Error(emsg)
            }
          }
        }
        throw e
      }
    },
    formatDateYMD(d) {
      if (!d) return ''
      const x = new Date(d)
      const pad = (n) => String(n).padStart(2, '0')
      return `${x.getFullYear()}-${pad(x.getMonth()+1)}-${pad(x.getDate())}`
    },
    mapToOverride(status) {
      const s = String(status || '').toLowerCase()
      if (s === 'available') return 'open'
      if (s === 'occupied') return 'closed'
      if (s === 'maintenance') return 'maintenance'
      if (s === 'cleaning') return 'cleaning'
      return 'open'
    },
    
    // 이미지 관련 메서드
    handleImageUrlChange() {
      // URL 입력 변경시 미리보기 업데이트
      if (this.editingRoom.imageUrlInput) {
        this.editingRoom.imageUrl = this.editingRoom.imageUrlInput
      }
    },
    
    handleImageChange(event) {
      const file = event.target.files[0]
      if (!file) return
      
      // 파일 크기 체크 (5MB)
      if (file.size > 5 * 1024 * 1024) {
        alert('이미지 파일 크기는 5MB 이하여야 합니다.')
        event.target.value = ''
        return
      }
      
      // 이미지 파일 타입 체크
      if (!file.type.startsWith('image/')) {
        alert('이미지 파일만 업로드 가능합니다.')
        event.target.value = ''
        return
      }
      
      this.editingRoom.imageFile = file
      
      // 미리보기 생성
      const reader = new FileReader()
      reader.onload = (e) => {
        this.editingRoom.imageUrl = e.target.result
      }
      reader.readAsDataURL(file)
    },
    
    removeImage() {
      this.editingRoom.imageUrl = ''
      this.editingRoom.imageUrlInput = ''
      this.editingRoom.imageFile = null
      if (this.$refs.imageInput) {
        this.$refs.imageInput.value = ''
      }
    },

    parseRoomSize(value) {
      if (!value) {
        return ''
      }
      const trimmed = value.toString().trim()
      if (!trimmed || trimmed === '기본크기' || trimmed === '-') {
        return ''
      }
      return trimmed.replace(/㎡/gi, '').trim()
    },

    formatRoomSize(value, fallback) {
      const base = value !== undefined && value !== null ? value.toString().trim() : ''
      if (base) {
        if (/^\d+(\.\d+)?$/.test(base)) {
          return `${base}㎡`
        }
        return base.endsWith('㎡') ? base : base
      }

      if (fallback && fallback !== '-') {
        return fallback
      }
      return '기본크기'
    },
    
    handleImageError(event) {
      console.error('이미지 로드 실패:', this.editingRoom.imageUrl)
      event.target.src = 'data:image/svg+xml;base64,PHN2ZyB3aWR0aD0iMzAwIiBoZWlnaHQ9IjIwMCIgeG1sbnM9Imh0dHA6Ly93d3cudzMub3JnLzIwMDAvc3ZnIj48cmVjdCB3aWR0aD0iMzAwIiBoZWlnaHQ9IjIwMCIgZmlsbD0iI2YwZjBmMCIvPjx0ZXh0IHg9IjUwJSIgeT0iNTAlIiBmb250LXNpemU9IjE4IiBmaWxsPSIjOTk5IiB0ZXh0LWFuY2hvcj0ibWlkZGxlIiBkeT0iLjNlbSI+7J2066+47KeAIOuhnOuTnCDsi6TtjKg8L3RleHQ+PC9zdmc+'
    }
  }
}
</script>

<style scoped src="@/assets/css/admin/hotel-management-new.css"></style>