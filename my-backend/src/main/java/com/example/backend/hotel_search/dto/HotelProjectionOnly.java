package com.example.backend.hotel_search.dto;

public interface HotelProjectionOnly {
    Long getId();
    String getName();
    String getCity();         // Hotel.address를 city로 alias 해서 표시용
    String getCountry();
    Double getRating();       // star_rating -> DOUBLE 캐스팅
    Integer getLowestPrice(); // ★ 프론트에서 쓰는 이름과 통일
    String getThumbnailUrl();
}
