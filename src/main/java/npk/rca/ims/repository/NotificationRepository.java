package npk.rca.ims.repository;

import npk.rca.ims.model.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {
    List<Notification> findAllByOrderByCreatedAtDesc();
    List<Notification> findByUserIdOrderByCreatedAtDesc(Long userId);
    long countByReadFalse();
    long countByUserIdAndReadFalse(Long userId);
}
