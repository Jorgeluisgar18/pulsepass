package com.pulsepass.controller;

import com.pulsepass.dto.request.RegisterUserRequest;
import com.pulsepass.dto.response.UserResponse;
import com.pulsepass.exception.DuplicateResourceException;
import com.pulsepass.exception.GlobalExceptionHandler;
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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(UserController.class)
@Import(GlobalExceptionHandler.class)
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserService userService;

    @Test
    void shouldRegisterUserAndReturn201() throws Exception {

        UserResponse response = new UserResponse(
                1L,
                "andrea",
                "andrea@example.com",
                true,
                "Andrea",
                "Garcia",
                "3001234567",
                "Santa Marta",
                LocalDate.of(1998, 5, 10)
        );

        when(userService.register(any(RegisterUserRequest.class)))
                .thenReturn(response);

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
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(requestBody)
                )
                .andExpect(status().isCreated())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.username").value("andrea"))
                .andExpect(jsonPath("$.email").value("andrea@example.com"))
                .andExpect(jsonPath("$.active").value(true))
                .andExpect(jsonPath("$.firstName").value("Andrea"));

        verify(userService)
                .register(any(RegisterUserRequest.class));
    }

    @Test
    void shouldReturn400WhenEmailIsInvalid() throws Exception {

        String requestBody = """
                {
                  "username": "andrea",
                  "email": "correo-invalido",
                  "firstName": "Andrea",
                  "lastName": "Garcia",
                  "phone": "3001234567",
                  "city": "Santa Marta",
                  "birthDate": "1998-05-10"
                }
                """;

        mockMvc.perform(
                        post("/api/users")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(requestBody)
                )
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.details.email").value("Email must be valid"));

        verify(userService, never())
                .register(any(RegisterUserRequest.class));
    }

    @Test
    void shouldReturn409WhenUsernameIsDuplicated() throws Exception {

        when(userService.register(any(RegisterUserRequest.class)))
                .thenThrow(
                        new DuplicateResourceException(
                                "Username already exists: andrea"
                        )
                );

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
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(requestBody)
                )
                .andExpect(status().isConflict())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.error").value("Conflict"))
                .andExpect(
                        jsonPath("$.message")
                                .value("Username already exists: andrea")
                );

        verify(userService)
                .register(any(RegisterUserRequest.class));
    }

    @Test
    void shouldReturnUserByEmail() throws Exception {

        UserResponse response = new UserResponse(
                1L,
                "andrea",
                "andrea@example.com",
                true,
                "Andrea",
                "Garcia",
                "3001234567",
                "Santa Marta",
                LocalDate.of(1998, 5, 10)
        );

        when(userService.findByEmail("andrea@example.com"))
                .thenReturn(response);

        mockMvc.perform(
                        get("/api/users/by-email")
                                .param("email", "andrea@example.com")
                )
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.username").value("andrea"))
                .andExpect(jsonPath("$.email").value("andrea@example.com"));

        verify(userService)
                .findByEmail("andrea@example.com");
    }

    @Test
    void shouldReturnUserByUsername() throws Exception {

        UserResponse response = new UserResponse(
                1L,
                "andrea",
                "andrea@example.com",
                true,
                "Andrea",
                "Garcia",
                "3001234567",
                "Santa Marta",
                LocalDate.of(1998, 5, 10)
        );

        when(userService.findByUsername("andrea"))
                .thenReturn(response);

        mockMvc.perform(
                        get("/api/users/by-username")
                                .param("username", "andrea")
                )
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.username").value("andrea"))
                .andExpect(jsonPath("$.email").value("andrea@example.com"));

        verify(userService)
                .findByUsername("andrea");
    }
}