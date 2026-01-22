package npk.rca.ims.controller;

import lombok.RequiredArgsConstructor;
import npk.rca.ims.dto.AnalyticsSummaryDTO;
import npk.rca.ims.dto.NotificationDTO;
import npk.rca.ims.dto.StockMetricsDTO;
import npk.rca.ims.dto.StockTransactionDTO;
import npk.rca.ims.service.AnalyticsService;
import npk.rca.ims.service.StockService;
import npk.rca.ims.service.StockTransactionService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * DashboardController - Dashboard statistics and metrics
 */
@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final StockService stockService;
    private final StockTransactionService transactionService;
    private final AnalyticsService analyticsService;

    /**
     * GET /api/dashboard/metrics
     * Get dashboard statistics
     */
    @GetMapping("/metrics")
    public ResponseEntity<StockMetricsDTO> getMetrics() {
        StockMetricsDTO metrics = stockService.getMetrics();
        return ResponseEntity.ok(metrics);
    }

    /**
     * GET /api/dashboard/chart-data
     * Get chart data for monthly transactions
     * Optional query params: ?year=2024
     */
    @GetMapping("/chart-data")
    public ResponseEntity<List<Map<String, Object>>> getChartData(
            @RequestParam(required = false) Integer year) {
        
        // Use AnalyticsService to get the robust monthly trends data
        // This ensures consistency with the Analytics tab
        AnalyticsSummaryDTO analyticsSummary = analyticsService.getAnalyticsSummary();
        
        // Map the MonthlyTrendDTO list to the format expected by the frontend dashboard chart
        List<Map<String, Object>> chartData = analyticsSummary.getMonthlyTrends().stream()
                .map(trend -> {
                    Map<String, Object> monthData = new HashMap<>();
                    monthData.put("name", trend.getMonth());
                    monthData.put("in", trend.getStockIn());
                    monthData.put("out", trend.getConsumed()); // Map 'consumed' to 'out' for dashboard compatibility
                    monthData.put("damaged", trend.getLoss()); // Add 'damaged' field
                    return monthData;
                })
                .collect(Collectors.toList());

        return ResponseEntity.ok(chartData);
    }

    /**
     * GET /api/dashboard/recent-transactions
     * Get recent transactions for activity feed
     * Optional query param: ?limit=10
     */
    @GetMapping("/recent-transactions")
    public ResponseEntity<List<StockTransactionDTO>> getRecentTransactions(
            @RequestParam(defaultValue = "10") int limit) {
        
        List<StockTransactionDTO> allTransactions = transactionService.getAllTransactions();
        
        // Sort by createdAt descending (most recent first) and limit
        List<StockTransactionDTO> recent = allTransactions.stream()
                .sorted((a, b) -> {
                    // Sort by transaction date first, then by createdAt if available
                    int dateCompare = b.getTransactionDate().compareTo(a.getTransactionDate());
                    if (dateCompare != 0) return dateCompare;
                    // If dates are equal, sort by createdAt if available
                    if (a.getCreatedAt() != null && b.getCreatedAt() != null) {
                        return b.getCreatedAt().compareTo(a.getCreatedAt());
                    }
                    return dateCompare;
                })
                .limit(limit)
                .collect(Collectors.toList());
        
        return ResponseEntity.ok(recent);
    }

    /**
     * GET /api/dashboard/notifications
     * Get system notifications (e.g., low stock alerts)
     */
    @GetMapping("/notifications")
    public ResponseEntity<List<NotificationDTO>> getNotifications() {
        return ResponseEntity.ok(stockService.getNotifications());
    }
}
