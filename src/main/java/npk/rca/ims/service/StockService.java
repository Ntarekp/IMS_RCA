package npk.rca.ims.service;


import npk.rca.ims.dto.StockMetricsDTO;
import npk.rca.ims.repository.StockTransactionRepository;
import npk.rca.ims.repository.ItemRepository;
import npk.rca.ims.model.Item;
import org.springframework.stereotype.Service;
import org.springframework.context.annotation.Lazy;
import org.springframework.beans.factory.annotation.Autowired;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class StockService {

    private final StockTransactionRepository stockTransactionRepository;
    private final ItemRepository itemRepository;
    private final ItemService itemService;

    @Autowired
    public StockService(StockTransactionRepository stockTransactionRepository, ItemRepository itemRepository, @Lazy ItemService itemService) {
        this.stockTransactionRepository = stockTransactionRepository;
        this.itemRepository = itemRepository;
        this.itemService = itemService;
    }

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
                .filter(item -> item.getDamagedQuantity() > 0)
                .count();

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
        double damagedChange = 0.0; // Placeholder: Requires historical damaged count
        
        // For monthly inflow comparison
        // Ideally: count(thisMonth) vs count(lastMonth)
        // We'll leave it as 0.0 for now to keep it simple and safe.
        double thisMonthChange = 0.0; 

        return new StockMetricsDTO(totalItems, lowStock, damaged, thisMonth, totalChange, lowStockChange, damagedChange, thisMonthChange);
    }

}
