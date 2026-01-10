package npk.rca.ims.controller;

import npk.rca.ims.dto.LoginRequest;
import npk.rca.ims.dto.ResetPasswordRequest;
import npk.rca.ims.model.User;
import npk.rca.ims.service.EmailService;
import npk.rca.ims.service.JwtService;
import npk.rca.ims.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    @Mock
    private UserService userService;

    @Mock
    private JwtService jwtService;

    @Mock
    private EmailService emailService;

    @InjectMocks
    private AuthController authController;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setEmail("test@example.com");
        testUser.setRole("USER");
        testUser.setEnabled(true);
    }

    @Test
    @DisplayName("Should return token when login credentials are valid")
    void login_ShouldReturnToken_WhenCredentialsAreValid() {
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setEmail("test@example.com");
        loginRequest.setPassword("password");

        when(userService.authenticate("test@example.com", "password")).thenReturn(testUser);
        when(jwtService.generateToken(testUser)).thenReturn("valid-token");

        ResponseEntity<?> response = authController.login(loginRequest);

        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    @DisplayName("Should return Unauthorized when login credentials are invalid")
    void login_ShouldReturnUnauthorized_WhenCredentialsAreInvalid() {
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setEmail("test@example.com");
        loginRequest.setPassword("wrong-password");

        when(userService.authenticate("test@example.com", "wrong-password")).thenReturn(null);

        ResponseEntity<?> response = authController.login(loginRequest);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
    }

    @Test
    @DisplayName("Should send reset email when forgot password user exists")
    void forgotPassword_ShouldSendEmail_WhenUserExists() {
        Map<String, String> request = new HashMap<>();
        request.put("email", "test@example.com");

        when(userService.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));
        when(jwtService.generateResetToken(testUser)).thenReturn("reset-token");

        ResponseEntity<?> response = authController.forgotPassword(request);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(emailService, times(1)).sendPasswordResetEmail("test@example.com", "reset-token");
    }

    @Test
    @DisplayName("Should reset password when token is valid")
    void resetPassword_ShouldResetPassword_WhenTokenIsValid() {
        ResetPasswordRequest request = new ResetPasswordRequest();
        request.setToken("valid-token");
        request.setNewPassword("newPassword");
        request.setConfirmPassword("newPassword");

        when(jwtService.validateToken("valid-token")).thenReturn(true);
        when(jwtService.extractEmail("valid-token")).thenReturn("test@example.com");

        ResponseEntity<?> response = authController.resetPassword(request);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(userService, times(1)).resetPassword("test@example.com", "newPassword");
    }
}
