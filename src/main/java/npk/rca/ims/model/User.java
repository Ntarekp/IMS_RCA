package npk.rca.ims.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * User Entity - Stores user credentials with encrypted passwords
 */
@Entity
@Table(name = "users")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Email is required")
    @Email(message = "Email must be valid")
    @Column(nullable = false, unique = true, length = 100)
    private String email;

    @NotBlank(message = "Password is required")
    @Column(nullable = false, length = 255)
    private String password; // Will be encrypted using BCrypt

    @Column(nullable = false)
    private boolean enabled = true;

    @Column(nullable = false)
    private String role = "USER"; // USER, ADMIN, etc.

    // New profile fields
    @Column(length = 100)
    private String name;

    @Column(length = 50)
    private String phone;

    @Column(length = 100)
    private String department;

    @Column(length = 100)
    private String location;
    
    @Column(columnDefinition = "TEXT")
    private String avatarUrl;
    
    @Column(columnDefinition = "TEXT")
    private String coverUrl;

    // Settings fields
    @Column(nullable = false)
    private boolean emailNotifications = true;

    @Column(nullable = false)
    private boolean smsNotifications = false;

    @Column(nullable = false)
    private boolean twoFactorAuth = false;

    @Column(length = 20)
    private String theme = "LIGHT";

    @Column(length = 10)
    private String language = "en";
    
    // Field to store the original email used for system notifications (like password resets)
    // This allows the user to change their login/display email without breaking system emails
    @Column(length = 100)
    private String systemEmail;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedAt;
}
