package com.restaurant.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

    @Bean
    UserDetailsService managerUser(
            @Value("${MANAGER_USERNAME:manager}") String username,
            @Value("${MANAGER_PASSWORD:change-me-now}") String password) {
        return new InMemoryUserDetailsManager(
                User.withUsername(username)
                        .password("{noop}" + password)
                        .roles("MANAGER")
                        .build());
    }

    @Bean
    SecurityFilterChain applicationSecurity(HttpSecurity http) throws Exception {
        return http
                .cors(Customizer.withDefaults())
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/public/**", "/actuator/health").permitAll()
                        .anyRequest().hasRole("MANAGER"))
                .httpBasic(Customizer.withDefaults())
                .build();
    }
}
