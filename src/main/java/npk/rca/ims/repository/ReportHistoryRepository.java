package npk.rca.ims.repository;

import npk.rca.ims.model.ReportHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ReportHistoryRepository extends JpaRepository<ReportHistory, Long> {
    List<ReportHistory> findAllByOrderByGeneratedDateDesc();
    
    List<ReportHistory> findByStatusNotAndGeneratedDateBefore(String status, LocalDateTime date);
}
