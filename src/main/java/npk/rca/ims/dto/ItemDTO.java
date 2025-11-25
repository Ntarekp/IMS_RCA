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
 *
 * WHY DO WE NEED DTOs?
 *
 * Problem with returning Entity directly:
 * 1. Exposes internal structure (security risk)
 * 2. Can cause lazy loading issues (N+1 query problem)
 * 3. Can't customize JSON response easily
 * 4. Circular reference problems with relationships
 *
 * DTO Benefits:
 * 1. Control what data is sent to frontend
 * 2. Different DTOs for different use cases
 * 3. Add computed fields (like currentBalance)
 * 4. Validation on incoming data
 *
 * Entity (Item.java) = Internal database model
 * DTO (ItemDTO.java) = External API contract
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ItemDTO {

    /**
     * For responses, id is populated
     * For create requests, id is null (auto-generated)
     */
    private Long id;

    /**
     * Validation annotations apply to incoming requests
     * When frontend sends data, Spring validates these
     */
    @NotBlank(message = "Item name is required")
    private String name;

    @NotBlank(message = "Unit is required")
    private String unit;

    @NotNull(message = "Minimum stock is required")
    @Positive(message = "Minimum stock must be positive")
    private Integer minimumStock;

    private String description;

    /**
     * Audit fields - only in responses, not in create requests
     * Frontend doesn't send these, backend fills them
     */
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    /**
     * EXTRA FIELD - not in Entity!
     * This will be calculated by Service layer
     * Shows current stock balance
     */
    private Integer currentBalance;

    /**
     * Another computed field
     * true if currentBalance < minimumStock
     */
    private Boolean isLowStock;

    /**
     * USAGE EXAMPLES:
     *
     * Creating new item (POST /api/items):
     * {
     *   "name": "Rice",
     *   "unit": "Sacks",
     *   "minimumStock": 10,
     *   "description": "Thai jasmine rice"
     * }
     *
     * Response (after save):
     * {
     *   "id": 1,
     *   "name": "Rice",
     *   "unit": "Sacks",
     *   "minimumStock": 10,
     *   "description": "Thai jasmine rice",
     *   "currentBalance": 0,
     *   "isLowStock": true,
     *   "createdAt": "2024-12-01T10:30:00",
     *   "updatedAt": "2024-12-01T10:30:00"
     * }
     *
     */
}