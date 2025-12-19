package npk.rca.ims.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import npk.rca.ims.dto.*;
import npk.rca.ims.model.User;
import npk.rca.ims.service.JwtService;
import npk.rca.ims.service.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * AuthController - Authentication endpoints
 * Public endpoints (no authentication required)
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class AuthController {

    private final UserService userService;
    private final JwtService jwtService;

    /**
     * POST /api/auth/login
     * Authenticate user and return JWT token
     * 
     * Request body:
     * {
     *   "email": "ntarekayitare@gmail.com",
     *   "password": "RcaIMS@1234.5"
     * }
     * 
     * Response:
     * {
     *   "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
     *   "email": "ntarekayitare@gmail.com",
     *   "role": "ADMIN",
     *   "message": "Login successful"
     * }
     */
    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest loginRequest) {
        try {
            // Trim email to remove any whitespace
            String email = loginRequest.getEmail() != null ? loginRequest.getEmail().trim() : null;
            String password = loginRequest.getPassword() != null ? loginRequest.getPassword().trim() : null;
            
            // Authenticate user
            User user = userService.authenticate(email, password);
            
            if (user == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new LoginResponse(null, null, null, "Invalid email or password"));
            }
            
            // Generate JWT token
            String token = jwtService.generateToken(user);
            
            // Return success response
            LoginResponse response = new LoginResponse(token, user.getEmail(), user.getRole(), "Login successful");
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new LoginResponse(null, null, null, "An error occurred during login: " + e.getMessage()));
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
                request.getDepartment()
            );
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Profile updated successfully");
            response.put("email", updatedUser.getEmail());
            
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

