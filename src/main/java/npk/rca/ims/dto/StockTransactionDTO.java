package npk.rca.ims.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import npk.rca.ims.model.TransactionType;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * StockTransactionDTO - Data Transfer Object for transactions
 *
 * Notice: We DON'T include the full Item object
 * Instead, we include:
 * - itemId (for creating transactions)
 * - itemName (for displaying transactions)
 *
 * This prevents circular references and reduces payload size
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class StockTransactionDTO {

    /**
     * Transaction ID (null for new transactions)
     */
    private Long id;

    /**
     * For CREATE: Frontend sends itemId
     * Backend looks up the Item entity
     */
    @NotNull(message = "Item ID is required")
    private Long itemId;


    private String itemName;

    /**
     * Transaction type: IN or OUT
     */
    @NotNull(message = "Transaction type is required")
    private TransactionType transactionType;

    /**
     * Quantity (always positive)
     */
    @NotNull(message = "Quantity is required")
    @Positive(message = "Quantity must be positive")
    private Integer quantity;

    /**
     * When did this transaction occur?
     * Defaults to today if not provided
     */
    @NotNull(message = "Transaction date is required")
    private LocalDate transactionDate;

    /**
     * Optional fields
     */
    private String referenceNumber;
    private String notes;
    private String recordedBy;

    /**
     * System timestamp (auto-filled)
     */
    private LocalDateTime createdAt;

    /**
     * REQUEST EXAMPLE (POST /api/transactions):
     * {
     *   "itemId": 1,
     *   "transactionType": "IN",
     *   "quantity": 50,
     *   "transactionDate": "2024-12-01",
     *   "referenceNumber": "PO-2024-001",
     *   "notes": "Monthly stock replenishment",
     *   "recordedBy": "John Doe"
     * }
     *
     * RESPONSE EXAMPLE:
     * {
     *   "id": 15,
     *   "itemId": 1,
     *   "itemName": "Rice",
     *   "transactionType": "IN",
     *   "quantity": 50,
     *   "transactionDate": "2024-12-01",
     *   "referenceNumber": "PO-2024-001",
     *   "notes": "Monthly stock replenishment",
     *   "recordedBy": "John Doe",
     *   "createdAt": "2024-12-01T14:30:00"
     * }
     */
}