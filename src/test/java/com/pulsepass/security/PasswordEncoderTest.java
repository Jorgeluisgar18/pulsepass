package com.pulsepass.security;

import org.junit.jupiter.api.Test;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;

class PasswordEncoderTest {

    private final PasswordEncoder passwordEncoder =
            new BCryptPasswordEncoder();

    @Test
    void shouldEncodePasswordWithBCrypt() {

        String rawPassword =
                "UnaFraseSeguraLarga2026!";

        String encodedPassword =
                passwordEncoder.encode(
                        rawPassword
                );

        assertThat(encodedPassword)
                .isNotEqualTo(rawPassword);

        assertThat(encodedPassword)
                .startsWith("$2");

        assertThat(
                passwordEncoder.matches(
                        rawPassword,
                        encodedPassword
                )
        ).isTrue();
    }

    @Test
    void shouldGenerateDifferentHashesForSamePassword() {

        String rawPassword =
                "UnaFraseSeguraLarga2026!";

        String firstHash =
                passwordEncoder.encode(
                        rawPassword
                );

        String secondHash =
                passwordEncoder.encode(
                        rawPassword
                );

        assertThat(firstHash)
                .isNotEqualTo(secondHash);

        assertThat(
                passwordEncoder.matches(
                        rawPassword,
                        firstHash
                )
        ).isTrue();

        assertThat(
                passwordEncoder.matches(
                        rawPassword,
                        secondHash
                )
        ).isTrue();
    }
}