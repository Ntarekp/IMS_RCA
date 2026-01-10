package npk.rca.ims.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmailServiceTest {

    @Mock
    private JavaMailSender mailSender;

    @InjectMocks
    private EmailService emailService;

    @Mock
    private MimeMessage mimeMessage;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(emailService, "fromEmail", "noreply@rca-ims.com");
    }

    @Test
    @DisplayName("Should send password reset email successfully")
    void sendPasswordResetEmail_ShouldSendEmail() {
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

        emailService.sendPasswordResetEmail("test@example.com", "reset-token");

        verify(mailSender, times(1)).send(mimeMessage);
    }

    @Test
    @DisplayName("Should handle exception gracefully when sending password reset email fails")
    void sendPasswordResetEmail_ShouldHandleException() {
        when(mailSender.createMimeMessage()).thenThrow(new RuntimeException("Mail server error"));

        // Should not throw exception, just log error
        emailService.sendPasswordResetEmail("test@example.com", "reset-token");
        
        verify(mailSender, times(0)).send(any(MimeMessage.class));
    }

    @Test
    @DisplayName("Should send welcome email successfully")
    void sendWelcomeEmail_ShouldSendEmail() {
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

        emailService.sendWelcomeEmail("test@example.com", "temp-password");

        verify(mailSender, times(1)).send(mimeMessage);
    }
    
    @Test
    @DisplayName("Should handle exception gracefully when sending welcome email fails")
    void sendWelcomeEmail_ShouldHandleException() {
        when(mailSender.createMimeMessage()).thenThrow(new RuntimeException("Mail server error"));

        // Should not throw exception, just log error
        emailService.sendWelcomeEmail("test@example.com", "temp-password");
        
        verify(mailSender, times(0)).send(any(MimeMessage.class));
    }
}
