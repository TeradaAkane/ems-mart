package com.springmart.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import com.springmart.exception.OutOfStockException;
import com.springmart.exception.ResourceNotFoundException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.dao.CannotAcquireLockException;
import org.springframework.web.server.ResponseStatusException;

import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> handleValidationExceptions(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach((error) -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errors);
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<Map<String, String>> handleResourceNotFoundException(ResourceNotFoundException ex) {
        Map<String, String> errorResponse = new HashMap<>();
        errorResponse.put("error", "NotFound");
        errorResponse.put("message", ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
    }

    @ExceptionHandler(OutOfStockException.class)
    public ResponseEntity<Map<String, String>> handleOutOfStockException(OutOfStockException ex) {
        Map<String, String> errorResponse = new HashMap<>();
        errorResponse.put("error", "Conflict");
        errorResponse.put("message", ex.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(errorResponse);
    }

    @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
    public ResponseEntity<Map<String, String>> handleOptimisticLockingFailureException(
            ObjectOptimisticLockingFailureException ex) {
        Map<String, String> errorResponse = new HashMap<>();
        errorResponse.put("error", "Conflict");
        errorResponse.put("message", "他のユーザーによってデータが更新されました。画面をリロードして再度お試しください。");
        return ResponseEntity.status(HttpStatus.CONFLICT).body(errorResponse);
    }

    @ExceptionHandler({PessimisticLockingFailureException.class, CannotAcquireLockException.class})
    public ResponseEntity<Map<String, String>> handlePessimisticLockingFailureException(Exception ex) {
        Map<String, String> errorResponse = new HashMap<>();
        errorResponse.put("error", "Conflict");
        errorResponse.put("message", "現在、商品の在庫情報が他の方によって更新されています。しばらく待ってから再度お試しください。");
        return ResponseEntity.status(HttpStatus.CONFLICT).body(errorResponse);
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Map<String, String>> handleResponseStatusException(ResponseStatusException ex) {
        Map<String, String> errorResponse = new HashMap<>();
        errorResponse.put("error", ex.getStatusCode().toString());
        errorResponse.put("message", ex.getReason());
        return ResponseEntity.status(ex.getStatusCode()).body(errorResponse);
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Map<String, String>> handleRuntimeException(RuntimeException ex) {
        Map<String, String> errorResponse = new HashMap<>();
        errorResponse.put("error", "InternalServerError");
        errorResponse.put("message", "システムエラーが発生しました: " + ex.getMessage());
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
    }
}
