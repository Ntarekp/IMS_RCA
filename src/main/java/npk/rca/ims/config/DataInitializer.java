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
            log.info("Encrypted password hash: {}", encryptedPassword);
            log.info("Role: ADMIN");
            log.info("==========================================");
        } else {
            // User exists - verify and update password if needed
            boolean passwordMatches = passwordEncoder.matches(defaultPassword, existingUser.getPassword());
            
            if (!passwordMatches) {
                log.warn("==========================================");
                log.warn("Existing user password doesn't match!");
                log.warn("Updating password for user: {}", defaultEmail);
                log.warn("Old password hash: {}", existingUser.getPassword());
                
                // Force update password
                String newEncryptedPassword = passwordEncoder.encode(defaultPassword);
                existingUser.setPassword(newEncryptedPassword);
                existingUser.setRole("ADMIN"); // Ensure role is correct
                existingUser.setEnabled(true); // Ensure enabled
                userRepository.save(existingUser);
                
                log.warn("New password hash: {}", newEncryptedPassword);
                log.warn("Password updated successfully!");
                log.warn("==========================================");
            } else {
                log.info("Default user already exists: {} (password verified)", defaultEmail);
            }
            
            // Verify user is enabled and has correct role
            if (!existingUser.isEnabled() || !"ADMIN".equals(existingUser.getRole())) {
                log.warn("Updating user settings: enabled={}, role={}", existingUser.isEnabled(), existingUser.getRole());
                existingUser.setEnabled(true);
                existingUser.setRole("ADMIN");
                userRepository.save(existingUser);
                log.info("User settings updated");
            }
        }
    }
}

