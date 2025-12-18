package npk.rca.ims.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * Item Entity - Represents items in the school inventory (Rice, Beans, Oil, etc.)
 *
 * KEY CONCEPTS TO UNDERSTAND:
 * 1. @Entity - Tells JPA "This class is a database table"
 * 2. @Table - Specifies the actual table name in MySQL
 * 3. @Data (Lombok) - Auto-generates getters, setters, toString, equals, hashCode
 * 4. @NoArgsConstructor - Creates empty constructor (required by JPA)
 * 5. @AllArgsConstructor - Creates constructor with all fields
 */
@Entity
@Table(name = "items")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Item {

    /**
     * Primary Key Configuration
     * @Id - Marks this field as primary key
     * @GeneratedValue - Auto-increment (MySQL handles this)
     * IDENTITY strategy = MySQL's AUTO_INCREMENT
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Item Name (e.g., "Rice", "Beans", "Cooking Oil")
     * @NotBlank - Validation: Cannot be null or empty
     * @Column - Customizes database column
     *   - nullable = false: Database enforces NOT NULL
     *   - length = 100: VARCHAR(100) in MySQL
     */
    @NotBlank(message = "Item name is required")
    @Column(nullable = false, length = 100)
    private String name;

    /**
     * Measurement Unit (e.g., "Sacks", "Liters", "Kg")
     * @NotBlank - Must provide a unit
     */

    @NotBlank(message = "Unit is required")
    @Column(nullable = false, length = 50)
    private String unit;

    /**
     * Damaged quantity for this item (e.g., spoiled, broken, unusable)
     * Default is 0. This is updated when damaged stock is recorded.
     */
    @Column(nullable = false)
    private int damagedQuantity = 0;

    /**
     * Minimum Stock Level (alert threshold)
     * When balance falls below this, it's "low stock"
     * @Positive - Must be greater than 0
     */
    @NotNull(message = "Minimum stock level is required")
    @Positive(message = "Minimum stock must be positive")
    @Column(nullable = false)
    private Integer minimumStock;

    /**
     * Optional description field
     * nullable = true (default) - Can be empty
     */
    @Column(length = 500)
    private String description;

    /**
     * Audit Fields (Who/When was this created/modified?)
     * @CreationTimestamp - Auto-fills when record is created
     * @UpdateTimestamp - Auto-updates when record is modified
     * updatable = false - createdAt never changes after creation
     */
    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    public int getDamagedQuantity() {
        return damagedQuantity;
    }

    public void setDamagedQuantity(int damagedQuantity) {
        this.damagedQuantity = damagedQuantity;
    }
}