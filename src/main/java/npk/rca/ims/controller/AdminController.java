package npk.rca.ims.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import npk.rca.ims.dto.CreateUserRequest;
import npk.rca.ims.dto.UserDTO;
import npk.rca.ims.model.User;
import npk.rca.ims.repository.UserRepository;
import npk.rca.ims.service.EmailService;
import npk.rca.ims.service.UserService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * AdminController - Administrative endpoints
 * For resetting passwords and managing users
 */
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@Slf4j
public class AdminController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserService userService;
    private final EmailService emailService;

    @Value("${app.admin.default-email:ntarekayitare@gmail.com}")
    private String defaultEmail;

    @Value("${app.admin.default-password:RcaIMS@1234.5}")
    private String defaultPassword;

    /**
     * GET /api/admin/users
     * List all users
     */
    @GetMapping("/users")
    public ResponseEntity<List<UserDTO>> getAllUsers() {
        List<User> users = userRepository.findAll();
        List<UserDTO> userDTOs = users.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
        return ResponseEntity.ok(userDTOs);
    }

    /**
     * POST /api/admin/users
     * Create a new user
     */
    @PostMapping("/users")
    public ResponseEntity<?> createUser(@Valid @RequestBody CreateUserRequest request) {
        try {
            if (userRepository.existsByEmail(request.getEmail())) {
                return ResponseEntity.badRequest().body(Map.of("message", "Email already exists"));
            }

            User user = new User();
            user.setEmail(request.getEmail());
            // Initialize systemEmail with the creation email
            user.setSystemEmail(request.getEmail());
            
            // Set default password if not provided
            String password = request.getPassword();
            if (password == null || password.isEmpty()) {
                password = "Password@123";
            }
            user.setPassword(passwordEncoder.encode(password));
            user.setRole(request.getRole());
            user.setName(request.getName());
            user.setPhone(request.getPhone());
            user.setDepartment(request.getDepartment());
            user.setLocation(request.getLocation());
            user.setEnabled(true);

            User savedUser = userRepository.save(user);
            
            // Send welcome email with the raw password
            // Use systemEmail if available, otherwise fallback to email
            String emailToSend = savedUser.getSystemEmail() != null ? savedUser.getSystemEmail() : savedUser.getEmail();

            try {
                emailService.sendWelcomeEmail(emailToSend, password);
            } catch (Exception e) {
                log.error("Failed to send welcome email to user: {}", emailToSend, e);
                // Don't fail the request if email sending fails, but log it
            }
            
            return ResponseEntity.ok(convertToDTO(savedUser));
        } catch (Exception e) {
            log.error("Error creating user", e);
            return ResponseEntity.internalServerError().body(Map.of("message", "Error creating user: " + e.getMessage()));
        }
    }

    /**
     * DELETE /api/admin/users/{id}
     * Delete a user
     */
    @DeleteMapping("/users/{id}")
    public ResponseEntity<?> deleteUser(@PathVariable Long id) {
        if (!userRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        
        // Prevent deleting the main admin
        User user = userRepository.findById(id).orElseThrow();
        // Check both email and systemEmail to protect the main admin account
        if (defaultEmail.equals(user.getEmail()) || defaultEmail.equals(user.getSystemEmail())) {
             return ResponseEntity.badRequest().body(Map.of("message", "Cannot delete the main administrator account"));
        }
        
        userRepository.deleteById(id);
        return ResponseEntity.ok(Map.of("message", "User deleted successfully"));
    }

    /**
     * POST /api/admin/reset-password
     * Reset password for default admin user
     */
    @PostMapping("/reset-password")
    public ResponseEntity<?> resetDefaultPassword() {
        try {
            // Try to find by email or systemEmail
            User user = userRepository.findByEmail(defaultEmail).orElse(null);
            
            if (user == null) {
                // If not found by email, try to find any user with this systemEmail
                // This is a bit tricky with standard JPA methods, so we might need a custom query
                // For now, let's assume if the email changed, we can't easily find it by the old email unless we add a finder
                // But for the default admin reset, we usually expect the email to be the default one
                
                user = userService.createUser(defaultEmail, defaultPassword, "ADMIN");
                log.info("Created default user: {}", defaultEmail);
            } else {
                user.setPassword(passwordEncoder.encode(defaultPassword));
                user.setEnabled(true);
                user.setRole("ADMIN");
                // Ensure system email is set
                if (user.getSystemEmail() == null) {
                    user.setSystemEmail(defaultEmail);
                }
                userRepository.save(user);
                log.info("Reset password for user: {}", defaultEmail);
            }
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Password reset successfully");
            response.put("email", defaultEmail);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Error resetting password", e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Error resetting password: " + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }

    /**
     * GET /api/admin/verify-user
     * Verify if default user exists and can authenticate
     */
    @GetMapping("/verify-user")
    public ResponseEntity<?> verifyDefaultUser() {
        try {
            User user = userRepository.findByEmail(defaultEmail).orElse(null);
            
            Map<String, Object> response = new HashMap<>();
            
            if (user == null) {
                response.put("exists", false);
                response.put("message", "User does not exist");
                return ResponseEntity.ok(response);
            }
            
            boolean passwordMatches = passwordEncoder.matches(defaultPassword, user.getPassword());
            
            response.put("exists", true);
            response.put("email", user.getEmail());
            response.put("role", user.getRole());
            response.put("enabled", user.isEnabled());
            response.put("passwordMatches", passwordMatches);
            response.put("message", passwordMatches ? "User verified successfully" : "Password does not match");
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Error verifying user", e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Error verifying user: " + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }

    private UserDTO convertToDTO(User user) {
        return new UserDTO(
                user.getId(),
                user.getEmail(),
                user.getRole(),
                user.isEnabled(),
                user.getName(),
                user.getPhone(),
                user.getDepartment(),
                user.getLocation(),
                user.getAvatarUrl(),
                user.getCoverUrl(),
                user.getCreatedAt()
        );
    }
}
