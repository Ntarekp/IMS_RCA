package npk.rca.ims.service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import npk.rca.ims.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class JwtServiceTest {

    @InjectMocks
    private JwtService jwtService;

    private User testUser;
    private String secret = "your-secret-key-change-this-in-production-min-256-bits";
    private Long expiration = 86400000L;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(jwtService, "secret", secret);
        ReflectionTestUtils.setField(jwtService, "expiration", expiration);

        testUser = new User();
        testUser.setId(1L);
        testUser.setEmail("test@example.com");
        testUser.setRole("USER");
    }

    @Test
    @DisplayName("Should generate valid token with correct claims")
    void generateToken_ShouldGenerateValidToken() {
        String token = jwtService.generateToken(testUser);

        assertNotNull(token);
        assertTrue(jwtService.validateToken(token));
        assertEquals("test@example.com", jwtService.extractEmail(token));
        assertEquals("USER", jwtService.extractRole(token));
    }

    @Test
    @DisplayName("Should generate valid reset token")
    void generateResetToken_ShouldGenerateValidToken() {
        String token = jwtService.generateResetToken(testUser);

        assertNotNull(token);
        assertTrue(jwtService.validateToken(token));
        assertEquals("test@example.com", jwtService.extractEmail(token));
    }

    @Test
    @DisplayName("Should return false when validating invalid token string")
    void validateToken_ShouldReturnFalse_WhenTokenIsInvalid() {
        assertFalse(jwtService.validateToken("invalid.token.string"));
    }

    @Test
    @DisplayName("Should return false when validating expired token")
    void validateToken_ShouldReturnFalse_WhenTokenIsExpired() {
        // Create an expired token manually or mock time (mocking time is harder with JJWT)
        // For simplicity, we'll set expiration to -1000ms
        ReflectionTestUtils.setField(jwtService, "expiration", -1000L);
        String expiredToken = jwtService.generateToken(testUser);
        
        assertFalse(jwtService.validateToken(expiredToken));
    }
}
