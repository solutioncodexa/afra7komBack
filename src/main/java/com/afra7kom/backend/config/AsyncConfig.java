package com.afra7kom.backend.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

@Configuration
@EnableAsync
public class AsyncConfig {
    
    /**
     * Configuration du pool de threads pour les tâches asynchrones
     * Utilisé pour les notifications (email, WhatsApp) qui ne doivent pas bloquer les requêtes HTTP
     */
    @Bean(name = "taskExecutor")
    public Executor taskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        
        // Nombre de threads de base (augmenté pour meilleure performance)
        executor.setCorePoolSize(10);
        
        // Nombre maximum de threads (augmenté)
        executor.setMaxPoolSize(20);
        
        // Capacité de la file d'attente
        executor.setQueueCapacity(200);
        
        // Préfixe du nom des threads (facilite le debugging)
        executor.setThreadNamePrefix("async-");
        
        // Permettre le timeout des threads inactifs
        executor.setAllowCoreThreadTimeOut(true);
        executor.setKeepAliveSeconds(60);
        
        // Initialiser l'executor
        executor.initialize();
        
        return executor;
    }
}

