package npk.rca.ims.service;

import npk.rca.ims.dto.SupplierDTO;
import npk.rca.ims.exceptions.ResourceNotFoundException;
import npk.rca.ims.model.Supplier;
import npk.rca.ims.repository.SupplierRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SupplierServiceTest {

    @Mock
    private SupplierRepository supplierRepository;

    @InjectMocks
    private SupplierService supplierService;

    private Supplier testSupplier;
    private SupplierDTO testSupplierDTO;

    @BeforeEach
    void setUp() {
        testSupplier = new Supplier();
        testSupplier.setId(1L);
        testSupplier.setName("Test Supplier");
        testSupplier.setContactPerson("John Doe");
        testSupplier.setPhone("1234567890");
        testSupplier.setEmail("supplier@example.com");
        testSupplier.setItemsSupplied("Rice, Beans");
        testSupplier.setActive(true);

        testSupplierDTO = new SupplierDTO(
            1L,
            "Test Supplier",
            "John Doe",
            "1234567890",
            "supplier@example.com",
            "Rice, Beans",
            true
        );
    }

    @Test
    void getAllActiveSuppliers_ShouldReturnListOfActiveSuppliers() {
        when(supplierRepository.findByActiveTrue()).thenReturn(Arrays.asList(testSupplier));

        List<SupplierDTO> result = supplierService.getAllActiveSuppliers();

        assertFalse(result.isEmpty());
        assertEquals(1, result.size());
        assertEquals("Test Supplier", result.get(0).getName());
    }

    @Test
    void createSupplier_ShouldReturnCreatedSupplier_WhenDataIsUnique() {
        when(supplierRepository.findByNameIgnoreCase("Test Supplier")).thenReturn(Optional.empty());
        when(supplierRepository.findByEmailIgnoreCase("supplier@example.com")).thenReturn(Optional.empty());
        when(supplierRepository.save(any(Supplier.class))).thenReturn(testSupplier);

        SupplierDTO result = supplierService.createSupplier(testSupplierDTO);

        assertNotNull(result);
        assertEquals("Test Supplier", result.getName());
        assertTrue(result.isActive());
    }

    @Test
    void createSupplier_ShouldThrowException_WhenNameExists() {
        when(supplierRepository.findByNameIgnoreCase("Test Supplier")).thenReturn(Optional.of(testSupplier));

        assertThrows(IllegalArgumentException.class, () -> 
            supplierService.createSupplier(testSupplierDTO));
    }

    @Test
    void createSupplier_ShouldThrowException_WhenEmailExists() {
        when(supplierRepository.findByNameIgnoreCase("Test Supplier")).thenReturn(Optional.empty());
        when(supplierRepository.findByEmailIgnoreCase("supplier@example.com")).thenReturn(Optional.of(testSupplier));

        assertThrows(IllegalArgumentException.class, () -> 
            supplierService.createSupplier(testSupplierDTO));
    }

    @Test
    void updateSupplier_ShouldReturnUpdatedSupplier_WhenDataIsUnique() {
        when(supplierRepository.findById(1L)).thenReturn(Optional.of(testSupplier));
        when(supplierRepository.findByNameIgnoreCaseAndIdIsNot("Test Supplier", 1L)).thenReturn(Optional.empty());
        when(supplierRepository.findByEmailIgnoreCaseAndIdIsNot("supplier@example.com", 1L)).thenReturn(Optional.empty());
        when(supplierRepository.save(any(Supplier.class))).thenReturn(testSupplier);

        SupplierDTO result = supplierService.updateSupplier(1L, testSupplierDTO);

        assertNotNull(result);
        assertEquals("Test Supplier", result.getName());
    }

    @Test
    void updateSupplier_ShouldThrowException_WhenSupplierNotFound() {
        when(supplierRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> 
            supplierService.updateSupplier(1L, testSupplierDTO));
    }

    @Test
    void updateSupplier_ShouldThrowException_WhenNameExistsForAnotherSupplier() {
        when(supplierRepository.findById(1L)).thenReturn(Optional.of(testSupplier));
        when(supplierRepository.findByNameIgnoreCaseAndIdIsNot("Test Supplier", 1L)).thenReturn(Optional.of(new Supplier()));

        assertThrows(IllegalArgumentException.class, () -> 
            supplierService.updateSupplier(1L, testSupplierDTO));
    }

    @Test
    void deactivateSupplier_ShouldSetActiveToFalse() {
        when(supplierRepository.findById(1L)).thenReturn(Optional.of(testSupplier));
        when(supplierRepository.save(any(Supplier.class))).thenAnswer(invocation -> invocation.getArgument(0));

        supplierService.deactivateSupplier(1L);

        assertFalse(testSupplier.isActive());
    }
}
