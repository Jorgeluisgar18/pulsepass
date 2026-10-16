package com.pulsepass.service;

import com.pulsepass.domain.Role;
import com.pulsepass.domain.User;
import com.pulsepass.domain.UserCredential;
import com.pulsepass.domain.UserRole;
import com.pulsepass.dto.request.LoginRequest;
import com.pulsepass.dto.request.RegisterUserRequest;
import com.pulsepass.dto.response.AuthenticatedUserResponse;
import com.pulsepass.dto.response.UserResponse;
import com.pulsepass.repository.UserCredentialRepository;
import com.pulsepass.repository.UserRepository;
import com.pulsepass.repository.UserRoleRepository;
import com.pulsepass.security.SecurityPrincipal;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserService userService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserCredentialRepository userCredentialRepository;

    @Mock
    private UserRoleRepository userRoleRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private AuthenticationManager authenticationManager;

    private AuthService authService;

    @BeforeEach
    void setUp() {

        authService =
                new AuthService(
                        userService,
                        userRepository,
                        userCredentialRepository,
                        userRoleRepository,
                        passwordEncoder,
                        authenticationManager
                );
    }

    @Test
    void shouldRegisterUserWithCredentialAndUserRole() {

        RegisterUserRequest request =
                registerRequest();

        UserResponse response =
                userResponse();

        User user =
                mock(User.class);

        when(user.getId())
                .thenReturn(1L);

        when(userService.register(request))
                .thenReturn(response);

        when(userRepository.findById(1L))
                .thenReturn(
                        Optional.of(user)
                );

        when(
                passwordEncoder.encode(
                        request.password()
                )
        ).thenReturn(
                "$2a$10$encodedPassword"
        );

        UserResponse result =
                authService.register(request);

        ArgumentCaptor<UserCredential>
                credentialCaptor =
                ArgumentCaptor.forClass(
                        UserCredential.class
                );

        ArgumentCaptor<UserRole>
                roleCaptor =
                ArgumentCaptor.forClass(
                        UserRole.class
                );

        verify(userCredentialRepository)
                .save(
                        credentialCaptor.capture()
                );

        verify(userRoleRepository)
                .save(
                        roleCaptor.capture()
                );

        UserCredential credential =
                credentialCaptor.getValue();

        UserRole role =
                roleCaptor.getValue();

        assertThat(result)
                .isEqualTo(response);

        assertThat(
                credential.getPasswordHash()
        ).isEqualTo(
                "$2a$10$encodedPassword"
        );

        assertThat(
                credential.getPasswordHash()
        ).isNotEqualTo(
                request.password()
        );

        assertThat(role.getRole())
                .isEqualTo(Role.USER);

        verify(passwordEncoder)
                .encode(
                        "UnaFraseSeguraLarga2026!"
                );
    }

    @Test
    void shouldAuthenticateValidCredentials() {

        LoginRequest request =
                new LoginRequest(
                        "andrea",
                        "UnaFraseSeguraLarga2026!"
                );

        SecurityPrincipal principal =
                securityPrincipal();

        Authentication authentication =
                mock(Authentication.class);

        when(
                authentication.getPrincipal()
        ).thenReturn(principal);

        when(
                authenticationManager.authenticate(
                        any(Authentication.class)
                )
        ).thenReturn(authentication);

        SecurityPrincipal result =
                authService.authenticate(
                        request
                );

        assertThat(result)
                .isSameAs(principal);

        verify(authenticationManager)
                .authenticate(
                        any(
                                UsernamePasswordAuthenticationToken.class
                        )
                );
    }

    @Test
    void shouldRejectInvalidCredentials() {

        LoginRequest request =
                new LoginRequest(
                        "andrea",
                        "password-incorrecto"
                );

        when(
                authenticationManager.authenticate(
                        any(Authentication.class)
                )
        ).thenThrow(
                new BadCredentialsException(
                        "Bad credentials"
                )
        );

        assertThatThrownBy(() ->
                authService.authenticate(request)
        ).isInstanceOf(
                BadCredentialsException.class
        );
    }

    @Test
    void shouldReturnAuthenticatedUserWithoutSecrets() {

        SecurityPrincipal principal =
                securityPrincipal();

        AuthenticatedUserResponse response =
                authService.currentUser(
                        principal
                );

        assertThat(response.userId())
                .isEqualTo(1L);

        assertThat(response.username())
                .isEqualTo("andrea");

        assertThat(response.email())
                .isEqualTo(
                        "andrea@example.com"
                );

        assertThat(response.roles())
                .containsExactly("USER");
    }

    private RegisterUserRequest registerRequest() {

        return new RegisterUserRequest(
                "andrea",
                "andrea@example.com",
                "Andrea",
                "Garcia",
                "3001234567",
                "Santa Marta",
                LocalDate.of(
                        1998,
                        5,
                        10
                ),
                "UnaFraseSeguraLarga2026!"
        );
    }

    private UserResponse userResponse() {

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

    private SecurityPrincipal securityPrincipal() {

        return new SecurityPrincipal(
                1L,
                "andrea",
                "andrea@example.com",
                "$2a$10$encodedPassword",
                true,
                List.of(
                        new SimpleGrantedAuthority(
                                "ROLE_USER"
                        )
                )
        );
    }
}