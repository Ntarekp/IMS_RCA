package npk.rca.ims.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * StockBalanceDTO - Report data for current stock levels
 *
 * This DTO is ONLY for responses (GET requests)
 * Not used for creating/updating data
 *
 * Use case: Generate balance report showing all items and their stock
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class StockBalanceDTO {

    /**
     * Item details
     */
    private Long itemId;
    private String itemName;
    private String unit;

    /**
     * Stock calculations
     */
    private Integer totalIn;
    private Integer totalOut;
    private Integer currentBalance;

    /**
     * Alert information
     */
    private Integer minimumStock;
    private Boolean isLowStock;

    /**
     * Status indicator for frontend
     * "CRITICAL" - balance is 0 or negative
     * "LOW" - balance < minimum
     * "ADEQUATE" - balance >= minimum
     */
    private String status;

    /**
     * Calculate status based on balance
     */
    public String getStatus() {
        if (currentBalance <= 0) {
            return "CRITICAL";
        } else if (isLowStock) {
            return "LOW";
        } else {
            return "ADEQUATE";
        }
    }

    /**
     * REPORT RESPONSE EXAMPLE:
     * GET /api/reports/balance
     *
     * [
     *   {
     *     "itemId": 1,
     *     "itemName": "Rice",
     *     "unit": "Sacks",
     *     "totalIn": 200,
     *     "totalOut": 150,
     *     "currentBalance": 50,
     *     "minimumStock": 30,
     *     "isLowStock": false,
     *     "status": "ADEQUATE"
     *   },
     *   {
     *     "itemId": 2,
     *     "itemName": "Beans",
     *     "unit": "Kg",
     *     "totalIn": 100,
     *     "totalOut": 95,
     *     "currentBalance": 5,
     *     "minimumStock": 20,
     *     "isLowStock": true,
     *     "status": "LOW"
     *   }
     * ]
     *
     * Frontend can use this to:
     * - Display inventory dashboard
     * - Show alerts for low stock
     * - Generate PDF reports
     * - Trigger automatic reorder
     */
}