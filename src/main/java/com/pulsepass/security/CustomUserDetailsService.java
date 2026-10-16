package com.pulsepass.security;

import com.pulsepass.domain.UserCredential;
import com.pulsepass.domain.UserRole;
import com.pulsepass.repository.UserCredentialRepository;
import com.pulsepass.repository.UserRoleRepository;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class CustomUserDetailsService
        implements UserDetailsService {

    private final UserCredentialRepository userCredentialRepository;
    private final UserRoleRepository userRoleRepository;

    public CustomUserDetailsService(
            UserCredentialRepository userCredentialRepository,
            UserRoleRepository userRoleRepository
    ) {
        this.userCredentialRepository =
                userCredentialRepository;

        this.userRoleRepository =
                userRoleRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(
            String identifier
    ) throws UsernameNotFoundException {

        if (identifier == null
                || identifier.isBlank()) {

            throw new UsernameNotFoundException(
                    "Invalid credentials"
            );
        }

        UserCredential credential =
                findCredential(identifier);

        return buildPrincipal(
                credential
        );
    }

    @Transactional(readOnly = true)
    public SecurityPrincipal loadUserById(
            Long userId
    ) throws UsernameNotFoundException {

        if (userId == null) {
            throw new UsernameNotFoundException(
                    "Invalid user"
            );
        }

        UserCredential credential =
                userCredentialRepository
                        .findById(userId)
                        .orElseThrow(() ->
                                new UsernameNotFoundException(
                                        "Invalid user"
                                )
                        );

        return buildPrincipal(
                credential
        );
    }

    private SecurityPrincipal buildPrincipal(
            UserCredential credential
    ) {

        Long userId =
                credential
                        .getUser()
                        .getId();

        List<UserRole> roles =
                userRoleRepository
                        .findByUser_Id(userId);

        return SecurityPrincipal.from(
                credential,
                roles
        );
    }

    private UserCredential findCredential(
            String identifier
    ) {

        return userCredentialRepository
                .findByUserUsername(identifier)
                .or(() ->
                        userCredentialRepository
                                .findByUserEmailIgnoreCase(
                                        identifier
                                )
                )
                .orElseThrow(() ->
                        new UsernameNotFoundException(
                                "Invalid credentials"
                        )
                );
    }
}