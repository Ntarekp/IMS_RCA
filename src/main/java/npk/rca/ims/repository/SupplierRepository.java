package npk.rca.ims.repository;

import npk.rca.ims.model.Supplier;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SupplierRepository extends JpaRepository<Supplier, Long> {
    
    // Find all active suppliers
    List<Supplier> findByActive(boolean active);
}
