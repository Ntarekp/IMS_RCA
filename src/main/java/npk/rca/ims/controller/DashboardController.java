package npk.rca.ims.controller;

import lombok.RequiredArgsConstructor;
import npk.rca.ims.dto.StockMetricsDTO;
import npk.rca.ims.dto.StockTransactionDTO;
import npk.rca.ims.service.StockService;
import npk.rca.ims.service.StockTransactionService;
import org.springframework.format.annotation.DateTimeFormat;
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
@CrossOrigin(origins = "*")
public class DashboardController {

    private final StockService stockService;
    private final StockTransactionService transactionService;

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
        
        if (year == null) {
            year = LocalDate.now().getYear();
        }
        
        LocalDate startDate = LocalDate.of(year, 1, 1);
        LocalDate endDate = LocalDate.of(year, 12, 31);
        
        List<StockTransactionDTO> transactions = transactionService.getTransactionsByDateRange(startDate, endDate);
        
        // Group by month and calculate IN/OUT totals
        Map<Integer, Map<String, Integer>> monthlyData = new HashMap<>();
        
        // Initialize all months
        for (int month = 1; month <= 12; month++) {
            monthlyData.put(month, new HashMap<>());
            monthlyData.get(month).put("in", 0);
            monthlyData.get(month).put("out", 0);
        }
        
        // Aggregate transactions by month
        for (StockTransactionDTO tx : transactions) {
            int month = tx.getTransactionDate().getMonthValue();
            Map<String, Integer> monthData = monthlyData.get(month);
            
            if (tx.getTransactionType().toString().equals("IN")) {
                monthData.put("in", monthData.get("in") + tx.getQuantity());
            } else {
                monthData.put("out", monthData.get("out") + tx.getQuantity());
            }
        }
        
        // Convert to response format
        String[] monthNames = {"Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"};
        List<Map<String, Object>> chartData = monthlyData.entrySet().stream()
                .map(entry -> {
                    Map<String, Object> monthData = new HashMap<>();
                    monthData.put("name", monthNames[entry.getKey() - 1]);
                    monthData.put("in", entry.getValue().get("in"));
                    monthData.put("out", entry.getValue().get("out"));
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
}

