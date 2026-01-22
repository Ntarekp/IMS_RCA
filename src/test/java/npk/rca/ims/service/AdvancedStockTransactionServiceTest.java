package npk.rca.ims.service;

import npk.rca.ims.dto.StockTransactionDTO;
import npk.rca.ims.model.Item;
import npk.rca.ims.model.StockTransaction;
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
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdvancedStockTransactionServiceTest {

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
    private StockTransactionDTO updateDTO;

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

        updateDTO = new StockTransactionDTO();
        updateDTO.setId(1L);
        updateDTO.setItemId(1L);
        updateDTO.setTransactionType(TransactionType.IN);
        updateDTO.setQuantity(100); // Default no change
        updateDTO.setTransactionDate(LocalDate.now());
    }

    // --- Advanced Edit Tests ---

    @Test
    @DisplayName("Should update metadata only when critical fields are unchanged")
    void updateTransaction_ShouldUpdateMetadataOnly() {
        // Arrange
        updateDTO.setNotes("Updated Notes");
        updateDTO.setReferenceNumber("REF-NEW");

        when(transactionRepository.findById(1L)).thenReturn(Optional.of(testTransactionIn));
        when(itemRepository.findById(1L)).thenReturn(Optional.of(testItem));
        when(transactionRepository.save(any(StockTransaction.class))).thenAnswer(i -> i.getArguments()[0]);
        // Mock balance calculation (needed for return DTO)
        when(transactionRepository.getTotalInByItemId(1L)).thenReturn(100); 
        when(transactionRepository.getTotalOutByItemId(1L)).thenReturn(0);

        // Act
        StockTransactionDTO result = stockTransactionService.updateTransaction(1L, updateDTO);

        // Assert
        assertEquals("Updated Notes", result.getNotes());
        assertEquals("REF-NEW", result.getReferenceNumber());
        assertEquals(100, result.getQuantity()); // Quantity unchanged
        
        // Verify no stock logic triggered (implicit by no exception and same balance)
        verify(transactionRepository).save(any(StockTransaction.class));
    }

    @Test
    @DisplayName("Should update quantity successfully when stock is sufficient")
    void updateTransaction_ShouldUpdateQuantity_WhenStockSufficient() {
        // Arrange: Change IN 100 to IN 80 (Reducing stock by 20)
        // Current Balance: 100.
        // Effect: -100 (undo old) + 80 (add new) = -20 net change.
        // New Balance: 100 - 20 = 80. >= 0. OK.
        
        updateDTO.setQuantity(80);

        when(transactionRepository.findById(1L)).thenReturn(Optional.of(testTransactionIn));
        when(itemRepository.findById(1L)).thenReturn(Optional.of(testItem));
        when(transactionRepository.save(any(StockTransaction.class))).thenAnswer(i -> i.getArguments()[0]);
        
        // Mock balance checks
        // 1. Initial balance check inside validation (calculateBalance(1L) -> 100)
        // 2. Final balance calculation for DTO (calculateBalance(1L) -> 80)
        when(transactionRepository.getTotalInByItemId(1L)).thenReturn(100).thenReturn(80);
        when(transactionRepository.getTotalOutByItemId(1L)).thenReturn(0);

        // Act
        StockTransactionDTO result = stockTransactionService.updateTransaction(1L, updateDTO);

        // Assert
        assertEquals(80, result.getQuantity());
        assertEquals(80, result.getBalanceAfter()); // New balance should be 80
    }

    @Test
    @DisplayName("Should fail to update quantity if it causes negative stock")
    void updateTransaction_ShouldFail_WhenStockInsufficient() {
        // Arrange: Change IN 100 to IN 10.
        // Assume we already used 80 items.
        // DB State: IN 100, OUT 80. Balance = 20.
        // Update: Undo IN 100 (-100), Add IN 10 (+10). Net = -90.
        // New Balance = 20 - 90 = -70. Fail.

        updateDTO.setQuantity(10);

        when(transactionRepository.findById(1L)).thenReturn(Optional.of(testTransactionIn));
        when(itemRepository.findById(1L)).thenReturn(Optional.of(testItem));
        
        // Mock DB State
        when(transactionRepository.getTotalInByItemId(1L)).thenReturn(100);
        when(transactionRepository.getTotalOutByItemId(1L)).thenReturn(80);

        // Act & Assert
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> 
            stockTransactionService.updateTransaction(1L, updateDTO));
            
        assertTrue(exception.getMessage().contains("Insufficient stock"));
    }

    @Test
    @DisplayName("Should allow changing item (Cross-Item Edit)")
    void updateTransaction_ShouldAllowChangingItem() {
        // Arrange: Change Item A (IN 100) to Item B (IN 100)
        // Item A: Remove 100. Check if Item A has enough stock (assume yes).
        // Item B: Add 100. Always safe for IN.

        Item newItem = new Item();
        newItem.setId(2L);
        newItem.setName("New Item");
        
        updateDTO.setItemId(2L);

        when(transactionRepository.findById(1L)).thenReturn(Optional.of(testTransactionIn));
        when(itemRepository.findById(2L)).thenReturn(Optional.of(newItem));
        when(transactionRepository.save(any(StockTransaction.class))).thenAnswer(i -> i.getArguments()[0]);

        // Mock DB State for Old Item (Item 1)
        // We are removing IN 100. Must have >= 100 balance.
        // Called during validation for Item 1
        when(transactionRepository.getTotalInByItemId(1L)).thenReturn(150); // 150 available
        when(transactionRepository.getTotalOutByItemId(1L)).thenReturn(0);
        
        // Mock DB State for New Item (Item 2) 
        // 1. Validation for Item 2 (calculateBalance(2L) -> 0)
        // 2. Final DTO calc (calculateBalance(2L) -> 100)
        when(transactionRepository.getTotalInByItemId(2L)).thenReturn(0).thenReturn(100);
        when(transactionRepository.getTotalOutByItemId(2L)).thenReturn(0);

        // Act
        StockTransactionDTO result = stockTransactionService.updateTransaction(1L, updateDTO);

        // Assert
        assertEquals(2L, result.getItemId());
        assertEquals(100, result.getBalanceAfter()); // Item 2 starts with 0, gets 100
    }

    // --- Undo Reverse Tests ---

    @Test
    @DisplayName("Should undo reversal of IN transaction")
    void undoReverseTransaction_ShouldUndoReversal_OfInTransaction() {
        // Arrange
        // Original: IN 100 (Reversed)
        testTransactionIn.setReversed(true);
        
        // Reversal Transaction: OUT 100
        StockTransaction reversal = new StockTransaction();
        reversal.setId(99L);
        reversal.setTransactionType(TransactionType.OUT);
        reversal.setQuantity(100);
        reversal.setItem(testItem);

        when(transactionRepository.findById(1L)).thenReturn(Optional.of(testTransactionIn));
        when(transactionRepository.findReversalTransaction(1L)).thenReturn(reversal);
        when(transactionRepository.save(any(StockTransaction.class))).thenAnswer(i -> i.getArguments()[0]); // Save original
        
        // Mock balance for final calculation
        // Original (IN 100) is restored. Reversal (OUT 100) is deleted.
        // DB still has them? We mock the "future" state or the "current" state?
        // Service calls calculateBalance(item.getId()).
        // Ideally we mock the repo to reflect the "after" state, or just return a value.
        when(transactionRepository.getTotalInByItemId(1L)).thenReturn(100);
        when(transactionRepository.getTotalOutByItemId(1L)).thenReturn(0);

        // Act
        StockTransactionDTO result = stockTransactionService.undoReverseTransaction(1L);

        // Assert
        assertFalse(testTransactionIn.isReversed());
        verify(transactionRepository).delete(reversal); // Verify reversal is deleted
        assertEquals(100, result.getBalanceAfter());
    }

    @Test
    @DisplayName("Should undo reversal of OUT transaction when stock sufficient")
    void undoReverseTransaction_ShouldUndoReversal_OfOutTransaction() {
        // Arrange
        // Original: OUT 50 (Reversed)
        StockTransaction originalOut = new StockTransaction();
        originalOut.setId(2L);
        originalOut.setItem(testItem);
        originalOut.setTransactionType(TransactionType.OUT);
        originalOut.setQuantity(50);
        originalOut.setReversed(true);

        // Reversal: IN 50
        StockTransaction reversal = new StockTransaction();
        reversal.setId(99L);
        reversal.setTransactionType(TransactionType.IN);
        reversal.setQuantity(50);
        reversal.setItem(testItem);

        when(transactionRepository.findById(2L)).thenReturn(Optional.of(originalOut));
        when(transactionRepository.findReversalTransaction(2L)).thenReturn(reversal);
        
        // Check stock availability
        // We are removing the Reversal (IN 50). This reduces stock by 50.
        // Need >= 50 current balance.
        when(transactionRepository.getTotalInByItemId(1L)).thenReturn(100); // 100 total IN
        when(transactionRepository.getTotalOutByItemId(1L)).thenReturn(0);  // 0 total OUT (since original OUT is reversed/ignored in logic? No, original OUT is still in DB?)
        // Wait, calculateBalance logic:
        // calculateBalance queries DB.
        // DB has: Original OUT 50, Reversal IN 50.
        // Balance = 50 (IN) - 50 (OUT) = 0? No, Reversal IN 50 is separate.
        // Total In = 50 (from reversal). Total Out = 50 (from original). Balance = 0.
        // If we have other stock, say Base IN 100.
        // Total In = 150. Total Out = 50. Balance = 100.
        // Remove Reversal IN 50. New Balance = 100 - 50 = 50. OK.
        
        // Let's assume we have Base IN 100.
        // Original OUT 50. Reversal IN 50.
        // Current Balance = 100 + 50 - 50 = 100.
        
        // Mock calculateBalance inside undoReverseTransaction:
        // 1st call: validation check
        when(transactionRepository.getTotalInByItemId(1L)).thenReturn(150); // 100 Base + 50 Reversal
        when(transactionRepository.getTotalOutByItemId(1L)).thenReturn(50); // 50 Original
        
        when(transactionRepository.save(any(StockTransaction.class))).thenAnswer(i -> i.getArguments()[0]);

        // Act
        stockTransactionService.undoReverseTransaction(2L);

        // Assert
        verify(transactionRepository).delete(reversal);
        assertFalse(originalOut.isReversed());
    }

    @Test
    @DisplayName("Should fail undo reversal of OUT transaction if stock insufficient")
    void undoReverseTransaction_ShouldFail_WhenStockInsufficient() {
        // Arrange
        // Original: OUT 50 (Reversed). Reversal: IN 50.
        // We used up all the stock provided by the reversal!
        // Current Balance: 50 (only from reversal).
        // If we remove reversal, balance = 0.
        // Wait, if balance = 0, we can remove 50 -> -50?
        
        StockTransaction originalOut = new StockTransaction();
        originalOut.setId(2L);
        originalOut.setItem(testItem);
        originalOut.setTransactionType(TransactionType.OUT);
        originalOut.setQuantity(50);
        originalOut.setReversed(true);

        StockTransaction reversal = new StockTransaction();
        reversal.setId(99L);
        reversal.setTransactionType(TransactionType.IN);
        reversal.setQuantity(50);
        reversal.setItem(testItem);

        when(transactionRepository.findById(2L)).thenReturn(Optional.of(originalOut));
        when(transactionRepository.findReversalTransaction(2L)).thenReturn(reversal);
        
        // Mock Balance: Only have the 50 from reversal.
        // Base: 0. Reversal: 50. Total IN: 50.
        // Original OUT: 50. Total OUT: 50.
        // Balance = 0.
        // Wait, calculateBalance = 0.
        // Logic: if (currentBalance - reversalQuantity < 0)
        // 0 - 50 = -50. Fail.
        
        when(transactionRepository.getTotalInByItemId(1L)).thenReturn(50);
        when(transactionRepository.getTotalOutByItemId(1L)).thenReturn(50);

        // Act & Assert
        assertThrows(IllegalStateException.class, () -> 
            stockTransactionService.undoReverseTransaction(2L));
            
        verify(transactionRepository, never()).delete(any(StockTransaction.class));
    }
}
