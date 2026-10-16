package com.pulsepass.controller;

import com.pulsepass.dto.request.RegisterUserRequest;
import com.pulsepass.dto.response.UserResponse;
import com.pulsepass.exception.DuplicateResourceException;
import com.pulsepass.exception.GlobalExceptionHandler;
import com.pulsepass.security.CustomUserDetailsService;
import com.pulsepass.security.JwtAuthenticationFilter;
import com.pulsepass.security.JwtService;
import com.pulsepass.security.RestAccessDeniedHandler;
import com.pulsepass.security.RestAuthenticationEntryPoint;
import com.pulsepass.security.SecurityConfig;
import com.pulsepass.service.AuthService;
import com.pulsepass.service.UserService;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(UserController.class)
@Import({
        SecurityConfig.class,
        JwtAuthenticationFilter.class,
        RestAuthenticationEntryPoint.class,
        RestAccessDeniedHandler.class,
        GlobalExceptionHandler.class
})
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private AuthService authService;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private CustomUserDetailsService userDetailsService;

    @Test
    void shouldRegisterUserAndReturn201()
            throws Exception {

        UserResponse response =
                response();

        when(
                authService.register(
                        any(RegisterUserRequest.class)
                )
        ).thenReturn(response);

        mockMvc.perform(
                        post("/api/users")
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content(
                                        validRegistrationJson()
                                )
                )
                .andExpect(
                        status().isCreated()
                )
                .andExpect(
                        content().contentTypeCompatibleWith(
                                MediaType.APPLICATION_JSON
                        )
                )
                .andExpect(
                        jsonPath("$.id")
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
                        jsonPath("$.active")
                                .value(true)
                )
                .andExpect(
                        jsonPath("$.firstName")
                                .value("Andrea")
                )
                .andExpect(
                        jsonPath("$.lastName")
                                .value("Garcia")
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
                .register(
                        any(RegisterUserRequest.class)
                );
    }

    @Test
    void shouldReturn400WhenEmailIsInvalid()
            throws Exception {

        String requestBody = """
                {
                  "username": "andrea",
                  "email": "correo-invalido",
                  "firstName": "Andrea",
                  "lastName": "Garcia",
                  "phone": "3001234567",
                  "city": "Santa Marta",
                  "birthDate": "1998-05-10",
                  "password": "UnaFraseSeguraLarga2026!"
                }
                """;

        mockMvc.perform(
                        post("/api/users")
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content(requestBody)
                )
                .andExpect(
                        status().isBadRequest()
                )
                .andExpect(
                        jsonPath("$.status")
                                .value(400)
                )
                .andExpect(
                        jsonPath("$.error")
                                .value("Bad Request")
                )
                .andExpect(
                        jsonPath("$.message")
                                .value(
                                        "Validation failed"
                                )
                )
                .andExpect(
                        jsonPath("$.details.email")
                                .value(
                                        "Email must be valid"
                                )
                );

        verify(
                authService,
                never()
        ).register(
                any(RegisterUserRequest.class)
        );
    }

    @Test
    void shouldReturn400WhenPasswordIsTooShort()
            throws Exception {

        String requestBody = """
                {
                  "username": "andrea",
                  "email": "andrea@example.com",
                  "firstName": "Andrea",
                  "lastName": "Garcia",
                  "phone": "3001234567",
                  "city": "Santa Marta",
                  "birthDate": "1998-05-10",
                  "password": "123456"
                }
                """;

        mockMvc.perform(
                        post("/api/users")
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content(requestBody)
                )
                .andExpect(
                        status().isBadRequest()
                )
                .andExpect(
                        jsonPath("$.status")
                                .value(400)
                )
                .andExpect(
                        jsonPath("$.details.password")
                                .value(
                                        "Password must have at least 12 characters and at most 72 UTF-8 bytes"
                                )
                );

        verify(
                authService,
                never()
        ).register(
                any(RegisterUserRequest.class)
        );
    }

    @Test
    void shouldReturn400WhenPasswordIsMissing()
            throws Exception {

        String requestBody = """
                {
                  "username": "andrea",
                  "email": "andrea@example.com",
                  "firstName": "Andrea",
                  "lastName": "Garcia",
                  "phone": "3001234567",
                  "city": "Santa Marta",
                  "birthDate": "1998-05-10"
                }
                """;

        mockMvc.perform(
                        post("/api/users")
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content(requestBody)
                )
                .andExpect(
                        status().isBadRequest()
                )
                .andExpect(
                        jsonPath("$.status")
                                .value(400)
                )
                .andExpect(
                        jsonPath("$.details.password")
                                .value(
                                        "Password is required"
                                )
                );

        verify(
                authService,
                never()
        ).register(
                any(RegisterUserRequest.class)
        );
    }

    @Test
    void shouldReturn409WhenUsernameIsDuplicated()
            throws Exception {

        when(
                authService.register(
                        any(RegisterUserRequest.class)
                )
        ).thenThrow(
                new DuplicateResourceException(
                        "Username already exists: andrea"
                )
        );

        mockMvc.perform(
                        post("/api/users")
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content(
                                        validRegistrationJson()
                                )
                )
                .andExpect(
                        status().isConflict()
                )
                .andExpect(
                        jsonPath("$.status")
                                .value(409)
                )
                .andExpect(
                        jsonPath("$.error")
                                .value("Conflict")
                )
                .andExpect(
                        jsonPath("$.message")
                                .value(
                                        "Username already exists: andrea"
                                )
                );

        verify(authService)
                .register(
                        any(RegisterUserRequest.class)
                );
    }

    @Test
    void shouldReturnUserByEmail()
            throws Exception {

        when(
                userService.findByEmail(
                        "andrea@example.com"
                )
        ).thenReturn(
                response()
        );

        mockMvc.perform(
                        get("/api/users/by-email")
                                .with(
                                        user("andrea")
                                                .roles("USER")
                                )
                                .param(
                                        "email",
                                        "andrea@example.com"
                                )
                )
                .andExpect(
                        status().isOk()
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
                        jsonPath("$.password")
                                .doesNotExist()
                )
                .andExpect(
                        jsonPath("$.passwordHash")
                                .doesNotExist()
                );

        verify(userService)
                .findByEmail(
                        "andrea@example.com"
                );
    }

    @Test
    void shouldReturnUserByUsername()
            throws Exception {

        when(
                userService.findByUsername(
                        "andrea"
                )
        ).thenReturn(
                response()
        );

        mockMvc.perform(
                        get("/api/users/by-username")
                                .with(
                                        user("andrea")
                                                .roles("USER")
                                )
                                .param(
                                        "username",
                                        "andrea"
                                )
                )
                .andExpect(
                        status().isOk()
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
                        jsonPath("$.password")
                                .doesNotExist()
                )
                .andExpect(
                        jsonPath("$.passwordHash")
                                .doesNotExist()
                );

        verify(userService)
                .findByUsername(
                        "andrea"
                );
    }

    private String validRegistrationJson() {

        return """
                {
                  "username": "andrea",
                  "email": "andrea@example.com",
                  "firstName": "Andrea",
                  "lastName": "Garcia",
                  "phone": "3001234567",
                  "city": "Santa Marta",
                  "birthDate": "1998-05-10",
                  "password": "UnaFraseSeguraLarga2026!"
                }
                """;
    }

    private UserResponse response() {

        return new UserResponse(
                1L,
                "andrea",
                "andrea@example.com",
                true,
                "Andrea",
                "Garcia",
                "3001234567",
                "Santa Marta",
                LocalDate.of(
                        1998,
                        5,
                        10
                )
        );
    }
}