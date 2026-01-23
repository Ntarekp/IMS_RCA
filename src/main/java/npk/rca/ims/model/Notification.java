package npk.rca.ims.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "notifications")
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false, length = 1000)
    private String message;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private NotificationType type; // INFO, WARNING, ALERT, SUCCESS

    @Column(name = "is_read", nullable = false)
    private boolean read = false;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user; // Optional: null means system-wide or all users

    @CreationTimestamp
    private LocalDateTime createdAt;
    
    public enum NotificationType {
        INFO, WARNING, ALERT, SUCCESS
    }
}
