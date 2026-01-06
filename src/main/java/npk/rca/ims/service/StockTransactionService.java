package npk.rca.ims.service;

import lombok.RequiredArgsConstructor;
import npk.rca.ims.dto.StockBalanceDTO;
import npk.rca.ims.dto.StockTransactionDTO;
import npk.rca.ims.exceptions.ResourceNotFoundException;
import npk.rca.ims.model.Item;
import npk.rca.ims.model.StockTransaction;
import npk.rca.ims.model.TransactionType;
import npk.rca.ims.repository.ItemRepository;
import npk.rca.ims.repository.StockTransactionRepository;
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

        List<StockTransaction> transactions = transactionRepository.findByItemId(itemId);
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

        // Save transaction
        StockTransaction savedTransaction = transactionRepository.save(transaction);

        // Calculate the new balance to return in the DTO
        Integer newBalance = calculateBalance(item.getId()); // This will include the newly saved transaction
        
        StockTransactionDTO resultDTO = convertToDTO(savedTransaction);
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
        return dto;
    }

    /**
     * KEY BUSINESS LOGIC HIGHLIGHTS:
     *
     * 1. STOCK VALIDATION:
     *    - Prevents negative inventory (can't OUT more than available)
     *    - This is critical for data integrity!
     *
     * 2. BALANCE CALCULATION:
     *    - Real-time calculation from transactions
     *    - No separate balance table needed
     *    - Single source of truth
     *
     * 3. REPORT GENERATION:
     *    - Loops through all items
     *    - Calculates balances on-the-fly
     *    - Can filter by low stock
     *
     * 4. TRANSACTION IMMUTABILITY:
     *    - No update/delete methods
     *    - Once recorded, transactions are permanent
     *    - Corrections done via new transactions
     *
     * SCENARIO EXAMPLE:
     *
     * Day 1: Record 100 sacks of rice IN
     *   totalIn = 100, totalOut = 0, balance = 100
     *
     * Day 2: Record 30 sacks OUT (used for lunch)
     *   totalIn = 100, totalOut = 30, balance = 70
     *
     * Day 3: Try to OUT 80 sacks
     *   ERROR! Only 70 available. Transaction rejected.
     *
     * Day 4: Record 50 sacks IN (new purchase)
     *   totalIn = 150, totalOut = 30, balance = 120
     *
     */
}
