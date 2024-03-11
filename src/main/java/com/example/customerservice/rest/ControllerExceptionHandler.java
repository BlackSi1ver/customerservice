package com.example.customerservice.rest;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.NoSuchElementException;

@ControllerAdvice
public class ControllerExceptionHandler {

    @ExceptionHandler({IllegalArgumentException.class})
    @ResponseBody
    public ResponseEntity<Object> illegalArgumentException(IllegalArgumentException e) {
        ResponseEntity.BodyBuilder bodyBuilder = ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .header("X-Backend-Status", String.valueOf(e.getMessage()));
        return bodyBuilder.body(null);
    }

    @ExceptionHandler({NoSuchElementException.class})
    @ResponseBody
    public ResponseEntity<Object> noSuchElementException(NoSuchElementException e) {
        ResponseEntity.BodyBuilder bodyBuilder = ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .header("X-Backend-Status", String.valueOf(e.getMessage()));
        return bodyBuilder.body(null);
    }
}
