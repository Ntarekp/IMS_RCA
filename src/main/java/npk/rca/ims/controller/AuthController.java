package npk.rca.ims.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import npk.rca.ims.dto.LoginRequest;
import npk.rca.ims.dto.LoginResponse;
import npk.rca.ims.model.User;
import npk.rca.ims.service.JwtService;
import npk.rca.ims.service.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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
}

