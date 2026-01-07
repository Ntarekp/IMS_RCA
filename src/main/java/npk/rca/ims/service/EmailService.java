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
            
            String resetLink = "http://localhost:3000/reset-password?token=" + token;
            
            // Public URL for RCA Logo (using the one from GitHub as a reliable placeholder)
            String logoUrl = "https://raw.githubusercontent.com/Ntarekp/RCA_IMS_frontend/main/Rca-stock-management/public/rca-logo.png\n";
            
            String content = String.format("""
                <div style="font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; padding: 40px 20px; max-width: 600px; margin: 0 auto; background-color: #f1f5f9;">
                    <div style="background-color: #ffffff; border-radius: 16px; overflow: hidden; box-shadow: 0 4px 6px -1px rgba(0, 0, 0, 0.1), 0 2px 4px -1px rgba(0, 0, 0, 0.06);">
                        
                        <!-- Header with Logo -->
                        <div style="background-color: #1E293B; padding: 30px; text-align: center;">
                            <img src="%s" alt="RCA Logo" style="width: 80px; height: auto; margin-bottom: 10px;" />
                            <h1 style="color: #ffffff; margin: 0; font-size: 24px; letter-spacing: 1px;">RCA IMS</h1>
                            <p style="color: #94a3b8; margin: 5px 0 0; font-size: 14px; text-transform: uppercase; letter-spacing: 2px;">Inventory Management</p>
                        </div>
                        
                        <div style="padding: 40px 30px;">
                            <h2 style="color: #1e293b; margin-top: 0; font-size: 20px; font-weight: 600;">Reset Your Password</h2>
                            
                            <p style="color: #475569; line-height: 1.6; margin-top: 20px;">
                                Hello,
                            </p>
                            <p style="color: #475569; line-height: 1.6;">
                                We received a request to reset the password for your RCA Inventory Management System account. 
                                If you didn't make this request, you can safely ignore this email.
                            </p>
                            
                            <div style="text-align: center; margin: 35px 0;">
                                <a href="%s" style="display: inline-block; background-color: #2563eb; color: white; padding: 14px 32px; text-decoration: none; border-radius: 8px; font-weight: 600; font-size: 16px; box-shadow: 0 4px 6px -1px rgba(37, 99, 235, 0.2);">
                                    Reset Password
                                </a>
                            </div>
                            
                            <p style="color: #64748b; font-size: 13px; line-height: 1.6; margin-top: 30px; border-top: 1px solid #e2e8f0; padding-top: 20px;">
                                If the button above doesn't work, copy and paste this link into your browser:
                                <br/>
                                <a href="%s" style="color: #2563eb; text-decoration: none; word-break: break-all;">%s</a>
                            </p>
                            
                            <p style="color: #ef4444; font-size: 13px; margin-top: 15px;">
                                <strong>Note:</strong> This link will expire in 15 minutes for security reasons.
                            </p>
                        </div>
                        
                        <div style="background-color: #f8fafc; padding: 20px; text-align: center; border-top: 1px solid #e2e8f0;">
                            <p style="color: #94a3b8; font-size: 12px; margin: 0;">
                                &copy; 2024 Rwanda Coding Academy. All rights reserved.
                            </p>
                        </div>
                    </div>
                </div>
                """, logoUrl, resetLink, resetLink, resetLink);
            
            helper.setText(content, true);
            
            mailSender.send(message);
            log.info("Password reset email sent to {}", to);
            
        } catch (MessagingException e) {
            log.error("Failed to send password reset email to {}", to, e);
        }
    }
}
