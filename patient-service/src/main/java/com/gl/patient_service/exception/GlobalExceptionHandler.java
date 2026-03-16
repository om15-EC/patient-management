package com.gl.patient_service.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String,String>> handleValidationException(MethodArgumentNotValidException ex){

        Map<String,String> errors = new HashMap<>();

        ex.getBindingResult().getFieldErrors().forEach(
                error -> errors.put(error.getField(), error.getDefaultMessage()));

        return ResponseEntity.badRequest().body(errors);
    }

    @ExceptionHandler(EmailAlreadyExistException.class)
    public ResponseEntity<Map<String, String>> handleEmailAlreadyExistsException(
            EmailAlreadyExistException ex){
        Map<String,String> error = new HashMap<>();
        log.warn("Email address already exist {}", ex.getMessage());
        error.put("message", "email already exists");
        return ResponseEntity.badRequest().body(error);
    }

    @ExceptionHandler(PatientNotFoundById.class)
    public ResponseEntity<Map<String, String>> handlePatientNotFoundException(
            PatientNotFoundById ex){
        Map<String,String> error = new HashMap<>();
        log.warn("Patient not found {}", ex.getMessage());
        error.put("message", "Patient not found");
        return ResponseEntity.badRequest().body(error);
    }
}
