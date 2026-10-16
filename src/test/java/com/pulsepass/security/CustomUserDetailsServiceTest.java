package com.pulsepass.security;

import com.pulsepass.domain.Role;
import com.pulsepass.domain.User;
import com.pulsepass.domain.UserCredential;
import com.pulsepass.domain.UserRole;
import com.pulsepass.repository.UserCredentialRepository;
import com.pulsepass.repository.UserRoleRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CustomUserDetailsServiceTest {

    @Mock
    private UserCredentialRepository userCredentialRepository;

    @Mock
    private UserRoleRepository userRoleRepository;

    private CustomUserDetailsService userDetailsService;

    @BeforeEach
    void setUp() {

        userDetailsService =
                new CustomUserDetailsService(
                        userCredentialRepository,
                        userRoleRepository
                );
    }

    @Test
    void shouldLoadUserByUsername() {

        UserCredential credential =
                credential(
                        1L,
                        "andrea",
                        "andrea@pulsepass.com",
                        true,
                        "$2a$10$testhash"
                );

        UserRole role =
                role(Role.USER);

        when(
                userCredentialRepository
                        .findByUserUsername(
                                "andrea"
                        )
        ).thenReturn(
                Optional.of(credential)
        );

        when(
                userRoleRepository
                        .findByUser_Id(1L)
        ).thenReturn(
                List.of(role)
        );

        SecurityPrincipal principal =
                (SecurityPrincipal)
                        userDetailsService
                                .loadUserByUsername(
                                        "andrea"
                                );

        assertThat(principal.getUserId())
                .isEqualTo(1L);

        assertThat(principal.getUsername())
                .isEqualTo("andrea");

        assertThat(principal.getEmail())
                .isEqualTo(
                        "andrea@pulsepass.com"
                );

        assertThat(principal.isEnabled())
                .isTrue();

        assertThat(principal.getAuthorities())
                .extracting(
                        GrantedAuthority::getAuthority
                )
                .containsExactly(
                        "ROLE_USER"
                );

        verify(
                userCredentialRepository,
                never()
        ).findByUserEmailIgnoreCase(
                "andrea"
        );
    }

    @Test
    void shouldLoadUserByEmail() {

        UserCredential credential =
                credential(
                        2L,
                        "carlos",
                        "carlos@pulsepass.com",
                        true,
                        "$2a$10$testhash"
                );

        UserRole role =
                role(Role.USER);

        when(
                userCredentialRepository
                        .findByUserUsername(
                                "carlos@pulsepass.com"
                        )
        ).thenReturn(
                Optional.empty()
        );

        when(
                userCredentialRepository
                        .findByUserEmailIgnoreCase(
                                "carlos@pulsepass.com"
                        )
        ).thenReturn(
                Optional.of(credential)
        );

        when(
                userRoleRepository
                        .findByUser_Id(2L)
        ).thenReturn(
                List.of(role)
        );

        SecurityPrincipal principal =
                (SecurityPrincipal)
                        userDetailsService
                                .loadUserByUsername(
                                        "carlos@pulsepass.com"
                                );

        assertThat(principal.getUserId())
                .isEqualTo(2L);

        assertThat(principal.getUsername())
                .isEqualTo("carlos");

        assertThat(principal.getEmail())
                .isEqualTo(
                        "carlos@pulsepass.com"
                );
    }

    @Test
    void shouldRejectUnknownIdentifier() {

        when(
                userCredentialRepository
                        .findByUserUsername(
                                "missing"
                        )
        ).thenReturn(
                Optional.empty()
        );

        when(
                userCredentialRepository
                        .findByUserEmailIgnoreCase(
                                "missing"
                        )
        ).thenReturn(
                Optional.empty()
        );

        assertThatThrownBy(() ->
                userDetailsService
                        .loadUserByUsername(
                                "missing"
                        )
        )
                .isInstanceOf(
                        UsernameNotFoundException.class
                )
                .hasMessage(
                        "Invalid credentials"
                );

        verify(
                userRoleRepository,
                never()
        ).findByUser_Id(
                1L
        );
    }

    @Test
    void shouldExposeInactiveUserAsDisabled() {

        UserCredential credential =
                credential(
                        3L,
                        "inactive.user",
                        "inactive@pulsepass.com",
                        false,
                        "$2a$10$testhash"
                );

        UserRole role =
                role(Role.USER);

        when(
                userCredentialRepository
                        .findByUserUsername(
                                "inactive.user"
                        )
        ).thenReturn(
                Optional.of(credential)
        );

        when(
                userRoleRepository
                        .findByUser_Id(3L)
        ).thenReturn(
                List.of(role)
        );

        SecurityPrincipal principal =
                (SecurityPrincipal)
                        userDetailsService
                                .loadUserByUsername(
                                        "inactive.user"
                                );

        assertThat(principal.isEnabled())
                .isFalse();
    }

    @Test
    void shouldReloadCurrentRolesFromPersistence() {

        UserCredential credential =
                credential(
                        4L,
                        "role.user",
                        "role.user@pulsepass.com",
                        true,
                        "$2a$10$testhash"
                );

        UserRole userRole =
                role(Role.USER);

        UserRole adminRole =
                role(Role.ADMIN);

        when(
                userCredentialRepository
                        .findByUserUsername(
                                "role.user"
                        )
        ).thenReturn(
                Optional.of(credential)
        );

        when(
                userRoleRepository
                        .findByUser_Id(4L)
        ).thenReturn(
                List.of(userRole),
                List.of(adminRole)
        );

        SecurityPrincipal firstPrincipal =
                (SecurityPrincipal)
                        userDetailsService
                                .loadUserByUsername(
                                        "role.user"
                                );

        SecurityPrincipal secondPrincipal =
                (SecurityPrincipal)
                        userDetailsService
                                .loadUserByUsername(
                                        "role.user"
                                );

        assertThat(
                firstPrincipal
                        .getAuthorities()
        )
                .extracting(
                        GrantedAuthority::getAuthority
                )
                .containsExactly(
                        "ROLE_USER"
                );

        assertThat(
                secondPrincipal
                        .getAuthorities()
        )
                .extracting(
                        GrantedAuthority::getAuthority
                )
                .containsExactly(
                        "ROLE_ADMIN"
                );
    }

    private UserCredential credential(
            Long userId,
            String username,
            String email,
            boolean active,
            String passwordHash
    ) {

        User user =
                mock(User.class);

        when(user.getId())
                .thenReturn(userId);

        when(user.getUsername())
                .thenReturn(username);

        when(user.getEmail())
                .thenReturn(email);

        when(user.isActive())
                .thenReturn(active);

        UserCredential credential =
                mock(UserCredential.class);

        when(credential.getUser())
                .thenReturn(user);

        when(credential.getPasswordHash())
                .thenReturn(passwordHash);

        return credential;
    }

    private UserRole role(
            Role role
    ) {

        UserRole userRole =
                mock(UserRole.class);

        when(userRole.getRole())
                .thenReturn(role);

        return userRole;
    }
}