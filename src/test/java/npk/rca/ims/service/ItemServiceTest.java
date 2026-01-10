package npk.rca.ims.service;

import npk.rca.ims.dto.ItemDTO;
import npk.rca.ims.exceptions.ResourceNotFoundException;
import npk.rca.ims.model.Item;
import npk.rca.ims.model.User;
import npk.rca.ims.repository.ItemRepository;
import npk.rca.ims.repository.StockTransactionRepository;
import npk.rca.ims.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ItemServiceTest {

    @Mock
    private ItemRepository itemRepository;

    @Mock
    private StockTransactionRepository stockTransactionRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private ItemService itemService;

    private Item testItem;
    private ItemDTO testItemDTO;

    @BeforeEach
    void setUp() {
        testItem = new Item();
        testItem.setId(1L);
        testItem.setName("Test Item");
        testItem.setUnit("kg");
        testItem.setMinimumStock(10);
        testItem.setDescription("Test Description");
        testItem.setDamagedQuantity(0);

        testItemDTO = new ItemDTO();
        testItemDTO.setId(1L);
        testItemDTO.setName("Test Item");
        testItemDTO.setUnit("kg");
        testItemDTO.setMinimumStock(10);
        testItemDTO.setDescription("Test Description");
    }

    @Test
    @DisplayName("Should return all items with calculated balance")
    void getAllItems_ShouldReturnListOfItems() {
        when(itemRepository.findAll()).thenReturn(Arrays.asList(testItem));
        when(stockTransactionRepository.getTotalInByItemId(1L)).thenReturn(100);
        when(stockTransactionRepository.getTotalOutByItemId(1L)).thenReturn(50);

        List<ItemDTO> result = itemService.getAllItems();

        assertFalse(result.isEmpty());
        assertEquals(1, result.size());
        assertEquals("Test Item", result.get(0).getName());
        assertEquals(50, result.get(0).getCurrentBalance());
    }

    @Test
    @DisplayName("Should return item by ID when it exists")
    void getItemById_ShouldReturnItem_WhenItemExists() {
        when(itemRepository.findById(1L)).thenReturn(Optional.of(testItem));
        when(stockTransactionRepository.getTotalInByItemId(1L)).thenReturn(100);
        when(stockTransactionRepository.getTotalOutByItemId(1L)).thenReturn(50);

        ItemDTO result = itemService.getItemById(1L);

        assertNotNull(result);
        assertEquals("Test Item", result.getName());
        assertEquals(50, result.getCurrentBalance());
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when item ID does not exist")
    void getItemById_ShouldThrowException_WhenItemNotFound() {
        when(itemRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> itemService.getItemById(1L));
    }

    @Test
    @DisplayName("Should create new item successfully")
    void createItem_ShouldReturnCreatedItem() {
        when(itemRepository.save(any(Item.class))).thenReturn(testItem);
        // For a new item, balance calculation might be called if convertToDTO is used on the saved item
        // Usually new items have 0 transactions
        when(stockTransactionRepository.getTotalInByItemId(1L)).thenReturn(0);
        when(stockTransactionRepository.getTotalOutByItemId(1L)).thenReturn(0);

        ItemDTO result = itemService.createItem(testItemDTO);

        assertNotNull(result);
        assertEquals("Test Item", result.getName());
        assertEquals(0, result.getCurrentBalance());
    }

    @Test
    @DisplayName("Should update existing item successfully")
    void updateItem_ShouldReturnUpdatedItem_WhenItemExists() {
        when(itemRepository.findById(1L)).thenReturn(Optional.of(testItem));
        when(itemRepository.save(any(Item.class))).thenReturn(testItem);
        when(stockTransactionRepository.getTotalInByItemId(1L)).thenReturn(100);
        when(stockTransactionRepository.getTotalOutByItemId(1L)).thenReturn(50);

        testItemDTO.setName("Updated Item");
        ItemDTO result = itemService.updateItem(1L, testItemDTO);

        assertNotNull(result);
        assertEquals("Updated Item", result.getName());
    }

    @Test
    @DisplayName("Should delete item and its transactions when item exists")
    void deleteItem_ShouldDeleteItem_WhenItemExists() {
        when(itemRepository.findById(1L)).thenReturn(Optional.of(testItem));
        when(stockTransactionRepository.findByItemId(1L)).thenReturn(Collections.emptyList());

        itemService.deleteItem(1L);

        verify(stockTransactionRepository).deleteAll(anyList());
        verify(itemRepository, times(1)).delete(testItem);
    }

    @Test
    @DisplayName("Should delete item with password verification")
    void deleteItemWithPasswordVerification_ShouldDeleteItem_WhenPasswordIsValid() {
        User user = new User();
        user.setEmail("admin@example.com");
        user.setPassword("encodedPassword");

        when(userRepository.findByEmail("admin@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("password", "encodedPassword")).thenReturn(true);
        when(itemRepository.findById(1L)).thenReturn(Optional.of(testItem));
        when(stockTransactionRepository.findByItemId(1L)).thenReturn(Collections.emptyList());

        itemService.deleteItemWithPasswordVerification(1L, "admin@example.com", "password");

        verify(itemRepository, times(1)).delete(testItem);
    }

    @Test
    @DisplayName("Should throw exception when password verification fails")
    void deleteItemWithPasswordVerification_ShouldThrowException_WhenPasswordIsInvalid() {
        User user = new User();
        user.setEmail("admin@example.com");
        user.setPassword("encodedPassword");

        when(userRepository.findByEmail("admin@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrongPassword", "encodedPassword")).thenReturn(false);

        assertThrows(IllegalArgumentException.class, () -> 
            itemService.deleteItemWithPasswordVerification(1L, "admin@example.com", "wrongPassword"));
        
        verify(itemRepository, never()).delete(any(Item.class));
    }

    @Test
    @DisplayName("Should record damaged quantity correctly")
    void recordDamagedQuantity_ShouldUpdateDamagedQuantity() {
        // Initial damaged quantity is 0
        when(itemRepository.findById(1L)).thenReturn(Optional.of(testItem));

        // Mock save to return item with updated damaged quantity
        Item updatedItem = new Item();
        updatedItem.setId(1L);
        updatedItem.setDamagedQuantity(5);
        when(itemRepository.save(any(Item.class))).thenReturn(updatedItem);

        when(stockTransactionRepository.getTotalInByItemId(1L)).thenReturn(100);
        when(stockTransactionRepository.getTotalOutByItemId(1L)).thenReturn(50);

        ItemDTO result = itemService.recordDamagedQuantity(1L, 5);

        assertEquals(5, result.getDamagedQuantity());
    }

    @Test
    @DisplayName("Should filter items by name")
    void getFilteredItems_ShouldFilterByName() {
        when(itemRepository.findAll()).thenReturn(Arrays.asList(testItem));
        when(stockTransactionRepository.getTotalInByItemId(1L)).thenReturn(100);
        when(stockTransactionRepository.getTotalOutByItemId(1L)).thenReturn(50);

        List<ItemDTO> result = itemService.getFilteredItems(null, null, "Test", "name,asc");

        assertEquals(1, result.size());
        assertEquals("Test Item", result.get(0).getName());
    }
    
    @Test
    @DisplayName("Should filter items by status 'Mucye' (Low Stock)")
    void getFilteredItems_ShouldFilterByStatus_LowStock() {
        testItem.setMinimumStock(60); // Min stock > Current Balance (50)
        
        when(itemRepository.findAll()).thenReturn(Arrays.asList(testItem));
        when(stockTransactionRepository.getTotalInByItemId(1L)).thenReturn(100);
        when(stockTransactionRepository.getTotalOutByItemId(1L)).thenReturn(50);

        List<ItemDTO> result = itemService.getFilteredItems(null, "Mucye", null, "name,asc");

        assertEquals(1, result.size());
    }

    @Test
    @DisplayName("Should filter items by status 'Birahagije' (Adequate Stock)")
    void getFilteredItems_ShouldFilterByStatus_AdequateStock() {
        testItem.setMinimumStock(10); // Min stock < Current Balance (50)

        when(itemRepository.findAll()).thenReturn(Arrays.asList(testItem));
        when(stockTransactionRepository.getTotalInByItemId(1L)).thenReturn(100);
        when(stockTransactionRepository.getTotalOutByItemId(1L)).thenReturn(50);

        List<ItemDTO> result = itemService.getFilteredItems(null, "Birahagije", null, "name,asc");

        assertEquals(1, result.size());
    }

    @Test
    @DisplayName("Should filter items by status 'Byashize' (Out of Stock)")
    void getFilteredItems_ShouldFilterByStatus_OutOfStock() {
        // Balance 0
        when(itemRepository.findAll()).thenReturn(Arrays.asList(testItem));
        when(stockTransactionRepository.getTotalInByItemId(1L)).thenReturn(50);
        when(stockTransactionRepository.getTotalOutByItemId(1L)).thenReturn(50);

        List<ItemDTO> result = itemService.getFilteredItems(null, "Byashize", null, "name,asc");

        assertEquals(1, result.size());
    }

    @Test
    @DisplayName("Should calculate current balance correctly")
    void getCurrentBalance_ShouldReturnCorrectBalance() {
        when(itemRepository.findById(1L)).thenReturn(Optional.of(testItem));
        when(stockTransactionRepository.getTotalInByItemId(1L)).thenReturn(100);
        when(stockTransactionRepository.getTotalOutByItemId(1L)).thenReturn(30);

        int balance = itemService.getCurrentBalance(1L);

        assertEquals(70, balance);
    }
}
