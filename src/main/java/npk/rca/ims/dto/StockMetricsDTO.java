package npk.rca.ims.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class StockMetricsDTO {

    private long total;
    private long lowStock;
    private long damaged;
    private long thisMonth;
    
    // Comparison metrics (vs last month)
    private double totalChange;
    private double lowStockChange;
    private double damagedChange;
    private double thisMonthChange;
}
