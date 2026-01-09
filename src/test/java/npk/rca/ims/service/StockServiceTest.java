package npk.rca.ims.service;

import npk.rca.ims.dto.StockMetricsDTO;
import npk.rca.ims.model.Item;
import npk.rca.ims.repository.ItemRepository;
import npk.rca.ims.repository.StockTransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StockServiceTest {

    @Mock
    private StockTransactionRepository stockTransactionRepository;

    @Mock
    private ItemRepository itemRepository;

    @Mock
    private ItemService itemService;

    private StockService stockService;

    @BeforeEach
    void setUp() {
        stockService = new StockService(stockTransactionRepository, itemRepository, itemService);
    }

    @Test
    void getMetrics_ShouldReturnCorrectMetrics() {
        // Setup Items
        Item item1 = new Item();
        item1.setId(1L);
        item1.setMinimumStock(10);
        item1.setDamagedQuantity(0);

        Item item2 = new Item();
        item2.setId(2L);
        item2.setMinimumStock(50);
        item2.setDamagedQuantity(5); // Damaged

        when(itemRepository.findAll()).thenReturn(Arrays.asList(item1, item2));

        // Setup Balances
        // Item 1: Balance 20 (OK)
        when(itemService.getCurrentBalance(1L)).thenReturn(20);
        // Item 2: Balance 40 (Low Stock)
        when(itemService.getCurrentBalance(2L)).thenReturn(40);

        // Setup Transaction Count
        when(stockTransactionRepository.countByCreatedAtAfter(any(LocalDateTime.class))).thenReturn(15L);

        StockMetricsDTO metrics = stockService.getMetrics();

        assertNotNull(metrics);
        assertEquals(2, metrics.getTotalItems());
        assertEquals(1, metrics.getLowStock()); // Item 2 is low stock
        assertEquals(1, metrics.getDamaged()); // Item 2 has damaged quantity
        assertEquals(15, metrics.getThisMonth());
        
        // Verify placeholders
        assertEquals(0.0, metrics.getTotalChange());
        assertEquals(0.0, metrics.getLowStockChange());
        assertEquals(0.0, metrics.getDamagedChange());
        assertEquals(0.0, metrics.getThisMonthChange());
    }

    @Test
    void getMetrics_ShouldHandleEmptyData() {
        when(itemRepository.findAll()).thenReturn(Collections.emptyList());
        when(stockTransactionRepository.countByCreatedAtAfter(any(LocalDateTime.class))).thenReturn(0L);

        StockMetricsDTO metrics = stockService.getMetrics();

        assertNotNull(metrics);
        assertEquals(0, metrics.getTotalItems());
        assertEquals(0, metrics.getLowStock());
        assertEquals(0, metrics.getDamaged());
        assertEquals(0, metrics.getThisMonth());
    }
}
