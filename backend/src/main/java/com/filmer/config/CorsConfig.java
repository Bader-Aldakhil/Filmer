package com.filmer.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * CORS (Cross-Origin Resource Sharing) configuration.
 * Allows the Angular frontend running on localhost:4200 to make requests
 * to the Spring Boot backend running on localhost:8080.
 */
@Configuration
public class CorsConfig implements WebMvcConfigurer {

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOrigins(
                    "https://localhost:4200",     // Angular dev server with SSL
                    "http://localhost:4200",      // Angular dev server
                    "http://127.0.0.1:4200",      // Alternative localhost
                    "http://localhost:3000"       // Alternative port if needed
                )
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH")
                .allowedHeaders("*")
                .allowCredentials(true)
                .maxAge(3600);
    }
}
