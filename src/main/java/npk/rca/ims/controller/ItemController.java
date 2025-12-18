package npk.rca.ims.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import npk.rca.ims.dto.ItemDTO;
import npk.rca.ims.service.ItemService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * ItemController - REST API endpoints for Item management
 *
 * @RestController = @Controller + @ResponseBody
 *   - All methods return JSON (not views)
 * @RequestMapping - Base URL for all endpoints
 * @CrossOrigin - Allow frontend from different domain (CORS)
 *
 * REST API CONVENTIONS:
 * GET /api/items          → List all items (200 OK)
 * GET /api/items/{id}     → Get one item (200 OK or 404 NOT FOUND)
 * POST /api/items         → Create item (201 CREATED)
 * PUT /api/items/{id}     → Update item (200 OK or 404 NOT FOUND)
 * DELETE /api/items/{id}  → Delete item (204 NO CONTENT or 404)
 */
@RestController
@RequestMapping("/api/items")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")  // Allow all origins (change in production!)
public class ItemController {

    private final ItemService itemService;

    /**
     * GET /api/items
     * List all items with their current balances
     *
     * Response: 200 OK
     * [
     *   {
     *     "id": 1,
     *     "name": "Rice",
     *     "unit": "Sacks",
     *     "minimumStock": 10,
     *     "currentBalance": 50,
     *     "isLowStock": false,
     *     ...
     *   }
     * ]
     */
    @GetMapping
    public ResponseEntity<List<ItemDTO>> getAllItems() {
        List<ItemDTO> items = itemService.getAllItems();
        return ResponseEntity.ok(items);
    }

    /**
     * GET /api/items/{id}
     * Get single item by ID
     *
     * @PathVariable - Extracts {id} from URL
     *
     * Response: 200 OK or 404 NOT FOUND
     */
    @GetMapping("/{id}")
    public ResponseEntity<ItemDTO> getItemById(@PathVariable Long id) {
        ItemDTO item = itemService.getItemById(id);
        return ResponseEntity.ok(item);
    }

    /**
     * POST /api/items
     * Create new item
     *
     * @RequestBody - Converts JSON to ItemDTO
     * @Valid - Triggers validation (@NotBlank, @Positive, etc.)
     *
     * Request body:
     * {
     *   "name": "Rice",
     *   "unit": "Sacks",
     *   "minimumStock": 10,
     *   "description": "Thai jasmine rice"
     * }
     *
     * Response: 201 CREATED
     */
    @PostMapping
    public ResponseEntity<ItemDTO> createItem(@Valid @RequestBody ItemDTO itemDTO) {
        ItemDTO createdItem = itemService.createItem(itemDTO);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdItem);
    }

    /**
     * PUT /api/items/{id}
     * Update existing item
     *
     * Both path variable {id} and request body required
     *
     * Response: 200 OK or 404 NOT FOUND
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
     * Delete item
     *
     * Response: 204 NO CONTENT (success, no body)
     * or 404 NOT FOUND or 400 BAD REQUEST (if has transactions)
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteItem(@PathVariable Long id) {
        itemService.deleteItem(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * PATCH /api/items/{id}/damaged
     * Record damaged quantity for an item
     *
     * Request body: { "damagedQuantity": 5 }
     *
     * Response: 200 OK with updated item
     *
     * Example:
     * curl -X PATCH http://localhost:8080/api/items/1/damaged \
     *   -H "Content-Type: application/json" \
     *   -d '{ "damagedQuantity": 5 }'
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