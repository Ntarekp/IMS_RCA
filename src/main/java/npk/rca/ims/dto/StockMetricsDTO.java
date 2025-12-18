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
}
