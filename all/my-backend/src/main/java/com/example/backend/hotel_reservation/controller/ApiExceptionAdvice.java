package com.example.backend.hotel_reservation.controller;

import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestControllerAdvice
public class ApiExceptionAdvice {
    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String,Object> badReq(Exception e){
        return Map.of("message", e.getMessage());
    }

    @ExceptionHandler(IllegalStateException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public Map<String,Object> conflict(Exception e){
        return Map.of("message", e.getMessage());
    }

    @ExceptionHandler(NoSuchElementException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public Map<String,Object> notFound(Exception e){
        return Map.of("message", e.getMessage());
    }


    //1010 추가 문의시 필요 사항
    @ExceptionHandler(SecurityException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED) 
    public Map<String,Object> unauthorized(Exception e){
        return Map.of("message", e.getMessage());
    }
}
