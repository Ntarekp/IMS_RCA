package npk.rca.ims.controller;

import lombok.RequiredArgsConstructor;
import npk.rca.ims.dto.StockBalanceDTO;
import npk.rca.ims.service.StockTransactionService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * ReportController - Generate inventory reports
 *
 * This is what the school will use to:
 * - Check current stock levels
 * - Identify low stock items
 * - Generate printable reports
 * - Make purchasing decisions
 */
@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class ReportController {

    private final StockTransactionService transactionService;

    /**
     * GET /api/reports/balance
     * Generate complete stock balance report
     *
     * Shows all items with:
     * - Total IN quantity
     * - Total OUT quantity
     * - Current balance
     * - Low stock status
     *
     * Response example:
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
     *   },
     *   {
     *     "itemId": 3,
     *     "itemName": "Cooking Oil",
     *     "unit": "Liters",
     *     "totalIn": 50,
     *     "totalOut": 50,
     *     "currentBalance": 0,
     *     "minimumStock": 10,
     *     "isLowStock": true,
     *     "status": "CRITICAL"
     *   }
     * ]
     *
     * USE CASES:
     * - Weekly inventory check
     * - Monthly reports to management
     * - Identify what needs to be ordered
     */
    @GetMapping("/balance")
    public ResponseEntity<List<StockBalanceDTO>> getBalanceReport() {
        List<StockBalanceDTO> report = transactionService.generateBalanceReport();
        return ResponseEntity.ok(report);
    }

    /**
     * GET /api/reports/low-stock
     * Get items that need restocking
     *
     * Filters items where currentBalance < minimumStock
     *
     * Response: Same format as /balance but only low stock items
     *
     * USE CASES:
     * - Daily alerts
     * - Purchase order creation
     * - Emergency stock checks
     * - Dashboard warning indicators
     */
    @GetMapping("/low-stock")
    public ResponseEntity<List<StockBalanceDTO>> getLowStockReport() {
        List<StockBalanceDTO> report = transactionService.getLowStockItems();
        return ResponseEntity.ok(report);
    }

    /**
     * TESTING:
     *
     * 1. Get full balance report:
     * curl http://localhost:8080/api/reports/balance
     *
     * 2. Get low stock alerts:
     * curl http://localhost:8080/api/reports/low-stock
     *
     * FRONTEND INTEGRATION IDEAS:
     *
     * Dashboard View:
     * - Show total items
     * - Show items in CRITICAL status (balance = 0)
     * - Show items in LOW status
     * - Show items ADEQUATE
     *
     * Alert System:
     * - If any item is CRITICAL → Red alert banner
     * - If any item is LOW → Yellow warning
     * - Send email/SMS to procurement officer
     *
     * Purchasing Workflow:
     * 1. Check /api/reports/low-stock daily
     * 2. For each low item, calculate needed quantity:
     *    neededQty = minimumStock * 2 - currentBalance
     * 3. Generate purchase order
     * 4. When stock arrives, record via POST /api/transactions (IN)
     *
     * Report Formatting:
     * - Export to PDF (use library like iText)
     * - Export to Excel (use Apache POI)
     * - Print for physical records
     *
     * REAL-WORLD EXAMPLE:
     *
     * Monday morning check:
     * GET /api/reports/low-stock
     *
     * Response:
     * [
     *   {
     *     "itemName": "Rice",
     *     "currentBalance": 8,
     *     "minimumStock": 30,
     *     "status": "LOW"
     *   },
     *   {
     *     "itemName": "Cooking Oil",
     *     "currentBalance": 0,
     *     "minimumStock": 10,
     *     "status": "CRITICAL"
     *   }
     * ]
     *
     * Action:
     * - Order 50 sacks of Rice
     * - Order 20 liters of Cooking Oil (URGENT!)
     *
     */
}