package com.pulsepass.security;

import io.jsonwebtoken.JwtException;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.http.HttpHeaders;

import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;

import org.springframework.stereotype.Component;

import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

@Component
public class JwtAuthenticationFilter
        extends OncePerRequestFilter {

    private static final String BEARER_PREFIX =
            "Bearer ";

    private final JwtService jwtService;
    private final CustomUserDetailsService userDetailsService;
    private final RestAuthenticationEntryPoint authenticationEntryPoint;

    public JwtAuthenticationFilter(
            JwtService jwtService,
            CustomUserDetailsService userDetailsService,
            RestAuthenticationEntryPoint authenticationEntryPoint
    ) {
        this.jwtService =
                jwtService;

        this.userDetailsService =
                userDetailsService;

        this.authenticationEntryPoint =
                authenticationEntryPoint;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        Authentication existingAuthentication =
                SecurityContextHolder
                        .getContext()
                        .getAuthentication();

        if (existingAuthentication != null
                && existingAuthentication.isAuthenticated()) {

            filterChain.doFilter(
                    request,
                    response
            );

            return;
        }

        List<String> authorizationHeaders =
                Collections.list(
                        request.getHeaders(
                                HttpHeaders.AUTHORIZATION
                        )
                );

        if (authorizationHeaders.size() > 1) {

            rejectUnauthorized(
                    request,
                    response,
                    new BadCredentialsException(
                            "Multiple Authorization headers are not allowed"
                    )
            );

            return;
        }

        if (authorizationHeaders.isEmpty()) {

            filterChain.doFilter(
                    request,
                    response
            );

            return;
        }

        String authorizationHeader =
                authorizationHeaders.getFirst();

        if (authorizationHeader == null
                || authorizationHeader.isBlank()) {

            filterChain.doFilter(
                    request,
                    response
            );

            return;
        }

        if (!authorizationHeader.startsWith(
                BEARER_PREFIX
        )) {

            filterChain.doFilter(
                    request,
                    response
            );

            return;
        }

        String token =
                authorizationHeader
                        .substring(
                                BEARER_PREFIX.length()
                        )
                        .trim();

        try {

            Long userId =
                    jwtService.extractUserId(
                            token
                    );

            SecurityPrincipal principal =
                    userDetailsService
                            .loadUserById(
                                    userId
                            );

            if (!principal.isEnabled()) {
                throw new DisabledException(
                        "User is disabled"
                );
            }

            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(
                            principal,
                            null,
                            principal.getAuthorities()
                    );

            authentication.setDetails(
                    new WebAuthenticationDetailsSource()
                            .buildDetails(request)
            );

            SecurityContext context =
                    SecurityContextHolder
                            .createEmptyContext();

            context.setAuthentication(
                    authentication
            );

            SecurityContextHolder.setContext(
                    context
            );

            filterChain.doFilter(
                    request,
                    response
            );

        } catch (
                JwtException
                | IllegalArgumentException ex
        ) {

            rejectUnauthorized(
                    request,
                    response,
                    new BadCredentialsException(
                            "Invalid bearer token",
                            ex
                    )
            );

        } catch (
                AuthenticationException ex
        ) {

            rejectUnauthorized(
                    request,
                    response,
                    ex
            );
        }
    }

    private void rejectUnauthorized(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException exception
    ) throws IOException, ServletException {

        SecurityContextHolder
                .clearContext();

        authenticationEntryPoint.commence(
                request,
                response,
                exception
        );
    }
}