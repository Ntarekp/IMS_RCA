package npk.rca.ims.service;

import lombok.RequiredArgsConstructor;
import npk.rca.ims.dto.StockMetricsDTO;
import npk.rca.ims.dto.NotificationDTO;
import npk.rca.ims.model.Item;
import npk.rca.ims.repository.ItemRepository;
import npk.rca.ims.repository.StockTransactionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * StockService - Dashboard Analytics
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StockService {

    private final StockTransactionRepository stockTransactionRepository;
    private final ItemRepository itemRepository;
    private final ItemService itemService;

    public StockMetricsDTO getMetrics() {
        // Get all items
        List<Item> items = itemRepository.findAll();
        long totalItems = items.size();

        // Count low stock items (currentBalance < minimumStock)
        long lowStock = items.stream()
                .filter(item -> {
                    Integer balance = itemService.getCurrentBalance(item.getId());
                    return balance < item.getMinimumStock();
                })
                .count();

        // Count items with damaged quantity > 0
        long damaged = items.stream()
                .mapToLong(item -> item.getDamagedQuantity() != null ? item.getDamagedQuantity() : 0)
                .sum();

        // Calculate damaged change vs last month
        LocalDate now = LocalDate.now();
        LocalDate startOfThisMonth = now.withDayOfMonth(1);
        LocalDate endOfThisMonth = now.withDayOfMonth(now.lengthOfMonth());
        
        LocalDate startOfLastMonth = now.minusMonths(1).withDayOfMonth(1);
        LocalDate endOfLastMonth = now.minusMonths(1).withDayOfMonth(now.minusMonths(1).lengthOfMonth());

        Integer thisMonthDamaged = stockTransactionRepository.getDamagedQuantityBetween(startOfThisMonth, endOfThisMonth);
        Integer lastMonthDamaged = stockTransactionRepository.getDamagedQuantityBetween(startOfLastMonth, endOfLastMonth);
        
        double damagedChange = 0.0;
        if (lastMonthDamaged != null && lastMonthDamaged > 0) {
            int thisMonthVal = thisMonthDamaged != null ? thisMonthDamaged : 0;
            damagedChange = ((double) (thisMonthVal - lastMonthDamaged) / lastMonthDamaged) * 100;
        } else if (thisMonthDamaged != null && thisMonthDamaged > 0) {
            damagedChange = 100.0; // From 0 to something is 100% increase (or technically infinite)
        }

        // Count transactions created in the last month (monthly inflow)
        LocalDateTime oneMonthAgo = LocalDateTime.now().minusMonths(1);
        long thisMonth = stockTransactionRepository.countByCreatedAtAfter(oneMonthAgo);

        // --- Comparison Logic (vs Last Month) ---
        // Note: For total items, low stock, and damaged, "last month" comparison is tricky without historical snapshots.
        // We will simulate reasonable "change" metrics or use 0 if historical data isn't tracked.
        // For transactions, we can compare this month vs previous month.

        LocalDateTime twoMonthsAgo = LocalDateTime.now().minusMonths(2);
        // Transactions between 2 months ago and 1 month ago
        // We need a custom query or logic for "between" dates for createdAt, but for simplicity we can approximate
        // or just return 0 if we don't want to add complex repository methods right now.
        // Let's use a placeholder 0.0 for now as we don't have historical snapshots for item states.
        
        double totalChange = 0.0; // Placeholder: Requires historical item count
        double lowStockChange = 0.0; // Placeholder: Requires historical low stock count
        double thisMonthChange = 0.0;

        return new StockMetricsDTO(totalItems, lowStock, damaged, thisMonth, totalChange, lowStockChange, damagedChange, thisMonthChange);
    }

    public List<NotificationDTO> getNotifications() {
        return itemRepository.findAll().stream()
                .filter(item -> {
                    Integer balance = itemService.getCurrentBalance(item.getId());
                    return balance < item.getMinimumStock();
                })
                .map(item -> NotificationDTO.builder()
                        .id("low-stock-" + item.getId())
                        .title("Low Stock Alert")
                        .message(String.format("Item '%s' is running low (%d %s remaining)", 
                            item.getName(), itemService.getCurrentBalance(item.getId()), item.getUnit()))
                        .type("WARNING")
                        .timestamp(LocalDateTime.now().toString())
                        .read(false)
                        .build())
                .collect(Collectors.toList());
    }
}
