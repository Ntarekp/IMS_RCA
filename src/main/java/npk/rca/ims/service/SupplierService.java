package npk.rca.ims.service;

import lombok.RequiredArgsConstructor;
import npk.rca.ims.dto.SupplierDTO;
import npk.rca.ims.exceptions.ResourceNotFoundException;
import npk.rca.ims.model.Supplier;
import npk.rca.ims.repository.SupplierRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class SupplierService {

    private final SupplierRepository supplierRepository;

    public List<SupplierDTO> getAllActiveSuppliers() {
        return supplierRepository.findByActiveTrue().stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    public SupplierDTO createSupplier(SupplierDTO dto) {
        // Check for duplicate name
        if (supplierRepository.findByNameIgnoreCase(dto.getName()).isPresent()) {
            throw new IllegalArgumentException("Supplier with name '" + dto.getName() + "' already exists.");
        }

        // Check for duplicate email
        if (supplierRepository.findByEmailIgnoreCase(dto.getEmail()).isPresent()) {
            throw new IllegalArgumentException("Supplier with email '" + dto.getEmail() + "' already exists.");
        }

        Supplier supplier = new Supplier();
        updateEntityFromDTO(supplier, dto);
        supplier.setActive(true);
        return convertToDTO(supplierRepository.save(supplier));
    }

    public SupplierDTO updateSupplier(Long id, SupplierDTO dto) {
        Supplier supplier = supplierRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Supplier not found with id: " + id));
        
        // Check for duplicate name (excluding current supplier)
        Optional<Supplier> existingName = supplierRepository.findByNameIgnoreCaseAndIdIsNot(dto.getName(), id);
        if (existingName.isPresent()) {
            throw new IllegalArgumentException("Supplier with name '" + dto.getName() + "' already exists.");
        }

        // Check for duplicate email (excluding current supplier)
        Optional<Supplier> existingEmail = supplierRepository.findByEmailIgnoreCaseAndIdIsNot(dto.getEmail(), id);
        if (existingEmail.isPresent()) {
            throw new IllegalArgumentException("Supplier with email '" + dto.getEmail() + "' already exists.");
        }

        updateEntityFromDTO(supplier, dto);
        return convertToDTO(supplierRepository.save(supplier));
    }

    public void deactivateSupplier(Long id) {
        Supplier supplier = supplierRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Supplier not found with id: " + id));
        
        supplier.setActive(false); // Soft delete
        supplierRepository.save(supplier);
    }

    private void updateEntityFromDTO(Supplier supplier, SupplierDTO dto) {
        supplier.setName(dto.getName());
        supplier.setContactPerson(dto.getContactPerson());
        supplier.setPhone(dto.getPhone());
        supplier.setEmail(dto.getEmail());
        supplier.setItemsSupplied(dto.getItemsSupplied());
    }

    private SupplierDTO convertToDTO(Supplier supplier) {
        return new SupplierDTO(
            supplier.getId(),
            supplier.getName(),
            supplier.getContactPerson(),
            supplier.getPhone(),
            supplier.getEmail(),
            supplier.getItemsSupplied(),
            supplier.isActive()
        );
    }
}
