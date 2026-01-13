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

    @Value("${app.frontend.url:http://localhost:3000}")
    private String frontendUrl;

    @Async
    public void sendPasswordResetEmail(String to, String token) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(to);
            helper.setSubject("Password Reset Request - RCA IMS");

            String resetLink = frontendUrl + "/reset-password?token=" + token;

            // Public raw GitHub logo URL
            String logoUrl = "https://raw.githubusercontent.com/Ntarekp/RCA_IMS_frontend/main/Rca-stock-management/public/rca-logo.png";

            String content = String.format("""
                <div style="font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; padding: 40px 20px; max-width: 600px; margin: 0 auto; background-color: #f1f5f9;">
                    <div style="background-color: #ffffff; border-radius: 16px; overflow: hidden; box-shadow: 0 10px 20px rgba(0,0,0,0.08);">

                        <!-- Header (Clean, No Blue Background) -->
                        <div style="display: flex; align-items: center; gap: 14px; padding: 28px 30px; border-bottom: 1px solid #e5e7eb;">
                            <img src="%s" alt="RCA Logo" style="width: 48px; height: auto;" />
                            <div>
                                <h1 style="margin: 0; font-size: 20px; color: #0f172a; font-weight: 700;">RCA IMS</h1>
                                <p style="margin: 2px 0 0; font-size: 12px; color: #64748b; letter-spacing: 1px; text-transform: uppercase;">
                                    Inventory Management System
                                </p>
                            </div>
                        </div>

                        <!-- Body -->
                        <div style="padding: 36px 30px;">
                            <h2 style="color: #0f172a; margin-top: 0; font-size: 22px; font-weight: 600;">Reset Your Password</h2>

                            <p style="color: #475569; line-height: 1.7; margin-top: 20px;">
                                Hello,
                            </p>

                            <p style="color: #475569; line-height: 1.7;">
                                We received a request to reset the password for your RCA Inventory Management System account.
                                If you did not request a password reset, you can safely ignore this email.
                            </p>

                            <div style="text-align: center; margin: 36px 0;">
                                <a href="%s" style="display: inline-block; background-color: #2563eb; color: #ffffff; padding: 14px 34px; text-decoration: none; border-radius: 10px; font-weight: 600; font-size: 16px; box-shadow: 0 8px 16px rgba(37,99,235,0.25);">
                                    Reset Password
                                </a>
                            </div>

                            <p style="color: #64748b; font-size: 13px; line-height: 1.6; margin-top: 28px; border-top: 1px solid #e5e7eb; padding-top: 20px;">
                                If the button above does not work, copy and paste this link into your browser:
                                <br />
                                <a href="%s" style="color: #2563eb; text-decoration: none; word-break: break-all;">%s</a>
                            </p>

                            <p style="color: #dc2626; font-size: 13px; margin-top: 16px;">
                                <strong>Security Notice:</strong> This link will expire in 15 minutes.
                            </p>
                        </div>

                        <!-- Footer -->
                        <div style="background-color: #f8fafc; padding: 20px; text-align: center; border-top: 1px solid #e5e7eb;">
                            <p style="color: #94a3b8; font-size: 12px; margin: 0;">
                                &copy; 2024 Rwanda Coding Academy. All rights reserved.
                            </p>
                        </div>
                    </div>
                </div>
                """,
                    logoUrl,
                    resetLink,
                    resetLink,
                    resetLink
            );

            helper.setText(content, true);
            mailSender.send(message);

            log.info("Password reset email sent successfully to {}", to);

        } catch (MessagingException e) {
            log.error("Failed to send password reset email to {}", to, e);
        } catch (Exception e) {
            log.error("Unexpected error sending password reset email to {}", to, e);
        }
    }

    @Async
    public void sendWelcomeEmail(String to, String tempPassword) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(to);
            helper.setSubject("Welcome to RCA IMS - Your Account Details");

            String loginLink = frontendUrl + "/login";
            String logoUrl = "https://raw.githubusercontent.com/Ntarekp/RCA_IMS_frontend/main/Rca-stock-management/public/rca-logo.png";

            String content = String.format("""
                <div style="font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; padding: 40px 20px; max-width: 600px; margin: 0 auto; background-color: #f1f5f9;">
                    <div style="background-color: #ffffff; border-radius: 16px; overflow: hidden; box-shadow: 0 10px 20px rgba(0,0,0,0.08);">

                        <!-- Header -->
                        <div style="display: flex; align-items: center; gap: 14px; padding: 28px 30px; border-bottom: 1px solid #e5e7eb;">
                            <img src="%s" alt="RCA Logo" style="width: 48px; height: auto;" />
                            <div>
                                <h1 style="margin: 0; font-size: 20px; color: #0f172a; font-weight: 700;">RCA IMS</h1>
                                <p style="margin: 2px 0 0; font-size: 12px; color: #64748b; letter-spacing: 1px; text-transform: uppercase;">
                                    Inventory Management System
                                </p>
                            </div>
                        </div>

                        <!-- Body -->
                        <div style="padding: 36px 30px;">
                            <h2 style="color: #0f172a; margin-top: 0; font-size: 22px; font-weight: 600;">Welcome Aboard!</h2>

                            <p style="color: #475569; line-height: 1.7; margin-top: 20px;">
                                Hello,
                            </p>

                            <p style="color: #475569; line-height: 1.7;">
                                An account has been created for you on the RCA Inventory Management System.
                                Below are your temporary login credentials. Please change your password immediately after logging in.
                            </p>

                            <div style="background-color: #f8fafc; border: 1px solid #e2e8f0; border-radius: 8px; padding: 20px; margin: 24px 0;">
                                <p style="margin: 0 0 10px 0; color: #64748b; font-size: 14px;">Email:</p>
                                <p style="margin: 0 0 20px 0; color: #0f172a; font-weight: 600; font-size: 16px;">%s</p>
                                
                                <p style="margin: 0 0 10px 0; color: #64748b; font-size: 14px;">Temporary Password:</p>
                                <p style="margin: 0; color: #0f172a; font-weight: 600; font-size: 16px; font-family: monospace; background: #e2e8f0; padding: 4px 8px; border-radius: 4px; display: inline-block;">%s</p>
                            </div>

                            <div style="text-align: center; margin: 36px 0;">
                                <a href="%s" style="display: inline-block; background-color: #2563eb; color: #ffffff; padding: 14px 34px; text-decoration: none; border-radius: 10px; font-weight: 600; font-size: 16px; box-shadow: 0 8px 16px rgba(37,99,235,0.25);">
                                    Login to Dashboard
                                </a>
                            </div>
                        </div>

                        <!-- Footer -->
                        <div style="background-color: #f8fafc; padding: 20px; text-align: center; border-top: 1px solid #e5e7eb;">
                            <p style="color: #94a3b8; font-size: 12px; margin: 0;">
                                &copy; 2024 Rwanda Coding Academy. All rights reserved.
                            </p>
                        </div>
                    </div>
                </div>
                """,
                    logoUrl,
                    to,
                    tempPassword,
                    loginLink
            );

            helper.setText(content, true);
            mailSender.send(message);

            log.info("Welcome email sent successfully to {}", to);

        } catch (MessagingException e) {
            log.error("Failed to send welcome email to {}", to, e);
        } catch (Exception e) {
            log.error("Unexpected error sending welcome email to {}", to, e);
        }
    }
}
