package com.afra7kom.backend.config;

import com.afra7kom.backend.security.JwtAuthenticationEntryPoint;
import com.afra7kom.backend.security.JwtAuthenticationFilter;
import com.afra7kom.backend.security.JwtTokenProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
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
import java.util.List;
import java.util.stream.Collectors;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
public class SecurityConfig {

    private final JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;
    private final JwtTokenProvider jwtTokenProvider;
    private final UserDetailsService userDetailsService;
    private final boolean debugEndpointsEnabled;
    private final List<String> corsAllowedOrigins;

    public SecurityConfig(JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint,
                         JwtTokenProvider jwtTokenProvider,
                         @Qualifier("customUserDetailsService") UserDetailsService userDetailsService,
                         @Value("${app.security.debug-endpoints-enabled:false}") boolean debugEndpointsEnabled,
                         @Value("${security.cors.allowed-origins:https://afra7kom.ma,https://www.afra7kom.ma,http://localhost:4200}") String corsAllowedOrigins) {
        this.jwtAuthenticationEntryPoint = jwtAuthenticationEntryPoint;
        this.jwtTokenProvider = jwtTokenProvider;
        this.userDetailsService = userDetailsService;
        this.debugEndpointsEnabled = debugEndpointsEnabled;
        this.corsAllowedOrigins = Arrays.stream(corsAllowedOrigins.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toList());
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

    /**
     * Chaîne dédiée aux APIs publiques — sans JWT, sans 401.
     * Priorité haute pour éviter que la chaîne principale bloque gallery/site-settings.
     */
    @Bean
    @Order(1)
    public SecurityFilterChain publicApiFilterChain(HttpSecurity http) throws Exception {
        http
            .securityMatcher(
                "/api/public/**",
                "/public/**",
                "/api/health",
                "/actuator/health",
                "/uploads/**",
                "/error"
            )
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .csrf(csrf -> csrf.disable())
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(authz -> authz.anyRequest().permitAll());

        return http.build();
    }

    @Bean
    @Order(2)
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .csrf(csrf -> csrf.disable())
            .exceptionHandling(ex -> ex.authenticationEntryPoint(jwtAuthenticationEntryPoint))
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(authz -> {
                authz.requestMatchers("/auth/**", "/api/auth/**").permitAll()
                .requestMatchers("/swagger-ui/**", "/v3/api-docs/**", "/swagger-ui.html").permitAll();

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
                .requestMatchers("/ws/**", "/ws/info").permitAll()

                // GET publics catalogue / packs / matériels
                .requestMatchers(HttpMethod.GET, "/api/packs/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/pack-details/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/materiels/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/categories/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/catalog/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/reservations/public/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/availability/**").permitAll()

                // Réservations / contacts invités
                .requestMatchers(HttpMethod.POST, "/api/reservations").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/reservations/check-availability").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/reservations/{id}").permitAll()
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

    /**
     * Source CORS unique (ne pas aussi déclarer CORS dans WebMvc — sinon ACAO dupliqué
     * ex. {@code https://afra7kom.ma,https://afra7kom.ma} → erreur CORS navigateur).
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(corsAllowedOrigins);
        configuration.setAllowedMethods(Arrays.asList(
            "GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH", "HEAD"
        ));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setExposedHeaders(Arrays.asList(
            "Authorization", "Content-Disposition", "Content-Type"
        ));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
