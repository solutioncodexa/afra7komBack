package com.afra7kom.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {
    
    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        // Activer un broker de messages simple en mémoire
        config.enableSimpleBroker("/topic", "/queue");
        
        // Préfixe pour les destinations des messages envoyés depuis le client
        config.setApplicationDestinationPrefixes("/app");
        
        // Préfixe pour les destinations des messages envoyés aux utilisateurs spécifiques
        config.setUserDestinationPrefix("/user");
    }
    
    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // Endpoint WebSocket pour les connexions STOMP
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns("*") // En production, spécifiez les domaines autorisés
                .withSockJS(); // Support pour les clients qui ne supportent pas WebSocket
        
        // Endpoint WebSocket simple (sans SockJS)
        registry.addEndpoint("/ws-simple")
                .setAllowedOriginPatterns("*");
    }
}











