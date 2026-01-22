package npk.rca.ims.service;

import lombok.RequiredArgsConstructor;
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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * StockTransactionService - Business logic for stock movements
 *
 * This is the CORE of the inventory system!
 * Handles all stock IN/OUT operations
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StockTransactionService {

    private final StockTransactionRepository transactionRepository;
    private final ItemRepository itemRepository;
    private final SupplierRepository supplierRepository;

    /**
     * Get all transactions
     */
    public List<StockTransactionDTO> getAllTransactions() {
        List<StockTransaction> transactions = transactionRepository.findAll();
        return calculateBalancesForTransactions(transactions);
    }

    /**
     * Get transactions for a specific item
     */
    public List<StockTransactionDTO> getTransactionsByItemId(Long itemId) {
        // Verify item exists
        if (!itemRepository.existsById(itemId)) {
            throw new ResourceNotFoundException("Item not found with id: " + itemId);
        }

        List<StockTransaction> transactions = transactionRepository.findByItem_Id(itemId);
        return calculateBalancesForTransactions(transactions);
    }

    /**
     * Get transactions within date range
     */
    public List<StockTransactionDTO> getTransactionsByDateRange(
            LocalDate startDate, LocalDate endDate) {

        if (startDate.isAfter(endDate)) {
            throw new IllegalArgumentException("Start date must be before end date");
        }

        List<StockTransaction> transactions = transactionRepository.findByTransactionDateBetween(startDate, endDate);
        return calculateBalancesForTransactions(transactions);
    }

    /**
     * Helper method to convert transactions to DTOs and calculate running balances
     * This ensures the frontend receives the "Balance After" for each transaction
     */
    private List<StockTransactionDTO> calculateBalancesForTransactions(List<StockTransaction> transactions) {
        // Sort transactions by date and creation time to ensure correct order
        transactions.sort(Comparator.comparing(StockTransaction::getTransactionDate)
                .thenComparing(StockTransaction::getCreatedAt));

        // Group by item to calculate balances per item
        Map<Long, List<StockTransaction>> transactionsByItem = transactions.stream()
                .collect(Collectors.groupingBy(t -> t.getItem().getId()));

        List<StockTransactionDTO> resultDTOs = new ArrayList<>();

        // Calculate running balance for each item's transaction history
        for (List<StockTransaction> itemTransactions : transactionsByItem.values()) {
            int runningBalance = 0;
            for (StockTransaction t : itemTransactions) {
                if (t.getTransactionType() == TransactionType.IN) {
                    runningBalance += t.getQuantity();
                } else {
                    runningBalance -= t.getQuantity();
                }
                
                StockTransactionDTO dto = convertToDTO(t);
                dto.setBalanceAfter(runningBalance);
                resultDTOs.add(dto);
            }
        }

        // Re-sort the final list by date descending (newest first) for display
        resultDTOs.sort(Comparator.comparing(StockTransactionDTO::getTransactionDate)
                .thenComparing(StockTransactionDTO::getCreatedAt).reversed());

        return resultDTOs;
    }

    /**
     * Record new transaction (stock IN or OUT)
     * THIS IS THE MOST IMPORTANT METHOD!
     */
    @Transactional
    public StockTransactionDTO recordTransaction(StockTransactionDTO transactionDTO) {
        // Validate item exists
        Item item = itemRepository.findById(transactionDTO.getItemId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Item not found with id: " + transactionDTO.getItemId()));

        // Business rule: Cannot OUT more than available
        if (transactionDTO.getTransactionType() == TransactionType.OUT) {
            Integer currentBalance = calculateBalance(transactionDTO.getItemId());
            if (transactionDTO.getQuantity() > currentBalance) {
                throw new IllegalArgumentException(
                        "Insufficient stock! Available: " + currentBalance +
                                ", Requested: " + transactionDTO.getQuantity());
            }
        }

        // Create transaction entity
        StockTransaction transaction = new StockTransaction();
        transaction.setItem(item);
        transaction.setTransactionType(transactionDTO.getTransactionType());
        transaction.setQuantity(transactionDTO.getQuantity());
        transaction.setTransactionDate(transactionDTO.getTransactionDate());
        transaction.setReferenceNumber(transactionDTO.getReferenceNumber());
        transaction.setNotes(transactionDTO.getNotes());
        transaction.setRecordedBy(transactionDTO.getRecordedBy());
        
        // Link Supplier if provided (only for IN transactions usually, but flexible)
        if (transactionDTO.getSupplierId() != null) {
            Supplier supplier = supplierRepository.findById(transactionDTO.getSupplierId())
                    .orElseThrow(() -> new ResourceNotFoundException("Supplier not found with id: " + transactionDTO.getSupplierId()));
            transaction.setSupplier(supplier);
        }

        // Save transaction
        StockTransaction savedTransaction = transactionRepository.save(transaction);

        // Update damaged quantity if applicable
        if (transaction.getTransactionType() == TransactionType.OUT && 
            transaction.getNotes() != null && 
            (transaction.getNotes().toLowerCase().startsWith("damaged") || 
             transaction.getNotes().toLowerCase().contains("damage"))) {
            
            item.setDamagedQuantity(item.getDamagedQuantity() + transaction.getQuantity());
            itemRepository.save(item);
        }

        // Calculate the new balance to return in the DTO
        Integer newBalance = calculateBalance(item.getId()); // This will include the newly saved transaction
        
        StockTransactionDTO resultDTO = convertToDTO(savedTransaction);
        resultDTO.setBalanceAfter(newBalance);
        
        return resultDTO;
    }

    /**
     * Update transaction (Advanced Edit)
     * Allows changing Item, Quantity, Type, etc.
     * Enforces stock availability checks.
     */
    @Transactional
    public StockTransactionDTO updateTransaction(Long id, StockTransactionDTO transactionDTO) {
        StockTransaction transaction = transactionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Transaction not found with id: " + id));

        if (transaction.isReversed()) {
            throw new IllegalStateException("Cannot update a reversed transaction");
        }

        // 1. Capture original state
        Item originalItem = transaction.getItem();
        int originalQuantity = transaction.getQuantity();
        TransactionType originalType = transaction.getTransactionType();
        
        // 2. Identify if critical fields are changing
        boolean isCriticalChange = !originalItem.getId().equals(transactionDTO.getItemId()) ||
                                   originalQuantity != transactionDTO.getQuantity() ||
                                   originalType != transactionDTO.getTransactionType();

        // 3. Update fields
        Item newItem = itemRepository.findById(transactionDTO.getItemId())
                .orElseThrow(() -> new ResourceNotFoundException("Item not found with id: " + transactionDTO.getItemId()));
        
        transaction.setItem(newItem);
        transaction.setTransactionType(transactionDTO.getTransactionType());
        transaction.setQuantity(transactionDTO.getQuantity());
        transaction.setNotes(transactionDTO.getNotes());
        transaction.setReferenceNumber(transactionDTO.getReferenceNumber());
        
        // 4. If critical fields changed, validate stock availability
        if (isCriticalChange) {
            // We need to ensure that the NET effect doesn't result in negative stock for EITHER the old item or new item.
            
            // Case A: Old Item (if item changed, or if just qty/type changed for same item)
            // We are effectively "undoing" the old transaction. 
            // If old was IN, we are removing stock. Check if we have enough to remove.
            if (originalType == TransactionType.IN) {
                // We are removing 'originalQuantity' from 'originalItem'
                // Current Balance of originalItem must be >= originalQuantity
                // UNLESS the new transaction is also for the same item and adds back enough... but let's be strict for safety.
                // Actually, if we are changing IN 100 to IN 80 (Same Item), we are removing 20.
                // If we are changing IN 100 to OUT 50 (Same Item), we are removing 150 (100 gone + 50 out).
                
                // Let's calculate the "Net Change" for the original item.
                int netChangeForOriginalItem = 0;
                
                // Remove old effect
                if (originalType == TransactionType.IN) netChangeForOriginalItem -= originalQuantity;
                else netChangeForOriginalItem += originalQuantity;
                
                // Add new effect (only if item is the same)
                if (originalItem.getId().equals(newItem.getId())) {
                     if (transactionDTO.getTransactionType() == TransactionType.IN) netChangeForOriginalItem += transactionDTO.getQuantity();
                     else netChangeForOriginalItem -= transactionDTO.getQuantity();
                }
                
                // If net change is negative, check if we have enough stock
                if (netChangeForOriginalItem < 0) {
                     int currentBalance = calculateBalance(originalItem.getId());
                     // We need to check if currentBalance + netChange >= 0
                     // Note: calculateBalance INCLUDES the old transaction already.
                     // So we are checking: (Current - Old_Effect + New_Effect) >= 0
                     // Wait, calculateBalance includes the "Old Transaction" as it is currently in DB.
                     // So "Current Balance" assumes Old Transaction is valid.
                     // We want to know if "Current Balance + netChangeForOriginalItem" is valid?
                     // No.
                     
                     // Let's look at it this way:
                     // Balance_After_Edit = Balance_Before_Edit - Old_Effect + New_Effect
                     // Balance_Before_Edit = Current_Balance_In_DB
                     // NO! Current_Balance_In_DB ALREADY CONTAINS Old_Effect.
                     // So Balance_After_Edit = Current_Balance_In_DB - Old_Effect + New_Effect
                     
                     // Let's verify logic:
                     // Current DB: IN 100. Balance = 100.
                     // Edit to: IN 80.
                     // Old Effect: +100. New Effect: +80.
                     // Balance After = 100 - (+100) + (+80) = 80. Correct.
                     
                     // Current DB: IN 100. Balance = 50 (50 consumed).
                     // Edit to: IN 20.
                     // Old Effect: +100. New Effect: +20.
                     // Balance After = 50 - 100 + 20 = -30. INVALID!
                     
                     // So we must check: (CurrentBalance - OldEffect + NewEffect) >= 0
                     
                     // Calculate Old Effect Signed
                     int oldEffect = (originalType == TransactionType.IN) ? originalQuantity : -originalQuantity;
                     
                     // Calculate New Effect Signed (only if same item)
                     int newEffect = 0;
                     if (originalItem.getId().equals(newItem.getId())) {
                         newEffect = (transactionDTO.getTransactionType() == TransactionType.IN) ? transactionDTO.getQuantity() : -transactionDTO.getQuantity();
                     }
                     
                     int projectedBalance = calculateBalance(originalItem.getId()) - oldEffect + newEffect;
                     if (projectedBalance < 0) {
                         throw new IllegalArgumentException("Insufficient stock to update this transaction for item: " + originalItem.getName());
                     }
                }
            } else {
                 // Old was OUT. Removing it ADDS stock. So for the old item, we are generally safe...
                 // Unless we change OUT 10 to OUT 100.
                 // Old Effect: -10. New Effect: -100.
                 // Balance After = Current - (-10) + (-100) = Current + 10 - 100 = Current - 90.
                 // Must check if Current - 90 >= 0.
                 
                 int oldEffect = (originalType == TransactionType.IN) ? originalQuantity : -originalQuantity;
                 int newEffect = 0;
                 if (originalItem.getId().equals(newItem.getId())) {
                     newEffect = (transactionDTO.getTransactionType() == TransactionType.IN) ? transactionDTO.getQuantity() : -transactionDTO.getQuantity();
                 }
                 
                 int projectedBalance = calculateBalance(originalItem.getId()) - oldEffect + newEffect;
                 if (projectedBalance < 0) {
                     throw new IllegalArgumentException("Insufficient stock to update this transaction for item: " + originalItem.getName());
                 }
            }
            
            // Case B: New Item (if item changed)
            // If we switched items, we must ALSO check the New Item.
            // We are applying the New Effect to the New Item.
            // Balance_After = Current_Balance_New_Item + New_Effect >= 0
            if (!originalItem.getId().equals(newItem.getId())) {
                 int newEffect = (transactionDTO.getTransactionType() == TransactionType.IN) ? transactionDTO.getQuantity() : -transactionDTO.getQuantity();
                 int currentBalanceNewItem = calculateBalance(newItem.getId());
                 if (currentBalanceNewItem + newEffect < 0) {
                     throw new IllegalArgumentException("Insufficient stock to update this transaction for NEW item: " + newItem.getName());
                 }
            }
        }

        StockTransaction savedTransaction = transactionRepository.save(transaction);
        
        // Calculate balance
        Integer currentBalance = calculateBalance(transaction.getItem().getId());
        
        StockTransactionDTO resultDTO = convertToDTO(savedTransaction);
        resultDTO.setBalanceAfter(currentBalance);
        
        return resultDTO;
    }

    /**
     * Undo a reversal (Rollback)
     * Restores the original transaction and deletes the reversal entry.
     */
    @Transactional
    public StockTransactionDTO undoReverseTransaction(Long originalId) {
        StockTransaction originalTransaction = transactionRepository.findById(originalId)
                .orElseThrow(() -> new ResourceNotFoundException("Transaction not found with id: " + originalId));

        if (!originalTransaction.isReversed()) {
            throw new IllegalStateException("Transaction is NOT reversed. Cannot undo.");
        }
        
        // Find the reversal transaction
        StockTransaction reversalTransaction = transactionRepository.findReversalTransaction(originalId);
        if (reversalTransaction == null) {
            // Should not happen if data is consistent, but handle gracefully
            // Just un-reverse the original? No, that would duplicate stock if the reversal effect is missing?
            // If reversal is missing, then the stock is ALREADY reflecting the original (or never was reversed effectively).
            // But 'isReversed' is true. 
            // Safety: Just set isReversed = false.
            originalTransaction.setReversed(false);
            return convertToDTO(transactionRepository.save(originalTransaction));
        }

        // Validate if we can remove the reversal
        // Reversal was: 
        // If Original IN 10 -> Reversal OUT 10.
        // Undo Reversal -> Remove OUT 10 -> Add 10. Always safe for stock levels (adding stock).
        // If Original OUT 10 -> Reversal IN 10.
        // Undo Reversal -> Remove IN 10 -> Remove 10. Must check stock!
        
        if (reversalTransaction.getTransactionType() == TransactionType.IN) {
            // We are about to remove an IN transaction (which was compensating for an OUT).
            // This means we are re-applying the OUT.
            // Check if we have enough stock to support the original OUT.
            
            int currentBalance = calculateBalance(originalTransaction.getItem().getId());
            // Effect of removing Reversal (IN 10) is -10.
            if (currentBalance - reversalTransaction.getQuantity() < 0) {
                 throw new IllegalStateException("Cannot undo reversal! Insufficient stock to restore the original OUT transaction.");
            }
        }

        // Delete reversal
        transactionRepository.delete(reversalTransaction);
        
        // Unmark original
        originalTransaction.setReversed(false);
        StockTransaction savedOriginal = transactionRepository.save(originalTransaction);

        // Calculate new balance
        Integer newBalance = calculateBalance(savedOriginal.getItem().getId());
        
        StockTransactionDTO resultDTO = convertToDTO(savedOriginal);
        resultDTO.setBalanceAfter(newBalance);
        
        return resultDTO;
    }

    /**
     * Reverse a transaction by creating a counter-transaction
     */
    @Transactional
    public StockTransactionDTO reverseTransaction(Long id, String reason, String reversedBy) {
        StockTransaction originalTransaction = transactionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Transaction not found with id: " + id));

        if (originalTransaction.isReversed()) {
            throw new IllegalStateException("Transaction is already reversed");
        }

        // Check if reversal is possible (e.g. reversing an IN transaction shouldn't make stock negative)
        if (originalTransaction.getTransactionType() == TransactionType.IN) {
            Integer currentBalance = calculateBalance(originalTransaction.getItem().getId());
            if (originalTransaction.getQuantity() > currentBalance) {
                throw new IllegalStateException(
                        "Cannot reverse this transaction! Insufficient stock available to remove " + 
                        originalTransaction.getQuantity() + " items.");
            }
        }

        // Mark original as reversed
        originalTransaction.setReversed(true);
        transactionRepository.save(originalTransaction);

        // Create counter-transaction
        StockTransaction reversalTransaction = new StockTransaction();
        reversalTransaction.setItem(originalTransaction.getItem());
        
        // Swap transaction type
        reversalTransaction.setTransactionType(
                originalTransaction.getTransactionType() == TransactionType.IN ? 
                        TransactionType.OUT : TransactionType.IN
        );
        
        reversalTransaction.setQuantity(originalTransaction.getQuantity());
        reversalTransaction.setTransactionDate(LocalDate.now());
        reversalTransaction.setReferenceNumber("REV-" + originalTransaction.getId());
        reversalTransaction.setNotes("Reversal of transaction #" + originalTransaction.getId() + ": " + reason);
        reversalTransaction.setRecordedBy(reversedBy);
        reversalTransaction.setOriginalTransaction(originalTransaction);
        
        StockTransaction savedReversal = transactionRepository.save(reversalTransaction);

        // Calculate new balance
        Integer newBalance = calculateBalance(originalTransaction.getItem().getId());
        
        StockTransactionDTO resultDTO = convertToDTO(savedReversal);
        resultDTO.setBalanceAfter(newBalance);
        
        return resultDTO;
    }

    /**
     * Generate stock balance report for all items
     * This is what the school will use for reports!
     */
    public List<StockBalanceDTO> generateBalanceReport() {
        return itemRepository.findAll()
                .stream()
                .map(this::calculateItemBalance)
                .collect(Collectors.toList());
    }

    /**
     * Generate balance report with low stock filter
     */
    public List<StockBalanceDTO> getLowStockItems() {
        return generateBalanceReport()
                .stream()
                .filter(StockBalanceDTO::getIsLowStock)
                .collect(Collectors.toList());
    }

    /**
     * Calculate balance for a single item
     */
    private Integer calculateBalance(Long itemId) {
        Integer totalIn = transactionRepository.getTotalInByItemId(itemId);
        Integer totalOut = transactionRepository.getTotalOutByItemId(itemId);
        
        int in = (totalIn != null) ? totalIn : 0;
        int out = (totalOut != null) ? totalOut : 0;
        
        return in - out;
    }

    /**
     * Create StockBalanceDTO for an item
     */
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

    /**
     * Convert entity to DTO
     */
    private StockTransactionDTO convertToDTO(StockTransaction transaction) {
        StockTransactionDTO dto = new StockTransactionDTO();
        dto.setId(transaction.getId());
        dto.setItemId(transaction.getItem().getId());
        dto.setItemName(transaction.getItem().getName());  // Populate item name
        dto.setTransactionType(transaction.getTransactionType());
        dto.setQuantity(transaction.getQuantity());
        dto.setTransactionDate(transaction.getTransactionDate());
        dto.setReferenceNumber(transaction.getReferenceNumber());
        dto.setNotes(transaction.getNotes());
        dto.setRecordedBy(transaction.getRecordedBy());
        dto.setCreatedAt(transaction.getCreatedAt());
        dto.setReversed(transaction.isReversed());
        
        if (transaction.getOriginalTransaction() != null) {
            dto.setOriginalTransactionId(transaction.getOriginalTransaction().getId());
        }

        if (transaction.getSupplier() != null) {
            dto.setSupplierId(transaction.getSupplier().getId());
            dto.setSupplierName(transaction.getSupplier().getName());
        }
        
        return dto;
    }
}
