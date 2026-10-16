package com.pulsepass.service.impl;

import com.pulsepass.domain.User;
import com.pulsepass.domain.UserProfile;

import com.pulsepass.dto.request.RegisterUserRequest;
import com.pulsepass.dto.response.UserResponse;

import com.pulsepass.exception.BusinessRuleException;
import com.pulsepass.exception.DuplicateResourceException;
import com.pulsepass.exception.ResourceNotFoundException;

import com.pulsepass.mapper.UserMapper;

import com.pulsepass.repository.UserProfileRepository;
import com.pulsepass.repository.UserRepository;

import com.pulsepass.service.AuthorizationService;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.security.access.AccessDeniedException;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserProfileRepository userProfileRepository;

    @Mock
    private UserMapper userMapper;

    @Mock
    private AuthorizationService authorizationService;

    @InjectMocks
    private UserServiceImpl userService;

    @Test
    void shouldRegisterValidUser() {

        RegisterUserRequest request =
                validRequest();

        UserResponse expectedResponse =
                userResponse();

        when(
                userRepository.existsByUsername(
                        request.username()
                )
        ).thenReturn(false);

        when(
                userRepository.existsByEmailIgnoreCase(
                        request.email()
                )
        ).thenReturn(false);

        when(
                userMapper.toResponse(
                        any(User.class)
                )
        ).thenReturn(
                expectedResponse
        );

        UserResponse result =
                userService.register(
                        request
                );

        ArgumentCaptor<User> userCaptor =
                ArgumentCaptor.forClass(
                        User.class
                );

        ArgumentCaptor<UserProfile> profileCaptor =
                ArgumentCaptor.forClass(
                        UserProfile.class
                );

        verify(userRepository)
                .save(
                        userCaptor.capture()
                );

        verify(userProfileRepository)
                .save(
                        profileCaptor.capture()
                );

        User savedUser =
                userCaptor.getValue();

        UserProfile savedProfile =
                profileCaptor.getValue();

        assertThat(result)
                .isEqualTo(
                        expectedResponse
                );

        assertThat(
                savedUser.getUsername()
        ).isEqualTo(
                "andrea"
        );

        assertThat(
                savedUser.getEmail()
        ).isEqualTo(
                "andrea@email.com"
        );

        assertThat(
                savedUser.isActive()
        ).isTrue();

        assertThat(
                savedUser.getProfile()
        ).isSameAs(
                savedProfile
        );

        assertThat(
                savedProfile.getUser()
        ).isSameAs(
                savedUser
        );

        assertThat(
                savedProfile.getFirstName()
        ).isEqualTo(
                "Andrea"
        );
    }

    @Test
    void shouldRejectDuplicatedUsername() {

        RegisterUserRequest request =
                validRequest();

        when(
                userRepository.existsByUsername(
                        request.username()
                )
        ).thenReturn(true);

        assertThatThrownBy(() ->
                userService.register(
                        request
                )
        )
                .isInstanceOf(
                        DuplicateResourceException.class
                )
                .hasMessageContaining(
                        request.username()
                );

        verify(
                userRepository,
                never()
        ).save(
                any(User.class)
        );

        verify(
                userProfileRepository,
                never()
        ).save(
                any(UserProfile.class)
        );

        verify(
                userRepository,
                never()
        ).existsByEmailIgnoreCase(
                any(String.class)
        );
    }

    @Test
    void shouldRejectDuplicatedEmail() {

        RegisterUserRequest request =
                validRequest();

        when(
                userRepository.existsByUsername(
                        request.username()
                )
        ).thenReturn(false);

        when(
                userRepository.existsByEmailIgnoreCase(
                        request.email()
                )
        ).thenReturn(true);

        assertThatThrownBy(() ->
                userService.register(
                        request
                )
        )
                .isInstanceOf(
                        DuplicateResourceException.class
                )
                .hasMessageContaining(
                        request.email()
                );

        verify(
                userRepository,
                never()
        ).save(
                any(User.class)
        );
    }

    @Test
    void shouldRejectFutureBirthDate() {

        RegisterUserRequest request =
                new RegisterUserRequest(
                        "andrea",
                        "andrea@email.com",
                        "Andrea",
                        "Martinez",
                        "3001112233",
                        "Santa Marta",
                        LocalDate.now()
                                .plusDays(1),
                        "UnaFraseSeguraLarga2026!"
                );

        when(
                userRepository.existsByUsername(
                        request.username()
                )
        ).thenReturn(false);

        when(
                userRepository.existsByEmailIgnoreCase(
                        request.email()
                )
        ).thenReturn(false);

        assertThatThrownBy(() ->
                userService.register(
                        request
                )
        )
                .isInstanceOf(
                        BusinessRuleException.class
                )
                .hasMessageContaining(
                        "future"
                );

        verify(
                userRepository,
                never()
        ).save(
                any(User.class)
        );
    }

    @Test
    void shouldFindUserByEmailWhenAuthorized() {

        User user =
                userWithProfile();

        UserResponse expectedResponse =
                userResponse();

        when(
                userRepository
                        .findByEmailIgnoreCase(
                                eq(
                                        "ANDREA@EMAIL.COM"
                                )
                        )
        ).thenReturn(
                Optional.of(user)
        );

        when(
                userMapper.toResponse(
                        user
                )
        ).thenReturn(
                expectedResponse
        );

        UserResponse result =
                userService.findByEmail(
                        "ANDREA@EMAIL.COM"
                );

        assertThat(result)
                .isEqualTo(
                        expectedResponse
                );

        verify(authorizationService)
                .requireSelfOrAdmin(
                        user
                );

        verify(userMapper)
                .toResponse(
                        user
                );
    }

    @Test
    void shouldFindUserByUsernameWhenAuthorized() {

        User user =
                userWithProfile();

        UserResponse expectedResponse =
                userResponse();

        when(
                userRepository.findByUsername(
                        "andrea"
                )
        ).thenReturn(
                Optional.of(user)
        );

        when(
                userMapper.toResponse(
                        user
                )
        ).thenReturn(
                expectedResponse
        );

        UserResponse result =
                userService.findByUsername(
                        "andrea"
                );

        assertThat(result)
                .isEqualTo(
                        expectedResponse
                );

        verify(authorizationService)
                .requireSelfOrAdmin(
                        user
                );

        verify(userMapper)
                .toResponse(
                        user
                );
    }

    @Test
    void shouldRejectAccessToAnotherUsersData() {

        User user =
                userWithProfile();

        when(
                userRepository.findByEmailIgnoreCase(
                        "another@email.com"
                )
        ).thenReturn(
                Optional.of(user)
        );

        doThrow(
                new AccessDeniedException(
                        "Access denied"
                )
        )
                .when(
                        authorizationService
                )
                .requireSelfOrAdmin(
                        user
                );

        assertThatThrownBy(() ->
                userService.findByEmail(
                        "another@email.com"
                )
        ).isInstanceOf(
                AccessDeniedException.class
        );

        verify(
                userMapper,
                never()
        ).toResponse(
                any(User.class)
        );
    }

    @Test
    void shouldThrowWhenUserEmailDoesNotExist() {

        when(
                userRepository.findByEmailIgnoreCase(
                        "unknown@email.com"
                )
        ).thenReturn(
                Optional.empty()
        );

        assertThatThrownBy(() ->
                userService.findByEmail(
                        "unknown@email.com"
                )
        )
                .isInstanceOf(
                        ResourceNotFoundException.class
                )
                .hasMessageContaining(
                        "unknown@email.com"
                );

        verify(
                authorizationService,
                never()
        ).requireSelfOrAdmin(
                any(User.class)
        );

        verify(
                userMapper,
                never()
        ).toResponse(
                any(User.class)
        );
    }

    private RegisterUserRequest validRequest() {

        return new RegisterUserRequest(
                "andrea",
                "andrea@email.com",
                "Andrea",
                "Martinez",
                "3001112233",
                "Santa Marta",
                LocalDate.of(
                        2000,
                        5,
                        10
                ),
                "UnaFraseSeguraLarga2026!"
        );
    }

    private User userWithProfile() {

        User user =
                new User(
                        "andrea",
                        "andrea@email.com",
                        true
                );

        UserProfile profile =
                new UserProfile(
                        "Andrea",
                        "Martinez",
                        "3001112233",
                        "Santa Marta",
                        LocalDate.of(
                                2000,
                                5,
                                10
                        )
                );

        user.assignProfile(
                profile
        );

        return user;
    }

    private UserResponse userResponse() {

        return new UserResponse(
                null,
                "andrea",
                "andrea@email.com",
                true,
                "Andrea",
                "Martinez",
                "3001112233",
                "Santa Marta",
                LocalDate.of(
                        2000,
                        5,
                        10
                )
        );
    }
}