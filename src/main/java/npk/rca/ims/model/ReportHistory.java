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
@Table(name = "report_history")
public class ReportHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false)
    private String type; // STOCK, FINANCIAL, AUDIT, etc.

    @Column(nullable = false)
    private String format; // PDF, CSV

    @Column(nullable = false)
    private String status; // READY, FAILED, PROCESSING

    @Column(nullable = false)
    private String size;

    @Column(name = "file_path")
    private String filePath;

    @CreationTimestamp
    private LocalDateTime generatedDate;
}
