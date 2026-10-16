package com.pulsepass;

import com.pulsepass.domain.Role;
import com.pulsepass.domain.User;
import com.pulsepass.domain.UserCredential;
import com.pulsepass.domain.UserRole;
import com.pulsepass.repository.UserCredentialRepository;
import com.pulsepass.repository.UserRepository;
import com.pulsepass.repository.UserRoleRepository;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Testcontainers
@SpringBootTest
@Transactional
class SecurityPersistenceIntegrationTest {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer postgres =
            new PostgreSQLContainer("postgres:18-alpine")
                    .withDatabaseName("pulsepass_security_test")
                    .withUsername("pulsepass")
                    .withPassword("pulsepass");

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserCredentialRepository userCredentialRepository;

    @Autowired
    private UserRoleRepository userRoleRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void shouldApplySecurityMigration() {

        Integer migrations = jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*)
                FROM flyway_schema_history
                WHERE success = true
                """,
                Integer.class
        );

        assertThat(migrations)
                .isEqualTo(4);

        Integer credentialTable = jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*)
                FROM information_schema.tables
                WHERE table_name = 'user_credentials'
                """,
                Integer.class
        );

        Integer roleTable = jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*)
                FROM information_schema.tables
                WHERE table_name = 'user_roles'
                """,
                Integer.class
        );

        assertThat(credentialTable)
                .isEqualTo(1);

        assertThat(roleTable)
                .isEqualTo(1);
    }

    @Test
    void shouldPersistCredentialAndRoles() {

        User user = new User(
                "security.user",
                "security.user@pulsepass.com",
                true
        );

        userRepository.saveAndFlush(user);

        UserCredential credential =
                new UserCredential(
                        user,
                        "test-hash-only",
                        LocalDateTime.of(
                                2026,
                                10,
                                15,
                                12,
                                0
                        )
                );

        userCredentialRepository.saveAndFlush(
                credential
        );

        UserRole userRole =
                new UserRole(
                        user,
                        Role.USER
                );

        UserRole staffRole =
                new UserRole(
                        user,
                        Role.STAFF
                );

        userRoleRepository.saveAll(
                List.of(
                        userRole,
                        staffRole
                )
        );

        userRoleRepository.flush();

        assertThat(
                userCredentialRepository.findById(
                        user.getId()
                )
        ).isPresent();

        List<UserRole> roles =
                userRoleRepository.findByUser_Id(
                        user.getId()
                );

        assertThat(roles)
                .extracting(UserRole::getRole)
                .containsExactlyInAnyOrder(
                        Role.USER,
                        Role.STAFF
                );
    }

    @Test
    void shouldAllowLegacyUserWithoutCredentials() {

        User legacyUser = new User(
                "legacy.user",
                "legacy.user@pulsepass.com",
                true
        );

        userRepository.saveAndFlush(
                legacyUser
        );

        assertThat(
                userCredentialRepository.findById(
                        legacyUser.getId()
                )
        ).isEmpty();

        assertThat(
                userRoleRepository.findByUser_Id(
                        legacyUser.getId()
                )
        ).isEmpty();
    }

    @Test
    void shouldRejectInvalidRoleAtDatabaseLevel() {

        User user = new User(
                "invalid.role.user",
                "invalid.role@pulsepass.com",
                true
        );

        userRepository.saveAndFlush(user);

        assertThatThrownBy(() ->
                jdbcTemplate.update(
                        """
                        INSERT INTO user_roles (
                            user_id,
                            role
                        )
                        VALUES (?, ?)
                        """,
                        user.getId(),
                        "SUPERADMIN"
                )
        ).isInstanceOf(
                DataIntegrityViolationException.class
        );
    }

    @Test
    void shouldRejectDuplicatedRoleForSameUser() {

        User user = new User(
                "duplicate.role.user",
                "duplicate.role@pulsepass.com",
                true
        );

        userRepository.saveAndFlush(user);

        jdbcTemplate.update(
                """
                INSERT INTO user_roles (
                    user_id,
                    role
                )
                VALUES (?, ?)
                """,
                user.getId(),
                "USER"
        );

        assertThatThrownBy(() ->
                jdbcTemplate.update(
                        """
                        INSERT INTO user_roles (
                            user_id,
                            role
                        )
                        VALUES (?, ?)
                        """,
                        user.getId(),
                        "USER"
                )
        ).isInstanceOf(
                DataIntegrityViolationException.class
        );
    }
}