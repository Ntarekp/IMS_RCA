package npk.rca.ims.dto;

import jakarta.validation.constraints.Email;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * UpdateProfileRequest DTO - For updating user profile
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateProfileRequest {
    
    private String name; // Full name
    private String email;
    private String phone;
    private String department;
    
    // Settings
    private Boolean emailNotifications;
    private Boolean smsNotifications;
    private Boolean twoFactorAuth;
}
