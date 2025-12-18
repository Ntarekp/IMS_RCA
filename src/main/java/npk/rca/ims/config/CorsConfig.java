package npk.rca.ims.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.filter.CorsFilter;

import java.util.Arrays;

@Configuration
public class CorsConfig {


    @Bean
    public CorsFilter corsFilter() {
        CorsConfiguration config = new CorsConfiguration();

        //Allow requests from frontend!
        config.setAllowedOrigins(Arrays.asList("http://localhost:3000"));

        // Allow all HTTP methods (Get, POST,put,Delete,...)
        config.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));

        //Allow all headers
        config.setAllowCredentials(Arrays.asList("*"));

    }
}
