package com.example.footprint.exception;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.ServletRequestBindingException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.server.ResponseStatusException;

import com.example.footprint.dto.CommonDtos.ApiError;

import jakarta.validation.ConstraintViolationException;

@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(ApiException.class)
    ResponseEntity<ApiError> api(ApiException e) { return ResponseEntity.status(e.status()).body(new ApiError(e.code(), e.getMessage())); }
    @ExceptionHandler({MethodArgumentNotValidException.class, HttpMessageNotReadableException.class,
            MethodArgumentTypeMismatchException.class, ServletRequestBindingException.class, ConstraintViolationException.class})
    ResponseEntity<ApiError> validation(Exception e) { return ResponseEntity.badRequest().body(new ApiError("INVALID_REQUEST", "요청 형식과 입력 값을 확인해 주세요.")); }
    @ExceptionHandler(DataIntegrityViolationException.class)
    ResponseEntity<ApiError> conflict(Exception e) { return ResponseEntity.status(409).body(new ApiError("DATA_CONFLICT", "이미 존재하거나 참조 중인 데이터입니다.")); }
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    ResponseEntity<ApiError> large(Exception e) { return ResponseEntity.status(413).body(new ApiError("FILE_TOO_LARGE", "파일 크기 제한을 초과했습니다.")); }
    @ExceptionHandler(AccessDeniedException.class)
    ResponseEntity<ApiError> denied(Exception e) { return api(ApiException.forbidden()); }
    @ExceptionHandler(ResponseStatusException.class)
    ResponseEntity<ApiError> legacy(ResponseStatusException e) {
        return ResponseEntity.status(e.getStatusCode()).body(new ApiError("HTTP_" + e.getStatusCode().value(), e.getReason() == null ? "요청을 처리할 수 없습니다." : e.getReason()));
    }
}
