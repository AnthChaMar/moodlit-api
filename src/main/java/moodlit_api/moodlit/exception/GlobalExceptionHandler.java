package moodlit_api.moodlit.exception;


import moodlit_api.moodlit.dto.AuthResponses;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    // Catches RuntimeExceptions from AuthService
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<AuthResponses.Error> handleRuntime(RuntimeException ex) {
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(new AuthResponses.Error(ex.getMessage()));
    }

    // Catches @Valid failures (blank email, short password, etc.)
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<AuthResponses.Error> handleValidation(
            MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .findFirst()
                .map(error -> error.getDefaultMessage())
                .orElse("Validation failed");

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(new AuthResponses.Error(message));
    }

    // Catches everything else
    @ExceptionHandler(Exception.class)
    public ResponseEntity<AuthResponses.Error> handleGeneral(Exception ex) {
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new AuthResponses.Error("Something went wrong. Please try again."));
    }
}