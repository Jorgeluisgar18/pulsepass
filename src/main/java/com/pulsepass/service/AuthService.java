package com.pulsepass.service;

import com.pulsepass.domain.Role;
import com.pulsepass.domain.User;
import com.pulsepass.domain.UserCredential;
import com.pulsepass.domain.UserRole;

import com.pulsepass.dto.request.LoginRequest;
import com.pulsepass.dto.request.RegisterUserRequest;

import com.pulsepass.dto.response.AuthResponse;
import com.pulsepass.dto.response.AuthenticatedUserResponse;
import com.pulsepass.dto.response.UserResponse;

import com.pulsepass.exception.ResourceNotFoundException;

import com.pulsepass.repository.UserCredentialRepository;
import com.pulsepass.repository.UserRepository;
import com.pulsepass.repository.UserRoleRepository;

import com.pulsepass.security.JwtService;
import com.pulsepass.security.SecurityPrincipal;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;

import org.springframework.security.crypto.password.PasswordEncoder;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class AuthService {

    private final UserService userService;
    private final UserRepository userRepository;
    private final UserCredentialRepository userCredentialRepository;
    private final UserRoleRepository userRoleRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;

    public AuthService(
            UserService userService,
            UserRepository userRepository,
            UserCredentialRepository userCredentialRepository,
            UserRoleRepository userRoleRepository,
            PasswordEncoder passwordEncoder,
            AuthenticationManager authenticationManager,
            JwtService jwtService
    ) {
        this.userService =
                userService;

        this.userRepository =
                userRepository;

        this.userCredentialRepository =
                userCredentialRepository;

        this.userRoleRepository =
                userRoleRepository;

        this.passwordEncoder =
                passwordEncoder;

        this.authenticationManager =
                authenticationManager;

        this.jwtService =
                jwtService;
    }

    @Transactional
    public UserResponse register(
            RegisterUserRequest request
    ) {

        UserResponse response =
                userService.register(
                        request
                );

        User user =
                userRepository
                        .findById(
                                response.id()
                        )
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "User not found after registration"
                                )
                        );

        String passwordHash =
                passwordEncoder.encode(
                        request.password()
                );

        UserCredential credential =
                new UserCredential(
                        user,
                        passwordHash,
                        LocalDateTime.now()
                );

        UserRole userRole =
                new UserRole(
                        user,
                        Role.USER
                );

        userCredentialRepository.save(
                credential
        );

        userRoleRepository.save(
                userRole
        );

        return response;
    }

    public AuthResponse login(
            LoginRequest request
    ) {

        SecurityPrincipal principal =
                authenticate(
                        request
                );

        String accessToken =
                jwtService.generateToken(
                        principal
                );

        return new AuthResponse(
                accessToken,
                "Bearer",
                jwtService
                        .getAccessTokenTtlSeconds()
        );
    }

    public SecurityPrincipal authenticate(
            LoginRequest request
    ) {

        Authentication authentication =
                authenticationManager.authenticate(
                        UsernamePasswordAuthenticationToken
                                .unauthenticated(
                                        request.identifier(),
                                        request.password()
                                )
                );

        Object principal =
                authentication
                        .getPrincipal();

        if (!(principal
                instanceof SecurityPrincipal securityPrincipal)) {

            throw new IllegalStateException(
                    "Unexpected authentication principal"
            );
        }

        return securityPrincipal;
    }

    public AuthenticatedUserResponse currentUser(
            SecurityPrincipal principal
    ) {

        List<String> roles =
                principal
                        .getAuthorities()
                        .stream()
                        .map(
                                GrantedAuthority::getAuthority
                        )
                        .map(authority ->
                                authority.startsWith(
                                        "ROLE_"
                                )
                                        ? authority.substring(5)
                                        : authority
                        )
                        .toList();

        return new AuthenticatedUserResponse(
                principal.getUserId(),
                principal.getUsername(),
                principal.getEmail(),
                roles
        );
    }
}