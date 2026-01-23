package npk.rca.ims.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "scheduled_report_configs")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ScheduledReportConfig {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String email;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ReportFrequency frequency;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ScheduledReportType reportType;

    private LocalDateTime lastSent;
    
    private boolean active;

    public enum ReportFrequency {
        DAILY, WEEKLY, MONTHLY
    }

    public enum ScheduledReportType {
        ALL_REPORTS_ZIP,
        TRANSACTION_HISTORY,
        STOCK_BALANCE,
        LOW_STOCK
    }
}
