package npk.rca.ims.service;

import lombok.RequiredArgsConstructor;
import npk.rca.ims.dto.ItemDTO;
import npk.rca.ims.model.Item;
import npk.rca.ims.exceptions.ResourceNotFoundException;
import npk.rca.ims.repository.ItemRepository;
import npk.rca.ims.repository.StockTransactionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
@Transactional
public class ItemService {

    private final ItemRepository itemRepository;
    private final StockTransactionRepository stockTransactionRepository;

    /**
     * Get filtered and sorted list of items
     */
    public List<ItemDTO> getFilteredItems(String category, String status, String name, String sort) {
        // Get all items first
        List<Item> items = itemRepository.findAll();

        // Apply filters
        Stream<Item> itemStream = items.stream();

        // Filter by name (case-insensitive partial match)
        if (name != null && !name.isEmpty()) {
            itemStream = itemStream.filter(item ->
                    item.getName().toLowerCase().contains(name.toLowerCase()));
        }

        // Convert to DTOs (with calculated balance)
        List<ItemDTO> itemDTOs = itemStream
                .map(this::convertToDTO)
                .collect(Collectors.toList());

        // Filter by status (low stock vs adequate stock) - AFTER DTO conversion
        if (status != null && !status.isEmpty()) {
            if ("Birahagije".equalsIgnoreCase(status)) {
                // Low stock items
                itemDTOs = itemDTOs.stream()
                        .filter(ItemDTO::getIsLowStock)
                        .collect(Collectors.toList());
            } else if ("Birahagera".equalsIgnoreCase(status)) {
                // Adequate stock items
                itemDTOs = itemDTOs.stream()
                        .filter(dto -> !dto.getIsLowStock())
                        .collect(Collectors.toList());
            }
        }

        // Apply sorting
        Comparator<ItemDTO> comparator = getComparator(sort);
        itemDTOs.sort(comparator);

        return itemDTOs;
    }

    /**
     * Create comparator based on sort parameter
     */
    private Comparator<ItemDTO> getComparator(String sort) {
        String[] sortParams = sort.split(",");
        String field = sortParams[0];
        boolean ascending = sortParams.length < 2 || "asc".equalsIgnoreCase(sortParams[1]);

        Comparator<ItemDTO> comparator;

        switch (field.toLowerCase()) {
            case "name":
                comparator = Comparator.comparing(ItemDTO::getName, String.CASE_INSENSITIVE_ORDER);
                break;
            case "currentbalance":
            case "current_balance":
                comparator = Comparator.comparing(dto ->
                        dto.getCurrentBalance() != null ? dto.getCurrentBalance() : 0);
                break;
            case "minimumstock":
            case "minimum_stock":
                comparator = Comparator.comparing(ItemDTO::getMinimumStock);
                break;
            default:
                comparator = Comparator.comparing(ItemDTO::getName, String.CASE_INSENSITIVE_ORDER);
        }

        return ascending ? comparator : comparator.reversed();
    }

    /**
     * Get all items
     */
    public List<ItemDTO> getAllItems() {
        return itemRepository.findAll().stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    /**
     * Get item by ID
     */
    public ItemDTO getItemById(Long id) {
        Item item = itemRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Item not found with id: " + id));
        return convertToDTO(item);
    }

    /**
     * Create new item
     */
    public ItemDTO createItem(ItemDTO itemDTO) {
        Item item = new Item();
        item.setName(itemDTO.getName());
        item.setUnit(itemDTO.getUnit());
        item.setMinimumStock(itemDTO.getMinimumStock());
        item.setDescription(itemDTO.getDescription());
        item.setDamagedQuantity(0);

        Item savedItem = itemRepository.save(item);
        return convertToDTO(savedItem);
    }

    /**
     * Update existing item
     */
    public ItemDTO updateItem(Long id, ItemDTO itemDTO) {
        Item item = itemRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Item not found with id: " + id));

        item.setName(itemDTO.getName());
        item.setUnit(itemDTO.getUnit());
        item.setMinimumStock(itemDTO.getMinimumStock());
        item.setDescription(itemDTO.getDescription());

        Item updatedItem = itemRepository.save(item);
        return convertToDTO(updatedItem);
    }

    /**
     * Delete item
     */
    public void deleteItem(Long id) {
        Item item = itemRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Item not found with id: " + id));
        itemRepository.delete(item);
    }

    /**
     * Record damaged quantity
     */
    public ItemDTO recordDamagedQuantity(Long id, int damagedQuantity) {
        Item item = itemRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Item not found with id: " + id));

        // Add to existing damaged quantity
        item.setDamagedQuantity(item.getDamagedQuantity() + damagedQuantity);

        Item updatedItem = itemRepository.save(item);
        return convertToDTO(updatedItem);
    }

    /**
     * Get current balance for an item (calculated from transactions)
     */
    public int getCurrentBalance(Long itemId) {
        // Verify item exists
        itemRepository.findById(itemId)
                .orElseThrow(() -> new ResourceNotFoundException("Item not found with id: " + itemId));

        // Calculate balance from transactions
        return calculateCurrentBalance(itemId);
    }

    /**
     * Calculate current balance from stock transactions
     * Balance = Total Stock-In - Total Stock-Out
     */
    private int calculateCurrentBalance(Long itemId) {
        Integer totalIn = stockTransactionRepository.getTotalInByItemId(itemId);
        Integer totalOut = stockTransactionRepository.getTotalOutByItemId(itemId);

        int stockIn = (totalIn != null) ? totalIn : 0;
        int stockOut = (totalOut != null) ? totalOut : 0;

        return stockIn - stockOut;
    }

    /**
     * Convert Item entity to ItemDTO
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

        // Calculate current balance from transactions
        int currentBalance = calculateCurrentBalance(item.getId());
        dto.setCurrentBalance(currentBalance);

        // Determine if low stock
        dto.setIsLowStock(currentBalance <= item.getMinimumStock());

        return dto;
    }
}
