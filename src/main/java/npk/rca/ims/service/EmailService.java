package npk.rca.ims.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import npk.rca.ims.dto.StockBalanceDTO;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;
    private final StockBalanceService stockBalanceService;

    @Value("${spring.mail.username:noreply@rca-ims.com}")
    private String fromEmail;

    @Value("${app.frontend.url:http://10.12.72.9:8080/rca_ims}")
    private String frontendUrl;

    @Value("${app.admin.default-email:ntarekayitare@gmail.com}")
    private String adminEmail;

    @Async
    public void sendPasswordResetEmail(String to, String token) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(to);
            helper.setSubject("Password Reset Request - RCA IMS");

            String baseUrl = frontendUrl.endsWith("/") ? frontendUrl.substring(0, frontendUrl.length() - 1) : frontendUrl;
            String resetLink = baseUrl + "/reset-password?token=" + token;

            String logoUrl = "https://raw.githubusercontent.com/Ntarekp/RCA_IMS_frontend/main/Rca-stock-management/public/rca-logo.png";

            String content = String.format("""
                <div style="font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; padding: 40px 20px; max-width: 600px; margin: 0 auto; background-color: #f1f5f9;">
                    <div style="background-color: #ffffff; border-radius: 16px; overflow: hidden; box-shadow: 0 10px 20px rgba(0,0,0,0.08);">

                        <!-- Header -->
                        <div style="background: linear-gradient(135deg, #1e3a8a 0%%, #2563eb 100%%); padding: 32px 30px;">
                            <table width="100%%" cellpadding="0" cellspacing="0" border="0">
                                <tr>
                                    <td style="width: 64px; vertical-align: middle;">
                                        <div style="background-color: #ffffff; border-radius: 12px; padding: 8px; display: inline-block; box-shadow: 0 4px 12px rgba(0,0,0,0.1);">
                                            <img src="%s" alt="RCA Logo" style="width: 48px; height: 48px; display: block;" />
                                        </div>
                                    </td>
                                    <td style="width: 24px;"></td>
                                    <td style="vertical-align: middle;">
                                        <h1 style="margin: 0; font-size: 24px; color: #ffffff; font-weight: 700; letter-spacing: -0.5px;">RCA IMS</h1>
                                        <p style="margin: 4px 0 0; font-size: 13px; color: #93c5fd; letter-spacing: 1.2px; text-transform: uppercase; font-weight: 500;">
                                            Inventory Management System
                                        </p>
                                    </td>
                                </tr>
                            </table>
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
                                <a href="%s" style="display: inline-block; background-color: #2563eb; color: #ffffff; padding: 14px 34px; text-decoration: none; border-radius: 10px; font-weight: 600; font-size: 16px; box-shadow: 0 8px 16px rgba(37,99,235,0.25); transition: all 0.3s ease;">
                                    Reset Password
                                </a>
                            </div>

                            <p style="color: #64748b; font-size: 13px; line-height: 1.6; margin-top: 28px; border-top: 1px solid #e5e7eb; padding-top: 20px;">
                                If the button above does not work, copy and paste this link into your browser:
                                <br />
                                <a href="%s" style="color: #2563eb; text-decoration: none; word-break: break-all;">%s</a>
                            </p>

                            <div style="background-color: #fef2f2; border-left: 4px solid #dc2626; border-radius: 6px; padding: 16px; margin-top: 24px;">
                                <p style="color: #dc2626; font-size: 13px; margin: 0; line-height: 1.5;">
                                    <strong>⚠️ Security Notice:</strong> This link will expire in 15 minutes.
                                </p>
                            </div>
                        </div>

                        <!-- Footer -->
                        <div style="background-color: #f8fafc; padding: 24px; text-align: center; border-top: 1px solid #e5e7eb;">
                            <p style="color: #94a3b8; font-size: 12px; margin: 0; line-height: 1.6;">
                                &copy; 2024 Rwanda Coding Academy. All rights reserved.
                            </p>
                            <p style="color: #cbd5e1; font-size: 11px; margin: 8px 0 0;">
                                This is an automated message, please do not reply.
                            </p>
                        </div>
                    </div>
                </div>
                """, logoUrl, resetLink, resetLink, resetLink);

            helper.setText(content, true);
            mailSender.send(message);

            log.info("Welcome email sent successfully to {}", to);

        } catch (MessagingException e) {
            log.error("Failed to send welcome email to {}", to, e);
        } catch (Exception e) {
            log.error("Unexpected error sending welcome email to {}", to, e);
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

            String baseUrl = frontendUrl.endsWith("/") ? frontendUrl.substring(0, frontendUrl.length() - 1) : frontendUrl;
            String loginLink = baseUrl + "/login";

            String logoUrl = "https://raw.githubusercontent.com/Ntarekp/RCA_IMS_frontend/main/Rca-stock-management/public/rca-logo.png";

            String content = String.format("""
                <div style="font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; padding: 40px 20px; max-width: 600px; margin: 0 auto; background-color: #f1f5f9;">
                    <div style="background-color: #ffffff; border-radius: 16px; overflow: hidden; box-shadow: 0 10px 20px rgba(0,0,0,0.08);">

                        <!-- Header -->
                        <div style="background: linear-gradient(135deg, #1e3a8a 0%%, #2563eb 100%%); padding: 32px 30px;">
                            <table width="100%%" cellpadding="0" cellspacing="0" border="0">
                                <tr>
                                    <td style="width: 64px; vertical-align: middle;">
                                        <div style="background-color: #ffffff; border-radius: 12px; padding: 8px; display: inline-block; box-shadow: 0 4px 12px rgba(0,0,0,0.1);">
                                            <img src="%s" alt="RCA Logo" style="width: 48px; height: 48px; display: block;" />
                                        </div>
                                    </td>
                                    <td style="width: 24px;"></td>
                                    <td style="vertical-align: middle;">
                                        <h1 style="margin: 0; font-size: 24px; color: #ffffff; font-weight: 700; letter-spacing: -0.5px;">RCA IMS</h1>
                                        <p style="margin: 4px 0 0; font-size: 13px; color: #93c5fd; letter-spacing: 1.2px; text-transform: uppercase; font-weight: 500;">
                                            Inventory Management System
                                        </p>
                                    </td>
                                </tr>
                            </table>
                        </div>

                        <!-- Body -->
                        <div style="padding: 36px 30px;">
                            <h2 style="color: #0f172a; margin-top: 0; font-size: 22px; font-weight: 600;">Welcome Aboard! 🎉</h2>

                            <p style="color: #475569; line-height: 1.7; margin-top: 20px;">
                                Hello,
                            </p>

                            <p style="color: #475569; line-height: 1.7;">
                                An account has been created for you on the RCA Inventory Management System.
                                Below are your temporary login credentials. Please change your password immediately after logging in.
                            </p>

                            <div style="background-color: #f8fafc; border: 2px solid #e2e8f0; border-radius: 12px; padding: 24px; margin: 28px 0;">
                                <p style="margin: 0 0 8px 0; color: #64748b; font-size: 13px; font-weight: 600; text-transform: uppercase; letter-spacing: 0.5px;">Email Address</p>
                                <p style="margin: 0 0 24px 0; color: #0f172a; font-weight: 600; font-size: 16px;">%s</p>
                                
                                <p style="margin: 0 0 8px 0; color: #64748b; font-size: 13px; font-weight: 600; text-transform: uppercase; letter-spacing: 0.5px;">Temporary Password</p>
                                <p style="margin: 0; color: #0f172a; font-weight: 600; font-size: 16px; font-family: 'Courier New', monospace; background: #e2e8f0; padding: 12px 16px; border-radius: 6px; display: inline-block; border: 1px dashed #94a3b8;">%s</p>
                            </div>

                            <div style="text-align: center; margin: 36px 0;">
                                <a href="%s" style="display: inline-block; background-color: #2563eb; color: #ffffff; padding: 14px 34px; text-decoration: none; border-radius: 10px; font-weight: 600; font-size: 16px; box-shadow: 0 8px 16px rgba(37,99,235,0.25);">
                                    Login to Dashboard
                                </a>
                            </div>

                            <div style="background-color: #fffbeb; border-left: 4px solid #f59e0b; border-radius: 6px; padding: 16px; margin-top: 24px;">
                                <p style="color: #92400e; font-size: 13px; margin: 0; line-height: 1.5;">
                                    <strong>🔒 Security Reminder:</strong> Change your password immediately after your first login to keep your account secure.
                                </p>
                            </div>
                        </div>

                        <!-- Footer -->
                        <div style="background-color: #f8fafc; padding: 24px; text-align: center; border-top: 1px solid #e5e7eb;">
                            <p style="color: #94a3b8; font-size: 12px; margin: 0; line-height: 1.6;">
                                &copy; 2024 Rwanda Coding Academy. All rights reserved.
                            </p>
                            <p style="color: #cbd5e1; font-size: 11px; margin: 8px 0 0;">
                                This is an automated message, please do not reply.
                            </p>
                        </div>
                    </div>
                </div>
                """, logoUrl, to, tempPassword, loginLink);

            helper.setText(content, true);
            mailSender.send(message);

            log.info("Welcome email sent successfully to {}", to);

        } catch (MessagingException e) {
            log.error("Failed to send welcome email to {}", to, e);
        } catch (Exception e) {
            log.error("Unexpected error sending welcome email to {}", to, e);
        }
    }

    @Scheduled(cron = "0 0 8 * * *") // Runs every day at 8 AM
    public void sendDailyStockSummary() {
        try {
            List<StockBalanceDTO> lowStockItems = stockBalanceService.getLowStockItems();
            
            if (lowStockItems.isEmpty()) {
                log.info("No low stock items to report today.");
                return;
            }

            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(adminEmail);
            helper.setSubject("Daily Low Stock Alert - RCA IMS");

            StringBuilder itemsHtml = new StringBuilder();
            for (StockBalanceDTO item : lowStockItems) {
                itemsHtml.append(String.format("""
                    <tr>
                        <td style="padding: 12px; border-bottom: 1px solid #e2e8f0;">%s</td>
                        <td style="padding: 12px; border-bottom: 1px solid #e2e8f0; text-align: center;">%d %s</td>
                        <td style="padding: 12px; border-bottom: 1px solid #e2e8f0; text-align: center;">%d %s</td>
                        <td style="padding: 12px; border-bottom: 1px solid #e2e8f0; text-align: center; color: #dc2626; font-weight: bold;">LOW</td>
                    </tr>
                """, item.getItemName(), item.getCurrentBalance(), item.getUnit(), item.getMinimumStock(), item.getUnit()));
            }

            String logoUrl = "https://raw.githubusercontent.com/Ntarekp/RCA_IMS_frontend/main/Rca-stock-management/public/rca-logo.png";
            String dashboardLink = frontendUrl.endsWith("/") ? frontendUrl : frontendUrl + "/";

            String content = String.format("""
                <div style="font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; padding: 40px 20px; max-width: 600px; margin: 0 auto; background-color: #f1f5f9;">
                    <div style="background-color: #ffffff; border-radius: 16px; overflow: hidden; box-shadow: 0 10px 20px rgba(0,0,0,0.08);">

                        <!-- Header -->
                        <div style="background: linear-gradient(135deg, #dc2626 0%%, #ef4444 100%%); padding: 32px 30px;">
                            <table width="100%%" cellpadding="0" cellspacing="0" border="0">
                                <tr>
                                    <td style="width: 64px; vertical-align: middle;">
                                        <div style="background-color: #ffffff; border-radius: 12px; padding: 8px; display: inline-block; box-shadow: 0 4px 12px rgba(0,0,0,0.1);">
                                            <img src="%s" alt="RCA Logo" style="width: 48px; height: 48px; display: block;" />
                                        </div>
                                    </td>
                                    <td style="width: 24px;"></td>
                                    <td style="vertical-align: middle;">
                                        <h1 style="margin: 0; font-size: 24px; color: #ffffff; font-weight: 700; letter-spacing: -0.5px;">Low Stock Alert</h1>
                                        <p style="margin: 4px 0 0; font-size: 13px; color: #fecaca; letter-spacing: 1.2px; text-transform: uppercase; font-weight: 500;">
                                            Action Required
                                        </p>
                                    </td>
                                </tr>
                            </table>
                        </div>

                        <!-- Body -->
                        <div style="padding: 36px 30px;">
                            <p style="color: #475569; line-height: 1.7; margin-top: 0;">
                                Hello Admin,
                            </p>

                            <p style="color: #475569; line-height: 1.7;">
                                The following items have fallen below their minimum stock levels and require restocking:
                            </p>

                            <table width="100%%" cellpadding="0" cellspacing="0" border="0" style="margin: 24px 0; font-size: 14px; color: #334155;">
                                <thead>
                                    <tr style="background-color: #f8fafc;">
                                        <th style="padding: 12px; text-align: left; font-weight: 600; border-bottom: 2px solid #e2e8f0;">Item</th>
                                        <th style="padding: 12px; text-align: center; font-weight: 600; border-bottom: 2px solid #e2e8f0;">Current</th>
                                        <th style="padding: 12px; text-align: center; font-weight: 600; border-bottom: 2px solid #e2e8f0;">Min Level</th>
                                        <th style="padding: 12px; text-align: center; font-weight: 600; border-bottom: 2px solid #e2e8f0;">Status</th>
                                    </tr>
                                </thead>
                                <tbody>
                                    %s
                                </tbody>
                            </table>

                            <div style="text-align: center; margin: 36px 0;">
                                <a href="%s" style="display: inline-block; background-color: #dc2626; color: #ffffff; padding: 14px 34px; text-decoration: none; border-radius: 10px; font-weight: 600; font-size: 16px; box-shadow: 0 8px 16px rgba(220,38,38,0.25);">
                                    View Inventory
                                </a>
                            </div>
                        </div>

                        <!-- Footer -->
                        <div style="background-color: #f8fafc; padding: 24px; text-align: center; border-top: 1px solid #e5e7eb;">
                            <p style="color: #94a3b8; font-size: 12px; margin: 0; line-height: 1.6;">
                                &copy; 2024 Rwanda Coding Academy. All rights reserved.
                            </p>
                        </div>
                    </div>
                </div>
                """, logoUrl, itemsHtml.toString(), dashboardLink);

            helper.setText(content, true);
            mailSender.send(message);

            log.info("Daily low stock alert sent to {}", adminEmail);

        } catch (MessagingException e) {
            log.error("Failed to send daily low stock alert", e);
        } catch (Exception e) {
            log.error("Unexpected error sending daily low stock alert", e);
        }
    }
}
