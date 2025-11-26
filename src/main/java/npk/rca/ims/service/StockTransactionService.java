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
import java.util.List;
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
        return transactionRepository.findAll()
                .stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    /**
     * Get transactions for a specific item
     */
    public List<StockTransactionDTO> getTransactionsByItemId(Long itemId) {
        // Verify item exists
        if (!itemRepository.existsById(itemId)) {
            throw new ResourceNotFoundException("Item not found with id: " + itemId);
        }

        return transactionRepository.findByItemId(itemId)
                .stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    /**
     * Get transactions within date range
     */
    public List<StockTransactionDTO> getTransactionsByDateRange(
            LocalDate startDate, LocalDate endDate) {

        if (startDate.isAfter(endDate)) {
            throw new IllegalArgumentException("Start date must be before end date");
        }

        return transactionRepository.findByTransactionDateBetween(startDate, endDate)
                .stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
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

        return convertToDTO(savedTransaction);
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
        return totalIn - totalOut;
    }

    /**
     * Create StockBalanceDTO for an item
     */
    private StockBalanceDTO calculateItemBalance(Item item) {
        Integer totalIn = transactionRepository.getTotalInByItemId(item.getId());
        Integer totalOut = transactionRepository.getTotalOutByItemId(item.getId());
        Integer balance = totalIn - totalOut;

        StockBalanceDTO dto = new StockBalanceDTO();
        dto.setItemId(item.getId());
        dto.setItemName(item.getName());
        dto.setUnit(item.getUnit());
        dto.setTotalIn(totalIn);
        dto.setTotalOut(totalOut);
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