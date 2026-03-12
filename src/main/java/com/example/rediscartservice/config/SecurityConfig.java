package com.example.rediscartservice.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Security configuration for the cart microservice.
 *
 * This service uses Redis session-IDs in URL paths as the identity mechanism —
 * not user-level HTTP authentication. All endpoints are intentionally open.
 * Spring Security is still on the classpath for security headers
 * (X-Content-Type-Options, X-Frame-Options, etc.).
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            // Disable CSRF — REST API with JSON, no browser form submissions
            .csrf(AbstractHttpConfigurer::disable)

            // Stateless — session IDs are in the URL path, not HTTP sessions
            .sessionManagement(sm ->
                sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

            // Security headers
            .headers(headers -> headers
                .frameOptions(frame -> frame.deny())
                .contentTypeOptions(Customizer.withDefaults())
            )

            // Permit ALL requests — cart API and actuator endpoints are all public.
            // Authentication is enforced upstream at the API gateway / JWT layer.
            .authorizeHttpRequests(auth -> auth.anyRequest().permitAll())

            // Disable HTTP Basic Auth — no login prompts
            .httpBasic(AbstractHttpConfigurer::disable)

            // Disable form login
            .formLogin(AbstractHttpConfigurer::disable);

        return http.build();
    }
}
