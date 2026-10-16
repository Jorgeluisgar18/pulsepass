package com.pulsepass.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;

import org.junit.jupiter.api.Test;

import org.springframework.security.core.authority.SimpleGrantedAuthority;

import javax.crypto.SecretKey;

import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtServiceTest {

    private static final String TEST_SECRET =
            "QUJDREVGR0hJSktMTU5PUFFSU1RVVldYWVoxMjM0NTY=";

    private static final String ISSUER =
            "pulsepass-api-test";

    @Test
    void shouldGenerateValidTokenWithRequiredClaims() {

        JwtService jwtService =
                jwtService();

        SecurityPrincipal principal =
                principal();

        String token =
                jwtService.generateToken(
                        principal
                );

        Claims claims =
                jwtService.parseClaims(
                        token
                );

        assertThat(token)
                .isNotBlank();

        assertThat(claims.getSubject())
                .isEqualTo("1");

        assertThat(claims.getIssuer())
                .isEqualTo(ISSUER);

        assertThat(claims.getIssuedAt())
                .isNotNull();

        assertThat(claims.getExpiration())
                .isNotNull();

        assertThat(claims.getId())
                .isNotBlank();

        assertThat(
                claims.getExpiration()
        ).isAfter(
                claims.getIssuedAt()
        );

        assertThat(
                jwtService.extractUserId(token)
        ).isEqualTo(1L);

        assertThat(
                jwtService.getAccessTokenTtlSeconds()
        ).isEqualTo(900L);
    }

    @Test
    void shouldUseHS256Algorithm() {

        JwtService jwtService =
                jwtService();

        String token =
                jwtService.generateToken(
                        principal()
                );

        SecretKey key =
                Keys.hmacShaKeyFor(
                        Decoders.BASE64.decode(
                                TEST_SECRET
                        )
                );

        String algorithm =
                Jwts.parser()
                        .verifyWith(key)
                        .build()
                        .parseSignedClaims(token)
                        .getHeader()
                        .getAlgorithm();

        assertThat(algorithm)
                .isEqualTo("HS256");
    }

    @Test
    void shouldNotIncludeSensitiveClaims() {

        JwtService jwtService =
                jwtService();

        Claims claims =
                jwtService.parseClaims(
                        jwtService.generateToken(
                                principal()
                        )
                );

        assertThat(claims)
                .doesNotContainKeys(
                        "email",
                        "username",
                        "password",
                        "passwordHash",
                        "roles"
                );
    }

    @Test
    void shouldRejectExpiredToken() {

        JwtService jwtService =
                jwtService();

        Instant now =
                Instant.now();

        String token =
                createToken(
                        ISSUER,
                        now.minusSeconds(120),
                        now.minusSeconds(60)
                );

        assertThatThrownBy(() ->
                jwtService.parseClaims(token)
        ).isInstanceOf(
                JwtException.class
        );
    }

    @Test
    void shouldRejectTokenWithWrongIssuer() {

        JwtService jwtService =
                jwtService();

        Instant now =
                Instant.now();

        String token =
                createToken(
                        "other-issuer",
                        now,
                        now.plusSeconds(900)
                );

        assertThatThrownBy(() ->
                jwtService.parseClaims(token)
        ).isInstanceOf(
                JwtException.class
        );
    }

    @Test
    void shouldRejectTamperedToken() {

        JwtService jwtService =
                jwtService();

        String validToken =
                jwtService.generateToken(
                        principal()
                );

        String tamperedToken =
                tamperPayload(
                        validToken
                );

        assertThatThrownBy(() ->
                jwtService.parseClaims(
                        tamperedToken
                )
        ).isInstanceOfAny(
                JwtException.class,
                IllegalArgumentException.class
        );
    }

    @Test
    void shouldRejectBlankToken() {

        JwtService jwtService =
                jwtService();

        assertThatThrownBy(() ->
                jwtService.parseClaims(" ")
        )
                .isInstanceOf(
                        IllegalArgumentException.class
                )
                .hasMessageContaining(
                        "must not be blank"
                );
    }

    @Test
    void shouldRejectWeakSecret() {

        JwtProperties properties =
                properties(
                        "MTIzNDU2Nzg5MDEyMzQ1Ng=="
                );

        assertThatThrownBy(() ->
                new JwtService(properties)
        )
                .isInstanceOf(
                        IllegalArgumentException.class
                )
                .hasMessageContaining(
                        "at least 32 bytes"
                );
    }

    private JwtService jwtService() {
        return new JwtService(
                properties(TEST_SECRET)
        );
    }

    private JwtProperties properties(
            String secret
    ) {

        JwtProperties properties =
                new JwtProperties();

        properties.setIssuer(
                ISSUER
        );

        properties.setSecretBase64(
                secret
        );

        properties.setAccessTokenTtl(
                Duration.ofMinutes(15)
        );

        return properties;
    }

    private SecurityPrincipal principal() {

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

    private String createToken(
            String issuer,
            Instant issuedAt,
            Instant expiration
    ) {

        SecretKey key =
                Keys.hmacShaKeyFor(
                        Decoders.BASE64.decode(
                                TEST_SECRET
                        )
                );

        return Jwts.builder()
                .subject("1")
                .issuer(issuer)
                .issuedAt(
                        Date.from(issuedAt)
                )
                .expiration(
                        Date.from(expiration)
                )
                .id(
                        UUID.randomUUID()
                                .toString()
                )
                .signWith(
                        key,
                        Jwts.SIG.HS256
                )
                .compact();
    }

    private String tamperPayload(
            String token
    ) {

        String[] parts =
                token.split("\\.");

        String payload =
                parts[1];

        char original =
                payload.charAt(0);

        char replacement =
                original == 'A'
                        ? 'B'
                        : 'A';

        parts[1] =
                replacement
                        + payload.substring(1);

        return String.join(
                ".",
                parts
        );
    }
}