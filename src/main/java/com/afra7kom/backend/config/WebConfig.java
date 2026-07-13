package com.afra7kom.backend.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Value("${file.upload-dir:./uploads/}")
    private String uploadDir;

    @Value("${security.cors.allowed-origins:https://afra7kom.ma,https://www.afra7kom.ma,http://localhost:4200}")
    private String[] allowedOrigins;

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        // Configuration CORS globale pour toutes les API
        registry.addMapping("/**")
                .allowedOrigins(allowedOrigins)
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH", "HEAD")
                .allowedHeaders("*")
                .allowCredentials(true)
                .maxAge(3600);
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // Servir les fichiers uploadés depuis le répertoire uploads
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations("file:" + uploadDir);
    }
    
    /**
     * Configuration CORS supplémentaire pour les fichiers uploads
     * Permet l'accès public aux images sans credentials
     */
    @Bean
    public WebMvcConfigurer corsConfigurerForUploads() {
        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(CorsRegistry registry) {
                registry.addMapping("/uploads/**")
                    .allowedOrigins(allowedOrigins)
                    .allowedMethods("GET", "HEAD", "OPTIONS")
                    .allowedHeaders("*")
                    .allowCredentials(false)
                    .maxAge(7200); // Cache plus long pour les images (2h)
            }
        };
    }
}


