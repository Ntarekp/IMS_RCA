package npk.rca.ims.exceptions;

/**
 * Custom Exception - Thrown when resource is not found
 *
 * Examples:
 * - GET /api/items/999 → Item ID 999 doesn't exist
 * - POST /api/transactions with itemId=999 → Item not found
 *
 * Why custom exception?
 * - More meaningful than generic Exception
 * - Can be caught and handled specifically
 * - Better error messages to frontend
 *
 * RuntimeException vs Exception:
 * - RuntimeException = Unchecked (no need for try-catch)
 * - Exception = Checked (must be caught or declared)
 *
 * We use RuntimeException for cleaner code
 */
public class ResourceNotFoundException extends RuntimeException {


        public ResourceNotFoundException(String message) {
            super(message);
        }

        /**
         * Constructor with message and cause
         * Useful for wrapping other exceptions
         */
        public ResourceNotFoundException(String message, Throwable cause) {
            super(message, cause);
        }

        /**
         * USAGE IN SERVICE:
         *
         * Item item = itemRepository.findById(id)
         *     .orElseThrow(() -> new ResourceNotFoundException(
         *         "Item not found with id: " + id
         *     ));
         *
         * If item doesn't exist:
         * 1. Exception is thrown
         * 2. GlobalExceptionHandler catches it
         * 3. Returns 404 NOT FOUND to client
         */
    }