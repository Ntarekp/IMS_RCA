package npk.rca.ims.controller;

import npk.rca.ims.dto.DeleteRequestDTO;
import npk.rca.ims.dto.ItemDTO;
import npk.rca.ims.service.ItemService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.security.Principal;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ItemControllerTest {

    @Mock
    private ItemService itemService;

    @InjectMocks
    private ItemController itemController;

    @Mock
    private Principal principal;

    private ItemDTO testItemDTO;

    @BeforeEach
    void setUp() {
        testItemDTO = new ItemDTO();
        testItemDTO.setId(1L);
        testItemDTO.setName("Test Item");
        testItemDTO.setUnit("kg");
        testItemDTO.setMinimumStock(10);
    }

    @Test
    void getAllItems_ShouldReturnListOfItems() {
        when(itemService.getFilteredItems(null, null, null, "name,asc"))
                .thenReturn(Arrays.asList(testItemDTO));

        ResponseEntity<List<ItemDTO>> response = itemController.getAllItems(null, null, null, "name,asc");

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(1, response.getBody().size());
    }

    @Test
    void getItemById_ShouldReturnItem() {
        when(itemService.getItemById(1L)).thenReturn(testItemDTO);

        ResponseEntity<ItemDTO> response = itemController.getItemById(1L);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("Test Item", response.getBody().getName());
    }

    @Test
    void createItem_ShouldReturnCreatedItem() {
        when(itemService.createItem(any(ItemDTO.class))).thenReturn(testItemDTO);

        ResponseEntity<ItemDTO> response = itemController.createItem(testItemDTO);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertEquals("Test Item", response.getBody().getName());
    }

    @Test
    void updateItem_ShouldReturnUpdatedItem() {
        when(itemService.updateItem(eq(1L), any(ItemDTO.class))).thenReturn(testItemDTO);

        ResponseEntity<ItemDTO> response = itemController.updateItem(1L, testItemDTO);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("Test Item", response.getBody().getName());
    }

    @Test
    void deleteItem_ShouldReturnNoContent() {
        DeleteRequestDTO deleteRequest = new DeleteRequestDTO();
        deleteRequest.setPassword("password");

        when(principal.getName()).thenReturn("user@example.com");
        doNothing().when(itemService).deleteItemWithPasswordVerification(1L, "user@example.com", "password");

        ResponseEntity<Void> response = itemController.deleteItem(1L, deleteRequest, principal);

        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
    }
}
