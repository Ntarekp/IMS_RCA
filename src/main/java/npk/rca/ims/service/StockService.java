package npk.rca.ims.service;


import npk.rca.ims.dto.StockMetricsDTO;
import npk.rca.ims.repository.StockTransactionRepository;
import npk.rca.ims.repository.ItemRepository;
import npk.rca.ims.model.Item;
import npk.rca.ims.service.ItemService;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class StockService {

    private final StockTransactionRepository stockTransactionRepository;
    private final ItemRepository itemRepository;
    private final ItemService itemService;

    @Autowired
    public StockService(StockTransactionRepository stockTransactionRepository, ItemRepository itemRepository, ItemService itemService) {
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
        long thisMonth = stockTransactionRepository.countByCreatedAtAfter(LocalDateTime.now().minusMonths(1));

        return new StockMetricsDTO(totalItems, lowStock, damaged, thisMonth);
    }

}
