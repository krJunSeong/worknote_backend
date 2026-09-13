package com.example.demo.common.exception;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {


    @ExceptionHandler(InvalidCredentialsException.class)
    public ResponseEntity<Map<String, String>> handleInvalidCredentials(
            InvalidCredentialsException exception
    ) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(errorBody(
                        InvalidCredentialsException.CODE,
                        InvalidCredentialsException.DEFAULT_MESSAGE
                ));
    }

    @ExceptionHandler(DailyUsageLimitExceededException.class)
    public ResponseEntity<Map<String, String>> handleDailyUsageLimit(
            DailyUsageLimitExceededException exception
    ) {
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                .body(errorBody(
                        DailyUsageLimitExceededException.CODE,
                        DailyUsageLimitExceededException.DEFAULT_MESSAGE
                ));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Map<String, String>> handleAccessDenied(
            AccessDeniedException exception
    ) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(errorBody(
                        "ACCESS_DENIED",
                        safeMessage(exception, "접근 권한이 없습니다.")
                ));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> handleBadRequest(
            IllegalArgumentException exception
    ) {
        return ResponseEntity.badRequest()
                .body(errorBody(
                        "INVALID_REQUEST",
                        safeMessage(exception, "요청 값을 확인해 주세요.")
                ));
    }


    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, String>> handleUnexpectedException(
            Exception exception
    ) {
        log.error("Unhandled server exception", exception);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(errorBody(
                        "INTERNAL_SERVER_ERROR",
                        "서버 오류가 발생했습니다. 잠시 후 다시 시도해 주세요."
                ));
    }

    private Map<String, String> errorBody(
            String code,
            String message
    ) {
        Map<String, String> body = new LinkedHashMap<>();
        body.put("code", code);
        body.put("message", message);
        return body;
    }

    private String safeMessage(
            Exception exception,
            String fallback
    ) {
        String message = exception.getMessage();
        return message == null || message.isBlank()
                ? fallback
                : message;
    }
}
