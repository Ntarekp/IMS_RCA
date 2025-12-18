package npk.rca.ims.service;

import lombok.RequiredArgsConstructor;
import npk.rca.ims.dto.ItemDTO;
import npk.rca.ims.exceptions.ResourceNotFoundException;
import npk.rca.ims.model.Item;
import npk.rca.ims.repository.ItemRepository;
import npk.rca.ims.repository.StockTransactionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * ItemService - Business logic for Items
 *
 * SERVICE LAYER RESPONSIBILITIES:
 * 1. Coordinate between Controller and Repository
 * 2. Implement business logic
 * 3. Convert between Entity and DTO
 * 4. Handle transactions
 * 5. Validate business rules
 *
 * @Service - Marks this as a service component
 * @RequiredArgsConstructor (Lombok) - Creates constructor for final fields
 * @Transactional - Manages database transactions
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)  // All methods are read-only by default
public class ItemService {

    /**
     * Dependencies (auto-injected by Spring)
     * final = required dependencies, injected via constructor
     */
    private final ItemRepository itemRepository;
    private final StockTransactionRepository transactionRepository;

    /**
     * Get all items with their current balances
     */
    public List<ItemDTO> getAllItems() {
        return itemRepository.findAll()
                .stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    /**
     * Get single item by ID
     * Throws exception if not found
     */
    public ItemDTO getItemById(Long id) {
        Item item = itemRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Item not found with id: " + id));

        return convertToDTO(item);
    }

    /**
     * Create new item
     * @Transactional (write mode) - allows database writes
     */
    @Transactional
    public ItemDTO createItem(ItemDTO itemDTO) {
        // Business rule: No duplicate names
        if (itemRepository.existsByName(itemDTO.getName())) {
            throw new IllegalArgumentException(
                    "Item with name '" + itemDTO.getName() + "' already exists");
        }

        // Convert DTO to Entity
        Item item = convertToEntity(itemDTO);

        // Save to database
        Item savedItem = itemRepository.save(item);

        // Convert back to DTO for response
        return convertToDTO(savedItem);
    }

    /**
     * Update existing item
     */
    @Transactional
    public ItemDTO updateItem(Long id, ItemDTO itemDTO) {
        // Check if item exists
        Item existingItem = itemRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Item not found with id: " + id));

        // Check for duplicate name (excluding current item)
        if (itemRepository.existsByName(itemDTO.getName()) &&
                !existingItem.getName().equals(itemDTO.getName())) {
            throw new IllegalArgumentException(
                    "Item with name '" + itemDTO.getName() + "' already exists");
        }

        // Update fields
        existingItem.setName(itemDTO.getName());
        existingItem.setUnit(itemDTO.getUnit());
        existingItem.setMinimumStock(itemDTO.getMinimumStock());
        existingItem.setDescription(itemDTO.getDescription());

        // Save updates (updatedAt timestamp auto-updates)
        Item updatedItem = itemRepository.save(existingItem);

        return convertToDTO(updatedItem);
    }

    /**
     * Delete item
     * Business rule: Cannot delete if transactions exist
     */
    @Transactional
    public void deleteItem(Long id) {
        // Check if item exists
        Item item = itemRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Item not found with id: " + id));

        // Business rule: Check for transactions
        List<?> transactions = transactionRepository.findByItemId(id);
        if (!transactions.isEmpty()) {
            throw new IllegalArgumentException(
                    "Cannot delete item with existing transactions. " +
                            "Found " + transactions.size() + " transaction(s).");
        }

        itemRepository.delete(item);
    }

    /**
     * Calculate current balance for an item
     */
    public Integer getCurrentBalance(Long itemId) {
        Integer totalIn = transactionRepository.getTotalInByItemId(itemId);
        Integer totalOut = transactionRepository.getTotalOutByItemId(itemId);
        return totalIn - totalOut;
    }

    /**
     * HELPER METHODS - Convert between Entity and DTO
     */

    /**
     * Convert Item entity to ItemDTO
     * Includes calculated fields (balance, low stock status)
     */
    private ItemDTO convertToDTO(Item item) {
        ItemDTO dto = new ItemDTO();
        dto.setId(item.getId());
        dto.setName(item.getName());
        dto.setUnit(item.getUnit());
        dto.setMinimumStock(item.getMinimumStock());
        dto.setDescription(item.getDescription());
        dto.setCreatedAt(item.getCreatedAt());
        dto.setUpdatedAt(item.getUpdatedAt());
        dto.setDamagedQuantity(item.getDamagedQuantity());

        // Calculate current balance
        Integer balance = getCurrentBalance(item.getId());
        dto.setCurrentBalance(balance);

        // Determine if low stock
        dto.setIsLowStock(balance < item.getMinimumStock());

        return dto;
    }

    /**
     * Convert ItemDTO to Item entity (for creating/updating)
     */
    private Item convertToEntity(ItemDTO dto) {
        Item item = new Item();
        item.setName(dto.getName());
        item.setUnit(dto.getUnit());
        item.setMinimumStock(dto.getMinimumStock());
        item.setDescription(dto.getDescription());
        if (dto.getDamagedQuantity() != null) {
            item.setDamagedQuantity(dto.getDamagedQuantity());
        }
        // Don't set id, createdAt, updatedAt - managed by JPA
        return item;
    }

    /**
     * Record damaged quantity for an item
     * @param itemId Item ID
     * @param damagedQty Quantity to record as damaged (added to existing)
     * @return Updated ItemDTO
     */
    @Transactional
    public ItemDTO recordDamagedQuantity(Long itemId, int damagedQty) {
        Item item = itemRepository.findById(itemId)
                .orElseThrow(() -> new ResourceNotFoundException("Item not found with id: " + itemId));
        int newDamaged = item.getDamagedQuantity() + damagedQty;
        if (newDamaged < 0) newDamaged = 0;
        item.setDamagedQuantity(newDamaged);
        itemRepository.save(item);
        return convertToDTO(item);
    }

    /**
     * KEY CONCEPTS IN THIS CLASS:
     *
     * 1. DEPENDENCY INJECTION:
     *    - Repositories are injected via constructor
     *    - @RequiredArgsConstructor creates constructor automatically
     *
     * 2. TRANSACTION MANAGEMENT:
     *    - @Transactional(readOnly = true) for queries (performance)
     *    - @Transactional for writes (ensures consistency)
     *
     * 3. BUSINESS RULES:
     *    - No duplicate names
     *    - Can't delete items with transactions
     *    - Validates before saving
     *
     * 4. SEPARATION OF CONCERNS:
     *    - Service doesn't know about HTTP (no ResponseEntity)
     *    - Service doesn't know about JSON (uses DTOs)
     *    - Controller will handle HTTP/REST concerns
     *
    */
}