package npk.rca.ims.service;

import com.lowagie.text.*;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import npk.rca.ims.dto.CategoryDistributionDTO;
import npk.rca.ims.dto.StockBalanceDTO;
import npk.rca.ims.dto.StockTransactionDTO;
import npk.rca.ims.dto.SupplierDTO;
import npk.rca.ims.exceptions.ReportGenerationException;
import npk.rca.ims.model.Item;
import npk.rca.ims.model.ReportHistory;
import npk.rca.ims.model.TransactionType;
import npk.rca.ims.repository.ItemRepository;
import npk.rca.ims.repository.ReportHistoryRepository;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.util.IOUtils;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.core.io.ClassPathResource;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReportService {

    private static final String HEADER_IMAGE_PATH = "static/rca-info.png";
    private static final String STORAGE_DIR = "reports_storage";
    private static final String ORGANIZATION_NAME = "RWANDA CODING ACADEMY";
    private static final String DATE_FORMAT = "dd-MM-yyyy";
    private static final String DATETIME_FORMAT = "dd-MM-yyyy HH:mm";
    private static final String DEFAULT_UNIT = "Kg";
    private static final String PLACEHOLDER = "-";

    private final StockTransactionService transactionService;
    private final StockBalanceService balanceService;
    private final SupplierService supplierService;
    private final ReportHistoryRepository reportHistoryRepository;
    private final ItemRepository itemRepository;

    public List<ReportHistory> getReportHistory() {
        return reportHistoryRepository.findAllByOrderByGeneratedDateDesc();
    }

    public ReportHistory getReportHistoryById(Long id) {
        return reportHistoryRepository.findById(id)
                .orElseThrow(() -> new npk.rca.ims.exceptions.ResourceNotFoundException("Report history not found with id: " + id));
    }

    public byte[] getReportFileContent(Long id) {
        ReportHistory history = reportHistoryRepository.findById(id)
                .orElseThrow(() -> new npk.rca.ims.exceptions.ResourceNotFoundException("Report history not found with id: " + id));
        
        if ("EXPIRED".equals(history.getStatus())) {
            throw new npk.rca.ims.exceptions.ResourceNotFoundException("Report has expired and is no longer available.");
        }

        if (history.getFilePath() == null) {
             throw new npk.rca.ims.exceptions.ResourceNotFoundException("File path not found for report: " + id);
        }
        
        try {
            return Files.readAllBytes(Paths.get(history.getFilePath()));
        } catch (IOException e) {
            log.error("Failed to read report file", e);
            throw new RuntimeException("Failed to read report file", e);
        }
    }

    @Scheduled(cron = "0 0 0 * * ?") // Run every day at midnight
    public void cleanupExpiredReports() {
        log.info("Starting report cleanup task...");
        LocalDateTime cutoffDate = LocalDateTime.now().minusDays(5);
        List<ReportHistory> expiredReports = reportHistoryRepository.findByStatusNotAndGeneratedDateBefore("EXPIRED", cutoffDate);
        
        int deletedCount = 0;
        for (ReportHistory report : expiredReports) {
            try {
                if (report.getFilePath() != null) {
                    Path filePath = Paths.get(report.getFilePath());
                    if (Files.exists(filePath)) {
                        Files.delete(filePath);
                    }
                }
                report.setFilePath(null);
                report.setStatus("EXPIRED");
                reportHistoryRepository.save(report);
                deletedCount++;
            } catch (Exception e) {
                log.error("Failed to cleanup report id: " + report.getId(), e);
            }
        }
        
        log.info("Report cleanup completed. Deleted {} reports.", deletedCount);
    }

    private String saveFileToDisk(byte[] content, String prefix, String extension) {
        try {
            Path storagePath = Paths.get(STORAGE_DIR);
            if (!Files.exists(storagePath)) {
                Files.createDirectories(storagePath);
            }
            
            String filename = prefix.replaceAll("[^a-zA-Z0-9.-]", "_") + "_" + UUID.randomUUID().toString().substring(0, 8) + "." + extension.toLowerCase();
            Path filePath = storagePath.resolve(filename);
            Files.write(filePath, content);
            return filePath.toString();
        } catch (IOException e) {
            log.error("Failed to save report to disk", e);
            return null;
        }
    }

    private void saveReportHistory(String title, String type, String format, String status, int sizeBytes, String filePath) {
        String size;
        if (sizeBytes > 1024 * 1024) {
            size = String.format("%.2f MB", sizeBytes / (1024.0 * 1024.0));
        } else {
            size = String.format("%.2f KB", sizeBytes / 1024.0);
        }

        ReportHistory history = ReportHistory.builder()
                .title(title)
                .type(type)
                .format(format)
                .status(status)
                .size(size)
                .filePath(filePath)
                .build();

        reportHistoryRepository.save(history);
    }

    // ============ TRANSACTION REPORTS ============

    public byte[] generateTransactionReportPdf(LocalDate startDate, LocalDate endDate, Long itemId, String title) {
        try {
            String reportTitle = (title != null && !title.isEmpty()) ? title : "Complete Transaction History";
            List<StockTransactionDTO> transactions = getFilteredTransactions(startDate, endDate, itemId, null);
            byte[] report = createTransactionPdfReport(transactions, reportTitle, startDate, endDate);
            
            String filePath = saveFileToDisk(report, "Transaction_Report", "pdf");
            saveReportHistory(reportTitle, "TRANSACTION", "PDF", "READY", report.length, filePath);
            
            return report;
        } catch (Exception e) {
            log.error("Error generating transaction PDF report", e);
            saveReportHistory(title != null ? title : "Complete Transaction History", "TRANSACTION", "PDF", "FAILED", 0, null);
            throw new ReportGenerationException("Failed to generate transaction PDF report", e);
        }
    }

    public byte[] generateTransactionReportExcel(LocalDate startDate, LocalDate endDate, Long itemId, String title) {
        try {
            String reportTitle = (title != null && !title.isEmpty()) ? title : "Complete Transaction History";
            List<StockTransactionDTO> transactions = getFilteredTransactions(startDate, endDate, itemId, null);
            byte[] report = createTransactionExcelReport(transactions, reportTitle, startDate, endDate, itemId);
            
            String filePath = saveFileToDisk(report, "Transaction_Report", "xlsx");
            saveReportHistory(reportTitle, "TRANSACTION", "EXCEL", "READY", report.length, filePath);
            
            return report;
        } catch (Exception e) {
            log.error("Error generating transaction Excel report", e);
            saveReportHistory(title != null ? title : "Complete Transaction History", "TRANSACTION", "EXCEL", "FAILED", 0, null);
            throw new ReportGenerationException("Failed to generate transaction Excel report", e);
        }
    }

    // ============ STOCK IN REPORTS ============

    public byte[] generateStockInReportPdf(LocalDate startDate, LocalDate endDate, Long supplierId) {
        try {
            List<StockTransactionDTO> transactions = getFilteredTransactions(startDate, endDate, null, TransactionType.IN);
            if (supplierId != null) {
                transactions = transactions.stream()
                        .filter(t -> supplierId.equals(t.getSupplierId()))
                        .collect(Collectors.toList());
            }
            byte[] report = createTransactionPdfReport(transactions, "Stock IN Report", startDate, endDate);
            
            String filePath = saveFileToDisk(report, "Stock_IN_Report", "pdf");
            saveReportHistory("Stock IN Report", "STOCK_IN", "PDF", "READY", report.length, filePath);
            
            return report;
        } catch (Exception e) {
            log.error("Error generating stock-in PDF report", e);
            saveReportHistory("Stock IN Report", "STOCK_IN", "PDF", "FAILED", 0, null);
            throw new ReportGenerationException("Failed to generate stock-in PDF report", e);
        }
    }

    public byte[] generateStockInReportExcel(LocalDate startDate, LocalDate endDate, Long supplierId) {
        try {
            List<StockTransactionDTO> transactions = getFilteredTransactions(startDate, endDate, null, TransactionType.IN);
            if (supplierId != null) {
                transactions = transactions.stream()
                        .filter(t -> supplierId.equals(t.getSupplierId()))
                        .collect(Collectors.toList());
            }
            byte[] report = createTransactionExcelReport(transactions, "Stock IN Report", startDate, endDate, null);
            
            String filePath = saveFileToDisk(report, "Stock_IN_Report", "xlsx");
            saveReportHistory("Stock IN Report", "STOCK_IN", "EXCEL", "READY", report.length, filePath);
            
            return report;
        } catch (Exception e) {
            log.error("Error generating stock-in Excel report", e);
            saveReportHistory("Stock IN Report", "STOCK_IN", "EXCEL", "FAILED", 0, null);
            throw new ReportGenerationException("Failed to generate stock-in Excel report", e);
        }
    }

    // ============ STOCK OUT REPORTS ============

    public byte[] generateStockOutReportPdf(LocalDate startDate, LocalDate endDate) {
        try {
            List<StockTransactionDTO> transactions = getFilteredTransactions(startDate, endDate, null, TransactionType.OUT);
            byte[] report = createTransactionPdfReport(transactions, "Stock OUT Report", startDate, endDate);
            
            String filePath = saveFileToDisk(report, "Stock_OUT_Report", "pdf");
            saveReportHistory("Stock OUT Report", "STOCK_OUT", "PDF", "READY", report.length, filePath);
            
            return report;
        } catch (Exception e) {
            log.error("Error generating stock-out PDF report", e);
            saveReportHistory("Stock OUT Report", "STOCK_OUT", "PDF", "FAILED", 0, null);
            throw new ReportGenerationException("Failed to generate stock-out PDF report", e);
        }
    }

    public byte[] generateStockOutReportExcel(LocalDate startDate, LocalDate endDate) {
        try {
            List<StockTransactionDTO> transactions = getFilteredTransactions(startDate, endDate, null, TransactionType.OUT);
            byte[] report = createTransactionExcelReport(transactions, "Stock OUT Report", startDate, endDate, null);
            
            String filePath = saveFileToDisk(report, "Stock_OUT_Report", "xlsx");
            saveReportHistory("Stock OUT Report", "STOCK_OUT", "EXCEL", "READY", report.length, filePath);
            
            return report;
        } catch (Exception e) {
            log.error("Error generating stock-out Excel report", e);
            saveReportHistory("Stock OUT Report", "STOCK_OUT", "EXCEL", "FAILED", 0, null);
            throw new ReportGenerationException("Failed to generate stock-out Excel report", e);
        }
    }

    // ============ BALANCE REPORTS ============

    public byte[] generateBalanceReportPdf() {
        try {
            List<StockBalanceDTO> balances = balanceService.getAllBalances();
            byte[] report = createBalancePdfReport(balances);
            
            String filePath = saveFileToDisk(report, "Stock_Balance_Report", "pdf");
            saveReportHistory("Stock Balance Report", "BALANCE", "PDF", "READY", report.length, filePath);
            
            return report;
        } catch (Exception e) {
            log.error("Error generating balance PDF report", e);
            saveReportHistory("Stock Balance Report", "BALANCE", "PDF", "FAILED", 0, null);
            throw new ReportGenerationException("Failed to generate balance PDF report", e);
        }
    }

    public byte[] generateBalanceReportExcel() {
        try {
            List<StockBalanceDTO> balances = balanceService.getAllBalances();
            byte[] report = createBalanceExcelReport(balances);
            
            String filePath = saveFileToDisk(report, "Stock_Balance_Report", "xlsx");
            saveReportHistory("Stock Balance Report", "BALANCE", "EXCEL", "READY", report.length, filePath);
            
            return report;
        } catch (Exception e) {
            log.error("Error generating balance Excel report", e);
            saveReportHistory("Stock Balance Report", "BALANCE", "EXCEL", "FAILED", 0, null);
            throw new ReportGenerationException("Failed to generate balance Excel report", e);
        }
    }

    // ============ LOW STOCK REPORTS ============

    public byte[] generateLowStockReportPdf() {
        try {
            List<StockBalanceDTO> lowStockItems = balanceService.getLowStockItems();
            byte[] report = createLowStockPdfReport(lowStockItems);
            
            String filePath = saveFileToDisk(report, "Low_Stock_Report", "pdf");
            saveReportHistory("Low Stock Report", "LOW_STOCK", "PDF", "READY", report.length, filePath);
            
            return report;
        } catch (Exception e) {
            log.error("Error generating low stock PDF report", e);
            saveReportHistory("Low Stock Report", "LOW_STOCK", "PDF", "FAILED", 0, null);
            throw new ReportGenerationException("Failed to generate low stock PDF report", e);
        }
    }

    public byte[] generateLowStockReportExcel() {
        try {
            List<StockBalanceDTO> lowStockItems = balanceService.getLowStockItems();
            byte[] report = createLowStockExcelReport(lowStockItems);
            
            String filePath = saveFileToDisk(report, "Low_Stock_Report", "xlsx");
            saveReportHistory("Low Stock Report", "LOW_STOCK", "EXCEL", "READY", report.length, filePath);
            
            return report;
        } catch (Exception e) {
            log.error("Error generating low stock Excel report", e);
            saveReportHistory("Low Stock Report", "LOW_STOCK", "EXCEL", "FAILED", 0, null);
            throw new ReportGenerationException("Failed to generate low stock Excel report", e);
        }
    }

    // ============ SUPPLIER REPORTS ============

    public byte[] generateSupplierReportPdf() {
        try {
            List<SupplierDTO> suppliers = supplierService.getAllActiveSuppliers();
            byte[] report = createSupplierPdfReport(suppliers);
            
            String filePath = saveFileToDisk(report, "Supplier_Report", "pdf");
            saveReportHistory("Supplier Report", "SUPPLIER", "PDF", "READY", report.length, filePath);
            
            return report;
        } catch (Exception e) {
            log.error("Error generating supplier PDF report", e);
            saveReportHistory("Supplier Report", "SUPPLIER", "PDF", "FAILED", 0, null);
            throw new ReportGenerationException("Failed to generate supplier PDF report", e);
        }
    }

    public byte[] generateSupplierReportExcel() {
        try {
            List<SupplierDTO> suppliers = supplierService.getAllActiveSuppliers();
            byte[] report = createSupplierExcelReport(suppliers);
            
            String filePath = saveFileToDisk(report, "Supplier_Report", "xlsx");
            saveReportHistory("Supplier Report", "SUPPLIER", "EXCEL", "READY", report.length, filePath);
            
            return report;
        } catch (Exception e) {
            log.error("Error generating supplier Excel report", e);
            saveReportHistory("Supplier Report", "SUPPLIER", "EXCEL", "FAILED", 0, null);
            throw new ReportGenerationException("Failed to generate supplier Excel report", e);
        }
    }

    // ============ ANALYTICS REPORTS ============

    public List<CategoryDistributionDTO> getCategoryDistribution() {
        List<StockBalanceDTO> balances = balanceService.getAllBalances();

        Map<String, List<StockBalanceDTO>> groupedByCategory = balances.stream()
                .collect(Collectors.groupingBy(item ->
                        item.getCategory() != null && !item.getCategory().isEmpty()
                                ? item.getCategory()
                                : "Uncategorized"
                ));

        return groupedByCategory.entrySet().stream()
                .map(entry -> CategoryDistributionDTO.builder()
                        .category(entry.getKey())
                        .itemCount(entry.getValue().size())
                        .totalStock(entry.getValue().stream()
                                .mapToLong(StockBalanceDTO::getCurrentBalance)
                                .sum())
                        .build())
                .collect(Collectors.toList());
    }

    // ============ HELPER METHODS ============

    private List<StockTransactionDTO> getFilteredTransactions(
            LocalDate startDate,
            LocalDate endDate,
            Long itemId,
            TransactionType type
    ) {
        List<StockTransactionDTO> transactions = transactionService.getAllTransactions();

        return transactions.stream()
                .filter(t -> startDate == null || !t.getTransactionDate().isBefore(startDate))
                .filter(t -> endDate == null || !t.getTransactionDate().isAfter(endDate))
                .filter(t -> itemId == null || itemId.equals(t.getItemId()))
                .filter(t -> type == null || type.equals(t.getTransactionType()))
                .sorted(Comparator.comparing(StockTransactionDTO::getTransactionDate).reversed())
                .collect(Collectors.toList());
    }

    // ============ TRANSACTION PDF REPORTS ============

    private byte[] createTransactionPdfReport(
            List<StockTransactionDTO> transactions,
            String reportTitle,
            LocalDate startDate,
            LocalDate endDate
    ) throws DocumentException, IOException {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Document document = new Document(PageSize.A4.rotate());
            PdfWriter writer = PdfWriter.getInstance(document, out);
            writer.setPageEvent(new PageNumberFooter());
            document.open();

            addHeaderImage(document);
            addTitle(document, reportTitle);
            addDateRangeInfo(document, startDate, endDate);
            addTimestamp(document);

            if (transactions.isEmpty()) {
                addNoDataMessage(document);
            } else {
                PdfPTable table = createTransactionPdfTable(transactions);
                document.add(table);
                addSummaryStats(document, transactions);
            }

            document.close();
            return out.toByteArray();
        }
    }

    private PdfPTable createTransactionPdfTable(List<StockTransactionDTO> transactions) throws DocumentException {
        PdfPTable table = new PdfPTable(9);
        table.setWidthPercentage(100);
        // Date, Ref, Item, Unit, In, Out, Balance, Source/Dest, Remarks
        table.setWidths(new float[]{3, 3, 4, 2, 2, 2, 2, 4, 4});

        addTransactionPdfTableHeader(table);

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(DATE_FORMAT);
        for (StockTransactionDTO tx : transactions) {
            addTransactionPdfDataRow(table, tx, formatter);
        }

        return table;
    }

    private void addTransactionPdfTableHeader(PdfPTable table) {
        String[] headers = {
                "Date", "Ref No.", "Item Name", "Unit",
                "Stock IN", "Stock OUT", "Balance",
                "Source / Destination", "Remarks"
        };

        for (String header : headers) {
            PdfPCell cell = new PdfPCell(
                    new Phrase(header, FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, Color.WHITE))
            );
            cell.setBackgroundColor(Color.DARK_GRAY);
            cell.setHorizontalAlignment(Element.ALIGN_CENTER);
            cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
            cell.setPadding(5);
            table.addCell(cell);
        }
    }

    private void addTransactionPdfDataRow(PdfPTable table, StockTransactionDTO tx, DateTimeFormatter formatter) {
        // 1. Date
        addCell(table, tx.getTransactionDate().format(formatter));
        
        // 2. Reference
        String reference = (tx.getReferenceNumber() != null && !tx.getReferenceNumber().isEmpty()) 
                ? tx.getReferenceNumber() 
                : "TX-" + tx.getId();
        addCell(table, reference);
        
        // 3. Item Name
        addCell(table, tx.getItemName());
        
        // 4. Unit
        addCell(table, Optional.ofNullable(tx.getUnit()).orElse(DEFAULT_UNIT));
        
        // 5. Stock IN & 6. Stock OUT
        if (tx.getTransactionType() == TransactionType.IN) {
            addCell(table, String.valueOf(tx.getQuantity())); // IN
            addCell(table, "-"); // OUT
        } else {
            addCell(table, "-"); // IN
            addCell(table, String.valueOf(tx.getQuantity())); // OUT
        }
        
        // 7. Balance
        addCell(table, String.valueOf(tx.getBalanceAfter()));
        
        // 8. Source / Destination
        String sourceOrDest = PLACEHOLDER;
        if (tx.getTransactionType() == TransactionType.IN) {
            sourceOrDest = Optional.ofNullable(tx.getSupplierName()).orElse("Internal Adjustment");
        } else {
            // For OUT transactions, try to get recipient from notes, otherwise default
            sourceOrDest = (tx.getNotes() != null && !tx.getNotes().isEmpty()) ? tx.getNotes() : "Issued Out";
        }
        addCell(table, sourceOrDest);
        
        // 9. Remarks
        addCell(table, Optional.ofNullable(tx.getNotes()).orElse(PLACEHOLDER));
    }

    // ============ TRANSACTION EXCEL REPORTS ============

    private byte[] createTransactionExcelReport(
            List<StockTransactionDTO> transactions,
            String reportTitle,
            LocalDate startDate,
            LocalDate endDate,
            Long itemId
    ) throws IOException {
        try (XSSFWorkbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            Sheet sheet = workbook.createSheet("Transactions");

            addExcelHeaderImage(workbook, sheet);
            addExcelTitle(workbook, sheet, reportTitle, 4); // Writes to 4 and 5

            int headerRow = 6;
            
            // Add Item Details if specific item selected (Stock Card style)
            if (itemId != null) {
                Optional<Item> itemOpt = itemRepository.findById(itemId);
                if (itemOpt.isPresent()) {
                    addExcelItemDetails(workbook, sheet, itemOpt.get(), headerRow);
                    headerRow += 4; // Shift down
                }
            }

            if (startDate != null && endDate != null) {
                addExcelDateRange(workbook, sheet, startDate, endDate, headerRow);
                headerRow += 1;
            } else {
                 // If no date range, just add a small spacer or title row if needed.
                 // Current logic assumes date range usually exists. 
                 // If not, we just proceed.
            }

            createTransactionExcelHeader(workbook, sheet, headerRow);

            if (transactions.isEmpty()) {
                addExcelNoDataMessage(workbook, sheet, headerRow + 1);
            } else {
                populateTransactionExcelData(sheet, transactions, headerRow + 1);
            }

            autoSizeColumns(sheet, 9);

            workbook.write(out);
            return out.toByteArray();
        }
    }

    private void addExcelItemDetails(Workbook workbook, Sheet sheet, Item item, int startRow) {
        CellStyle labelStyle = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        labelStyle.setFont(font);

        // Row 1
        Row row1 = sheet.createRow(startRow);
        Cell c1 = row1.createCell(0); c1.setCellValue("Item Name:"); c1.setCellStyle(labelStyle);
        row1.createCell(1).setCellValue(item.getName());
        
        Cell c2 = row1.createCell(3); c2.setCellValue("Category:"); c2.setCellStyle(labelStyle);
        row1.createCell(4).setCellValue(Optional.ofNullable(item.getCategory()).orElse("-"));

        // Row 2
        Row row2 = sheet.createRow(startRow + 1);
        Cell c3 = row2.createCell(0); c3.setCellValue("SKU / ID:"); c3.setCellStyle(labelStyle);
        row2.createCell(1).setCellValue("ITEM-" + item.getId());
        
        Cell c4 = row2.createCell(3); c4.setCellValue("Min Stock:"); c4.setCellStyle(labelStyle);
        row2.createCell(4).setCellValue(item.getMinimumStock());

        // Row 3
        Row row3 = sheet.createRow(startRow + 2);
        Cell c5 = row3.createCell(0); c5.setCellValue("Unit:"); c5.setCellStyle(labelStyle);
        row3.createCell(1).setCellValue(item.getUnit());
    }

    private void createTransactionExcelHeader(Workbook workbook, Sheet sheet, int rowNum) {
        Row headerRow = sheet.createRow(rowNum);
        String[] headers = {
                "Date", "Ref No.", "Item Name", "Unit",
                "Stock IN", "Stock OUT", "Balance",
                "Source / Destination", "Remarks"
        };

        CellStyle headerStyle = createHeaderCellStyle(workbook);

        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
        }
    }

    private void populateTransactionExcelData(Sheet sheet, List<StockTransactionDTO> transactions, int startRow) {
        int rowNum = startRow;
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(DATE_FORMAT);

        for (StockTransactionDTO tx : transactions) {
            Row row = sheet.createRow(rowNum++);

            // 1. Date
            row.createCell(0).setCellValue(tx.getTransactionDate().format(formatter));
            
            // 2. Reference
            String reference = (tx.getReferenceNumber() != null && !tx.getReferenceNumber().isEmpty()) 
                    ? tx.getReferenceNumber() 
                    : "TX-" + tx.getId();
            row.createCell(1).setCellValue(reference);
            
            // 3. Item Name
            row.createCell(2).setCellValue(tx.getItemName());
            
            // 4. Unit
            row.createCell(3).setCellValue(Optional.ofNullable(tx.getUnit()).orElse(DEFAULT_UNIT));
            
            // 5. Stock IN & 6. Stock OUT
            if (tx.getTransactionType() == TransactionType.IN) {
                row.createCell(4).setCellValue(tx.getQuantity()); // IN
                row.createCell(5).setCellValue("-"); // OUT
            } else {
                row.createCell(4).setCellValue("-"); // IN
                row.createCell(5).setCellValue(tx.getQuantity()); // OUT
            }
            
            // 7. Balance
            row.createCell(6).setCellValue(tx.getBalanceAfter());
            
            // 8. Source / Destination
            String sourceOrDest = PLACEHOLDER;
            if (tx.getTransactionType() == TransactionType.IN) {
                sourceOrDest = Optional.ofNullable(tx.getSupplierName()).orElse("Internal Adjustment");
            } else {
                // For OUT transactions, try to get recipient from notes, otherwise default
                sourceOrDest = (tx.getNotes() != null && !tx.getNotes().isEmpty()) ? tx.getNotes() : "Issued Out";
            }
            row.createCell(7).setCellValue(sourceOrDest);

            // 9. Remarks
            row.createCell(8).setCellValue(Optional.ofNullable(tx.getNotes()).orElse(PLACEHOLDER));
        }
    }

    // ============ BALANCE PDF REPORTS ============

    private byte[] createBalancePdfReport(List<StockBalanceDTO> balances) throws DocumentException, IOException {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Document document = new Document(PageSize.A4);
            PdfWriter writer = PdfWriter.getInstance(document, out);
            writer.setPageEvent(new PageNumberFooter());
            document.open();

            addHeaderImage(document);
            addTitle(document, "Current Stock Balance Report");
            addTimestamp(document);

            if (balances.isEmpty()) {
                addNoDataMessage(document);
            } else {
                PdfPTable table = createBalancePdfTable(balances);
                document.add(table);
            }

            document.close();
            return out.toByteArray();
        }
    }

    private PdfPTable createBalancePdfTable(List<StockBalanceDTO> balances) throws DocumentException {
        PdfPTable table = new PdfPTable(5);
        table.setWidthPercentage(100);
        table.setWidths(new float[]{4, 3, 3, 3, 3});

        String[] headers = {"Item Name", "Unit", "Current Stock", "Minimum Stock", "Status"};
        for (String header : headers) {
            PdfPCell cell = new PdfPCell(
                    new Phrase(header, FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, Color.WHITE))
            );
            cell.setBackgroundColor(Color.DARK_GRAY);
            cell.setHorizontalAlignment(Element.ALIGN_CENTER);
            cell.setPadding(5);
            table.addCell(cell);
        }

        for (StockBalanceDTO balance : balances) {
            addCell(table, balance.getItemName());
            addCell(table, Optional.ofNullable(balance.getUnit()).orElse(DEFAULT_UNIT));
            addCell(table, String.valueOf(balance.getCurrentBalance()));
            addCell(table, String.valueOf(balance.getMinimumStock()));

            String status = balance.getCurrentBalance() <= balance.getMinimumStock() ? "LOW" : "OK";
            PdfPCell statusCell = new PdfPCell(new Phrase(status, FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9)));
            statusCell.setPadding(4);
            statusCell.setHorizontalAlignment(Element.ALIGN_CENTER);
            statusCell.setBackgroundColor(status.equals("LOW") ? Color.ORANGE : Color.GREEN);
            table.addCell(statusCell);
        }

        return table;
    }

    // ============ BALANCE EXCEL REPORTS ============

    private byte[] createBalanceExcelReport(List<StockBalanceDTO> balances) throws IOException {
        try (XSSFWorkbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            Sheet sheet = workbook.createSheet("Stock Balance");

            addExcelHeaderImage(workbook, sheet);
            addExcelTitle(workbook, sheet, "Current Stock Balance Report", 4);

            createBalanceExcelHeader(workbook, sheet, 5);

            if (balances.isEmpty()) {
                addExcelNoDataMessage(workbook, sheet, 6);
            } else {
                populateBalanceExcelData(workbook, sheet, balances, 6);
            }

            autoSizeColumns(sheet, 7);

            workbook.write(out);
            return out.toByteArray();
        }
    }

    private void createBalanceExcelHeader(Workbook workbook, Sheet sheet, int rowNum) {
        Row headerRow = sheet.createRow(rowNum);
        String[] headers = {"SKU / ID", "Item Name", "Category", "Unit", "Current Stock", "Minimum Stock", "Status", "Last Updated"};

        CellStyle headerStyle = createHeaderCellStyle(workbook);

        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
        }
    }

    private void populateBalanceExcelData(Workbook workbook, Sheet sheet, List<StockBalanceDTO> balances, int startRow) {
        int rowNum = startRow;
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(DATE_FORMAT);

        CellStyle lowStockStyle = workbook.createCellStyle();
        lowStockStyle.setFillForegroundColor(IndexedColors.LIGHT_ORANGE.getIndex());
        lowStockStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

        CellStyle okStyle = workbook.createCellStyle();
        okStyle.setFillForegroundColor(IndexedColors.LIGHT_GREEN.getIndex());
        okStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

        for (StockBalanceDTO balance : balances) {
            Row row = sheet.createRow(rowNum++);

            row.createCell(0).setCellValue("ITEM-" + balance.getItemId());
            row.createCell(1).setCellValue(balance.getItemName());
            row.createCell(2).setCellValue(Optional.ofNullable(balance.getCategory()).orElse("-"));
            row.createCell(3).setCellValue(Optional.ofNullable(balance.getUnit()).orElse(DEFAULT_UNIT));
            row.createCell(4).setCellValue(balance.getCurrentBalance());
            row.createCell(5).setCellValue(balance.getMinimumStock());

            String status = balance.getCurrentBalance() <= balance.getMinimumStock() ? "LOW" : "OK";
            Cell statusCell = row.createCell(6);
            statusCell.setCellValue(status);
            statusCell.setCellStyle(status.equals("LOW") ? lowStockStyle : okStyle);
            
            row.createCell(7).setCellValue(
                balance.getLastUpdated() != null ? balance.getLastUpdated().format(formatter) : "-"
            );
        }
    }

    // ============ LOW STOCK REPORTS ============

    private byte[] createLowStockPdfReport(List<StockBalanceDTO> lowStockItems) throws DocumentException, IOException {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Document document = new Document(PageSize.A4);
            PdfWriter writer = PdfWriter.getInstance(document, out);
            writer.setPageEvent(new PageNumberFooter());
            document.open();

            addHeaderImage(document);
            addTitle(document, "Low Stock Report");
            addTimestamp(document);

            if (lowStockItems.isEmpty()) {
                addNoDataMessage(document);
            } else {
                PdfPTable table = createBalancePdfTable(lowStockItems); // Reuse balance table
                document.add(table);
            }

            document.close();
            return out.toByteArray();
        }
    }

    private byte[] createLowStockExcelReport(List<StockBalanceDTO> lowStockItems) throws IOException {
        return createBalanceExcelReport(lowStockItems); // Reuse balance report structure
    }

    // ============ SUPPLIER REPORTS ============

    private byte[] createSupplierPdfReport(List<SupplierDTO> suppliers) throws DocumentException, IOException {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Document document = new Document(PageSize.A4);
            PdfWriter writer = PdfWriter.getInstance(document, out);
            writer.setPageEvent(new PageNumberFooter());
            document.open();

            addHeaderImage(document);
            addTitle(document, "Supplier List Report");
            addTimestamp(document);

            if (suppliers.isEmpty()) {
                addNoDataMessage(document);
            } else {
                PdfPTable table = createSupplierPdfTable(suppliers);
                document.add(table);
            }

            document.close();
            return out.toByteArray();
        }
    }

    private PdfPTable createSupplierPdfTable(List<SupplierDTO> suppliers) throws DocumentException {
        PdfPTable table = new PdfPTable(4);
        table.setWidthPercentage(100);
        table.setWidths(new float[]{3, 3, 3, 4});

        String[] headers = {"Company Name", "Contact Person", "Phone", "Email"};
        for (String header : headers) {
            PdfPCell cell = new PdfPCell(
                    new Phrase(header, FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, Color.WHITE))
            );
            cell.setBackgroundColor(Color.DARK_GRAY);
            cell.setHorizontalAlignment(Element.ALIGN_CENTER);
            cell.setPadding(5);
            table.addCell(cell);
        }

        for (SupplierDTO supplier : suppliers) {
            addCell(table, supplier.getName());
            addCell(table, supplier.getContactPerson());
            addCell(table, supplier.getPhone());
            addCell(table, supplier.getEmail());
        }
        return table;
    }

    private byte[] createSupplierExcelReport(List<SupplierDTO> suppliers) throws IOException {
        try (XSSFWorkbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            Sheet sheet = workbook.createSheet("Suppliers");

            addExcelHeaderImage(workbook, sheet);
            addExcelTitle(workbook, sheet, "Active Suppliers Report", 4); // Writes to 4 and 5

            Row headerRow = sheet.createRow(6); // Moved to 6
            String[] headers = {"Company Name", "Contact Person", "Phone", "Email", "Items Supplied"};
            CellStyle headerStyle = createHeaderCellStyle(workbook);

            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            int rowNum = 7; // Moved to 7
            for (SupplierDTO supplier : suppliers) {
                Row row = sheet.createRow(rowNum++);
                row.createCell(0).setCellValue(supplier.getName());
                row.createCell(1).setCellValue(supplier.getContactPerson());
                row.createCell(2).setCellValue(supplier.getPhone());
                row.createCell(3).setCellValue(supplier.getEmail());
                row.createCell(4).setCellValue(supplier.getItemsSupplied());
            }

            autoSizeColumns(sheet, 5);

            workbook.write(out);
            return out.toByteArray();
        }
    }

    // ============ COMMON PDF HELPERS ============

    private void addHeaderImage(Document document) {
        try {
            ClassPathResource imgFile = new ClassPathResource(HEADER_IMAGE_PATH);
            if (imgFile.exists()) {
                Image image = Image.getInstance(imgFile.getURL());
                image.scaleToFit(500, 100);
                image.setAlignment(Element.ALIGN_CENTER);
                document.add(image);
            }
            
            // Add Organization Name
            Paragraph orgName = new Paragraph(
                    ORGANIZATION_NAME,
                    FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14, Color.BLACK)
            );
            orgName.setAlignment(Element.ALIGN_CENTER);
            orgName.setSpacingAfter(5);
            document.add(orgName);
            
        } catch (Exception e) {
            log.warn("Failed to add header image to PDF", e);
        }
    }

    private void addTitle(Document document, String title) throws DocumentException {
        Paragraph titlePara = new Paragraph(
                title,
                FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18)
        );
        titlePara.setAlignment(Element.ALIGN_CENTER);
        titlePara.setSpacingAfter(10);
        document.add(titlePara);
    }

    private void addTimestamp(Document document) throws DocumentException {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern(DATETIME_FORMAT));
        Paragraph timestampPara = new Paragraph(
                "Generated on: " + timestamp,
                FontFactory.getFont(FontFactory.HELVETICA, 10, Color.GRAY)
        );
        timestampPara.setAlignment(Element.ALIGN_CENTER);
        timestampPara.setSpacingAfter(20);
        document.add(timestampPara);
    }

    private void addDateRangeInfo(Document document, LocalDate startDate, LocalDate endDate) throws DocumentException {
        if (startDate != null && endDate != null) {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern(DATE_FORMAT);
            Paragraph dateRange = new Paragraph(
                    String.format("Period: %s to %s", startDate.format(formatter), endDate.format(formatter)),
                    FontFactory.getFont(FontFactory.HELVETICA, 10, Color.DARK_GRAY)
            );
            dateRange.setAlignment(Element.ALIGN_CENTER);
            dateRange.setSpacingAfter(10);
            document.add(dateRange);
        }
    }

    private void addNoDataMessage(Document document) throws DocumentException {
        Paragraph message = new Paragraph(
                "No data available for the selected criteria.",
                FontFactory.getFont(FontFactory.HELVETICA, 14)
        );
        message.setAlignment(Element.ALIGN_CENTER);
        message.setSpacingBefore(50);
        document.add(message);
    }

    private void addSummaryStats(Document document, List<StockTransactionDTO> transactions) throws DocumentException {
        long inCount = transactions.stream().filter(t -> t.getTransactionType() == TransactionType.IN).count();
        long outCount = transactions.stream().filter(t -> t.getTransactionType() == TransactionType.OUT).count();

        Paragraph summary = new Paragraph(
                String.format("\nSummary: Total Transactions: %d | Stock IN: %d | Stock OUT: %d",
                        transactions.size(), inCount, outCount),
                FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10)
        );
        summary.setSpacingBefore(15);
        document.add(summary);
    }

    private void addCell(PdfPTable table, String text) {
        PdfPCell cell = new PdfPCell(new Phrase(text, FontFactory.getFont(FontFactory.HELVETICA, 9)));
        cell.setPadding(4);
        table.addCell(cell);
    }

    // ============ COMMON EXCEL HELPERS ============

    private void addExcelHeaderImage(Workbook workbook, Sheet sheet) {
        try {
            ClassPathResource imgFile = new ClassPathResource(HEADER_IMAGE_PATH);
            if (imgFile.exists()) {
                try (InputStream is = imgFile.getInputStream()) {
                    byte[] bytes = IOUtils.toByteArray(is);
                    int pictureIdx = workbook.addPicture(bytes, Workbook.PICTURE_TYPE_PNG);

                    CreationHelper helper = workbook.getCreationHelper();
                    Drawing<?> drawing = sheet.createDrawingPatriarch();
                    ClientAnchor anchor = helper.createClientAnchor();
                    anchor.setCol1(0);
                    anchor.setRow1(0);
                    anchor.setCol2(10);
                    anchor.setRow2(4);

                    Picture pict = drawing.createPicture(anchor, pictureIdx);
                    pict.resize(1.0, 1.0);
                }
            }
        } catch (Exception e) {
            log.warn("Failed to add header image to Excel", e);
        }
    }

    private void addExcelTitle(Workbook workbook, Sheet sheet, String title, int startRow) {
        // Org Name
        Row orgRow = sheet.createRow(startRow);
        Cell orgCell = orgRow.createCell(0);
        orgCell.setCellValue(ORGANIZATION_NAME);
        CellStyle orgStyle = workbook.createCellStyle();
        Font orgFont = workbook.createFont();
        orgFont.setBold(true);
        orgFont.setFontHeightInPoints((short) 14);
        orgStyle.setFont(orgFont);
        orgCell.setCellStyle(orgStyle);

        // Report Title
        Row titleRow = sheet.createRow(startRow + 1);
        Cell titleCell = titleRow.createCell(0);
        titleCell.setCellValue(title + " - Generated: " +
                LocalDateTime.now().format(DateTimeFormatter.ofPattern(DATETIME_FORMAT)));

        CellStyle titleStyle = workbook.createCellStyle();
        Font titleFont = workbook.createFont();
        titleFont.setBold(true);
        titleFont.setFontHeightInPoints((short) 12);
        titleStyle.setFont(titleFont);
        titleCell.setCellStyle(titleStyle);
    }

    private void addExcelDateRange(Workbook workbook, Sheet sheet, LocalDate startDate, LocalDate endDate, int rowNum) {
        Row dateRow = sheet.createRow(rowNum);
        Cell dateCell = dateRow.createCell(0);
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(DATE_FORMAT);
        dateCell.setCellValue(String.format("Period: %s to %s",
                startDate.format(formatter), endDate.format(formatter)));

        CellStyle dateStyle = workbook.createCellStyle();
        Font dateFont = workbook.createFont();
        dateFont.setItalic(true);
        dateStyle.setFont(dateFont);
        dateCell.setCellStyle(dateStyle);
    }

    private void addExcelNoDataMessage(Workbook workbook, Sheet sheet, int rowNum) {
        Row row = sheet.createRow(rowNum);
        Cell cell = row.createCell(0);
        cell.setCellValue("No data available for the selected criteria.");

        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setItalic(true);
        font.setColor(IndexedColors.GREY_50_PERCENT.getIndex());
        style.setFont(font);
        cell.setCellStyle(style);
    }

    private CellStyle createHeaderCellStyle(Workbook workbook) {
        CellStyle headerStyle = workbook.createCellStyle();
        Font headerFont = workbook.createFont();
        headerFont.setBold(true);
        headerFont.setColor(IndexedColors.WHITE.getIndex());
        headerStyle.setFont(headerFont);
        headerStyle.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());
        headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        headerStyle.setAlignment(HorizontalAlignment.CENTER);
        headerStyle.setBorderBottom(BorderStyle.THIN);
        headerStyle.setBorderTop(BorderStyle.THIN);
        headerStyle.setBorderLeft(BorderStyle.THIN);
        headerStyle.setBorderRight(BorderStyle.THIN);
        return headerStyle;
    }

    private void autoSizeColumns(Sheet sheet, int columnCount) {
        for (int i = 0; i < columnCount; i++) {
            sheet.autoSizeColumn(i);
            int currentWidth = sheet.getColumnWidth(i);
            sheet.setColumnWidth(i, currentWidth + 1000);
        }
    }

    public static class ReportGenerationException extends RuntimeException {
        public ReportGenerationException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    class PageNumberFooter extends com.lowagie.text.pdf.PdfPageEventHelper {
        @Override
        public void onEndPage(com.lowagie.text.pdf.PdfWriter writer, Document document) {
            com.lowagie.text.pdf.PdfContentByte cb = writer.getDirectContent();
            String pageText = "Page " + writer.getPageNumber();
            
            try {
                com.lowagie.text.pdf.BaseFont bf = com.lowagie.text.pdf.BaseFont.createFont(
                    com.lowagie.text.pdf.BaseFont.HELVETICA, 
                    com.lowagie.text.pdf.BaseFont.CP1252, 
                    com.lowagie.text.pdf.BaseFont.NOT_EMBEDDED
                );
                
                float width = bf.getWidthPoint(pageText, 10);
                float x = (document.right() + document.left()) / 2 - (width / 2);
                float y = document.bottom() - 20;
                
                cb.beginText();
                cb.setFontAndSize(bf, 10);
                cb.setTextMatrix(x, y);
                cb.showText(pageText);
                cb.endText();
                
            } catch (Exception e) {
                // Ignore font errors
            }
        }
    }
}