package npk.rca.ims.repository;

import npk.rca.ims.model.Item;
import npk.rca.ims.model.StockTransaction;
import npk.rca.ims.model.TransactionType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface StockTransactionRepository extends JpaRepository<StockTransaction, Integer> {

    List<StockTransaction> findByItem(Item item);
    List<StockTransaction> findByItemId(Integer itemId);
    List<StockTransaction> findByTransactionType(TransactionType type);

    List<StockTransaction> findByTransactionDateBetween(LocalDate startDate, LocalDate endDate);

    List<StockTransaction> findByItemAndTransactionType(Item item, TransactionType type);

    List<StockTransaction> findByItemAndTransactionDateBetween(Item item, LocalDate startDate, LocalDate endDate);


    @Query("SELECT COALESCE(SUM(t.quantity),0) FROM StockTransaction t" +
    "WHERE t.item.id =:itemId AND t.transactionType= 'IN'")
    Integer getTotalInByItemId(@Param("itemId") Long itemId);

    @Query("SELECT t FROM StockTransaction t ORDER BY t.transactionDate DESC, t.createdAt DESC")
    List<StockTransaction> findRecentTransactions();
}
