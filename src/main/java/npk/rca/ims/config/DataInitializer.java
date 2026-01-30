package npk.rca.ims.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import npk.rca.ims.model.User;
import npk.rca.ims.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
@RequiredArgsConstructor
@Slf4j
public class DataInitializer {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Bean
    public CommandLineRunner initData() {
        return args -> {
            String adminEmail = "ntarekayitare@gmail.com";
            
            if (userRepository.findByEmail(adminEmail).isEmpty()) {
                log.info("Creating default admin user...");
                User admin = new User();
                admin.setEmail(adminEmail);
                admin.setPassword(passwordEncoder.encode("RcaIMS@1234.5"));
                admin.setRole("ADMIN");
                admin.setName("Default Admin");
                admin.setEnabled(true);
                admin.setEmailNotifications(true);
                admin.setTheme("LIGHT");
                admin.setLanguage("en");
                
                userRepository.save(admin);
                log.info("Default admin user created successfully.");
            } else {
                log.info("Default admin user already exists.");
            }
        };
    }
}
