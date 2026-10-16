package com.pulsepass.controller;

import com.pulsepass.dto.request.LoginRequest;
import com.pulsepass.dto.response.AuthResponse;
import com.pulsepass.dto.response.AuthenticatedUserResponse;

import com.pulsepass.exception.GlobalExceptionHandler;

import com.pulsepass.security.CustomUserDetailsService;
import com.pulsepass.security.JwtAuthenticationFilter;
import com.pulsepass.security.JwtService;
import com.pulsepass.security.RestAccessDeniedHandler;
import com.pulsepass.security.RestAuthenticationEntryPoint;
import com.pulsepass.security.SecurityConfig;
import com.pulsepass.security.SecurityPrincipal;

import com.pulsepass.service.AuthService;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;

import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthController.class)
@Import({
        SecurityConfig.class,
        JwtAuthenticationFilter.class,
        RestAuthenticationEntryPoint.class,
        RestAccessDeniedHandler.class,
        GlobalExceptionHandler.class
})
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AuthService authService;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private CustomUserDetailsService userDetailsService;

    @Test
    void shouldLoginAndReturnBearerToken()
            throws Exception {

        when(
                authService.login(
                        any(LoginRequest.class)
                )
        ).thenReturn(
                new AuthResponse(
                        "signed.jwt.token",
                        "Bearer",
                        900L
                )
        );

        String requestBody = """
                {
                  "identifier": "andrea",
                  "password": "UnaFraseSeguraLarga2026!"
                }
                """;

        mockMvc.perform(
                        post(
                                "/api/auth/login"
                        )
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content(requestBody)
                )
                .andExpect(
                        status().isOk()
                )
                .andExpect(
                        jsonPath("$.accessToken")
                                .value(
                                        "signed.jwt.token"
                                )
                )
                .andExpect(
                        jsonPath("$.tokenType")
                                .value("Bearer")
                )
                .andExpect(
                        jsonPath("$.expiresIn")
                                .value(900)
                );

        verify(authService)
                .login(
                        any(LoginRequest.class)
                );
    }

    @Test
    void shouldReturn401ForInvalidCredentials()
            throws Exception {

        when(
                authService.login(
                        any(LoginRequest.class)
                )
        ).thenThrow(
                new BadCredentialsException(
                        "Bad credentials"
                )
        );

        String requestBody = """
                {
                  "identifier": "andrea",
                  "password": "incorrecta"
                }
                """;

        mockMvc.perform(
                        post(
                                "/api/auth/login"
                        )
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content(requestBody)
                )
                .andExpect(
                        status().isUnauthorized()
                )
                .andExpect(
                        jsonPath("$.status")
                                .value(401)
                )
                .andExpect(
                        jsonPath("$.message")
                                .value(
                                        "Invalid credentials"
                                )
                );
    }

    @Test
    void shouldReturnCurrentAuthenticatedUser()
            throws Exception {

        SecurityPrincipal principal =
                principal();

        AuthenticatedUserResponse response =
                new AuthenticatedUserResponse(
                        1L,
                        "andrea",
                        "andrea@example.com",
                        List.of("USER")
                );

        when(
                authService.currentUser(
                        principal
                )
        ).thenReturn(response);

        mockMvc.perform(
                        get(
                                "/api/auth/me"
                        )
                                .with(
                                        user(principal)
                                )
                )
                .andExpect(
                        status().isOk()
                )
                .andExpect(
                        jsonPath("$.userId")
                                .value(1)
                )
                .andExpect(
                        jsonPath("$.username")
                                .value("andrea")
                )
                .andExpect(
                        jsonPath("$.email")
                                .value(
                                        "andrea@example.com"
                                )
                )
                .andExpect(
                        jsonPath("$.roles[0]")
                                .value("USER")
                )
                .andExpect(
                        jsonPath("$.password")
                                .doesNotExist()
                )
                .andExpect(
                        jsonPath("$.passwordHash")
                                .doesNotExist()
                );

        verify(authService)
                .currentUser(
                        principal
                );
    }

    @Test
    void anonymousShouldNotAccessMe()
            throws Exception {

        mockMvc.perform(
                        get(
                                "/api/auth/me"
                        )
                )
                .andExpect(
                        status().isUnauthorized()
                );

        verify(
                authService,
                never()
        ).currentUser(
                any(SecurityPrincipal.class)
        );
    }

    private SecurityPrincipal principal() {

        return new SecurityPrincipal(
                1L,
                "andrea",
                "andrea@example.com",
                "$2a$10$testHash",
                true,
                List.of(
                        new SimpleGrantedAuthority(
                                "ROLE_USER"
                        )
                )
        );
    }
}