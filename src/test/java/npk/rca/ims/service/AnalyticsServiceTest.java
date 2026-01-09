package npk.rca.ims.service;

import npk.rca.ims.dto.AnalyticsSummaryDTO;
import npk.rca.ims.model.Item;
import npk.rca.ims.model.StockTransaction;
import npk.rca.ims.model.TransactionType;
import npk.rca.ims.repository.StockTransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AnalyticsServiceTest {

    @Mock
    private StockTransactionRepository transactionRepository;

    @InjectMocks
    private AnalyticsService analyticsService;

    private Item testItem;
    private StockTransaction txIn;
    private StockTransaction txOutConsumed;
    private StockTransaction txOutDamaged;

    @BeforeEach
    void setUp() {
        testItem = new Item();
        testItem.setId(1L);
        testItem.setName("Rice");

        txIn = new StockTransaction();
        txIn.setId(1L);
        txIn.setItem(testItem);
        txIn.setTransactionType(TransactionType.IN);
        txIn.setQuantity(100);
        txIn.setTransactionDate(LocalDate.now());
        txIn.setNotes("Purchase");

        txOutConsumed = new StockTransaction();
        txOutConsumed.setId(2L);
        txOutConsumed.setItem(testItem);
        txOutConsumed.setTransactionType(TransactionType.OUT);
        txOutConsumed.setQuantity(50);
        txOutConsumed.setTransactionDate(LocalDate.now());
        txOutConsumed.setNotes("Consumed: Kitchen");

        txOutDamaged = new StockTransaction();
        txOutDamaged.setId(3L);
        txOutDamaged.setItem(testItem);
        txOutDamaged.setTransactionType(TransactionType.OUT);
        txOutDamaged.setQuantity(10);
        txOutDamaged.setTransactionDate(LocalDate.now());
        txOutDamaged.setNotes("Damaged: Water leak");
    }

    @Test
    void getAnalyticsSummary_ShouldReturnCorrectMetrics() {
        when(transactionRepository.findAll()).thenReturn(Arrays.asList(txIn, txOutConsumed, txOutDamaged));

        AnalyticsSummaryDTO result = analyticsService.getAnalyticsSummary();

        assertNotNull(result);
        
        // Total Out = 50 (Consumed) + 10 (Damaged) = 60
        // Wastage Ratio = (10 / 60) * 100 = 16.67%
        assertEquals(16.67, result.getWastageRatio(), 0.01);
        
        // Consumption Rate = 50 / 6 months = 8.33
        assertEquals(8.33, result.getConsumptionRate(), 0.01);
        
        // Restock Frequency = 180 / 1 (IN transaction) = 180
        assertEquals(180.0, result.getRestockFrequency(), 0.01);
        
        // Stock Out Reasons
        assertTrue(result.getStockOutReasons().containsKey("Consumed"));
        assertTrue(result.getStockOutReasons().containsKey("Damaged"));
        assertEquals(50, result.getStockOutReasons().get("Consumed"));
        assertEquals(10, result.getStockOutReasons().get("Damaged"));
        
        // Top Items
        assertTrue(result.getTopConsumedItems().containsKey("Rice"));
        assertEquals(50, result.getTopConsumedItems().get("Rice"));
        
        // Monthly Trends
        assertFalse(result.getMonthlyTrends().isEmpty());
        // Current month should have data
        assertEquals(100, result.getMonthlyTrends().get(5).getStockIn());
        assertEquals(50, result.getMonthlyTrends().get(5).getConsumed());
        assertEquals(10, result.getMonthlyTrends().get(5).getLoss());
    }

    @Test
    void getAnalyticsSummary_ShouldHandleEmptyData() {
        when(transactionRepository.findAll()).thenReturn(Collections.emptyList());

        AnalyticsSummaryDTO result = analyticsService.getAnalyticsSummary();

        assertNotNull(result);
        assertEquals(0.0, result.getWastageRatio());
        assertEquals(0.0, result.getConsumptionRate());
        assertEquals(0.0, result.getRestockFrequency());
        assertTrue(result.getStockOutReasons().isEmpty());
        assertTrue(result.getTopConsumedItems().isEmpty());
        assertFalse(result.getMonthlyTrends().isEmpty()); // Should still have 6 months of zero data
    }
}
