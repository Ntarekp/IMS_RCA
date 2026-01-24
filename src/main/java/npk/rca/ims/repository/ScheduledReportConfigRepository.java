package npk.rca.ims.repository;

import npk.rca.ims.model.ScheduledReportConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ScheduledReportConfigRepository extends JpaRepository<ScheduledReportConfig, Long> {
    List<ScheduledReportConfig> findByActiveTrue();
}
