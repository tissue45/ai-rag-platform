package com.astra.backend.api.error;

import com.astra.backend.document.DocumentNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleValidation(MethodArgumentNotValidException ex, HttpServletRequest req) {
        Map<String, String> fieldErrors = new LinkedHashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(err -> {
            if (!fieldErrors.containsKey(err.getField())) {
                fieldErrors.put(err.getField(), err.getDefaultMessage());
            }
        });

        ApiError body = ApiError.of(
                "VALIDATION_ERROR",
                "요청 값이 올바르지 않습니다.",
                req.getRequestURI(),
                fieldErrors.isEmpty() ? null : fieldErrors
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiError> handleMalformedJson(HttpMessageNotReadableException ex, HttpServletRequest req) {
        ApiError body = ApiError.of(
                "MALFORMED_JSON",
                "요청 본문(JSON)을 파싱할 수 없습니다.",
                req.getRequestURI(),
                null
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiError> handleDataIntegrity(DataIntegrityViolationException ex, HttpServletRequest req) {
        String detail = ex.getMostSpecificCause() != null ? ex.getMostSpecificCause().getMessage() : ex.getMessage();
        log.warn("Data integrity violation {} : {}", req.getRequestURI(), detail);
        String m = detail != null ? detail.toLowerCase() : "";
        boolean likelyDuplicateEmail = m.contains("duplicate key")
                || m.contains("unique constraint")
                || m.contains("users_email")
                || m.contains("idx_users_email");
        if (likelyDuplicateEmail) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(ApiError.of(
                    "EMAIL_TAKEN",
                    "이미 사용 중인 이메일입니다.",
                    req.getRequestURI(),
                    null
            ));
        }
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ApiError.of(
                "DATA_INTEGRITY",
                "데이터 저장 중 제약 조건 오류가 발생했습니다.",
                req.getRequestURI(),
                null
        ));
    }

    @ExceptionHandler(DocumentNotFoundException.class)
    public ResponseEntity<ApiError> handleDocumentNotFound(DocumentNotFoundException ex, HttpServletRequest req) {
        ApiError body = ApiError.of(
                "DOCUMENT_NOT_FOUND",
                ex.getMessage(),
                req.getRequestURI(),
                null
        );
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(body);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleUnexpected(Exception ex, HttpServletRequest req) {
        log.error("Unhandled exception for {} {}", req.getMethod(), req.getRequestURI(), ex);
        ApiError body = ApiError.of(
                "INTERNAL_ERROR",
                "서버 오류가 발생했습니다.",
                req.getRequestURI(),
                null
        );
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(body);
    }
}

