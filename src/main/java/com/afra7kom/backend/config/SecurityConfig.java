package com.afra7kom.backend.config;

import com.afra7kom.backend.security.JwtAuthenticationEntryPoint;
import com.afra7kom.backend.security.JwtAuthenticationFilter;
import com.afra7kom.backend.security.JwtTokenProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.http.HttpMethod;

import java.util.Arrays;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
public class SecurityConfig {

    private final JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;
    private final JwtTokenProvider jwtTokenProvider;
    private final UserDetailsService userDetailsService;
    private final boolean debugEndpointsEnabled;

    public SecurityConfig(JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint, 
                         JwtTokenProvider jwtTokenProvider,
                         @Qualifier("customUserDetailsService") UserDetailsService userDetailsService,
                         @Value("${app.security.debug-endpoints-enabled:false}") boolean debugEndpointsEnabled) {
        this.jwtAuthenticationEntryPoint = jwtAuthenticationEntryPoint;
        this.jwtTokenProvider = jwtTokenProvider;
        this.userDetailsService = userDetailsService;
        this.debugEndpointsEnabled = debugEndpointsEnabled;
    }

    @Bean
    public JwtAuthenticationFilter jwtAuthenticationFilter(@Qualifier("customUserDetailsService") UserDetailsService userDetailsService) {
        return new JwtAuthenticationFilter(jwtTokenProvider, userDetailsService);
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authConfig) throws Exception {
        return authConfig.getAuthenticationManager();
    }

    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(userDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder());
        return authProvider;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .cors().and().csrf().disable()
            .exceptionHandling().authenticationEntryPoint(jwtAuthenticationEntryPoint)
            .and()
            .sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            .and()
            .authorizeHttpRequests(authz -> {
                authz.requestMatchers("/auth/**", "/api/auth/**").permitAll()
                .requestMatchers("/public/**", "/api/public/**").permitAll()
                .requestMatchers("/actuator/health").permitAll()
                .requestMatchers("/api/health", "/api/public/health").permitAll()
                .requestMatchers("/swagger-ui/**", "/v3/api-docs/**", "/swagger-ui.html").permitAll()
                .requestMatchers("/error").permitAll();

                if (debugEndpointsEnabled) {
                    authz.requestMatchers("/api/test/**").hasRole("ADMIN")
                        .requestMatchers("/api/performance-test/**").hasRole("ADMIN")
                        .requestMatchers("/api/performance/lists/**").hasRole("ADMIN")
                        .requestMatchers("/api/availability/debug/**").hasAnyRole("ADMIN", "MANAGER")
                        .requestMatchers("/api/notifications/debug/**").hasRole("ADMIN")
                        .requestMatchers("/api/admin/debug/**").hasRole("ADMIN");
                } else {
                    authz.requestMatchers(
                        "/api/test/**",
                        "/api/performance-test/**",
                        "/api/performance/lists/**",
                        "/api/availability/debug/**",
                        "/api/notifications/debug/**",
                        "/api/admin/debug/**"
                    ).denyAll();
                }

                authz.requestMatchers("/api/admin/public-test").denyAll()
                
                // Permettre l'accès public aux images uploadées
                .requestMatchers("/uploads/**").permitAll()
                
                // WebSocket endpoints - permettre l'accès sans authentification pour les tests
                .requestMatchers("/ws/**").permitAll()
                .requestMatchers("/ws/info").permitAll()
                
                // Endpoints publics pour le site web (GET seulement)
                .requestMatchers(HttpMethod.GET, "/api/packs/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/pack-details/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/materiels/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/categories/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/catalog/**").permitAll()
                .requestMatchers("/api/public/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/reservations/public/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/public/gallery/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/availability/**").permitAll()
                
                // Endpoints de réservation pour invités (POST pour créer, GET pour vérifier)
                .requestMatchers(HttpMethod.POST, "/api/reservations").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/reservations/check-availability").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/reservations/{id}").permitAll()
                
                // Endpoint de contact public (POST pour créer)
                .requestMatchers(HttpMethod.POST, "/api/contacts").permitAll()
                
                .requestMatchers("/api/admin/**").hasAnyRole("ADMIN", "AGENT")
                .requestMatchers("/api/gallery/**").hasAnyRole("ADMIN", "MANAGER", "AGENT")
                .requestMatchers("/api/manager/**").hasAnyRole("ADMIN", "MANAGER")
                .requestMatchers("/api/agent/**").hasAnyRole("ADMIN", "MANAGER", "AGENT")
                .requestMatchers("/api/client/**").hasAnyRole("ADMIN", "MANAGER", "AGENT", "CLIENT")
                .anyRequest().authenticated();
            })
            .addFilterBefore(jwtAuthenticationFilter(userDetailsService), UsernamePasswordAuthenticationFilter.class)
            .authenticationProvider(authenticationProvider());

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        
        // Autoriser toutes les origines temporairement pour le développement
        configuration.setAllowedOriginPatterns(Arrays.asList("*"));
        
        configuration.setAllowedMethods(Arrays.asList(
            "GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH", "HEAD"
        ));
        
        configuration.setAllowedHeaders(Arrays.asList(
            "Authorization", "Content-Type", "X-Requested-With", 
            "Accept", "Origin", "Access-Control-Request-Method", 
            "Access-Control-Request-Headers", "Cache-Control",
            "Content-Disposition", "Content-Length"
        ));
        
        configuration.setExposedHeaders(Arrays.asList(
            "Access-Control-Allow-Origin", "Access-Control-Allow-Credentials"
        ));
        
        configuration.setAllowCredentials(false); // Désactivé temporairement pour permettre *
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
