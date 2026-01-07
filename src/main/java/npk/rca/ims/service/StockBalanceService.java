package npk.rca.ims.service;

import lombok.RequiredArgsConstructor;
import npk.rca.ims.dto.StockBalanceDTO;
import npk.rca.ims.model.Item;
import npk.rca.ims.repository.ItemRepository;
import npk.rca.ims.repository.StockTransactionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StockBalanceService {

    private final ItemRepository itemRepository;
    private final StockTransactionRepository transactionRepository;

    public List<StockBalanceDTO> getAllBalances() {
        return itemRepository.findAll().stream()
                .map(this::calculateItemBalance)
                .collect(Collectors.toList());
    }

    public List<StockBalanceDTO> getLowStockItems() {
        return getAllBalances().stream()
                .filter(StockBalanceDTO::getIsLowStock)
                .collect(Collectors.toList());
    }

    private StockBalanceDTO calculateItemBalance(Item item) {
        Integer totalIn = transactionRepository.getTotalInByItemId(item.getId());
        Integer totalOut = transactionRepository.getTotalOutByItemId(item.getId());
        
        int in = (totalIn != null) ? totalIn : 0;
        int out = (totalOut != null) ? totalOut : 0;
        int balance = in - out;

        StockBalanceDTO dto = new StockBalanceDTO();
        dto.setItemId(item.getId());
        dto.setItemName(item.getName());
        dto.setUnit(item.getUnit());
        dto.setTotalIn(in);
        dto.setTotalOut(out);
        dto.setCurrentBalance(balance);
        dto.setMinimumStock(item.getMinimumStock());
        dto.setIsLowStock(balance < item.getMinimumStock());
        // status is calculated by DTO's getStatus() method

        return dto;
    }
}
