package uz.server.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import uz.server.domain.exception.BaseException;

@RestControllerAdvice
@Slf4j
public class RestExceptionHandler {
    @ExceptionHandler(BaseException.class)
    public ResponseEntity<String> handleBaseException(BaseException e) {
        log.error("BaseException: {}", e.getMessage());
        return ResponseEntity.ok().body(e.getMessage());
    }
}
