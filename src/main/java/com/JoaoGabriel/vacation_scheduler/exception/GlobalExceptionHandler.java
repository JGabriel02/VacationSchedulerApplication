package com.JoaoGabriel.vacation_scheduler.exception;

import com.JoaoGabriel.vacation_scheduler.auth.exception.InvalidCredentialsException;
import com.JoaoGabriel.vacation_scheduler.auth.exception.TooManyLoginAttemptsException;
import com.JoaoGabriel.vacation_scheduler.employee.exception.EmailAlreadyExistsException;
import com.JoaoGabriel.vacation_scheduler.employee.exception.ManagerCodeNotFoundException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import com.JoaoGabriel.vacation_scheduler.vacation.exception.InvalidVacationException;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(EmailAlreadyExistsException.class)
    public ResponseEntity<Map<String, String>> handleEmailAlreadyExists(
            EmailAlreadyExistsException exception) {

        Map<String, String> body = Map.of(
                "message", exception.getMessage()
        );

        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(body);
    }

    @ExceptionHandler(InvalidCredentialsException.class)
    public ResponseEntity<Map<String, String>> handleInvalidCredentials(
            InvalidCredentialsException exception) {

        Map<String, String> body = Map.of(
                "message", exception.getMessage()
        );

        return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .body(body);
    }
    @ExceptionHandler(TooManyLoginAttemptsException.class)
    public ResponseEntity<Map<String, String>> handleTooManyLoginAttempts(
            TooManyLoginAttemptsException exception) {

        return ResponseEntity
                .status(HttpStatus.TOO_MANY_REQUESTS)
                .header(
                        HttpHeaders.RETRY_AFTER,
                        String.valueOf(exception.getRetryAfterSeconds())
                )
                .body(Map.of("message", exception.getMessage()));
    }

    @ExceptionHandler(ManagerCodeNotFoundException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String, String> handleManagerCodeNotFound(
            ManagerCodeNotFoundException exception
    ) {
        return Map.of(
                "message",
                exception.getMessage()
        );
    }

    /*
     * Fallback para violações de constraint do banco (unique, FK etc.)
     * que escaparem das validações dos services, evitando um 500 genérico.
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public Map<String, String> handleDataIntegrityViolation(
            DataIntegrityViolationException exception
    ) {
        return Map.of(
                "message",
                "A operação conflita com dados já existentes"
        );
    }

    @ExceptionHandler(InvalidVacationException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String, String> handleInvalidVacation(
            InvalidVacationException exception
    ) {
        return Map.of(
                "message",
                exception.getMessage()
        );
    }
}