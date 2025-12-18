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

@Entity
@Table(name = "items")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Item {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Item name is required")
    @Column(nullable = false, length = 100)
    private String name;

    @NotBlank(message = "Unit is required")
    @Column(nullable = false, length = 50)
    private String unit;

    /**
     * Damaged quantity for this item (e.g., spoiled, broken, unusable)
     * Default is 0. This is updated when damaged stock is recorded.
     */
    @Column(nullable = false)
    private int damagedQuantity = 0;

    @NotNull(message = "Minimum stock level is required")
    @Positive(message = "Minimum stock must be positive")
    @Column(nullable = false)
    private Integer minimumStock;

    @Column(length = 500)
    private String description;

    /**
     * Audit Fields (Who/When was this created/modified?)
     */
    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedAt;
}