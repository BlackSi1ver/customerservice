package com.customerservice.rest.controller;

import com.customerservice.exception.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;

import java.sql.SQLException;

@ControllerAdvice
public class CustomExceptionController {

    @ExceptionHandler({InvalidArgumentException.class, InvalidStateException.class})
    @ResponseBody
    public ResponseEntity<Object> invalidArgumentException(RuntimeException e) {
        ResponseEntity.BodyBuilder bodyBuilder = ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .header("X-Backend-Status", String.valueOf(e.getMessage()));
        return bodyBuilder.body(null);
    }

    @ExceptionHandler({ForbiddenAccessException.class})
    @ResponseBody
    public ResponseEntity<Object> forbiddenAccessException(ForbiddenAccessException e) {
        ResponseEntity.BodyBuilder bodyBuilder = ResponseEntity
                .status(HttpStatus.FORBIDDEN)
                .header("X-Backend-Status", String.valueOf(e.getMessage()));
        return bodyBuilder.body(null);
    }

    @ExceptionHandler({NotFoundUserException.class, NotFoundClaimException.class})
    @ResponseBody
    public ResponseEntity<Object> noSuchElementException(RuntimeException e) {
        ResponseEntity.BodyBuilder bodyBuilder = ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .header("X-Backend-Status", String.valueOf(e.getMessage()));
        return bodyBuilder.body(null);
    }

    @ExceptionHandler({ObjectOptimisticLockingFailureException.class, SQLException.class})
    @ResponseBody
    public ResponseEntity<Object> optimisticLockingFailureException(Exception e) {
        ResponseEntity.BodyBuilder bodyBuilder = ResponseEntity
                .status(HttpStatus.CONFLICT)
                .header("X-Backend-Status", String.valueOf(e.getMessage()));
        return bodyBuilder.body(null);
    }

}
