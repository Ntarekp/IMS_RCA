package npk.rca.ims.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import npk.rca.ims.model.ScheduledReportConfig;
import npk.rca.ims.repository.ScheduledReportConfigRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Service
@RequiredArgsConstructor
@Slf4j
public class ScheduledReportService {

    private final ScheduledReportConfigRepository configRepository;
    private final ReportService reportService;
    private final EmailService emailService;

    @Scheduled(cron = "0 * * * * *") // Run every minute to be responsive
    public void processScheduledReports() {
        log.info("Starting scheduled report processing check...");
        List<ScheduledReportConfig> configs = configRepository.findByActiveTrue();
        log.info("Found {} active scheduled report configs", configs.size());
        
        for (ScheduledReportConfig config : configs) {
            try {
                if (shouldSendReport(config)) {
                    log.info("Sending scheduled report for config ID: {}", config.getId());
                    generateAndSendReport(config);
                    config.setLastSent(LocalDateTime.now());
                    configRepository.save(config);
                    log.info("Successfully sent and updated report for config ID: {}", config.getId());
                } else {
                    log.debug("Skipping report for config ID: {} - Not due yet", config.getId());
                }
            } catch (Exception e) {
                log.error("Failed to process scheduled report for config ID: {}", config.getId(), e);
            }
        }
        log.info("Scheduled report processing completed.");
    }

    private boolean shouldSendReport(ScheduledReportConfig config) {
        LocalDateTime now = LocalDateTime.now();
        
        // Handle INTERVAL frequency separately
        if (config.getFrequency() == ScheduledReportConfig.ReportFrequency.INTERVAL) {
            if (config.getLastSent() == null) {
                log.info("Interval report (ID: {}) ready: First run", config.getId());
                return true;
            }
            
            // Default to 24 hours if not specified
            int intervalHours = config.getIntervalHours() != null ? config.getIntervalHours() : 24;
            long hoursSinceLast = java.time.Duration.between(config.getLastSent(), now).toHours();
            
            boolean ready = hoursSinceLast >= intervalHours;
            if (ready) {
                log.info("Interval report (ID: {}) ready: {} hours passed (interval: {})", config.getId(), hoursSinceLast, intervalHours);
            }
            return ready;
        }

        // For other frequencies, check time preference (Hour AND Minute precision)
        java.time.LocalTime scheduledTime = config.getScheduledTime() != null ? config.getScheduledTime() : java.time.LocalTime.of(8, 0);
        
        // If current time is BEFORE the scheduled time, wait.
        if (now.toLocalTime().isBefore(scheduledTime)) {
            // log.debug("Report (ID: {}) not ready: Current time {} is before Scheduled time {}", config.getId(), now.toLocalTime(), scheduledTime);
            return false;
        }

        // Check if already sent recently based on frequency
        if (config.getLastSent() == null) {
            log.info("Report (ID: {}) ready: First run for non-interval", config.getId());
            return true;
        }
        
        LocalDateTime lastSent = config.getLastSent();
        boolean ready = false;
        
        switch (config.getFrequency()) {
            case DAILY:
                ready = !lastSent.toLocalDate().isEqual(now.toLocalDate());
                break;
            case WEEKLY:
                // Check if at least 1 week has passed (ensure we are on or after the target date)
                ready = lastSent.plusWeeks(1).toLocalDate().compareTo(now.toLocalDate()) <= 0;
                break;
            case MONTHLY:
                // Check if at least 1 month has passed
                ready = lastSent.plusMonths(1).toLocalDate().compareTo(now.toLocalDate()) <= 0;
                break;
            case INTERVAL:
                ready = true; // Should be handled above
                break;
        }
        
        if (ready) {
            log.info("Report (ID: {}) ready: Frequency check passed for {}", config.getId(), config.getFrequency());
        }
        
        return ready;
    }

    private void generateAndSendReport(ScheduledReportConfig config) throws IOException {
        LocalDate end = LocalDate.now();
        LocalDate start = calculateStartDate(config, end);
        
        Map<String, File> attachments = new HashMap<>();
        String subject = "RCA IMS - Automated Report (" + config.getFrequency() + ")";
        if (config.getFrequency() == ScheduledReportConfig.ReportFrequency.INTERVAL) {
             subject += " - Every " + (config.getIntervalHours() != null ? config.getIntervalHours() : 24) + "h";
        }
        
        String body = createEmailBody(config, start, end);

        if (config.getReportType() == ScheduledReportConfig.ScheduledReportType.ALL_REPORTS_ZIP) {
            File zipFile = createAllReportsZip(start, end);
            attachments.put("Reports_" + end.toString() + ".zip", zipFile);
        } else {
            // Handle individual reports
            File reportFile = generateSpecificReport(config.getReportType(), start, end);
            if (reportFile != null) {
                attachments.put(reportFile.getName(), reportFile);
            }
        }

        emailService.sendReportEmailWithAttachment(config.getEmail(), subject, body, attachments);
        
        // Cleanup temp files
        cleanupAttachments(attachments);
    }

    private LocalDate calculateStartDate(ScheduledReportConfig config, LocalDate end) {
        if (config.getFrequency() == ScheduledReportConfig.ReportFrequency.INTERVAL) {
            int hours = config.getIntervalHours() != null ? config.getIntervalHours() : 24;
            // Approximate days for interval
            long days = Math.max(1, hours / 24);
            return end.minusDays(days);
        }

        return switch (config.getFrequency()) {
            case DAILY -> end.minusDays(1);
            case WEEKLY -> end.minusWeeks(1);
            case MONTHLY -> end.minusMonths(1);
            case INTERVAL -> end.minusDays(1); // Fallback
        };
    }

    private File createAllReportsZip(LocalDate start, LocalDate end) throws IOException {
        Path tempZip = Files.createTempFile("reports_bundle_", ".zip");
        
        try (ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(tempZip.toFile()))) {
            // 1. Transaction Report PDF
            addToZip(zos, "Transaction_History.pdf", reportService.generateTransactionReportPdf(start, end, null, "Transaction History"));
            
            // 2. Transaction Report Excel
            addToZip(zos, "Transaction_History.xlsx", reportService.generateTransactionReportExcel(start, end, null, "Transaction History"));
            
            // 3. Stock Balance Excel
            addToZip(zos, "Stock_Balance.xlsx", reportService.generateBalanceReportExcel());
            
            // 4. Low Stock PDF
            addToZip(zos, "Low_Stock.pdf", reportService.generateLowStockReportPdf());
            
            // 5. Supplier List Excel
            addToZip(zos, "Supplier_List.xlsx", reportService.generateSupplierReportExcel());
        }
        
        return tempZip.toFile();
    }

    private void addToZip(ZipOutputStream zos, String filename, byte[] content) throws IOException {
        ZipEntry entry = new ZipEntry(filename);
        zos.putNextEntry(entry);
        zos.write(content);
        zos.closeEntry();
    }

    private File generateSpecificReport(ScheduledReportConfig.ScheduledReportType type, LocalDate start, LocalDate end) throws IOException {
        byte[] content;
        String filename;
        
        switch (type) {
            case TRANSACTION_HISTORY -> {
                content = reportService.generateTransactionReportPdf(start, end, null, "Transaction History");
                filename = "Transaction_History.pdf";
            }
            case STOCK_BALANCE -> {
                content = reportService.generateBalanceReportExcel();
                filename = "Stock_Balance.xlsx";
            }
            case LOW_STOCK -> {
                content = reportService.generateLowStockReportPdf();
                filename = "Low_Stock.pdf";
            }
            default -> {
                return null;
            }
        }
        
        Path tempFile = Files.createTempFile("report_", "_" + filename);
        Files.write(tempFile, content);
        return tempFile.toFile();
    }

    private String createEmailBody(ScheduledReportConfig config, LocalDate start, LocalDate end) {
        return String.format("""
            <div style="font-family: Arial, sans-serif; padding: 20px;">
                <h2>Automated Report</h2>
                <p>Please find attached the requested reports.</p>
                <p><strong>Frequency:</strong> %s</p>
                <p><strong>Period:</strong> %s to %s</p>
                <br>
                <p>Best regards,<br>RCA Inventory Management System</p>
            </div>
            """, config.getFrequency(), start, end);
    }
    
    private void cleanupAttachments(Map<String, File> attachments) {
        for (File file : attachments.values()) {
            try {
                Files.deleteIfExists(file.toPath());
            } catch (IOException e) {
                log.warn("Failed to delete temp file: {}", file.getAbsolutePath());
            }
        }
    }
}
