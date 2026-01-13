package npk.rca.ims.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import npk.rca.ims.model.User;
import npk.rca.ims.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * DataInitializer - Creates default user on application startup
 * Password is encrypted using BCrypt
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.admin.default-email:ntarekayitare@gmail.com}")
    private String defaultEmail;

    @Value("${app.admin.default-password:RcaIMS@1234.5}")
    private String defaultPassword;

    @Override
    public void run(String... args) throws Exception {
        // Normalize email (lowercase, trim)
        String email = defaultEmail.trim().toLowerCase();
        
        // Check if default user already exists
        User existingUser = userRepository.findByEmail(email).orElse(null);
        
        if (existingUser == null) {
            // Create default user with encrypted password
            User defaultUser = new User();
            defaultUser.setEmail(email);
            // Set system email
            defaultUser.setSystemEmail(email);
            String encryptedPassword = passwordEncoder.encode(defaultPassword);
            defaultUser.setPassword(encryptedPassword); // Encrypt password
            defaultUser.setRole("ADMIN");
            defaultUser.setEnabled(true);
            
            userRepository.save(defaultUser);
            
            log.info("==========================================");
            log.info("Default user created successfully!");
            log.info("Email: {}", email);
            log.info("Password: {} (encrypted in database)", defaultPassword);
            log.info("Role: ADMIN");
            log.info("==========================================");
        } else {
            // Do not reset password for existing users in production
            // Just ensure the role is correct if needed, or do nothing
            if (!"ADMIN".equals(existingUser.getRole())) {
                existingUser.setRole("ADMIN");
                userRepository.save(existingUser);
                log.info("Updated existing default user role to ADMIN");
            }
            
            log.info("Default user already exists. Skipping creation.");
        }
    }
}
