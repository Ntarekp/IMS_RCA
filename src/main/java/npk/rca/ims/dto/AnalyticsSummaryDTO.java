package npk.rca.ims.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AnalyticsSummaryDTO {

    private double consumptionRate; // Avg items consumed per month
    private double wastageRatio; // (Damaged + Expired) / Total Stock Out
    private double restockFrequency; // Avg days between IN transactions
    
    // Charts Data
    private Map<String, Integer> stockOutReasons; // Reason -> Quantity
    private List<MonthlyTrendDTO> monthlyTrends;
    private Map<String, Integer> topConsumedItems; // Item Name -> Quantity
}
