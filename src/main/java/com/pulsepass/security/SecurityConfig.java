package com.pulsepass.security;

import org.springframework.boot.context.properties.EnableConfigurationProperties;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import org.springframework.http.HttpMethod;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;

import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;

import org.springframework.security.config.http.SessionCreationPolicy;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@EnableConfigurationProperties(
        JwtProperties.class
)
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {

        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(
            CustomUserDetailsService userDetailsService,
            PasswordEncoder passwordEncoder
    ) {

        DaoAuthenticationProvider provider =
                new DaoAuthenticationProvider(
                        userDetailsService
                );

        provider.setPasswordEncoder(
                passwordEncoder
        );

        return new ProviderManager(
                provider
        );
    }

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            JwtAuthenticationFilter jwtAuthenticationFilter,
            RestAuthenticationEntryPoint authenticationEntryPoint,
            RestAccessDeniedHandler accessDeniedHandler
    ) throws Exception {

        http
                .csrf(
                        AbstractHttpConfigurer::disable
                )

                .sessionManagement(
                        session ->
                                session.sessionCreationPolicy(
                                        SessionCreationPolicy.STATELESS
                                )
                )

                .formLogin(
                        AbstractHttpConfigurer::disable
                )

                .httpBasic(
                        AbstractHttpConfigurer::disable
                )

                .exceptionHandling(
                        exceptions ->
                                exceptions
                                        .authenticationEntryPoint(
                                                authenticationEntryPoint
                                        )
                                        .accessDeniedHandler(
                                                accessDeniedHandler
                                        )
                )

                .authorizeHttpRequests(
                        auth -> auth

                                // =============================
                                // AUTH PUBLICO
                                // =============================

                                .requestMatchers(
                                        HttpMethod.POST,
                                        "/api/auth/login"
                                )
                                .permitAll()

                                .requestMatchers(
                                        HttpMethod.POST,
                                        "/api/users"
                                )
                                .permitAll()

                                // =============================
                                // VENUES PUBLICOS
                                // =============================

                                .requestMatchers(
                                        HttpMethod.GET,
                                        "/api/venues/**"
                                )
                                .permitAll()

                                // =============================
                                // ARTISTS PUBLICOS
                                // =============================

                                .requestMatchers(
                                        HttpMethod.GET,
                                        "/api/artists/**"
                                )
                                .permitAll()

                                // =============================
                                // EVENTOS PUBLICOS SEGUROS
                                // =============================

                                .requestMatchers(
                                        HttpMethod.GET,
                                        "/api/events/published"
                                )
                                .permitAll()

                                .requestMatchers(
                                        HttpMethod.GET,
                                        "/api/events/by-artist"
                                )
                                .permitAll()

                                /*
                                 * El Service controla que anónimo
                                 * o USER solo vea PUBLISHED.
                                 */
                                .requestMatchers(
                                        HttpMethod.GET,
                                        "/api/events/*"
                                )
                                .permitAll()

                                // =============================
                                // ADMINISTRACION DE EVENTOS
                                // =============================

                                .requestMatchers(
                                        HttpMethod.POST,
                                        "/api/events"
                                )
                                .hasRole(
                                        "ADMIN"
                                )

                                .requestMatchers(
                                        HttpMethod.PATCH,
                                        "/api/events/*/publish"
                                )
                                .hasRole(
                                        "ADMIN"
                                )

                                .requestMatchers(
                                        HttpMethod.POST,
                                        "/api/events/*/artists/*"
                                )
                                .hasRole(
                                        "ADMIN"
                                )

                                // =============================
                                // OPERACION DE TICKETS
                                // =============================

                                .requestMatchers(
                                        HttpMethod.PATCH,
                                        "/api/tickets/*/use"
                                )
                                .hasAnyRole(
                                        "STAFF",
                                        "ADMIN"
                                )

                                .requestMatchers(
                                        HttpMethod.GET,
                                        "/api/events/*/tickets/paid"
                                )
                                .hasAnyRole(
                                        "STAFF",
                                        "ADMIN"
                                )

                                // =============================
                                // COMPRA DE TICKETS
                                // =============================

                                .requestMatchers(
                                        HttpMethod.POST,
                                        "/api/tickets"
                                )
                                .hasRole(
                                        "USER"
                                )

                                // =============================
                                // CANCELACION
                                // Ownership adicional en Service
                                // =============================

                                .requestMatchers(
                                        HttpMethod.PATCH,
                                        "/api/tickets/*/cancel"
                                )
                                .hasAnyRole(
                                        "USER",
                                        "ADMIN"
                                )

                                // =============================
                                // DATOS PERSONALES
                                // Ownership adicional en Service
                                // =============================

                                .requestMatchers(
                                        HttpMethod.GET,
                                        "/api/users/by-email",
                                        "/api/users/by-username"
                                )
                                .hasAnyRole(
                                        "USER",
                                        "ADMIN"
                                )

                                // =============================
                                // RESTO DE LA API
                                // =============================

                                .anyRequest()
                                .authenticated()
                )

                .addFilterBefore(
                        jwtAuthenticationFilter,
                        UsernamePasswordAuthenticationFilter.class
                );

        return http.build();
    }
}