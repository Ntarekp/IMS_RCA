package npk.rca.ims.service;

import com.lowagie.text.*;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import npk.rca.ims.dto.StockTransactionDTO;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.util.IOUtils;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReportService {

    private static final String HEADER_IMAGE_PATH = "static/rca-info.png";
    private static final String DATE_FORMAT = "dd-MM-yyyy";
    private static final String DEFAULT_UNIT = "Kg";
    private static final String PLACEHOLDER = "-";

    private final StockTransactionService transactionService;

    /**
     * Generates PDF report of all stock transactions
     * @return byte array of PDF document
     * @throws ReportGenerationException if PDF generation fails
     */
    public byte[] generateTransactionReportPdf() {
        try {
            List<StockTransactionDTO> transactions = transactionService.getAllTransactions();

            if (transactions == null || transactions.isEmpty()) {
                log.warn("No transactions found for PDF report generation");
                return generateEmptyReportPdf();
            }

            return createPdfReport(transactions);

        } catch (Exception e) {
            log.error("Error generating PDF report", e);
            throw new ReportGenerationException("Failed to generate PDF report", e);
        }
    }

    /**
     * Generates Excel report of all stock transactions
     * @return byte array of Excel workbook
     * @throws ReportGenerationException if Excel generation fails
     */
    public byte[] generateTransactionReportExcel() {
        try {
            List<StockTransactionDTO> transactions = transactionService.getAllTransactions();

            if (transactions == null || transactions.isEmpty()) {
                log.warn("No transactions found for Excel report generation");
                return generateEmptyReportExcel();
            }

            return createExcelReport(transactions);

        } catch (Exception e) {
            log.error("Error generating Excel report", e);
            throw new ReportGenerationException("Failed to generate Excel report", e);
        }
    }

    private byte[] createPdfReport(List<StockTransactionDTO> transactions) throws DocumentException, IOException {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Document document = new Document(PageSize.A4.rotate());
            PdfWriter.getInstance(document, out);
            document.open();

            // Add header image if available
            addHeaderImage(document);

            // Add title
            addTitle(document, "Stock Transaction History");

            // Add generation timestamp
            addTimestamp(document);

            // Create and populate table
            PdfPTable table = createPdfTable(transactions);
            document.add(table);

            document.close();
            return out.toByteArray();
        }
    }

    private byte[] createExcelReport(List<StockTransactionDTO> transactions) throws IOException {
        try (XSSFWorkbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            Sheet sheet = workbook.createSheet("Transactions");

            // Add header image
            addExcelHeaderImage(workbook, sheet);

            // Add title and timestamp
            addExcelTitle(workbook, sheet);

            // Create header row
            createExcelHeader(workbook, sheet);

            // Add data rows
            populateExcelData(sheet, transactions);

            // Auto-size columns for better readability
            autoSizeColumns(sheet, 10);

            workbook.write(out);
            return out.toByteArray();
        }
    }

    private void addHeaderImage(Document document) {
        try {
            ClassPathResource imgFile = new ClassPathResource(HEADER_IMAGE_PATH);
            if (imgFile.exists()) {
                Image image = Image.getInstance(imgFile.getURL());
                image.scaleToFit(500, 100);
                image.setAlignment(Element.ALIGN_CENTER);
                document.add(image);
            } else {
                log.debug("Header image not found at: {}", HEADER_IMAGE_PATH);
            }
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
        String timestamp = LocalDateTime.now()
                .format(DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss"));
        Paragraph timestampPara = new Paragraph(
                "Generated on: " + timestamp,
                FontFactory.getFont(FontFactory.HELVETICA, 10, Color.GRAY)
        );
        timestampPara.setAlignment(Element.ALIGN_CENTER);
        timestampPara.setSpacingAfter(20);
        document.add(timestampPara);
    }

    private PdfPTable createPdfTable(List<StockTransactionDTO> transactions) throws DocumentException {
        PdfPTable table = new PdfPTable(10);
        table.setWidthPercentage(100);
        table.setWidths(new float[]{3, 3, 3, 2, 2, 2, 2, 3, 4, 3});

        // Add header row
        addPdfTableHeader(table);

        // Add data rows
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(DATE_FORMAT);
        for (StockTransactionDTO tx : transactions) {
            addPdfDataRow(table, tx, formatter);
        }

        return table;
    }

    private void addPdfTableHeader(PdfPTable table) {
        String[] headers = {
                "Date", "Reference", "Item Name", "Unit", "Type",
                "Qty", "Balance", "Source/Issued To", "Purpose/Remarks", "Recorded By"
        };

        for (String header : headers) {
            PdfPCell cell = new PdfPCell(
                    new Phrase(header, FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, Color.WHITE))
            );
            cell.setBackgroundColor(Color.DARK_GRAY);
            cell.setHorizontalAlignment(Element.ALIGN_CENTER);
            cell.setPadding(5);
            table.addCell(cell);
        }
    }

    private void addPdfDataRow(PdfPTable table, StockTransactionDTO tx, DateTimeFormatter formatter) {
        addCell(table, tx.getTransactionDate().format(formatter));
        addCell(table, Optional.ofNullable(tx.getReferenceNumber()).orElse(PLACEHOLDER));
        addCell(table, tx.getItemName());
        addCell(table, Optional.ofNullable(tx.getUnit()).orElse(DEFAULT_UNIT));
        addCell(table, tx.getTransactionType().toString());
        addCell(table, String.valueOf(tx.getQuantity()));
        addCell(table, String.valueOf(tx.getBalanceAfter()));
        addCell(table, Optional.ofNullable(tx.getSupplierName()).orElse(PLACEHOLDER));
        addCell(table, Optional.ofNullable(tx.getNotes()).orElse(PLACEHOLDER));
        addCell(table, Optional.ofNullable(tx.getRecordedBy()).orElse(PLACEHOLDER));
    }

    private void addCell(PdfPTable table, String text) {
        PdfPCell cell = new PdfPCell(
                new Phrase(text, FontFactory.getFont(FontFactory.HELVETICA, 9))
        );
        cell.setPadding(4);
        table.addCell(cell);
    }

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

    private void addExcelTitle(Workbook workbook, Sheet sheet) {
        Row titleRow = sheet.createRow(4);
        Cell titleCell = titleRow.createCell(0);
        titleCell.setCellValue("Stock Transaction History - Generated: " +
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm")));

        CellStyle titleStyle = workbook.createCellStyle();
        Font titleFont = workbook.createFont();
        titleFont.setBold(true);
        titleFont.setFontHeightInPoints((short) 14);
        titleStyle.setFont(titleFont);
        titleCell.setCellStyle(titleStyle);
    }

    private void createExcelHeader(Workbook workbook, Sheet sheet) {
        Row headerRow = sheet.createRow(5);
        String[] headers = {
                "Date", "Reference", "Item Name", "Unit", "Transaction Type",
                "Quantity", "Balance After", "Source / Issued To",
                "Purpose / Remarks", "Recorded By"
        };

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

        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
        }
    }

    private void populateExcelData(Sheet sheet, List<StockTransactionDTO> transactions) {
        int rowNum = 6;
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(DATE_FORMAT);

        for (StockTransactionDTO tx : transactions) {
            Row row = sheet.createRow(rowNum++);

            row.createCell(0).setCellValue(tx.getTransactionDate().format(formatter));
            row.createCell(1).setCellValue(
                    Optional.ofNullable(tx.getReferenceNumber()).orElse(PLACEHOLDER)
            );
            row.createCell(2).setCellValue(tx.getItemName());
            row.createCell(3).setCellValue(
                    Optional.ofNullable(tx.getUnit()).orElse(DEFAULT_UNIT)
            );
            row.createCell(4).setCellValue(tx.getTransactionType().toString());
            row.createCell(5).setCellValue(tx.getQuantity());
            row.createCell(6).setCellValue(tx.getBalanceAfter());
            row.createCell(7).setCellValue(
                    Optional.ofNullable(tx.getSupplierName()).orElse(PLACEHOLDER)
            );
            row.createCell(8).setCellValue(
                    Optional.ofNullable(tx.getNotes()).orElse(PLACEHOLDER)
            );
            row.createCell(9).setCellValue(
                    Optional.ofNullable(tx.getRecordedBy()).orElse(PLACEHOLDER)
            );
        }
    }

    private void autoSizeColumns(Sheet sheet, int columnCount) {
        for (int i = 0; i < columnCount; i++) {
            sheet.autoSizeColumn(i);
            // Add some extra width for better readability
            int currentWidth = sheet.getColumnWidth(i);
            sheet.setColumnWidth(i, currentWidth + 1000);
        }
    }

    private byte[] generateEmptyReportPdf() throws DocumentException, IOException {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Document document = new Document(PageSize.A4);
            PdfWriter.getInstance(document, out);
            document.open();

            Paragraph message = new Paragraph(
                    "No transactions available to generate report.",
                    FontFactory.getFont(FontFactory.HELVETICA, 14)
            );
            message.setAlignment(Element.ALIGN_CENTER);
            document.add(message);

            document.close();
            return out.toByteArray();
        }
    }

    private byte[] generateEmptyReportExcel() throws IOException {
        try (XSSFWorkbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            Sheet sheet = workbook.createSheet("Transactions");
            Row row = sheet.createRow(0);
            Cell cell = row.createCell(0);
            cell.setCellValue("No transactions available to generate report.");

            workbook.write(out);
            return out.toByteArray();
        }
    }

    /**
     * Custom exception for report generation errors
     */
    public static class ReportGenerationException extends RuntimeException {
        public ReportGenerationException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}