package npk.rca.ims.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateUserRequest {
    @NotBlank
    @Email
    private String email;
    
    @NotBlank
    private String password;
    
    private String role = "USER";
    private String name;
    private String phone;
    private String department;
    private String location;
}
