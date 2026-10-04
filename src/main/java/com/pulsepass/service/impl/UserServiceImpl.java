package com.pulsepass.service;

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
import com.pulsepass.service.UserService;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Service
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final UserProfileRepository userProfileRepository;
    private final UserMapper userMapper;

    public UserServiceImpl(
            UserRepository userRepository,
            UserProfileRepository userProfileRepository,
            UserMapper userMapper
    ) {
        this.userRepository = userRepository;
        this.userProfileRepository = userProfileRepository;
        this.userMapper = userMapper;
    }

    @Override
    @Transactional
    public UserResponse register(
            RegisterUserRequest request
    ) {

        if (userRepository.existsByUsername(
                request.username()
        )) {
            throw new DuplicateResourceException(
                    "Username already exists: "
                            + request.username()
            );
        }

        if (userRepository.existsByEmailIgnoreCase(
                request.email()
        )) {
            throw new DuplicateResourceException(
                    "Email already exists: "
                            + request.email()
            );
        }

        if (request.birthDate() != null
                && request.birthDate()
                .isAfter(LocalDate.now())) {

            throw new BusinessRuleException(
                    "Birth date cannot be in the future."
            );
        }

        User user = new User(
                request.username(),
                request.email(),
                true
        );

        UserProfile profile =
                new UserProfile(
                        request.firstName(),
                        request.lastName(),
                        request.phone(),
                        request.city(),
                        request.birthDate()
                );

        user.assignProfile(profile);

        userRepository.save(user);

        userProfileRepository.save(profile);

        return userMapper.toResponse(user);
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponse findByEmail(
            String email
    ) {

        User user = userRepository
                .findByEmailIgnoreCase(email)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "User not found by email: "
                                        + email
                        )
                );

        return userMapper.toResponse(user);
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponse findByUsername(
            String username
    ) {

        User user = userRepository
                .findByUsername(username)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "User not found by username: "
                                        + username
                        )
                );

        return userMapper.toResponse(user);
    }
}