package npk.rca.ims.service;

import npk.rca.ims.dto.StockBalanceDTO;
import npk.rca.ims.dto.StockTransactionDTO;
import npk.rca.ims.dto.SupplierDTO;
import npk.rca.ims.model.TransactionType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReportServiceTest {

    @Mock
    private StockTransactionService transactionService;

    @Mock
    private StockBalanceService balanceService;

    @Mock
    private SupplierService supplierService;

    @InjectMocks
    private ReportService reportService;

    private StockTransactionDTO testTransaction;
    private StockBalanceDTO testBalance;
    private SupplierDTO testSupplier;

    @BeforeEach
    void setUp() {
        testTransaction = new StockTransactionDTO();
        testTransaction.setId(1L);
        testTransaction.setItemId(1L);
        testTransaction.setItemName("Test Item");
        testTransaction.setTransactionType(TransactionType.IN);
        testTransaction.setQuantity(100);
        testTransaction.setTransactionDate(LocalDate.now());
        testTransaction.setBalanceAfter(100);
        testTransaction.setReferenceNumber("REF-001");
        testTransaction.setNotes("Test Note");
        testTransaction.setRecordedBy("Admin");

        testBalance = new StockBalanceDTO();
        testBalance.setItemId(1L);
        testBalance.setItemName("Test Item");
        testBalance.setCurrentBalance(100);
        testBalance.setMinimumStock(10);
        testBalance.setIsLowStock(false);

        testSupplier = new SupplierDTO(
            1L, "Test Supplier", "Contact", "123456", "test@example.com", "Items", true
        );
    }

    @Test
    void generateTransactionReportPdf_ShouldReturnPdfBytes() {
        when(transactionService.getAllTransactions()).thenReturn(Arrays.asList(testTransaction));

        byte[] result = reportService.generateTransactionReportPdf(null, null, null);

        assertNotNull(result);
        assertTrue(result.length > 0);
    }

    @Test
    void generateTransactionReportExcel_ShouldReturnExcelBytes() {
        when(transactionService.getAllTransactions()).thenReturn(Arrays.asList(testTransaction));

        byte[] result = reportService.generateTransactionReportExcel(null, null, null);

        assertNotNull(result);
        assertTrue(result.length > 0);
    }

    @Test
    void generateStockInReportPdf_ShouldReturnPdfBytes() {
        when(transactionService.getAllTransactions()).thenReturn(Arrays.asList(testTransaction));

        byte[] result = reportService.generateStockInReportPdf(null, null, null);

        assertNotNull(result);
        assertTrue(result.length > 0);
    }

    @Test
    void generateStockOutReportPdf_ShouldReturnPdfBytes() {
        testTransaction.setTransactionType(TransactionType.OUT);
        when(transactionService.getAllTransactions()).thenReturn(Arrays.asList(testTransaction));

        byte[] result = reportService.generateStockOutReportPdf(null, null);

        assertNotNull(result);
        assertTrue(result.length > 0);
    }

    @Test
    void generateBalanceReportPdf_ShouldReturnPdfBytes() {
        when(balanceService.getAllBalances()).thenReturn(Arrays.asList(testBalance));

        byte[] result = reportService.generateBalanceReportPdf();

        assertNotNull(result);
        assertTrue(result.length > 0);
    }

    @Test
    void generateBalanceReportExcel_ShouldReturnExcelBytes() {
        when(balanceService.getAllBalances()).thenReturn(Arrays.asList(testBalance));

        byte[] result = reportService.generateBalanceReportExcel();

        assertNotNull(result);
        assertTrue(result.length > 0);
    }

    @Test
    void generateLowStockReportPdf_ShouldReturnPdfBytes() {
        testBalance.setIsLowStock(true);
        when(balanceService.getLowStockItems()).thenReturn(Arrays.asList(testBalance));

        byte[] result = reportService.generateLowStockReportPdf();

        assertNotNull(result);
        assertTrue(result.length > 0);
    }

    @Test
    void generateSupplierReportPdf_ShouldReturnPdfBytes() {
        when(supplierService.getAllActiveSuppliers()).thenReturn(Arrays.asList(testSupplier));

        byte[] result = reportService.generateSupplierReportPdf();

        assertNotNull(result);
        assertTrue(result.length > 0);
    }

    @Test
    void generateSupplierReportExcel_ShouldReturnExcelBytes() {
        when(supplierService.getAllActiveSuppliers()).thenReturn(Arrays.asList(testSupplier));

        byte[] result = reportService.generateSupplierReportExcel();

        assertNotNull(result);
        assertTrue(result.length > 0);
    }
    
    @Test
    void generateTransactionReportPdf_ShouldHandleEmptyData() {
        when(transactionService.getAllTransactions()).thenReturn(Collections.emptyList());

        byte[] result = reportService.generateTransactionReportPdf(null, null, null);

        assertNotNull(result);
        assertTrue(result.length > 0);
    }
}
