package npk.rca.ims.service;

import lombok.RequiredArgsConstructor;
import npk.rca.ims.dto.AnalyticsSummaryDTO;
import npk.rca.ims.dto.MonthlyTrendDTO;
import npk.rca.ims.model.StockTransaction;
import npk.rca.ims.model.TransactionType;
import npk.rca.ims.repository.StockTransactionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.Month;
import java.time.format.TextStyle;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AnalyticsService {

    private final StockTransactionRepository transactionRepository;

    public AnalyticsSummaryDTO getAnalyticsSummary() {
        List<StockTransaction> allTransactions = transactionRepository.findAll();
        
        // 1. Stock Out Reasons Aggregation
        Map<String, Integer> stockOutReasons = new HashMap<>();
        int totalOut = 0;
        int totalLoss = 0; // Damaged + Expired
        int totalConsumed = 0;

        for (StockTransaction tx : allTransactions) {
            if (tx.getTransactionType() == TransactionType.OUT) {
                totalOut += tx.getQuantity();
                String reason = extractReason(tx.getNotes());
                stockOutReasons.put(reason, stockOutReasons.getOrDefault(reason, 0) + tx.getQuantity());
                
                if (isLoss(reason)) {
                    totalLoss += tx.getQuantity();
                } else {
                    totalConsumed += tx.getQuantity();
                }
            }
        }

        // 2. KPIs
        double wastageRatio = totalOut > 0 ? (double) totalLoss / totalOut * 100 : 0;
        
        // Calculate consumption rate (avg per month over last 6 months)
        LocalDate sixMonthsAgo = LocalDate.now().minusMonths(6);
        long monthsCount = 6; // Simplified
        double consumptionRate = (double) totalConsumed / monthsCount;

        // Calculate restock frequency
        long inTransactionCount = allTransactions.stream()
                .filter(t -> t.getTransactionType() == TransactionType.IN)
                .count();
        double restockFrequency = inTransactionCount > 0 ? (double) 180 / inTransactionCount : 0; // Days (approx 6 months)

        // 3. Monthly Trends (Last 6 Months)
        List<MonthlyTrendDTO> monthlyTrends = new ArrayList<>();
        for (int i = 5; i >= 0; i--) {
            LocalDate date = LocalDate.now().minusMonths(i);
            Month month = date.getMonth();
            int year = date.getYear();
            
            int monthlyIn = 0;
            int monthlyConsumed = 0;
            int monthlyLoss = 0;

            for (StockTransaction tx : allTransactions) {
                if (tx.getTransactionDate().getMonth() == month && tx.getTransactionDate().getYear() == year) {
                    if (tx.getTransactionType() == TransactionType.IN) {
                        monthlyIn += tx.getQuantity();
                    } else {
                        String reason = extractReason(tx.getNotes());
                        if (isLoss(reason)) {
                            monthlyLoss += tx.getQuantity();
                        } else {
                            monthlyConsumed += tx.getQuantity();
                        }
                    }
                }
            }
            monthlyTrends.add(new MonthlyTrendDTO(
                month.getDisplayName(TextStyle.SHORT, Locale.ENGLISH),
                monthlyIn,
                monthlyConsumed,
                monthlyLoss
            ));
        }

        // 4. Top Consumed Items
        Map<String, Integer> topItems = allTransactions.stream()
                .filter(t -> t.getTransactionType() == TransactionType.OUT && !isLoss(extractReason(t.getNotes())))
                .collect(Collectors.groupingBy(
                        t -> t.getItem().getName(),
                        Collectors.summingInt(StockTransaction::getQuantity)
                ))
                .entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .limit(5)
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e1, LinkedHashMap::new));

        return new AnalyticsSummaryDTO(
            Math.round(consumptionRate * 100.0) / 100.0,
            Math.round(wastageRatio * 100.0) / 100.0,
            Math.round(restockFrequency * 100.0) / 100.0,
            stockOutReasons,
            monthlyTrends,
            topItems
        );
    }

    private String extractReason(String notes) {
        if (notes == null || notes.isEmpty()) return "Unknown";
        // Assuming format "Reason: Details" from frontend
        if (notes.contains(":")) {
            return notes.split(":")[0].trim();
        }
        return "Consumed"; // Default if no prefix
    }

    private boolean isLoss(String reason) {
        return "Damaged".equalsIgnoreCase(reason) || "Expired".equalsIgnoreCase(reason) || "Lost".equalsIgnoreCase(reason);
    }
}
