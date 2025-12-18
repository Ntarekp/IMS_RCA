package npk.rca.ims.service;

import npk.rca.ims.dto.StockMetricsDTO;
import npk.rca.ims.repository.StockTransactionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class StockService {

    @Autowired
    private StockTransactionRepository stockTransactionRepository;
    public StockMetricsDTO getMetrics(){
        long total= stockTransactionRepository.count();

        //Count low stock items
        long lowStock = stockTransactionRepositoryController.countByQuantityLessThan(10);

        long damaged = stockTransactionRepository.countByStatus("Damaged");

        long thisMonth = stockTransactionRepository.countByCreatedDateAfter(LocalDateTime.now().minusMonths(1));

        return new StockMetricsDTO(total, lowStock, damaged, thisMonth);
    }

}
