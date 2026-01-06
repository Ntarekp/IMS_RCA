package npk.rca.ims.config;

import org.springframework.context.annotation.Configuration;

// This class is deprecated in favor of CORS configuration in SecurityConfig.java
// Keeping the file but removing the bean to avoid conflicts.
@Configuration
public class CorsConfig {
    // CorsFilter bean removed to prevent double CORS handling
}
