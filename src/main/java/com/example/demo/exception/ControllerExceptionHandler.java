package com.example.demo.exception;

import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ControllerExceptionHandler {
    @ExceptionHandler(InputException.class)
    public ResponseEntity<?> handleInputException(InputException ex){
        return new ResponseEntity<>(ex.getMessage(), HttpStatusCode.valueOf(400));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<?> handleGeneralException(Exception ex){
        return new ResponseEntity<>(ex.getMessage(), HttpStatusCode.valueOf(500));
    }
}
