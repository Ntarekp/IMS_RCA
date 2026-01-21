package npk.rca.ims.service;

import npk.rca.ims.dto.SupplierDTO;
import npk.rca.ims.exceptions.ResourceNotFoundException;
import npk.rca.ims.model.Supplier;
import npk.rca.ims.model.User;
import npk.rca.ims.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import npk.rca.ims.repository.SupplierRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
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
    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;


    @InjectMocks
    private SupplierService supplierService;

    private Supplier testSupplier;
    private SupplierDTO testSupplierDTO;
    private User testUser;

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

        testUser = new User();
        testUser.setEmail("admin@example.com");
        testUser.setPassword("encodedPassword");
    }

    @Test
    @DisplayName("Should return list of active suppliers")
    void getAllActiveSuppliers_ShouldReturnListOfActiveSuppliers() {
        when(supplierRepository.findByActiveTrue()).thenReturn(Arrays.asList(testSupplier));

        List<SupplierDTO> result = supplierService.getAllActiveSuppliers();

        assertFalse(result.isEmpty());
        assertEquals(1, result.size());
        assertEquals("Test Supplier", result.get(0).getName());
    }

    @Test
    @DisplayName("Should return list of inactive suppliers")
    void getAllInactiveSuppliers_ShouldReturnListOfInactiveSuppliers() {
        testSupplier.setActive(false);
        when(supplierRepository.findByActiveFalse()).thenReturn(Arrays.asList(testSupplier));

        List<SupplierDTO> result = supplierService.getAllInactiveSuppliers();

        assertFalse(result.isEmpty());
        assertEquals(1, result.size());
        assertFalse(result.get(0).isActive());
    }

    @Test
    @DisplayName("Should create supplier when data is unique")
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
    @DisplayName("Should throw exception when creating supplier with existing name")
    void createSupplier_ShouldThrowException_WhenNameExists() {
        when(supplierRepository.findByNameIgnoreCase("Test Supplier")).thenReturn(Optional.of(testSupplier));

        assertThrows(IllegalArgumentException.class, () -> 
            supplierService.createSupplier(testSupplierDTO));
    }

    @Test
    @DisplayName("Should throw exception when creating supplier with existing email")
    void createSupplier_ShouldThrowException_WhenEmailExists() {
        when(supplierRepository.findByNameIgnoreCase("Test Supplier")).thenReturn(Optional.empty());
        when(supplierRepository.findByEmailIgnoreCase("supplier@example.com")).thenReturn(Optional.of(testSupplier));

        assertThrows(IllegalArgumentException.class, () -> 
            supplierService.createSupplier(testSupplierDTO));
    }

    @Test
    @DisplayName("Should update supplier when data is unique")
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
    @DisplayName("Should throw exception when updating non-existent supplier")
    void updateSupplier_ShouldThrowException_WhenSupplierNotFound() {
        when(supplierRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> 
            supplierService.updateSupplier(1L, testSupplierDTO));
    }

    @Test
    @DisplayName("Should throw exception when updating supplier with name used by another supplier")
    void updateSupplier_ShouldThrowException_WhenNameExistsForAnotherSupplier() {
        when(supplierRepository.findById(1L)).thenReturn(Optional.of(testSupplier));
        when(supplierRepository.findByNameIgnoreCaseAndIdIsNot("Test Supplier", 1L)).thenReturn(Optional.of(new Supplier()));

        assertThrows(IllegalArgumentException.class, () -> 
            supplierService.updateSupplier(1L, testSupplierDTO));
    }

    @Test
    @DisplayName("Should deactivate supplier successfully")
    void deactivateSupplier_ShouldSetActiveToFalse() {
        when(userRepository.findByEmail("admin@example.com")).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("password", "encodedPassword")).thenReturn(true);
        when(supplierRepository.findById(1L)).thenReturn(Optional.of(testSupplier));
        when(supplierRepository.save(any(Supplier.class))).thenAnswer(invocation -> invocation.getArgument(0));

        supplierService.deactivateSupplier(1L, "admin@example.com", "password");

        assertFalse(testSupplier.isActive());
    }

    @Test
    @DisplayName("Should reactivate supplier successfully")
    void reactivateSupplier_ShouldSetActiveToTrue() {
        testSupplier.setActive(false);
        when(supplierRepository.findById(1L)).thenReturn(Optional.of(testSupplier));
        when(supplierRepository.save(any(Supplier.class))).thenAnswer(invocation -> invocation.getArgument(0));

        supplierService.reactivateSupplier(1L);

        assertTrue(testSupplier.isActive());
    }

    @Test
    @DisplayName("Should delete supplier successfully")
    void deleteSupplier_ShouldDelete_WhenPasswordIsCorrect() {
        when(userRepository.findByEmail("admin@example.com")).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("password", "encodedPassword")).thenReturn(true);
        when(supplierRepository.existsById(1L)).thenReturn(true);

        supplierService.deleteSupplier(1L, "admin@example.com", "password");

        verify(supplierRepository, times(1)).deleteById(1L);
    }

    @Test
    @DisplayName("Should throw exception when deleting supplier with incorrect password")
    void deleteSupplier_ShouldThrowException_WhenPasswordIsIncorrect() {
        when(userRepository.findByEmail("admin@example.com")).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("wrongPassword", "encodedPassword")).thenReturn(false);

        assertThrows(IllegalArgumentException.class, () -> 
            supplierService.deleteSupplier(1L, "admin@example.com", "wrongPassword"));
        
        verify(supplierRepository, never()).deleteById(anyLong());
    }
}
