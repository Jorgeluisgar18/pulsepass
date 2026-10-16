package com.pulsepass.security;

import io.jsonwebtoken.MalformedJwtException;

import jakarta.servlet.FilterChain;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.api.extension.ExtendWith;

import org.springframework.http.HttpHeaders;

import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterTest {

    @Mock
    private JwtService jwtService;

    @Mock
    private CustomUserDetailsService userDetailsService;

    @Mock
    private RestAuthenticationEntryPoint authenticationEntryPoint;

    @Mock
    private FilterChain filterChain;

    private JwtAuthenticationFilter filter;

    @BeforeEach
    void setUp() {

        filter =
                new JwtAuthenticationFilter(
                        jwtService,
                        userDetailsService,
                        authenticationEntryPoint
                );

        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void cleanSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void shouldContinueWhenAuthorizationHeaderIsMissing()
            throws Exception {

        MockHttpServletRequest request =
                new MockHttpServletRequest();

        MockHttpServletResponse response =
                new MockHttpServletResponse();

        filter.doFilter(
                request,
                response,
                filterChain
        );

        verify(filterChain)
                .doFilter(
                        request,
                        response
                );

        verifyNoInteractions(
                jwtService,
                userDetailsService,
                authenticationEntryPoint
        );

        assertThat(
                SecurityContextHolder
                        .getContext()
                        .getAuthentication()
        ).isNull();
    }

    @Test
    void shouldAuthenticateValidBearerToken()
            throws Exception {

        MockHttpServletRequest request =
                new MockHttpServletRequest();

        request.addHeader(
                HttpHeaders.AUTHORIZATION,
                "Bearer valid-token"
        );

        MockHttpServletResponse response =
                new MockHttpServletResponse();

        SecurityPrincipal principal =
                activePrincipal();

        when(
                jwtService.extractUserId(
                        "valid-token"
                )
        ).thenReturn(
                1L
        );

        when(
                userDetailsService.loadUserById(
                        1L
                )
        ).thenReturn(
                principal
        );

        filter.doFilter(
                request,
                response,
                filterChain
        );

        Authentication authentication =
                SecurityContextHolder
                        .getContext()
                        .getAuthentication();

        assertThat(authentication)
                .isNotNull();

        assertThat(
                authentication.getPrincipal()
        ).isSameAs(
                principal
        );

        assertThat(
                authentication.isAuthenticated()
        ).isTrue();

        assertThat(
                authentication.getAuthorities()
        )
                .extracting("authority")
                .containsExactly(
                        "ROLE_USER"
                );

        verify(filterChain)
                .doFilter(
                        request,
                        response
                );

        verifyNoInteractions(
                authenticationEntryPoint
        );
    }

    @Test
    void shouldRejectInvalidBearerToken()
            throws Exception {

        MockHttpServletRequest request =
                new MockHttpServletRequest();

        request.addHeader(
                HttpHeaders.AUTHORIZATION,
                "Bearer invalid-token"
        );

        MockHttpServletResponse response =
                new MockHttpServletResponse();

        when(
                jwtService.extractUserId(
                        "invalid-token"
                )
        ).thenThrow(
                new MalformedJwtException(
                        "Invalid JWT"
                )
        );

        filter.doFilter(
                request,
                response,
                filterChain
        );

        verify(
                filterChain,
                never()
        ).doFilter(
                request,
                response
        );

        verify(authenticationEntryPoint)
                .commence(
                        any(),
                        any(),
                        any()
                );

        assertThat(
                SecurityContextHolder
                        .getContext()
                        .getAuthentication()
        ).isNull();
    }

    @Test
    void shouldRejectInactiveUserEvenWithValidToken()
            throws Exception {

        MockHttpServletRequest request =
                new MockHttpServletRequest();

        request.addHeader(
                HttpHeaders.AUTHORIZATION,
                "Bearer valid-token"
        );

        MockHttpServletResponse response =
                new MockHttpServletResponse();

        SecurityPrincipal principal =
                inactivePrincipal();

        when(
                jwtService.extractUserId(
                        "valid-token"
                )
        ).thenReturn(
                5L
        );

        when(
                userDetailsService.loadUserById(
                        5L
                )
        ).thenReturn(
                principal
        );

        filter.doFilter(
                request,
                response,
                filterChain
        );

        verify(
                filterChain,
                never()
        ).doFilter(
                request,
                response
        );

        verify(authenticationEntryPoint)
                .commence(
                        any(),
                        any(),
                        any()
                );

        assertThat(
                SecurityContextHolder
                        .getContext()
                        .getAuthentication()
        ).isNull();
    }

    @Test
    void shouldRejectMultipleAuthorizationHeaders()
            throws Exception {

        MockHttpServletRequest request =
                new MockHttpServletRequest();

        request.addHeader(
                HttpHeaders.AUTHORIZATION,
                "Bearer first-token"
        );

        request.addHeader(
                HttpHeaders.AUTHORIZATION,
                "Bearer second-token"
        );

        MockHttpServletResponse response =
                new MockHttpServletResponse();

        filter.doFilter(
                request,
                response,
                filterChain
        );

        verify(
                filterChain,
                never()
        ).doFilter(
                request,
                response
        );

        verify(authenticationEntryPoint)
                .commence(
                        any(),
                        any(),
                        any()
                );

        verifyNoInteractions(
                jwtService,
                userDetailsService
        );

        assertThat(
                SecurityContextHolder
                        .getContext()
                        .getAuthentication()
        ).isNull();
    }

    @Test
    void shouldNotOverwriteExistingAuthentication()
            throws Exception {

        MockHttpServletRequest request =
                new MockHttpServletRequest();

        request.addHeader(
                HttpHeaders.AUTHORIZATION,
                "Bearer another-token"
        );

        MockHttpServletResponse response =
                new MockHttpServletResponse();

        Authentication existingAuthentication =
                new UsernamePasswordAuthenticationToken(
                        "existing-user",
                        null,
                        List.of(
                                new SimpleGrantedAuthority(
                                        "ROLE_ADMIN"
                                )
                        )
                );

        SecurityContextHolder
                .getContext()
                .setAuthentication(
                        existingAuthentication
                );

        filter.doFilter(
                request,
                response,
                filterChain
        );

        verify(filterChain)
                .doFilter(
                        request,
                        response
                );

        verifyNoInteractions(
                jwtService,
                userDetailsService,
                authenticationEntryPoint
        );

        assertThat(
                SecurityContextHolder
                        .getContext()
                        .getAuthentication()
        ).isSameAs(
                existingAuthentication
        );
    }

    private SecurityPrincipal activePrincipal() {

        return new SecurityPrincipal(
                1L,
                "andrea",
                "andrea@example.com",
                "$2a$10$hash",
                true,
                List.of(
                        new SimpleGrantedAuthority(
                                "ROLE_USER"
                        )
                )
        );
    }

    private SecurityPrincipal inactivePrincipal() {

        return new SecurityPrincipal(
                5L,
                "inactive",
                "inactive@example.com",
                "$2a$10$hash",
                false,
                List.of(
                        new SimpleGrantedAuthority(
                                "ROLE_USER"
                        )
                )
        );
    }
}