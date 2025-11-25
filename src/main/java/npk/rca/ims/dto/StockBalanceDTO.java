package npk.rca.ims.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import npk.rca.ims.model.TransactionType;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class StockBalanceDTO {

    private Long id;

    @NotNull(message = "Item ID is required")
    private Long itemId;
    private String itemName;

    @NotNull(message = "Transaction Type is required")
    private TransactionType transactionType;

    @NotNull(message = "Quantity is required")
    @Positive(message = "Quantitu must be positive")
    private Integer quantity;

    @NotNull(message = "Transaction date is required")
    private LocalDate transactionDate;

    private String referenceNumber;
    private String notes;
    private String recordedBy;

    private LocalDateTime createdAt;
}
