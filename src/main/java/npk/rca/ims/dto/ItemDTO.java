package npk.rca.ims.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * ItemDTO - Data Transfer Object for Item
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ItemDTO {

    private Long id;

    @NotBlank(message = "Item name is required")
    private String name;

    @NotBlank(message = "Unit is required")
    private String unit;

    private String category;

    @NotNull(message = "Minimum stock is required")
    @Positive(message = "Minimum stock must be positive")
    private Integer minimumStock;

    private String description;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    /**
     * Computed fields
     */
    private Integer currentBalance;
    private Integer totalIn;
    private Integer totalOut;
    private Boolean isLowStock;
    private Integer damagedQuantity;
}
