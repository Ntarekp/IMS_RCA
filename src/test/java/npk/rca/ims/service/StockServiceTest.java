package npk.rca.ims.service;

import npk.rca.ims.dto.StockMetricsDTO;
import npk.rca.ims.model.Item;
import npk.rca.ims.repository.ItemRepository;
import npk.rca.ims.repository.StockTransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

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
    @DisplayName("Should return correct metrics for mixed item states")
    void getMetrics_ShouldReturnCorrectMetrics() {
        // Arrange
        Item normalItem = createItem(1L, 10, 0); // Min 10, Damaged 0
        Item lowStockItem = createItem(2L, 50, 0); // Min 50, Damaged 0
        Item damagedItem = createItem(3L, 5, 5);   // Min 5, Damaged 5
        Item lowAndDamagedItem = createItem(4L, 20, 2); // Min 20, Damaged 2

        List<Item> items = Arrays.asList(normalItem, lowStockItem, damagedItem, lowAndDamagedItem);
        when(itemRepository.findAll()).thenReturn(items);

        // Setup Balances
        // Normal: Balance 20 > 10 (OK)
        when(itemService.getCurrentBalance(1L)).thenReturn(20);
        // Low Stock: Balance 40 < 50 (Low)
        when(itemService.getCurrentBalance(2L)).thenReturn(40);
        // Damaged: Balance 10 > 5 (OK)
        when(itemService.getCurrentBalance(3L)).thenReturn(10);
        // Low and Damaged: Balance 10 < 20 (Low)
        when(itemService.getCurrentBalance(4L)).thenReturn(10);

        // Setup Transaction Count
        long expectedTransactions = 15L;
        when(stockTransactionRepository.countByCreatedAtAfter(any(LocalDateTime.class))).thenReturn(expectedTransactions);

        // Act
        StockMetricsDTO metrics = stockService.getMetrics();

        // Assert
        assertNotNull(metrics);
        assertEquals(4, metrics.getTotal(), "Total items should be 4");
        assertEquals(2, metrics.getLowStock(), "Low stock items should be 2 (Item 2 and 4)");
        assertEquals(2, metrics.getDamaged(), "Damaged items should be 2 (Item 3 and 4)");
        assertEquals(expectedTransactions, metrics.getThisMonth(), "Transactions count should match");
        
        // Verify placeholders are 0.0
        assertEquals(0.0, metrics.getTotalChange());
        assertEquals(0.0, metrics.getLowStockChange());
        assertEquals(0.0, metrics.getDamagedChange());
        assertEquals(0.0, metrics.getThisMonthChange());

        // Verify interactions
        verify(itemRepository).findAll();
        verify(itemService).getCurrentBalance(1L);
        verify(itemService).getCurrentBalance(2L);
        verify(itemService).getCurrentBalance(3L);
        verify(itemService).getCurrentBalance(4L);
        verify(stockTransactionRepository).countByCreatedAtAfter(any(LocalDateTime.class));
    }

    @Test
    @DisplayName("Should handle empty data gracefully")
    void getMetrics_ShouldHandleEmptyData() {
        // Arrange
        when(itemRepository.findAll()).thenReturn(Collections.emptyList());
        when(stockTransactionRepository.countByCreatedAtAfter(any(LocalDateTime.class))).thenReturn(0L);

        // Act
        StockMetricsDTO metrics = stockService.getMetrics();

        // Assert
        assertNotNull(metrics);
        assertEquals(0, metrics.getTotal());
        assertEquals(0, metrics.getLowStock());
        assertEquals(0, metrics.getDamaged());
        assertEquals(0, metrics.getThisMonth());
        
        verify(itemService, never()).getCurrentBalance(anyLong());
    }

    @Test
    @DisplayName("Should correctly identify low stock boundary (Balance == MinimumStock)")
    void getMetrics_BoundaryCondition_BalanceEqualsMinimumStock() {
        // Arrange
        Item boundaryItem = createItem(1L, 10, 0); // Min 10
        when(itemRepository.findAll()).thenReturn(Collections.singletonList(boundaryItem));
        
        // Balance == Minimum Stock (10 == 10) -> Should NOT be low stock (since logic is balance < min)
        when(itemService.getCurrentBalance(1L)).thenReturn(10);
        
        when(stockTransactionRepository.countByCreatedAtAfter(any(LocalDateTime.class))).thenReturn(0L);

        // Act
        StockMetricsDTO metrics = stockService.getMetrics();

        // Assert
        assertEquals(1, metrics.getTotal());
        assertEquals(0, metrics.getLowStock(), "Item with balance == minStock should not be low stock");
    }

    @Test
    @DisplayName("Should correctly identify low stock boundary (Balance < MinimumStock)")
    void getMetrics_BoundaryCondition_BalanceLessThanMinimumStock() {
        // Arrange
        Item boundaryItem = createItem(1L, 10, 0); // Min 10
        when(itemRepository.findAll()).thenReturn(Collections.singletonList(boundaryItem));
        
        // Balance < Minimum Stock (9 < 10) -> Should be low stock
        when(itemService.getCurrentBalance(1L)).thenReturn(9);
        
        when(stockTransactionRepository.countByCreatedAtAfter(any(LocalDateTime.class))).thenReturn(0L);

        // Act
        StockMetricsDTO metrics = stockService.getMetrics();

        // Assert
        assertEquals(1, metrics.getTotal());
        assertEquals(1, metrics.getLowStock(), "Item with balance < minStock should be low stock");
    }

    @Test
    @DisplayName("Should verify date calculation for transaction count")
    void getMetrics_ShouldQueryTransactionsFromLastMonth() {
        // Arrange
        when(itemRepository.findAll()).thenReturn(Collections.emptyList());
        when(stockTransactionRepository.countByCreatedAtAfter(any(LocalDateTime.class))).thenReturn(5L);

        // Act
        stockService.getMetrics();

        // Assert
        ArgumentCaptor<LocalDateTime> dateCaptor = ArgumentCaptor.forClass(LocalDateTime.class);
        verify(stockTransactionRepository).countByCreatedAtAfter(dateCaptor.capture());
        
        LocalDateTime capturedDate = dateCaptor.getValue();
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime oneMonthAgo = now.minusMonths(1);
        
        // Allow a small delta for execution time (e.g., 1 second)
        // Since we can't control 'now' inside the service without a Clock bean, we check if it's roughly correct.
        // The captured date should be before now and roughly 1 month ago.
        assertTrue(capturedDate.isBefore(now));
        assertTrue(capturedDate.isAfter(oneMonthAgo.minusMinutes(1)));
        assertTrue(capturedDate.isBefore(oneMonthAgo.plusMinutes(1)));
    }
    
    @Test
    @DisplayName("Should propagate exception if ItemService fails")
    void getMetrics_WhenItemServiceFails_ShouldPropagateException() {
        // Arrange
        Item item = createItem(1L, 10, 0);
        when(itemRepository.findAll()).thenReturn(Collections.singletonList(item));
        when(itemService.getCurrentBalance(1L)).thenThrow(new RuntimeException("Service failure"));

        // Act & Assert
        assertThrows(RuntimeException.class, () -> stockService.getMetrics());
    }

    private Item createItem(Long id, Integer minStock, int damagedQty) {
        Item item = new Item();
        item.setId(id);
        item.setMinimumStock(minStock);
        item.setDamagedQuantity(damagedQty);
        return item;
    }
}
