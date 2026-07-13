package com.afra7kom.backend.service;

import com.afra7kom.backend.entity.User;
import com.afra7kom.backend.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.stream.Collectors;

@Service("customUserDetailsService")
public class CustomUserDetailsService implements UserDetailsService {

    private static final Logger logger = LoggerFactory.getLogger(CustomUserDetailsService.class);
    private final UserRepository userRepository;

    public CustomUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        logger.debug("CustomUserDetailsService - Loading user by email: {}", email);
        
        // Utiliser la méthode avec JOIN FETCH pour éviter les problèmes de lazy loading
        User user = userRepository.findByEmailWithRolesAndPermissions(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with email: " + email));
        
        logger.debug("CustomUserDetailsService - User found: {}", user.getEmail());
        logger.debug("CustomUserDetailsService - User roles count: {}", user.getRoles().size());
        logger.debug("CustomUserDetailsService - User authorities: {}", user.getAuthorities());
        
        return new UserDetailsWrapper(user);
    }

    private static class UserDetailsWrapper implements UserDetails {
        private final User user;

        public UserDetailsWrapper(User user) {
            this.user = user;
        }

        @Override
        public Collection<? extends GrantedAuthority> getAuthorities() {
            Collection<? extends GrantedAuthority> authorities = user.getAuthorities();
            logger.debug("UserDetailsWrapper - Authorities for user {}: {}", user.getEmail(), authorities);
            return authorities;
        }

        @Override
        public String getPassword() {
            return user.getPassword();
        }

        @Override
        public String getUsername() {
            return user.getUsername();
        }

        @Override
        public boolean isAccountNonExpired() {
            return user.isAccountNonExpired();
        }

        @Override
        public boolean isAccountNonLocked() {
            return user.isAccountNonLocked();
        }

        @Override
        public boolean isCredentialsNonExpired() {
            return user.isCredentialsNonExpired();
        }

        @Override
        public boolean isEnabled() {
            return user.isEnabled();
        }
    }
}
