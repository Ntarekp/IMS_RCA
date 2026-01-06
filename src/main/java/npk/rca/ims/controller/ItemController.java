package npk.rca.ims.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import npk.rca.ims.dto.DeleteRequestDTO;
import npk.rca.ims.dto.ItemDTO;
import npk.rca.ims.service.ItemService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

/**
 * ItemController - REST API endpoints for Item management
 */
@RestController
@RequestMapping("/api/items")
@RequiredArgsConstructor
public class ItemController {

    private final ItemService itemService;

    /**
     * GET /api/items (with optional filters and sorting)
     */
    @GetMapping
    public ResponseEntity<List<ItemDTO>> getAllItems(
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String name,
            @RequestParam(required = false, defaultValue = "name,asc") String sort
    ) {
        List<ItemDTO> items = itemService.getFilteredItems(category, status, name, sort);
        return ResponseEntity.ok(items);
    }

    /**
     * GET /api/items/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<ItemDTO> getItemById(@PathVariable Long id) {
        ItemDTO item = itemService.getItemById(id);
        return ResponseEntity.ok(item);
    }

    /**
     * POST /api/items
     */
    @PostMapping
    public ResponseEntity<ItemDTO> createItem(@Valid @RequestBody ItemDTO itemDTO) {
        ItemDTO createdItem = itemService.createItem(itemDTO);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdItem);
    }

    /**
     * PUT /api/items/{id}
     */
    @PutMapping("/{id}")
    public ResponseEntity<ItemDTO> updateItem(
            @PathVariable Long id,
            @Valid @RequestBody ItemDTO itemDTO) {

        ItemDTO updatedItem = itemService.updateItem(id, itemDTO);
        return ResponseEntity.ok(updatedItem);
    }

    /**
     * DELETE /api/items/{id}
     * Securely delete an item by verifying user's password.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteItem(
            @PathVariable Long id,
            @Valid @RequestBody DeleteRequestDTO deleteRequest,
            Principal principal) {
        
        String username = principal.getName(); // Get current user's email from security context
        itemService.deleteItemWithPasswordVerification(id, username, deleteRequest.getPassword());
        
        return ResponseEntity.noContent().build();
    }

    /**
     * PATCH /api/items/{id}/damaged
     */
    @PatchMapping("/{id}/damaged")
    public ResponseEntity<ItemDTO> recordDamagedQuantity(
            @PathVariable Long id,
            @RequestBody DamagedQuantityRequest request) {
        ItemDTO updated = itemService.recordDamagedQuantity(id, request.getDamagedQuantity());
        return ResponseEntity.ok(updated);
    }

    // DTO for damaged quantity request
    public static class DamagedQuantityRequest {
        private int damagedQuantity;
        public int getDamagedQuantity() { return damagedQuantity; }
        public void setDamagedQuantity(int damagedQuantity) { this.damagedQuantity = damagedQuantity; }
    }
}
