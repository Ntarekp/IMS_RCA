package npk.rca.ims.repository;

import npk.rca.ims.model.Item;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * ItemRepository - Database access for Items
 *
 * MAGIC OF SPRING DATA JPA:
 * - You write the INTERFACE only (no implementation!)
 * - Spring creates the implementation automatically at runtime
 * - You get methods like save(), findAll(), findById() for FREE
 *
 * JpaRepository<Item, Long>:
 *   - Item = Entity type
 *   - Long = Primary key type (id field)
 *
 * BUILT-IN METHODS (you get these automatically):
 * - save(item) - Insert or update
 * - findById(id) - Find by primary key
 * - findAll() - Get all items
 * - deleteById(id) - Delete by ID
 * - count() - Count total items
 * - existsById(id) - Check if exists
 *
 * You can ADD custom query methods below!
 */
@Repository
public interface ItemRepository extends JpaRepository<Item, Long> {

    /**
     * Custom Query Method - Find item by exact name
     *
     * Spring Data JPA reads the method name and generates SQL!
     * "findByName" → SELECT * FROM items WHERE name = ?
     *
     * Returns Optional<Item>:
     *   - If found: Optional.of(item)
     *   - If not found: Optional.empty()
     * Prevents NullPointerException!
     */
    Optional<Item> findByName(String name);

    /**
     * Custom Query - Check if item name exists
     *
     * "existsByName" → SELECT COUNT(*) > 0 FROM items WHERE name = ?
     * Returns boolean (true/false)
     *
     * Use case: Prevent duplicate item names
     */
    boolean existsByName(String name);


}