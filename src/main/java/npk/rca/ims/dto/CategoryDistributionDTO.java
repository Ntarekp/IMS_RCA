package npk.rca.ims.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CategoryDistributionDTO {
    private String category;
    private Integer itemCount;
    private Integer totalStock;
}
