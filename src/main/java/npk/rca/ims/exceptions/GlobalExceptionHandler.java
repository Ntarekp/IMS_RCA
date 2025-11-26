package npk.rca.ims.exception;

import npk.rca.ims.exceptions.ResourceNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * GlobalExceptionHandler - Centralized exception handling
 *
 * @RestControllerAdvice:
 * - Catches exceptions from ALL controllers
 * - Converts exceptions to proper HTTP responses
 * - Returns JSON error messages
 *
 * Without this:
 * - Client gets 500 errors with stack traces (ugly!)
 * - No consistent error format
 * - Security risk (exposes internal details)
 *
 * With this:
 * - Clean JSON error responses
 * - Proper HTTP status codes
 * - User-friendly messages
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Handle ResourceNotFoundException
     * Returns 404 NOT FOUND
     *
     * @ExceptionHandler - Catches specific exception type
     */
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleResourceNotFound(
            ResourceNotFoundException ex) {

        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("timestamp", LocalDateTime.now());
        errorResponse.put("status", HttpStatus.NOT_FOUND.value());
        errorResponse.put("error", "Not Found");
        errorResponse.put("message", ex.getMessage());

        return new ResponseEntity<>(errorResponse, HttpStatus.NOT_FOUND);
    }

    /**
     * Handle Validation Errors
     * Returns 400 BAD REQUEST
     *
     * Triggered when @Valid fails in controller
     * Example: @NotBlank validation fails
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidationErrors(
            MethodArgumentNotValidException ex) {

        Map<String, String> fieldErrors = new HashMap<>();

        // Extract all validation errors
        ex.getBindingResult().getAllErrors().forEach(error -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            fieldErrors.put(fieldName, errorMessage);
        });

        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("timestamp", LocalDateTime.now());
        errorResponse.put("status", HttpStatus.BAD_REQUEST.value());
        errorResponse.put("error", "Validation Failed");
        errorResponse.put("fieldErrors", fieldErrors);

        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }

    /**
     * Handle IllegalArgumentException
     * Returns 400 BAD REQUEST
     *
     * For business logic errors
     * Example: "Cannot delete item with existing transactions"
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalArgument(
            IllegalArgumentException ex) {

        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("timestamp", LocalDateTime.now());
        errorResponse.put("status", HttpStatus.BAD_REQUEST.value());
        errorResponse.put("error", "Bad Request");
        errorResponse.put("message", ex.getMessage());

        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }

    /**
     * Handle all other exceptions (catch-all)
     * Returns 500 INTERNAL SERVER ERROR
     *
     * For unexpected errors
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGenericException(
            Exception ex) {

        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("timestamp", LocalDateTime.now());
        errorResponse.put("status", HttpStatus.INTERNAL_SERVER_ERROR.value());
        errorResponse.put("error", "Internal Server Error");
        errorResponse.put("message", "An unexpected error occurred");
        // Don't expose internal error details in production!

        return new ResponseEntity<>(errorResponse, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    /**
     * ERROR RESPONSE EXAMPLES:
     *
     * 1. Resource Not Found (404):
     * {
     *   "timestamp": "2024-12-01T10:30:00",
     *   "status": 404,
     *   "error": "Not Found",
     *   "message": "Item not found with id: 999"
     * }
     *
     * 2. Validation Error (400):
     * {
     *   "timestamp": "2024-12-01T10:30:00",
     *   "status": 400,
     *   "error": "Validation Failed",
     *   "fieldErrors": {
     *     "name": "Item name is required",
     *     "minimumStock": "Minimum stock must be positive"
     *   }
     * }
     *
     * WHY THIS MATTERS:
     * - Frontend gets consistent error structure
     * - Easy to display validation errors on form fields
     * - Proper HTTP status codes for REST API standards
     * - Better debugging and logging
     */
}