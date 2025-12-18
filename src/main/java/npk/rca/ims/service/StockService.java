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
        long total = stockTransactionRepository.count();

        // Count low stock items (currentBalance < minimumStock)
        List<Item> items = itemRepository.findAll();
        long lowStock = items.stream()
                .filter(item -> {
                    Integer balance = itemService.getCurrentBalance(item.getId());
                    return balance < item.getMinimumStock();
                })
                .count();

        long damaged = stockTransactionRepository.countByStatus("Damaged");
        long thisMonth = stockTransactionRepository.countByCreatedDateAfter(LocalDateTime.now().minusMonths(1));

        return new StockMetricsDTO(total, lowStock, damaged, thisMonth);
    }

}
