package npk.rca.ims.repository;
import npk.rca.ims.model.Item;
import npk.rca.ims.model.StockTransaction;
import npk.rca.ims.model.TransactionType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * StockTransactionRepository - Database access for transactions
 *
 * This repository includes:
 * 1. Basic CRUD (from JpaRepository)
 * 2. Custom query methods (Spring generates SQL)
 * 3. Custom JPQL queries (we write SQL-like queries)
 */
@Repository
public interface StockTransactionRepository extends JpaRepository<StockTransaction, Long> {

    /**
     * Find all transactions for a specific item
     *
     * Method name pattern: findBy[RelationshipField]
     * Spring generates: SELECT * FROM stock_transactions WHERE item_id = ?
     */
    List<StockTransaction> findByItem(Item item);

    /**
     * Find transactions by item ID (more convenient than passing Item object)
     */
    List<StockTransaction> findByItemId(Long itemId);

    /**
     * Find transactions by type (IN or OUT)
     */
    List<StockTransaction> findByTransactionType(TransactionType type);

    /**
     * Find transactions within date range
     *
     * "Between" keyword generates: WHERE transaction_date BETWEEN ? AND ?
     */
    List<StockTransaction> findByTransactionDateBetween(LocalDate startDate, LocalDate endDate);

    /**
     * Combine filters: Find transactions by item and type
     *
     * "And" keyword generates: WHERE item_id = ? AND transaction_type = ?
     */
    List<StockTransaction> findByItemAndTransactionType(Item item, TransactionType type);

    /**
     * CUSTOM JPQL QUERY - Calculate total IN for an item
     *
     * @Query - Write your own query in JPQL (Java Persistence Query Language)
     * JPQL is like SQL but uses entity/field names instead of table/column names
     *
     * :itemId - Named parameter (safer than ?1, ?2)
     *
     * This calculates: SUM of all IN transactions for one item
     */
    @Query("SELECT COALESCE(SUM(t.quantity), 0) FROM StockTransaction t " +
            "WHERE t.item.id = :itemId AND t.transactionType = 'IN'")
    Integer getTotalInByItemId(@Param("itemId") Long itemId);

    /**
     * Calculate total OUT for an item
     *
     * COALESCE(SUM(t.quantity), 0):
     *   - If no transactions exist, SUM returns null
     *   - COALESCE converts null to 0
     *   - Prevents NullPointerException
     */
    @Query("SELECT COALESCE(SUM(t.quantity), 0) FROM StockTransaction t " +
            "WHERE t.item.id = :itemId AND t.transactionType = 'OUT'")
    Integer getTotalOutByItemId(@Param("itemId") Long itemId);

    /**
     * Get recent transactions (last N records)
     *
     * Custom query with sorting and limiting
     */
    @Query("SELECT t FROM StockTransaction t ORDER BY t.transactionDate DESC, t.createdAt DESC")
    List<StockTransaction> findRecentTransactions();

    /**
     * Count transactions created after a specific date/time
     * Used for metrics (e.g., transactions this month)
     */
    long countByCreatedAtAfter(LocalDateTime date);

}