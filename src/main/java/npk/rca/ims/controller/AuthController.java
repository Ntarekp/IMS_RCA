package npk.rca.ims.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import npk.rca.ims.dto.*;
import npk.rca.ims.model.User;
import npk.rca.ims.service.EmailService;
import npk.rca.ims.service.JwtService;
import npk.rca.ims.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * AuthController - Authentication endpoints
 * Public endpoints (no authentication required)
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private static final Logger log = LoggerFactory.getLogger(AuthController.class);

    private final UserService userService;
    private final JwtService jwtService;
    private final EmailService emailService;

    /**
     * POST /api/auth/login
     * Authenticate user and return JWT token
     */
    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest loginRequest) {
        log.info("Login request received for email: {}", loginRequest.getEmail());
        try {
            // Trim email to remove any whitespace
            String email = loginRequest.getEmail() != null ? loginRequest.getEmail().trim() : null;
            String password = loginRequest.getPassword() != null ? loginRequest.getPassword().trim() : null;
            
            // Authenticate user
            User user = userService.authenticate(email, password);
            
            if (user == null) {
                log.warn("Authentication failed for email: {}", email);
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new LoginResponse(null, null, null, "Invalid email or password"));
            }
            
            log.info("Authentication successful for email: {}", email);
            // Generate JWT token
            String token = jwtService.generateToken(user);
            
            // Return success response
            LoginResponse response = new LoginResponse(token, user.getEmail(), user.getRole(), "Login successful");
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("An error occurred during login for email: {}", loginRequest.getEmail(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new LoginResponse(null, null, null, "An error occurred during login: " + e.getMessage()));
        }
    }

    /**
     * POST /api/auth/forgot-password
     * Initiate password reset
     */
    @PostMapping("/forgot-password")
    public ResponseEntity<?> forgotPassword(@RequestBody Map<String, String> request) {
        String email = request.get("email");
        if (email == null || email.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("message", "Email is required"));
        }

        Optional<User> userOpt = userService.findByEmail(email);
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            String token = jwtService.generateResetToken(user);
            emailService.sendPasswordResetEmail(user.getEmail(), token);
        }
        
        // Always return success to prevent email enumeration
        return ResponseEntity.ok(Map.of("message", "If an account exists with that email, a password reset link has been sent."));
    }

    /**
     * POST /api/auth/reset-password
     * Complete password reset
     */
    @PostMapping("/reset-password")
    public ResponseEntity<?> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        try {
            if (!jwtService.validateToken(request.getToken())) {
                return ResponseEntity.badRequest().body(Map.of("message", "Invalid or expired token"));
            }

            if (!request.getNewPassword().equals(request.getConfirmPassword())) {
                return ResponseEntity.badRequest().body(Map.of("message", "Passwords do not match"));
            }

            String email = jwtService.extractEmail(request.getToken());
            // We can reuse the changePassword logic but we need to bypass old password check
            // So we'll use a new method in UserService or just update directly if we trust the token
            // Since UserService.changePassword requires old password, let's add a resetPassword method to UserService
            
            userService.resetPassword(email, request.getNewPassword());
            
            return ResponseEntity.ok(Map.of("message", "Password has been reset successfully"));
            
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("message", "Failed to reset password: " + e.getMessage()));
        }
    }

    /**
     * GET /api/auth/validate
     * Validate JWT token
     */
    @GetMapping("/validate")
    public ResponseEntity<?> validateToken(@RequestHeader("Authorization") String authHeader) {
        try {
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body("Invalid authorization header");
            }
            
            String token = authHeader.substring(7);
            if (jwtService.validateToken(token)) {
                return ResponseEntity.ok("Token is valid");
            } else {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body("Invalid or expired token");
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body("Token validation failed");
        }
    }

    /**
     * GET /api/auth/profile
     * Get current user profile
     */
    @GetMapping("/profile")
    public ResponseEntity<?> getProfile(@RequestHeader("Authorization") String authHeader) {
        try {
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body("Invalid authorization header");
            }
            
            String token = authHeader.substring(7);
            String email = jwtService.extractEmail(token);
            
            User user = userService.getProfile(email);
            
            Map<String, Object> response = new HashMap<>();
            response.put("email", user.getEmail());
            response.put("role", user.getRole());
            response.put("enabled", user.isEnabled());
            response.put("name", user.getName());
            response.put("phone", user.getPhone());
            response.put("department", user.getDepartment());
            response.put("location", user.getLocation());
            
            // Settings
            response.put("emailNotifications", user.isEmailNotifications());
            response.put("smsNotifications", user.isSmsNotifications());
            response.put("twoFactorAuth", user.isTwoFactorAuth());
            response.put("theme", user.getTheme());
            response.put("language", user.getLanguage());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Error fetching profile: " + e.getMessage());
        }
    }

    /**
     * PUT /api/auth/profile
     * Update user profile
     */
    @PutMapping("/profile")
    public ResponseEntity<?> updateProfile(
            @RequestHeader("Authorization") String authHeader,
            @Valid @RequestBody UpdateProfileRequest request) {
        try {
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body("Invalid authorization header");
            }
            
            String token = authHeader.substring(7);
            String email = jwtService.extractEmail(token);
            
            User updatedUser = userService.updateProfile(
                email,
                request.getEmail(),
                request.getName(),
                request.getPhone(),
                request.getDepartment(),
                request.getLocation(),
                request.getEmailNotifications(),
                request.getSmsNotifications(),
                request.getTwoFactorAuth(),
                request.getTheme(),
                request.getLanguage()
            );
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Profile updated successfully");
            response.put("email", updatedUser.getEmail());
            response.put("name", updatedUser.getName());
            response.put("phone", updatedUser.getPhone());
            response.put("department", updatedUser.getDepartment());
            response.put("location", updatedUser.getLocation());
            
            // Settings
            response.put("emailNotifications", updatedUser.isEmailNotifications());
            response.put("smsNotifications", updatedUser.isSmsNotifications());
            response.put("twoFactorAuth", updatedUser.isTwoFactorAuth());
            response.put("theme", updatedUser.getTheme());
            response.put("language", updatedUser.getLanguage());
            
            return ResponseEntity.ok(response);
            
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("success", false, "message", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("success", false, "message", "Error updating profile: " + e.getMessage()));
        }
    }

    /**
     * POST /api/auth/change-password
     * Change user password
     */
    @PostMapping("/change-password")
    public ResponseEntity<?> changePassword(
            @RequestHeader("Authorization") String authHeader,
            @Valid @RequestBody ChangePasswordRequest request) {
        try {
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body("Invalid authorization header");
            }
            
            // Validate passwords match
            if (!request.getNewPassword().equals(request.getConfirmPassword())) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("success", false, "message", "New password and confirm password do not match"));
            }
            
            String token = authHeader.substring(7);
            String email = jwtService.extractEmail(token);
            
            userService.changePassword(email, request.getCurrentPassword(), request.getNewPassword());
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Password changed successfully");
            
            return ResponseEntity.ok(response);
            
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("success", false, "message", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("success", false, "message", "Error changing password: " + e.getMessage()));
        }
    }
}
