package npk.rca.ims.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ItemDTO {

    private Long id;

    @NotBlank(message = "Item name is required")
    private String name;

    @NotBlank(message = "Unit is required")
    private String unit;

    @NotNull(message = "Minimum stock is required")
    @Positive(message = "Minimum stock must be positive")
    private Double minimumStock;

    private String description;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    private Integer currentBalalnce;

    private Boolean isLowStock;
}
