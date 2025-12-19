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
        
        // Check if default user already exists
        if (!userRepository.existsByEmail(defaultEmail)) {
            // Create default user with encrypted password
            User defaultUser = new User();
            defaultUser.setEmail(defaultEmail);
            defaultUser.setPassword(passwordEncoder.encode(defaultPassword)); // Encrypt password
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
            log.info("Default user already exists: {}", defaultEmail);
        }
    }
}

