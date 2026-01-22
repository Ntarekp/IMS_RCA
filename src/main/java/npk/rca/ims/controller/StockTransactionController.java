package npk.rca.ims.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import npk.rca.ims.dto.StockTransactionDTO;
import npk.rca.ims.service.StockTransactionService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * StockTransactionController - REST API for stock movements
 *
 * Handles all stock IN/OUT operations
 */
@RestController
@RequestMapping("/api/transactions")
@RequiredArgsConstructor
public class StockTransactionController {

    private final StockTransactionService transactionService;

    /**
     * GET /api/transactions
     * List all transactions
     *
     * Optional query parameter: ?itemId=1
     * Filters transactions by item
     *
     * Examples:
     * GET /api/transactions → All transactions
     * GET /api/transactions?itemId=1 → Only Rice transactions
     */
    @GetMapping
    public ResponseEntity<List<StockTransactionDTO>> getAllTransactions(
            @RequestParam(required = false) Long itemId) {

        if (itemId != null) {
            // Filter by item
            List<StockTransactionDTO> transactions =
                    transactionService.getTransactionsByItemId(itemId);
            return ResponseEntity.ok(transactions);
        } else {
            // Get all
            List<StockTransactionDTO> transactions =
                    transactionService.getAllTransactions();
            return ResponseEntity.ok(transactions);
        }
    }

    /**
     * GET /api/transactions/date-range
     * Get transactions within date range
     *
     * Query parameters: ?startDate=2024-01-01&endDate=2024-12-31
     *
     * @DateTimeFormat - Converts string to LocalDate
     */
    @GetMapping("/date-range")
    public ResponseEntity<List<StockTransactionDTO>> getTransactionsByDateRange(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        List<StockTransactionDTO> transactions =
                transactionService.getTransactionsByDateRange(startDate, endDate);
        return ResponseEntity.ok(transactions);
    }

    /**
     * POST /api/transactions
     * Record new stock movement (IN or OUT)
     *
     * THIS IS THE MOST IMPORTANT ENDPOINT!
     *
     * Request body example (Stock IN):
     * {
     *   "itemId": 1,
     *   "transactionType": "IN",
     *   "quantity": 50,
     *   "transactionDate": "2024-12-01",
     *   "referenceNumber": "PO-2024-001",
     *   "notes": "Monthly stock delivery",
     *   "recordedBy": "John Doe"
     * }
     *
     * Request body example (Stock OUT):
     * {
     *   "itemId": 1,
     *   "transactionType": "OUT",
     *   "quantity": 20,
     *   "transactionDate": "2024-12-01",
     *   "referenceNumber": "REQ-2024-050",
     *   "notes": "Used for school lunch",
     *   "recordedBy": "Jane Smith"
     * }
     *
     * Response: 201 CREATED
     */
    @PostMapping
    public ResponseEntity<StockTransactionDTO> recordTransaction(
            @Valid @RequestBody StockTransactionDTO transactionDTO) {

        StockTransactionDTO created =
                transactionService.recordTransaction(transactionDTO);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    /**
     * PUT /api/transactions/{id}
     * Update transaction metadata (notes, reference number)
     * 
     * Does NOT allow changing quantity, item, or type to preserve audit trail.
     */
    @PutMapping("/{id}")
    public ResponseEntity<StockTransactionDTO> updateTransaction(
            @PathVariable Long id,
            @RequestBody StockTransactionDTO transactionDTO) {
        
        StockTransactionDTO updated = transactionService.updateTransaction(id, transactionDTO);
        return ResponseEntity.ok(updated);
    }

    /**
     * DELETE /api/transactions/{id}
     * Reverse a transaction
     * 
     * Does not physically delete, but creates a counter-transaction.
     * 
     * Optional query param: ?reason=Entered%20by%20mistake
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<StockTransactionDTO> reverseTransaction(
            @PathVariable Long id,
            @RequestParam(required = false, defaultValue = "Reversed by user") String reason,
            @RequestParam(required = false, defaultValue = "System") String reversedBy) {
        
        StockTransactionDTO reversal = transactionService.reverseTransaction(id, reason, reversedBy);
        return ResponseEntity.ok(reversal);
    }

    /**
     * POST /api/transactions/{id}/undo-reverse
     * Undo a reversed transaction
     */
    @PostMapping("/{id}/undo-reverse")
    public ResponseEntity<StockTransactionDTO> undoReverseTransaction(@PathVariable Long id) {
        StockTransactionDTO result = transactionService.undoReverseTransaction(id);
        return ResponseEntity.ok(result);
    }

    /**
     * TESTING SCENARIOS:
     *
     * Scenario 1: Add stock (IN)
     * curl -X POST http://localhost:8080/api/transactions \
     *   -H "Content-Type: application/json" \
     *   -d '{
     *     "itemId": 1,
     *     "transactionType": "IN",
     *     "quantity": 100,
     *     "transactionDate": "2024-12-01",
     *     "referenceNumber": "PO-001",
     *     "recordedBy": "Admin"
     *   }'
     *
     * Scenario 2: Use stock (OUT)
     * curl -X POST http://localhost:8080/api/transactions \
     *   -H "Content-Type: application/json" \
     *   -d '{
     *     "itemId": 1,
     *     "transactionType": "OUT",
     *     "quantity": 30,
     *     "transactionDate": "2024-12-02",
     *     "recordedBy": "Admin"
     *   }'
     *
     * Scenario 3: Try to OUT more than available (SHOULD FAIL)
     * curl -X POST http://localhost:8080/api/transactions \
     *   -H "Content-Type: application/json" \
     *   -d '{
     *     "itemId": 1,
     *     "transactionType": "OUT",
     *     "quantity": 1000,
     *     "transactionDate": "2024-12-03"
     *   }'
     *
     * Expected error: "Insufficient stock! Available: 70, Requested: 1000"
     *
     * Scenario 4: Get transactions for item 1
     * curl http://localhost:8080/api/transactions?itemId=1
     *
     * Scenario 5: Get transactions in date range
     * curl "http://localhost:8080/api/transactions/date-range?startDate=2024-12-01&endDate=2024-12-31"
     *
     * KEY FEATURES:
     *
     * 1. FLEXIBLE FILTERING:
     *    - All transactions
     *    - By item
     *    - By date range
     *
     * 2. VALIDATION:
     *    - Required fields checked
     *    - Quantity must be positive
     *    - Item must exist
     *    - Sufficient stock for OUT
     *
     * 3. IMMUTABILITY:
     *    - No UPDATE or DELETE endpoints
     *    - Transactions are permanent records
     *    - Audit trail preserved
     *
     */
}
