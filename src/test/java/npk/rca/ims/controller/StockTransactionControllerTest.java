package npk.rca.ims.controller;

import npk.rca.ims.dto.StockTransactionDTO;
import npk.rca.ims.model.TransactionType;
import npk.rca.ims.service.StockTransactionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StockTransactionControllerTest {

    @Mock
    private StockTransactionService transactionService;

    @InjectMocks
    private StockTransactionController transactionController;

    private StockTransactionDTO testTransactionDTO;

    @BeforeEach
    void setUp() {
        testTransactionDTO = new StockTransactionDTO();
        testTransactionDTO.setItemId(1L);
        testTransactionDTO.setTransactionType(TransactionType.IN);
        testTransactionDTO.setQuantity(100);
        testTransactionDTO.setTransactionDate(LocalDate.now());
    }

    @Test
    void getAllTransactions_ShouldReturnAllTransactions_WhenNoItemIdProvided() {
        when(transactionService.getAllTransactions()).thenReturn(Arrays.asList(testTransactionDTO));

        ResponseEntity<List<StockTransactionDTO>> response = transactionController.getAllTransactions(null);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(1, response.getBody().size());
    }

    @Test
    void getAllTransactions_ShouldReturnFilteredTransactions_WhenItemIdProvided() {
        when(transactionService.getTransactionsByItemId(1L)).thenReturn(Arrays.asList(testTransactionDTO));

        ResponseEntity<List<StockTransactionDTO>> response = transactionController.getAllTransactions(1L);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(1, response.getBody().size());
    }

    @Test
    void getTransactionsByDateRange_ShouldReturnTransactions() {
        LocalDate startDate = LocalDate.now().minusDays(1);
        LocalDate endDate = LocalDate.now();

        when(transactionService.getTransactionsByDateRange(startDate, endDate))
                .thenReturn(Arrays.asList(testTransactionDTO));

        ResponseEntity<List<StockTransactionDTO>> response = 
                transactionController.getTransactionsByDateRange(startDate, endDate);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(1, response.getBody().size());
    }

    @Test
    void recordTransaction_ShouldReturnCreatedTransaction() {
        when(transactionService.recordTransaction(any(StockTransactionDTO.class))).thenReturn(testTransactionDTO);

        ResponseEntity<StockTransactionDTO> response = transactionController.recordTransaction(testTransactionDTO);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertEquals(100, response.getBody().getQuantity());
    }
}
