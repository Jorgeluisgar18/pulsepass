package com.pulsepass.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.JwtParser;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;

import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;

import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

@Service
public class JwtService {

    private static final int MIN_SECRET_BYTES = 32;

    private final JwtProperties properties;
    private final SecretKey key;
    private final JwtParser parser;

    public JwtService(
            JwtProperties properties
    ) {

        this.properties = properties;

        validateConfiguration(properties);

        byte[] secretBytes =
                decodeSecret(
                        properties.getSecretBase64()
                );

        if (secretBytes.length < MIN_SECRET_BYTES) {
            throw new IllegalArgumentException(
                    "JWT secret must contain at least 32 bytes"
            );
        }

        this.key =
                Keys.hmacShaKeyFor(
                        secretBytes
                );

        this.parser =
                Jwts.parser()
                        .verifyWith(key)
                        .sig()
                        .add(Jwts.SIG.HS256)
                        .and()
                        .requireIssuer(
                                properties.getIssuer()
                        )
                        .build();
    }

    public String generateToken(
            SecurityPrincipal principal
    ) {

        if (principal == null
                || principal.getUserId() == null) {

            throw new IllegalArgumentException(
                    "Authenticated principal is required"
            );
        }

        Instant now =
                Instant.now();

        Instant expiration =
                now.plus(
                        properties.getAccessTokenTtl()
                );

        return Jwts.builder()
                .subject(
                        principal
                                .getUserId()
                                .toString()
                )
                .issuer(
                        properties.getIssuer()
                )
                .issuedAt(
                        Date.from(now)
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

    public Claims parseClaims(
            String token
    ) {

        if (token == null
                || token.isBlank()) {

            throw new IllegalArgumentException(
                    "JWT must not be blank"
            );
        }

        Jws<Claims> signedClaims =
                parser.parseSignedClaims(
                        token
                );

        Claims claims =
                signedClaims.getPayload();

        validateRequiredClaims(
                claims
        );

        return claims;
    }

    public Long extractUserId(
            String token
    ) {

        String subject =
                parseClaims(token)
                        .getSubject();

        try {
            return Long.valueOf(subject);
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException(
                    "JWT subject must contain a valid user ID",
                    ex
            );
        }
    }

    public long getAccessTokenTtlSeconds() {
        return properties
                .getAccessTokenTtl()
                .toSeconds();
    }

    private void validateRequiredClaims(
            Claims claims
    ) {

        if (claims.getSubject() == null
                || claims.getSubject().isBlank()) {

            throw new IllegalArgumentException(
                    "JWT subject is required"
            );
        }

        if (claims.getIssuedAt() == null) {
            throw new IllegalArgumentException(
                    "JWT issued-at claim is required"
            );
        }

        if (claims.getExpiration() == null) {
            throw new IllegalArgumentException(
                    "JWT expiration claim is required"
            );
        }

        if (claims.getId() == null
                || claims.getId().isBlank()) {

            throw new IllegalArgumentException(
                    "JWT ID is required"
            );
        }
    }

    private byte[] decodeSecret(
            String secretBase64
    ) {

        try {
            return Decoders.BASE64.decode(
                    secretBase64
            );
        } catch (RuntimeException ex) {
            throw new IllegalArgumentException(
                    "JWT secret must be valid Base64",
                    ex
            );
        }
    }

    private void validateConfiguration(
            JwtProperties properties
    ) {

        if (properties == null) {
            throw new IllegalArgumentException(
                    "JWT configuration is required"
            );
        }

        if (properties.getIssuer() == null
                || properties
                .getIssuer()
                .isBlank()) {

            throw new IllegalArgumentException(
                    "JWT issuer is required"
            );
        }

        if (properties.getSecretBase64() == null
                || properties
                .getSecretBase64()
                .isBlank()) {

            throw new IllegalArgumentException(
                    "JWT secret is required"
            );
        }

        Duration ttl =
                properties
                        .getAccessTokenTtl();

        if (ttl == null
                || ttl.isZero()
                || ttl.isNegative()) {

            throw new IllegalArgumentException(
                    "JWT access token TTL must be positive"
            );
        }
    }
}