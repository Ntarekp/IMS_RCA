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

    @Scheduled(cron = "0 0 8 * * *") // Run every day at 8 AM
    public void processScheduledReports() {
        log.info("Starting scheduled report processing...");
        List<ScheduledReportConfig> configs = configRepository.findByActiveTrue();
        
        for (ScheduledReportConfig config : configs) {
            try {
                if (isReportDue(config)) {
                    generateAndSendReport(config);
                    config.setLastSent(LocalDateTime.now());
                    configRepository.save(config);
                }
            } catch (Exception e) {
                log.error("Failed to process scheduled report for config ID: {}", config.getId(), e);
            }
        }
        log.info("Scheduled report processing completed.");
    }

    private boolean isReportDue(ScheduledReportConfig config) {
        if (config.getLastSent() == null) return true;
        
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime lastSent = config.getLastSent();
        
        return switch (config.getFrequency()) {
            case DAILY -> lastSent.plusDays(1).isBefore(now);
            case WEEKLY -> lastSent.plusWeeks(1).isBefore(now);
            case MONTHLY -> lastSent.plusMonths(1).isBefore(now);
        };
    }

    private void generateAndSendReport(ScheduledReportConfig config) throws IOException {
        LocalDate end = LocalDate.now();
        LocalDate start = calculateStartDate(config.getFrequency(), end);
        
        Map<String, File> attachments = new HashMap<>();
        String subject = "RCA IMS - Automated Report (" + config.getFrequency() + ")";
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

    private LocalDate calculateStartDate(ScheduledReportConfig.ReportFrequency frequency, LocalDate end) {
        return switch (frequency) {
            case DAILY -> end.minusDays(1);
            case WEEKLY -> end.minusWeeks(1);
            case MONTHLY -> end.minusMonths(1);
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
