package com.pulsepass.security;

import com.pulsepass.dto.response.ErrorResponse;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.api.extension.ExtendWith;

import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;

import tools.jackson.databind.json.JsonMapper;

import java.io.OutputStream;

import static org.assertj.core.api.Assertions.assertThat;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class SecurityErrorHandlersTest {

    @Mock
    private JsonMapper jsonMapper;

    private RestAuthenticationEntryPoint authenticationEntryPoint;
    private RestAccessDeniedHandler accessDeniedHandler;

    @BeforeEach
    void setUp() {

        authenticationEntryPoint =
                new RestAuthenticationEntryPoint(
                        jsonMapper
                );

        accessDeniedHandler =
                new RestAccessDeniedHandler(
                        jsonMapper
                );
    }

    @Test
    void shouldReturn401ErrorResponse()
            throws Exception {

        MockHttpServletRequest request =
                new MockHttpServletRequest();

        MockHttpServletResponse response =
                new MockHttpServletResponse();

        authenticationEntryPoint.commence(
                request,
                response,
                new BadCredentialsException(
                        "Invalid token"
                )
        );

        ArgumentCaptor<ErrorResponse> captor =
                ArgumentCaptor.forClass(
                        ErrorResponse.class
                );

        verify(jsonMapper)
                .writeValue(
                        any(OutputStream.class),
                        captor.capture()
                );

        ErrorResponse error =
                captor.getValue();

        assertThat(response.getStatus())
                .isEqualTo(401);

        assertThat(error.status())
                .isEqualTo(401);

        assertThat(error.error())
                .isEqualTo("Unauthorized");

        assertThat(error.details())
                .isEmpty();
    }

    @Test
    void shouldReturn403ErrorResponse()
            throws Exception {

        MockHttpServletRequest request =
                new MockHttpServletRequest();

        MockHttpServletResponse response =
                new MockHttpServletResponse();

        accessDeniedHandler.handle(
                request,
                response,
                new AccessDeniedException(
                        "Denied"
                )
        );

        ArgumentCaptor<ErrorResponse> captor =
                ArgumentCaptor.forClass(
                        ErrorResponse.class
                );

        verify(jsonMapper)
                .writeValue(
                        any(OutputStream.class),
                        captor.capture()
                );

        ErrorResponse error =
                captor.getValue();

        assertThat(response.getStatus())
                .isEqualTo(403);

        assertThat(error.status())
                .isEqualTo(403);

        assertThat(error.error())
                .isEqualTo("Forbidden");

        assertThat(error.message())
                .isEqualTo(
                        "No tiene permisos para realizar esta operación"
                );

        assertThat(error.details())
                .isEmpty();
    }
}