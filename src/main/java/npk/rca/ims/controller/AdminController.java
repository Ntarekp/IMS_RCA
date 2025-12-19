package npk.rca.ims.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import npk.rca.ims.model.User;
import npk.rca.ims.repository.UserRepository;
import npk.rca.ims.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * AdminController - Administrative endpoints
 * For resetting passwords and managing users
 */
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
@Slf4j
public class AdminController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserService userService;

    /**
     * POST /api/admin/reset-password
     * Reset password for default admin user
     * This is a utility endpoint for fixing password issues
     */
    @PostMapping("/reset-password")
    public ResponseEntity<?> resetDefaultPassword() {
        try {
            String defaultEmail = "ntarekayitare@gmail.com";
            String defaultPassword = "RcaIMS@1234.5";
            
            User user = userRepository.findByEmail(defaultEmail).orElse(null);
            
            if (user == null) {
                // Create user if doesn't exist
                user = userService.createUser(defaultEmail, defaultPassword, "ADMIN");
                log.info("Created default user: {}", defaultEmail);
            } else {
                // Update password
                user.setPassword(passwordEncoder.encode(defaultPassword));
                user.setEnabled(true);
                user.setRole("ADMIN");
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
            String defaultEmail = "ntarekayitare@gmail.com";
            String defaultPassword = "RcaIMS@1234.5";
            
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
}

