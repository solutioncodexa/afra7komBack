package com.afra7kom.backend.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

@Configuration
@EnableCaching
public class CacheConfig {

    /**
     * Configuration du Cache Manager avec Caffeine
     */
    @Bean
    public CacheManager cacheManager() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager();
        
        // Configuration Caffeine
        cacheManager.setCaffeine(Caffeine.newBuilder()
                .maximumSize(1000)                    // Maximum 1000 entrées
                .expireAfterWrite(5, TimeUnit.MINUTES) // Expire après 5 minutes
                .expireAfterAccess(2, TimeUnit.MINUTES) // Expire après 2 minutes d'inactivité
                .recordStats()                        // Activer les statistiques
        );
        
        return cacheManager;
    }
}