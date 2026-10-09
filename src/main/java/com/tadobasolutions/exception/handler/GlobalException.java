package com.tadobasolutions.exception.handler;

import com.tadobasolutions.exception.BadRequestException;
import com.tadobasolutions.exception.CertificateNotFoundException;
import com.tadobasolutions.exception.ResourceNotFoundException;
import com.tadobasolutions.utilities.ApiResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

@RestControllerAdvice
public class GlobalException {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Map<String, String>>> handleValidationExceptions(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(error ->
                errors.put(error.getField(), error.getDefaultMessage())
        );

        ApiResponse<Map<String, String>> response = new ApiResponse<>();
        response.setData(errors);
        response.setMessage("Validation failed");
        response.setStatusCode(HttpStatus.BAD_REQUEST.value());
        response.setTimeStamp(LocalDateTime.now());
        response.setSuccess(false);

        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiResponse<?>> resourceNotFoundException(ResourceNotFoundException ex) {
        ApiResponse<Objects> apiResponse = ApiResponse.<Objects>builder()
                .data(null)
                .message(ex.getMessage())
                .timeStamp(LocalDateTime.now())
                .statusCode(HttpStatus.NOT_FOUND.value())
                .success(false)
                .build();
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(apiResponse);
    }

    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<ApiResponse<?>> badRequestException(BadRequestException ex) {
        ApiResponse<Objects> apiResponse = ApiResponse.<Objects>builder()
                .data(null)
                .message(ex.getMessage())
                .timeStamp(LocalDateTime.now())
                .statusCode(HttpStatus.BAD_REQUEST.value())
                .success(false)
                .build();
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(apiResponse);
    }


    @ExceptionHandler(CertificateNotFoundException.class)
    public ResponseEntity<ApiResponse<?>> certificateNotFoundException(CertificateNotFoundException ex) {
        ApiResponse<Objects> apiResponse = ApiResponse.<Objects>builder()
                .data(null)
                .message(ex.getMessage())
                .timeStamp(LocalDateTime.now())
                .statusCode(HttpStatus.NOT_FOUND.value())
                .success(false)
                .build();
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(apiResponse);
    }

}
