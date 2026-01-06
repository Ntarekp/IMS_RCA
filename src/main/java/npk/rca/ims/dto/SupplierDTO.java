package npk.rca.ims.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SupplierDTO {
    private Long id;

    @NotBlank(message = "Company name is required")
    private String name;

    @NotBlank(message = "Contact person is required")
    private String contactPerson;

    @NotBlank(message = "Phone number is required")
    private String phone;

    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    private String email;

    private String itemsSupplied;
    private boolean active;
}
