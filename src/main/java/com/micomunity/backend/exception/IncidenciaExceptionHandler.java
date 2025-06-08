package com.micomunity.backend.exception;

import com.micomunity.backend.dto.ErrorResponse;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import java.util.Date;

@RestControllerAdvice
public class IncidenciaExceptionHandler {
    
    @ExceptionHandler(IncidenciaNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ErrorResponse handleIncidenciaNotFound(IncidenciaNotFoundException ex) {
        return new ErrorResponse(new Date(), ex.getMessage(), "Not Found");
    }

    @ExceptionHandler(BadRequestException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleBadRequest(BadRequestException ex) {
        return new ErrorResponse(new Date(), ex.getMessage(), "Bad Request");
    }

    @ExceptionHandler(ForbiddenException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public ErrorResponse handleForbidden(ForbiddenException ex) {
        return new ErrorResponse(new Date(), ex.getMessage(), "Forbidden");
    }
} 