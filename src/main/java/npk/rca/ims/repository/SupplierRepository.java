package npk.rca.ims.repository;

import npk.rca.ims.model.Supplier;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SupplierRepository extends JpaRepository<Supplier, Long> {
    // Find all active suppliers
    List<Supplier> findByActiveTrue();

    // Check if a supplier with a given name exists (case-insensitive)
    Optional<Supplier> findByNameIgnoreCase(String name);

    // Check if a supplier with a given email exists (case-insensitive)
    Optional<Supplier> findByEmailIgnoreCase(String email);

    // Check if a supplier with a given name exists, excluding a specific ID
    Optional<Supplier> findByNameIgnoreCaseAndIdIsNot(String name, Long id);

    // Check if a supplier with a given email exists, excluding a specific ID
    Optional<Supplier> findByEmailIgnoreCaseAndIdIsNot(String email, Long id);
}
