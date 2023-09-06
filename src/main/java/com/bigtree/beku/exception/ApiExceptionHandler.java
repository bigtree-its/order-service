package com.bigtree.beku.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.util.UUID;

@Slf4j
@RestControllerAdvice
public class ApiExceptionHandler extends ResponseEntityExceptionHandler {

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleSystemError(Exception e){
        log.error("System Exception occurred. {}", e.getMessage());
        ApiErrorResponse apiErrorResponse = ApiErrorResponse.builder()
                .reference(UUID.randomUUID().toString())
                .title("Unexpected error occurred")
                .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                .detail(e.getMessage())
                .build();
        return new ResponseEntity<>(apiErrorResponse, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ApiErrorResponse> handleApiError(ApiException e){
        log.error("Api Exception occurred. {}", e.getMessage());
        ApiErrorResponse apiErrorResponse = ApiErrorResponse.builder()
                .reference(UUID.randomUUID().toString())
                .title(e.getTitle())
                .status(e.getStatus().value())
                .detail(e.getMessage())
                .build();
        return new ResponseEntity<>(apiErrorResponse, e.getStatus());
    }
}
