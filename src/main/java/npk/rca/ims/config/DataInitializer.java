package npk.rca.ims.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import npk.rca.ims.model.User;
import npk.rca.ims.repository.UserRepository;
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

    @Override
    public void run(String... args) throws Exception {
        // Default user credentials
        String defaultEmail = "ntarekayitare@gmail.com";
        String defaultPassword = "RcaIMS@1234.5";
        
        // Normalize email (lowercase, trim)
        defaultEmail = defaultEmail.trim().toLowerCase();
        
        // Check if default user already exists
        User existingUser = userRepository.findByEmail(defaultEmail).orElse(null);
        
        if (existingUser == null) {
            // Create default user with encrypted password
            User defaultUser = new User();
            defaultUser.setEmail(defaultEmail);
            String encryptedPassword = passwordEncoder.encode(defaultPassword);
            defaultUser.setPassword(encryptedPassword); // Encrypt password
            defaultUser.setRole("ADMIN");
            defaultUser.setEnabled(true);
            
            userRepository.save(defaultUser);
            
            log.info("==========================================");
            log.info("Default user created successfully!");
            log.info("Email: {}", defaultEmail);
            log.info("Password: {} (encrypted in database)", defaultPassword);
            log.info("Role: ADMIN");
            log.info("==========================================");
        } else {
            // Update the existing user's password to ensure it matches the expected default
            // This fixes issues where the DB has an old or different password
            String encryptedPassword = passwordEncoder.encode(defaultPassword);
            existingUser.setPassword(encryptedPassword);
            existingUser.setRole("ADMIN"); // Ensure role is ADMIN
            existingUser.setEnabled(true); // Ensure account is enabled
            
            userRepository.save(existingUser);
            
            log.info("==========================================");
            log.info("Default user updated successfully!");
            log.info("Email: {}", defaultEmail);
            log.info("Password: {} (reset to default)", defaultPassword);
            log.info("==========================================");
        }
    }
}
