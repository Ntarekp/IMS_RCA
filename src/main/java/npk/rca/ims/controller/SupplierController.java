package npk.rca.ims.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import npk.rca.ims.dto.DeleteRequestDTO;
import npk.rca.ims.dto.SupplierDTO;
import npk.rca.ims.service.SupplierService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/api/suppliers")
@RequiredArgsConstructor
public class SupplierController {

    private final SupplierService supplierService;

    @GetMapping
    public ResponseEntity<List<SupplierDTO>> getAllActiveSuppliers() {
        return ResponseEntity.ok(supplierService.getAllActiveSuppliers());
    }

    @GetMapping("/inactive")
    public ResponseEntity<List<SupplierDTO>> getAllInactiveSuppliers() {
        return ResponseEntity.ok(supplierService.getAllInactiveSuppliers());
    }

    @PostMapping
    public ResponseEntity<SupplierDTO> createSupplier(@Valid @RequestBody SupplierDTO supplierDTO) {
        SupplierDTO createdSupplier = supplierService.createSupplier(supplierDTO);
        return new ResponseEntity<>(createdSupplier, HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    public ResponseEntity<SupplierDTO> updateSupplier(@PathVariable Long id, @Valid @RequestBody SupplierDTO supplierDTO) {
        SupplierDTO updatedSupplier = supplierService.updateSupplier(id, supplierDTO);
        return ResponseEntity.ok(updatedSupplier);
    }

    @PatchMapping("/{id}/deactivate")
    public ResponseEntity<Void> deactivateSupplier(
            @PathVariable Long id,
            @Valid @RequestBody DeleteRequestDTO request,
            Principal principal) {
        supplierService.deactivateSupplier(id, principal.getName(), request.getPassword());
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/reactivate")
    public ResponseEntity<Void> reactivateSupplier(@PathVariable Long id) {
        supplierService.reactivateSupplier(id);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteSupplier(
            @PathVariable Long id,
            @Valid @RequestBody DeleteRequestDTO request,
            Principal principal) {
        supplierService.deleteSupplier(id, principal.getName(), request.getPassword());
        return ResponseEntity.noContent().build();
    }
}
