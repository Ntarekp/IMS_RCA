package npk.rca.ims.service;

import npk.rca.ims.dto.StockBalanceDTO;
import npk.rca.ims.model.Item;
import npk.rca.ims.repository.ItemRepository;
import npk.rca.ims.repository.StockTransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StockBalanceServiceTest {

    @Mock
    private ItemRepository itemRepository;

    @Mock
    private StockTransactionRepository transactionRepository;

    @InjectMocks
    private StockBalanceService stockBalanceService;

    private Item item1;
    private Item item2;

    @BeforeEach
    void setUp() {
        item1 = new Item();
        item1.setId(1L);
        item1.setName("Item 1");
        item1.setUnit("kg");
        item1.setMinimumStock(10);

        item2 = new Item();
        item2.setId(2L);
        item2.setName("Item 2");
        item2.setUnit("L");
        item2.setMinimumStock(50);
    }

    @Test
    @DisplayName("Should return correct balances for all items")
    void getAllBalances_ShouldReturnCorrectBalances() {
        when(itemRepository.findAll()).thenReturn(Arrays.asList(item1, item2));
        
        // Item 1: 100 IN, 20 OUT -> 80 Balance (OK)
        when(transactionRepository.getTotalInByItemId(1L)).thenReturn(100);
        when(transactionRepository.getTotalOutByItemId(1L)).thenReturn(20);
        
        // Item 2: 40 IN, 0 OUT -> 40 Balance (LOW)
        when(transactionRepository.getTotalInByItemId(2L)).thenReturn(40);
        when(transactionRepository.getTotalOutByItemId(2L)).thenReturn(0);

        List<StockBalanceDTO> result = stockBalanceService.getAllBalances();

        assertEquals(2, result.size());
        
        StockBalanceDTO dto1 = result.stream().filter(d -> d.getItemId().equals(1L)).findFirst().orElseThrow();
        assertEquals(80, dto1.getCurrentBalance());
        assertFalse(dto1.getIsLowStock());
        
        StockBalanceDTO dto2 = result.stream().filter(d -> d.getItemId().equals(2L)).findFirst().orElseThrow();
        assertEquals(40, dto2.getCurrentBalance());
        assertTrue(dto2.getIsLowStock());
    }

    @Test
    @DisplayName("Should return only low stock items")
    void getLowStockItems_ShouldReturnOnlyLowStockItems() {
        when(itemRepository.findAll()).thenReturn(Arrays.asList(item1, item2));
        
        // Item 1: OK
        when(transactionRepository.getTotalInByItemId(1L)).thenReturn(100);
        when(transactionRepository.getTotalOutByItemId(1L)).thenReturn(20);
        
        // Item 2: LOW
        when(transactionRepository.getTotalInByItemId(2L)).thenReturn(40);
        when(transactionRepository.getTotalOutByItemId(2L)).thenReturn(0);

        List<StockBalanceDTO> result = stockBalanceService.getLowStockItems();

        assertEquals(1, result.size());
        assertEquals(2L, result.get(0).getItemId());
    }
}
