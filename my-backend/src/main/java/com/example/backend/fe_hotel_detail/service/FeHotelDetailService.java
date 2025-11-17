package com.example.backend.fe_hotel_detail.service;

import com.example.backend.HotelOwner.domain.Amenity;
import com.example.backend.HotelOwner.domain.Hotel;
import com.example.backend.HotelOwner.domain.HotelImage;
import com.example.backend.HotelOwner.domain.Room;
import com.example.backend.HotelOwner.domain.RoomImage;
import com.example.backend.HotelOwner.repository.HotelAmenityRepository;
import com.example.backend.HotelOwner.repository.HotelImageRepository;
import com.example.backend.HotelOwner.repository.HotelRepository;
import com.example.backend.HotelOwner.repository.RoomRepository;
import com.example.backend.fe_hotel_detail.dto.HotelDetailDto;
import com.example.backend.review.service.UserReviewService;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.regex.Pattern;

@Slf4j
@Service("feHotelDetailService")
@RequiredArgsConstructor
public class FeHotelDetailService {

    private static final String DEFAULT_ORIGIN = "https://hwiyeong.shop";
    private static final String UPLOAD_SEGMENT = "/uploads";
    private static final Pattern UUID_PREFIX = Pattern.compile(
            "^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$",
            Pattern.CASE_INSENSITIVE);
    private static final Set<String> LEGACY_MEDIA_HOSTS = Set.of("localhost", "127.0.0.1", "images.example.com");

    private final HotelRepository hotelRepository;
    private final HotelImageRepository hotelImageRepository;
    private final RoomRepository roomRepository;
    private final HotelAmenityRepository hotelAmenityRepository;
    private final UserReviewService userReviewService;

    @Value("${file.upload.url:https://hwiyeong.shop/uploads}")
    private String configuredUploadBase;

    @Value("${file.upload.fallback-url:https://hwiyeong.shop/images}")
    private String configuredFallbackBase;

    @Value("${file.upload.fallback-default:hotel-placeholder.webp}")
    private String fallbackDefaultImage;

    @Value("${file.upload-dir:./uploads}")
    private String uploadDirectory;

    private Path uploadRootPath;
    private String uploadOriginBase;
    private String fallbackBaseNormalized;

    @PostConstruct
    void initialiseMediaBases() {
        this.uploadOriginBase = stripUploadsSuffix(normalizeBase(configuredUploadBase));
        this.fallbackBaseNormalized = normalizeBase(configuredFallbackBase);
        try {
            this.uploadRootPath = Paths.get(uploadDirectory).toAbsolutePath().normalize();
            Files.createDirectories(this.uploadRootPath);
        } catch (Exception ex) {
            log.warn("업로드 디렉터리를 확인할 수 없어 기본 경로를 사용합니다. dir={}", uploadDirectory, ex);
            this.uploadRootPath = null;
        }
    }

    public HotelDetailDto getHotelDetail(Long id) {
        Hotel h = hotelRepository.findById(id)
            .orElseThrow(() -> new NoSuchElementException("hotel not found"));

        // 호텔 이미지
        List<String> hotelImages = hotelImageRepository.findByHotel_IdOrderBySortNoAsc(id)
            .stream()
            .map(HotelImage::getUrl)
            .map(this::normalizeMediaUrl)
            .filter(Objects::nonNull)
            .toList();

        // 객실: LEFT JOIN FETCH로 모두 가져오되(이미지 없어도 포함), 혹시 모를 중복을 id기준 Dedup
        List<Room> fetched = roomRepository.findByHotelIdWithImages(id);
        Map<Long, Room> dedup = new LinkedHashMap<>();
        for (Room r : fetched) {
            // 동일 id면 최초 1건만 유지 (정렬은 쿼리에서 id ASC로 보장)
            dedup.putIfAbsent(r.getId(), r);
        }
        List<Room> roomEntities = new ArrayList<>(dedup.values());

        // 편의시설 → 좌/우 분배
        List<Amenity> amenList = hotelAmenityRepository.findAmenitiesByHotel_Id(id);
        List<String> amenNames = amenList.stream().map(Amenity::getName).toList();
        List<String> left = new ArrayList<>(), right = new ArrayList<>();
        for (int i = 0; i < amenNames.size(); i++) {
            (i % 2 == 0 ? left : right).add(amenNames.get(i));
        }

        // 호텔 DTO
        HotelDetailDto.HotelDto hotelDto = HotelDetailDto.HotelDto.builder()
            .id(h.getId())
            .name(h.getName())
            .address(h.getAddress())
            .description(h.getDescription())
            .images(hotelImages)
            .badges(List.of())
            .rating(buildRating(h.getId()))
            .amenities(new HotelDetailDto.Amenities(left, right))
            .notice(null)
            .build();

        // 객실 DTO
        List<HotelDetailDto.RoomDto> roomDtos = new ArrayList<>();
        for (Room r : roomEntities) {
            List<String> photos = (r.getImages() == null) ? List.of()
                : r.getImages().stream()
                    .map(RoomImage::getUrl)
                    .map(this::normalizeMediaUrl)
                    .filter(Objects::nonNull)
                    .toList();

            roomDtos.add(HotelDetailDto.RoomDto.builder()
                .id(r.getId())
                .name(safeRoomName(r))
                .size(parseIntSafe(r.getRoomSize()))
                .view(nullToDash(r.getViewName()))
                .bed(nullToDash(r.getBed()))
                .bath(r.getBath())
                .smoke(boolSafe(r.getSmoke()))
                .sharedBath(boolSafe(r.getSharedBath()))
                .window(boolSafe(r.getHasWindow()))
                .aircon(boolSafe(r.getAircon()))
                .water(boolSafe(r.getFreeWater()))
                .wifi(boolSafe(r.getWifi()))
                .cancelPolicy(r.getCancelPolicy())
                .payment(r.getPayment())
                .originalPrice(intSafe(r.getOriginalPrice()))
                .price(intSafe(r.getPrice()))
                .lastBookedHours(3) // 데모 값
                .photos(photos)
                .promos(List.of())
                .qty(intSafe(r.getRoomCount()))        // 재고(보유 수량)
                .capacityMin(intSafe(r.getCapacityMin()))
                .capacityMax(intSafe(r.getCapacityMax()))
                .build());
        }

        HotelDetailDto dto = new HotelDetailDto();
        dto.setHotel(hotelDto);
        dto.setRooms(roomDtos);
        return dto;
    }

    // "75㎡", "약 75 m²", "75m2" → 75
    private Integer parseIntSafe(String s) {
        if (s == null) return null;
        String digits = s.replaceAll("[^0-9]", "");
        if (digits.isEmpty()) return null;
        try { return Integer.parseInt(digits); }
        catch (NumberFormatException e) { return null; }
    }

    private int intSafe(Integer n) { return n == null ? 0 : n.intValue(); }
    private Boolean boolSafe(Boolean b) { return b != null && b; }
    private String nullToDash(String s) { return (s == null || s.isBlank()) ? "-" : s; }

    private String safeRoomName(Room r) {
        if (r.getName() != null && !r.getName().isBlank()) return r.getName();
        return (r.getRoomType() != null) ? r.getRoomType().name() : "객실";
    }

    private String buildUploadOrFallback(String path, String originalValue) {
        String normalized = ensureUploadsPrefix(path != null ? path : originalValue);
        if (uploadResourceExists(normalized)) {
            return composePublicUrl(normalized);
        }
        log.debug("업로드 리소스를 찾을 수 없어 대체 이미지를 사용합니다. path={}", normalized);
        return fallbackMediaUrl(originalValue != null ? originalValue : normalized);
    }

    private boolean uploadResourceExists(String normalizedPath) {
        if (normalizedPath == null || uploadRootPath == null) {
            return false;
        }
        String relative = normalizedPath.startsWith(UPLOAD_SEGMENT)
                ? normalizedPath.substring(UPLOAD_SEGMENT.length())
                : normalizedPath;
        relative = relative.replaceFirst("^/+", "");
        if (relative.isBlank()) {
            return false;
        }
        Path candidate = uploadRootPath.resolve(relative);
        try {
            return Files.exists(candidate);
        } catch (Exception ex) {
            log.debug("업로드 파일 확인 실패 path={}", candidate, ex);
            return false;
        }
    }

    private String fallbackMediaUrl(String originalValue) {
        String filename = extractOriginalFilename(originalValue);
        if (filename == null || filename.isBlank()) {
            return fallbackDefaultUrl();
        }
        return joinUrl(fallbackBaseNormalized, filename);
    }

    private String extractOriginalFilename(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String sanitized = value;
        int queryIdx = sanitized.indexOf('?');
        if (queryIdx >= 0) {
            sanitized = sanitized.substring(0, queryIdx);
        }
        int slashIdx = sanitized.lastIndexOf('/');
        String candidate = slashIdx >= 0 ? sanitized.substring(slashIdx + 1) : sanitized;
        if (candidate.isBlank()) {
            return null;
        }
        int underscoreIdx = candidate.indexOf('_');
        if (underscoreIdx > 0) {
            String prefix = candidate.substring(0, underscoreIdx);
            if (UUID_PREFIX.matcher(prefix).matches()) {
                candidate = candidate.substring(underscoreIdx + 1);
            }
        }
        return candidate;
    }

    private String fallbackDefaultUrl() {
        if (fallbackDefaultImage != null && fallbackDefaultImage.startsWith("http")) {
            return fallbackDefaultImage;
        }
        String candidate = (fallbackDefaultImage == null || fallbackDefaultImage.isBlank())
                ? "hotel-placeholder.webp"
                : fallbackDefaultImage;
        return joinUrl(fallbackBaseNormalized, candidate);
    }

    private String joinUrl(String base, String path) {
        if (path == null || path.isBlank()) {
            return getUploadOriginBase() + UPLOAD_SEGMENT;
        }
        if (path.startsWith("http://") || path.startsWith("https://")) {
            return path;
        }
        String effectiveBase = (base == null || base.isBlank()) ? getUploadOriginBase() : base;
        String trimmedBase = effectiveBase.endsWith("/") && effectiveBase.length() > 1
                ? effectiveBase.substring(0, effectiveBase.length() - 1)
                : effectiveBase;
        String normalisedPath = path.startsWith("/") ? path : "/" + path;
        return trimmedBase + normalisedPath;
    }

    private String getUploadOriginBase() {
        return (uploadOriginBase == null || uploadOriginBase.isBlank()) ? DEFAULT_ORIGIN : uploadOriginBase;
    }

    private String normalizeBase(String value) {
        if (value == null || value.isBlank()) {
            return DEFAULT_ORIGIN;
        }
        String trimmed = value.trim();
        while (trimmed.endsWith("/") && trimmed.length() > 1) {
            trimmed = trimmed.substring(0, trimmed.length() - 1);
        }
        return trimmed.isEmpty() ? DEFAULT_ORIGIN : trimmed;
    }

    private String stripUploadsSuffix(String value) {
        if (value == null || value.isBlank()) {
            return DEFAULT_ORIGIN;
        }
        if (value.endsWith(UPLOAD_SEGMENT)) {
            return value.substring(0, value.length() - UPLOAD_SEGMENT.length());
        }
        return value;
    }

    private String normalizeMediaUrl(String raw) {
        if (raw == null) {
            return fallbackDefaultUrl();
        }

        String value = raw.trim();
        if (value.isEmpty()) {
            return fallbackDefaultUrl();
        }

        try {
            URI uri = URI.create(value);
            String host = uri.getHost();
            String path = uri.getPath();

            if (host == null) {
                return resolveRelative(path != null ? path : value);
            }

            String normalizedHost = host.toLowerCase(Locale.ROOT);

            if (path != null && path.contains(UPLOAD_SEGMENT)) {
                return buildUploadOrFallback(path, value);
            }

            if (LEGACY_MEDIA_HOSTS.contains(normalizedHost)) {
                return buildUploadOrFallback(path, value);
            }

            if ("http".equalsIgnoreCase(uri.getScheme())) {
                return value.replaceFirst("^http://", "https://");
            }

            return value;
        } catch (IllegalArgumentException ex) {
            return resolveRelative(value);
        }
    }

    private String resolveRelative(String path) {
        if (path == null || path.isBlank()) {
            return fallbackDefaultUrl();
        }
        if (path.startsWith(UPLOAD_SEGMENT) || path.startsWith(UPLOAD_SEGMENT.substring(1))) {
            String normalized = ensureUploadsPrefix(path);
            if (uploadResourceExists(normalized)) {
                return composePublicUrl(normalized);
            }
            log.debug("상대 업로드 경로를 찾을 수 없어 대체 이미지를 반환합니다. path={}", path);
            return fallbackMediaUrl(path);
        }
        String normalised = path.startsWith("/") ? path : "/" + path;
        return getUploadOriginBase() + normalised;
    }

    private String composePublicUrl(String path) {
        if (path == null || path.isBlank()) {
            return getUploadOriginBase() + UPLOAD_SEGMENT;
        }
        return getUploadOriginBase() + ensureUploadsPrefix(path);
    }

    private String ensureUploadsPrefix(String path) {
        if (path == null || path.isBlank()) {
            return UPLOAD_SEGMENT;
        }

        String normalized = path.startsWith("/") ? path : "/" + path;

        if (normalized.equals(UPLOAD_SEGMENT) || normalized.startsWith(UPLOAD_SEGMENT + "/")) {
            return normalized;
        }

        int idx = normalized.indexOf(UPLOAD_SEGMENT);
        if (idx >= 0) {
            String remainder = normalized.substring(idx + UPLOAD_SEGMENT.length());
            remainder = remainder.startsWith("/") ? remainder : "/" + remainder;
            return UPLOAD_SEGMENT + remainder;
        }

        return UPLOAD_SEGMENT + normalized;
    }

    private HotelDetailDto.Rating buildRating(Long hotelId) {
        if (hotelId == null) {
            return new HotelDetailDto.Rating(0.0, Map.of("리뷰수", 0.0), Map.of());
        }

        Map<String, Object> stats = userReviewService.getHotelReviewStats(hotelId);
        if (stats == null || stats.isEmpty()) {
            return new HotelDetailDto.Rating(0.0, Map.of("리뷰수", 0.0), Map.of());
        }

        double avg = toDouble(stats.get("average"));
        double count = toDouble(stats.get("count"));
        Map<String, Double> details = toDoubleMap(stats.get("details"));

        Map<String, Double> subs = Map.of("리뷰수", count);
        return HotelDetailDto.Rating.builder()
            .score(avg)
            .subs(subs)
            .details(details)
            .build();
    }

    private double toDouble(Object value) {
        if (value instanceof Number number) {
            return number.doubleValue();
        }
        if (value instanceof String s) {
            try {
                return Double.parseDouble(s);
            } catch (NumberFormatException ignored) {
                return 0.0;
            }
        }
        return 0.0;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Double> toDoubleMap(Object raw) {
        if (raw instanceof Map<?, ?> map) {
            Map<String, Double> result = new LinkedHashMap<>();
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                Object keyObj = entry.getKey();
                if (keyObj == null) {
                    continue;
                }
                String key = String.valueOf(keyObj);
                double value = toDouble(entry.getValue());
                result.put(key, value);
            }
            return result;
        }
        return Map.of();
    }
}
