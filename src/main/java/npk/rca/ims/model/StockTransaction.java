package npk.rca.ims.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "stock_transaction")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class StockTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "item_id", nullable = false)
    @NotNull(message = "Item is required")
    private Item item;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false,length = 10)
    @NotNull(message = "Transaction type is required")
    private TransactionType transactionType;

    @Positive(message = "Quantity must be positive")
    @Column(nullable = false)
    private Integer quantity;

    @Column(nullable = false)
    @NotNull(message = "Transaction date is required")
    private LocalDateTime transactionDate;

    @Column(length = 100)
    private String referenceNumber;

    @Column(length = 100)
    private String notes;

    @Column(length = 100)
    private String recordedBy;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
