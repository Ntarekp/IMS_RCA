package npk.rca.ims.service;

import npk.rca.ims.dto.StockBalanceDTO;
import npk.rca.ims.dto.StockTransactionDTO;
import npk.rca.ims.exceptions.ResourceNotFoundException;
import npk.rca.ims.model.Item;
import npk.rca.ims.model.StockTransaction;
import npk.rca.ims.model.Supplier;
import npk.rca.ims.model.TransactionType;
import npk.rca.ims.repository.ItemRepository;
import npk.rca.ims.repository.StockTransactionRepository;
import npk.rca.ims.repository.SupplierRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StockTransactionServiceTest {

    @Mock
    private StockTransactionRepository transactionRepository;

    @Mock
    private ItemRepository itemRepository;

    @Mock
    private SupplierRepository supplierRepository;

    @InjectMocks
    private StockTransactionService stockTransactionService;

    private Item testItem;
    private StockTransaction testTransactionIn;
    private StockTransaction testTransactionOut;
    private StockTransactionDTO testTransactionDTO;

    @BeforeEach
    void setUp() {
        testItem = new Item();
        testItem.setId(1L);
        testItem.setName("Test Item");
        testItem.setMinimumStock(10);

        testTransactionIn = new StockTransaction();
        testTransactionIn.setId(1L);
        testTransactionIn.setItem(testItem);
        testTransactionIn.setTransactionType(TransactionType.IN);
        testTransactionIn.setQuantity(100);
        testTransactionIn.setTransactionDate(LocalDate.now());
        testTransactionIn.setCreatedAt(LocalDateTime.now());

        testTransactionOut = new StockTransaction();
        testTransactionOut.setId(2L);
        testTransactionOut.setItem(testItem);
        testTransactionOut.setTransactionType(TransactionType.OUT);
        testTransactionOut.setQuantity(20);
        testTransactionOut.setTransactionDate(LocalDate.now());
        testTransactionOut.setCreatedAt(LocalDateTime.now().plusMinutes(1));

        testTransactionDTO = new StockTransactionDTO();
        testTransactionDTO.setItemId(1L);
        testTransactionDTO.setTransactionType(TransactionType.IN);
        testTransactionDTO.setQuantity(50);
        testTransactionDTO.setTransactionDate(LocalDate.now());
    }

    @Test
    @DisplayName("Should return all transactions with calculated running balances")
    void getAllTransactions_ShouldReturnTransactionsWithBalances() {
        when(transactionRepository.findAll()).thenReturn(Arrays.asList(testTransactionIn, testTransactionOut));

        List<StockTransactionDTO> result = stockTransactionService.getAllTransactions();

        assertEquals(2, result.size());
        // Results are sorted by date desc, so OUT (newer) should be first
        assertEquals(TransactionType.OUT, result.get(0).getTransactionType());
        assertEquals(80, result.get(0).getBalanceAfter()); // 100 - 20 = 80
        
        assertEquals(TransactionType.IN, result.get(1).getTransactionType());
        assertEquals(100, result.get(1).getBalanceAfter()); // 100
    }

    @Test
    @DisplayName("Should return transactions for specific item when item exists")
    void getTransactionsByItemId_ShouldReturnTransactions_WhenItemExists() {
        when(itemRepository.existsById(1L)).thenReturn(true);
        when(transactionRepository.findByItemId(1L)).thenReturn(Arrays.asList(testTransactionIn));

        List<StockTransactionDTO> result = stockTransactionService.getTransactionsByItemId(1L);

        assertEquals(1, result.size());
        assertEquals(100, result.get(0).getBalanceAfter());
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when getting transactions for non-existent item")
    void getTransactionsByItemId_ShouldThrowException_WhenItemNotFound() {
        when(itemRepository.existsById(1L)).thenReturn(false);

        assertThrows(ResourceNotFoundException.class, () -> 
            stockTransactionService.getTransactionsByItemId(1L));
    }

    @Test
    @DisplayName("Should record IN transaction successfully")
    void recordTransaction_ShouldRecordInTransaction() {
        when(itemRepository.findById(1L)).thenReturn(Optional.of(testItem));
        when(transactionRepository.save(any(StockTransaction.class))).thenReturn(testTransactionIn);
        when(transactionRepository.getTotalInByItemId(1L)).thenReturn(100);
        when(transactionRepository.getTotalOutByItemId(1L)).thenReturn(0);

        StockTransactionDTO result = stockTransactionService.recordTransaction(testTransactionDTO);

        assertNotNull(result);
        assertEquals(100, result.getBalanceAfter());
    }

    @Test
    @DisplayName("Should record OUT transaction when stock is sufficient")
    void recordTransaction_ShouldRecordOutTransaction_WhenStockIsSufficient() {
        testTransactionDTO.setTransactionType(TransactionType.OUT);
        testTransactionDTO.setQuantity(50);

        when(itemRepository.findById(1L)).thenReturn(Optional.of(testItem));
        // Mock balance check: 100 IN, 0 OUT -> 100 Available
        when(transactionRepository.getTotalInByItemId(1L)).thenReturn(100);
        when(transactionRepository.getTotalOutByItemId(1L)).thenReturn(0);
        
        when(transactionRepository.save(any(StockTransaction.class))).thenReturn(testTransactionOut);

        StockTransactionDTO result = stockTransactionService.recordTransaction(testTransactionDTO);

        assertNotNull(result);
        // Balance calculation is called again after save
        assertEquals(100, result.getBalanceAfter()); 
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException when stock is insufficient for OUT transaction")
    void recordTransaction_ShouldThrowException_WhenStockIsInsufficient() {
        testTransactionDTO.setTransactionType(TransactionType.OUT);
        testTransactionDTO.setQuantity(150); // Requesting 150

        when(itemRepository.findById(1L)).thenReturn(Optional.of(testItem));
        // Mock balance check: 100 IN, 0 OUT -> 100 Available
        when(transactionRepository.getTotalInByItemId(1L)).thenReturn(100);
        when(transactionRepository.getTotalOutByItemId(1L)).thenReturn(0);

        assertThrows(IllegalArgumentException.class, () -> 
            stockTransactionService.recordTransaction(testTransactionDTO));
        
        verify(transactionRepository, never()).save(any(StockTransaction.class));
    }
    
    @Test
    @DisplayName("Should link supplier to transaction when supplier ID is provided")
    void recordTransaction_ShouldLinkSupplier_WhenSupplierIdProvided() {
        testTransactionDTO.setSupplierId(10L);
        Supplier supplier = new Supplier();
        supplier.setId(10L);
        
        when(itemRepository.findById(1L)).thenReturn(Optional.of(testItem));
        when(supplierRepository.findById(10L)).thenReturn(Optional.of(supplier));
        when(transactionRepository.save(any(StockTransaction.class))).thenAnswer(invocation -> {
            StockTransaction t = invocation.getArgument(0);
            assertEquals(supplier, t.getSupplier());
            return t;
        });
        
        stockTransactionService.recordTransaction(testTransactionDTO);
    }

    @Test
    @DisplayName("Should generate balance report for all items")
    void generateBalanceReport_ShouldReturnBalances() {
        when(itemRepository.findAll()).thenReturn(Arrays.asList(testItem));
        when(transactionRepository.getTotalInByItemId(1L)).thenReturn(100);
        when(transactionRepository.getTotalOutByItemId(1L)).thenReturn(20);

        List<StockBalanceDTO> result = stockTransactionService.generateBalanceReport();

        assertEquals(1, result.size());
        assertEquals(80, result.get(0).getCurrentBalance());
        assertEquals(100, result.get(0).getTotalIn());
        assertEquals(20, result.get(0).getTotalOut());
    }
    
    @Test
    @DisplayName("Should return only low stock items")
    void getLowStockItems_ShouldReturnOnlyLowStockItems() {
        Item lowStockItem = new Item();
        lowStockItem.setId(2L);
        lowStockItem.setMinimumStock(50);
        
        when(itemRepository.findAll()).thenReturn(Arrays.asList(testItem, lowStockItem));
        
        // Item 1: 100 - 20 = 80 (Min 10) -> OK
        when(transactionRepository.getTotalInByItemId(1L)).thenReturn(100);
        when(transactionRepository.getTotalOutByItemId(1L)).thenReturn(20);
        
        // Item 2: 40 - 0 = 40 (Min 50) -> LOW
        when(transactionRepository.getTotalInByItemId(2L)).thenReturn(40);
        when(transactionRepository.getTotalOutByItemId(2L)).thenReturn(0);

        List<StockBalanceDTO> result = stockTransactionService.getLowStockItems();

        assertEquals(1, result.size());
        assertEquals(2L, result.get(0).getItemId());
    }
}
