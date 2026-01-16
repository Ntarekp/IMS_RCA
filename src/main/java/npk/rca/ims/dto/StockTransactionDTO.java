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
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class StockTransactionDTO {

    private Long id;

    @NotNull(message = "Item ID is required")
    private Long itemId;

    private String itemName;

    // Unit of measurement (e.g., "Kg", "Pcs", "Liters", "Boxes")
    private String unit;

    @NotNull(message = "Transaction type is required")
    private TransactionType transactionType;

    @NotNull(message = "Quantity is required")
    @Positive(message = "Quantity must be positive")
    private Integer quantity;

    @NotNull(message = "Transaction date is required")
    private LocalDate transactionDate;

    private String referenceNumber;
    private String notes;
    private String recordedBy;

    private LocalDateTime createdAt;
    private Integer balanceAfter;

    // Supplier information
    private Long supplierId;
    private String supplierName;

    // Reversal information
    private boolean isReversed;
    private Long originalTransactionId;
}
