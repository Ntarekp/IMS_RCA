package npk.rca.ims.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * StockTransaction Entity
 *
 * Records every stock movement (IN/OUT) in the system
 * This is the "journal" of all inventory changes
 *
 * KEY CONCEPTS:
 * 1. @ManyToOne - Many transactions belong to ONE item
 * 2. @JoinColumn - Specifies foreign key column name
 * 3. @Enumerated - Stores enum as string in database
 * 4. This is an IMMUTABLE record - once created, never modified
 */
@Entity
@Table(name = "stock_transactions")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class StockTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * RELATIONSHIP: Many-to-One
     *
     * Many transactions can reference ONE item
     * Example:
     *   - 100 sacks of rice IN (transaction 1)
     *   - 50 sacks of rice OUT (transaction 2)
     *   - 30 sacks of rice IN (transaction 3)
     * All three transactions point to the same Item (Rice)
     *
     * @ManyToOne - Defines relationship type
     * @JoinColumn - Creates foreign key column "item_id" in this table
     * fetch = LAZY - Don't load Item data unless explicitly requested (performance)
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "item_id", nullable = false)
    @NotNull(message = "Item is required")
    private Item item;

    /**
     * Transaction Type (IN or OUT)
     * @Enumerated(STRING) - Store as "IN" or "OUT" in database
     * Alternative: ORDINAL would store as 0 or 1 (bad - unclear)
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    @NotNull(message = "Transaction type is required")
    private TransactionType transactionType;

    /**
     * Quantity moved (always positive)
     * Whether it's IN or OUT is determined by transactionType
     */
    @Positive(message = "Quantity must be positive")
    @Column(nullable = false)
    private Integer quantity;

    /**
     * Date of transaction (when did this happen?)
     * LocalDate = just date (2024-12-01)
     * LocalDateTime = date + time (2024-12-01 14:30:00)
     */
    @NotNull(message = "Transaction date is required")
    @Column(nullable = false)
    private LocalDate transactionDate;

    /**
     * Reference number (PO number, requisition number, etc.)
     * Optional field for tracking
     */
    @Column(length = 100)
    private String referenceNumber;

    /**
     * Optional notes (e.g., "Donated by XYZ Company")
     */
    @Column(length = 500)
    private String notes;

    /**
     * Who recorded this transaction? (username or staff ID)
     * In Phase 4, we'll link this to a User entity
     */
    @Column(length = 100)
    private String recordedBy;

    /**
     * System timestamp - when was this record created in the system?
     * Different from transactionDate:
     *   - transactionDate = when the physical movement happened
     *   - createdAt = when it was entered into the computer
     */
    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;
}