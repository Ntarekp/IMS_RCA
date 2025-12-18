package npk.rca.ims.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

import java.util.Arrays;

@Configuration
public class CorsConfig {


    @Bean
    public CorsFilter corsFilter() {
        CorsConfiguration config = new CorsConfiguration();


        // Allow all origins for development. Change to specific domains in production!
        config.setAllowedOriginPatterns(Arrays.asList("*"));

        // Allow all HTTP methods (Get, POST,put,Delete,...)
        config.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));

        //Allow all headers
        config.setAllowedHeaders(Arrays.asList("*"));

        //Allow credentials(cookies,authorization headers)
        config.setAllowCredentials(true);

        //Cache response period
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", config);

        return new CorsFilter(source);


    }
}
