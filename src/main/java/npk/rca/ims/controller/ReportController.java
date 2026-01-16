package npk.rca.ims.controller;

import lombok.RequiredArgsConstructor;
import npk.rca.ims.dto.StockBalanceDTO;
import npk.rca.ims.service.ReportService;
import npk.rca.ims.service.StockTransactionService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

/**
 * ReportController - Generate inventory reports
 */
@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
public class ReportController {

    private final StockTransactionService transactionService;
    private final ReportService reportService;

    /**
     * GET /api/reports/balance
     * Get JSON data for balance
     */
    @GetMapping("/balance")
    public ResponseEntity<List<StockBalanceDTO>> getBalanceReport() {
        List<StockBalanceDTO> report = transactionService.generateBalanceReport();
        return ResponseEntity.ok(report);
    }

    /**
     * GET /api/reports/low-stock
     * Get JSON data for low stock
     */
    @GetMapping("/low-stock")
    public ResponseEntity<List<StockBalanceDTO>> getLowStockReport() {
        List<StockBalanceDTO> report = transactionService.getLowStockItems();
        return ResponseEntity.ok(report);
    }

    /**
     * GET /api/reports/export/pdf
     * Export full transaction history as PDF
     */
    @GetMapping("/export/pdf")
    public ResponseEntity<byte[]> exportPdf(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) Long itemId) {
        try {
            byte[] pdfContent = reportService.generateTransactionReportPdf(startDate, endDate, itemId);
            
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=transaction_report.pdf")
                    .contentType(MediaType.APPLICATION_PDF)
                    .body(pdfContent);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * GET /api/reports/export/excel
     * Export full transaction history as Excel
     */
    @GetMapping("/export/excel")
    public ResponseEntity<byte[]> exportExcel(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) Long itemId) {
        try {
            byte[] excelContent = reportService.generateTransactionReportExcel(startDate, endDate, itemId);
            
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=transaction_report.xlsx")
                    .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                    .body(excelContent);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * GET /api/reports/export/balance/pdf
     * Export balance report as PDF
     */
    @GetMapping("/export/balance/pdf")
    public ResponseEntity<byte[]> exportBalancePdf() {
        try {
            byte[] pdfContent = reportService.generateBalanceReportPdf();
            
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=stock_balance_report.pdf")
                    .contentType(MediaType.APPLICATION_PDF)
                    .body(pdfContent);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * GET /api/reports/export/balance/excel
     * Export balance report as Excel
     */
    @GetMapping("/export/balance/excel")
    public ResponseEntity<byte[]> exportBalanceExcel() {
        try {
            byte[] excelContent = reportService.generateBalanceReportExcel();
            
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=stock_balance_report.xlsx")
                    .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                    .body(excelContent);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * GET /api/reports/export/low-stock/pdf
     * Export low stock report as PDF
     */
    @GetMapping("/export/low-stock/pdf")
    public ResponseEntity<byte[]> exportLowStockPdf() {
        try {
            byte[] pdfContent = reportService.generateLowStockReportPdf();
            
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=low_stock_report.pdf")
                    .contentType(MediaType.APPLICATION_PDF)
                    .body(pdfContent);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * GET /api/reports/export/low-stock/excel
     * Export low stock report as Excel
     */
    @GetMapping("/export/low-stock/excel")
    public ResponseEntity<byte[]> exportLowStockExcel() {
        try {
            byte[] excelContent = reportService.generateLowStockReportExcel();
            
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=low_stock_report.xlsx")
                    .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                    .body(excelContent);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * GET /api/reports/export/suppliers/pdf
     * Export active suppliers as PDF
     */
    @GetMapping("/export/suppliers/pdf")
    public ResponseEntity<byte[]> exportSuppliersPdf() {
        try {
            byte[] pdfContent = reportService.generateSupplierReportPdf();
            
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=suppliers_report.pdf")
                    .contentType(MediaType.APPLICATION_PDF)
                    .body(pdfContent);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * GET /api/reports/export/suppliers/excel
     * Export active suppliers as Excel
     */
    @GetMapping("/export/suppliers/excel")
    public ResponseEntity<byte[]> exportSuppliersExcel() {
        try {
            byte[] excelContent = reportService.generateSupplierReportExcel();
            
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=suppliers_report.xlsx")
                    .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                    .body(excelContent);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
}
