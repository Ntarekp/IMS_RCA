package npk.rca.ims.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username:noreply@rca-ims.com}")
    private String fromEmail;

    @Async
    public void sendPasswordResetEmail(String to, String token) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true);
            
            helper.setFrom(fromEmail);
            helper.setTo(to);
            helper.setSubject("Password Reset Request - RCA IMS");
            
            String resetLink = "http://localhost:5173/reset-password?token=" + token;
            
            String content = String.format("""
                <div style="font-family: Arial, sans-serif; padding: 20px; max-width: 600px; margin: 0 auto;">
                    <h2 style="color: #1E293B;">Password Reset Request</h2>
                    <p>Hello,</p>
                    <p>We received a request to reset your password for your RCA Inventory Management System account.</p>
                    <p>Click the button below to reset your password:</p>
                    <a href="%s" style="display: inline-block; background-color: #1E293B; color: white; padding: 10px 20px; text-decoration: none; border-radius: 5px; margin: 20px 0;">Reset Password</a>
                    <p>If you didn't request this, you can safely ignore this email.</p>
                    <p>This link will expire in 15 minutes.</p>
                    <hr style="border: 1px solid #eee; margin: 20px 0;" />
                    <p style="font-size: 12px; color: #666;">RCA Inventory Management System</p>
                </div>
                """, resetLink);
            
            helper.setText(content, true);
            
            mailSender.send(message);
            log.info("Password reset email sent to {}", to);
            
        } catch (MessagingException e) {
            log.error("Failed to send password reset email to {}", to, e);
            // In a real app, we might want to throw this up or handle it, 
            // but for async void, logging is key.
        }
    }
}
