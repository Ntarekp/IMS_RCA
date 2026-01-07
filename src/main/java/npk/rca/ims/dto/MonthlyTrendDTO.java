package npk.rca.ims.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MonthlyTrendDTO {
    private String month; // "Jan", "Feb", etc.
    private int stockIn;
    private int consumed;
    private int loss; // Damaged + Expired
}
