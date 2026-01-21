package npk.rca.ims.controller;

import npk.rca.ims.dto.SupplierDTO;
import npk.rca.ims.service.SupplierService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import java.security.Principal;
import npk.rca.ims.dto.DeleteRequestDTO;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SupplierControllerTest {

    @Mock
    private SupplierService supplierService;

    @InjectMocks
    private SupplierController supplierController;

    private SupplierDTO testSupplierDTO;

    @BeforeEach
    void setUp() {
        testSupplierDTO = new SupplierDTO(
            1L, "Test Supplier", "Contact", "123456", "test@example.com", "Items", true
        );
    }

    @Test
    void getAllActiveSuppliers_ShouldReturnList() {
        when(supplierService.getAllActiveSuppliers()).thenReturn(Arrays.asList(testSupplierDTO));

        ResponseEntity<List<SupplierDTO>> response = supplierController.getAllActiveSuppliers();

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(1, response.getBody().size());
    }

    @Test
    void createSupplier_ShouldReturnCreatedSupplier() {
        when(supplierService.createSupplier(any(SupplierDTO.class))).thenReturn(testSupplierDTO);

        ResponseEntity<SupplierDTO> response = supplierController.createSupplier(testSupplierDTO);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertEquals("Test Supplier", response.getBody().getName());
    }

    @Test
    void updateSupplier_ShouldReturnUpdatedSupplier() {
        when(supplierService.updateSupplier(eq(1L), any(SupplierDTO.class))).thenReturn(testSupplierDTO);

        ResponseEntity<SupplierDTO> response = supplierController.updateSupplier(1L, testSupplierDTO);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("Test Supplier", response.getBody().getName());
    }

    @Test
    void deactivateSupplier_ShouldReturnNoContent() {
        DeleteRequestDTO request = new DeleteRequestDTO();
        request.setPassword("password");

        Principal principal = () -> "test@example.com";

        doNothing().when(supplierService)
                .deactivateSupplier(1L, "test@example.com", "password");

        ResponseEntity<Void> response =
                supplierController.deactivateSupplier(1L, request, principal);

        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
    }

    @Test
    void reactivateSupplier_ShouldReturnNoContent() {
        doNothing().when(supplierService).reactivateSupplier(1L);

        ResponseEntity<Void> response = supplierController.reactivateSupplier(1L);

        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
    }

    @Test
    void deleteSupplier_ShouldReturnNoContent() {
        DeleteRequestDTO request = new DeleteRequestDTO();
        request.setPassword("password");

        Principal principal = () -> "test@example.com";

        doNothing().when(supplierService)
                .deleteSupplier(1L, "test@example.com", "password");

        ResponseEntity<Void> response =
                supplierController.deleteSupplier(1L, request, principal);

        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
    }
}
