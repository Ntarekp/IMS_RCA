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
 */
@Repository
public interface StockTransactionRepository extends JpaRepository<StockTransaction, Long> {

    List<StockTransaction> findByItem(Item item);
    List<StockTransaction> findByItemId(Long itemId);
    List<StockTransaction> findByTransactionType(TransactionType type);
    List<StockTransaction> findByTransactionDateBetween(LocalDate startDate, LocalDate endDate);
    List<StockTransaction> findByItemAndTransactionType(Item item, TransactionType type);

    @Query("SELECT COALESCE(SUM(t.quantity), 0) FROM StockTransaction t " +
            "WHERE t.item.id = :itemId AND t.transactionType = 'IN'")
    Integer getTotalInByItemId(@Param("itemId") Long itemId);

    @Query("SELECT COALESCE(SUM(t.quantity), 0) FROM StockTransaction t " +
            "WHERE t.item.id = :itemId AND t.transactionType = 'OUT'")
    Integer getTotalOutByItemId(@Param("itemId") Long itemId);

    @Query("SELECT t FROM StockTransaction t ORDER BY t.transactionDate DESC, t.createdAt DESC")
    List<StockTransaction> findRecentTransactions();

    long countByCreatedAtAfter(LocalDateTime date);

    // New query to fetch all OUT transactions for analytics
    @Query("SELECT t FROM StockTransaction t WHERE t.transactionType = 'OUT'")
    List<StockTransaction> findAllOutTransactions();
}
