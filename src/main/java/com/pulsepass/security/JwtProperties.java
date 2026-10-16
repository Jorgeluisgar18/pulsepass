package com.pulsepass.security;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

@Validated
@ConfigurationProperties(
        prefix = "pulsepass.security.jwt"
)
public class JwtProperties {

    @NotBlank
    private String issuer;

    @NotBlank
    private String secretBase64;

    @NotNull
    private Duration accessTokenTtl;

    public String getIssuer() {
        return issuer;
    }

    public void setIssuer(
            String issuer
    ) {
        this.issuer = issuer;
    }

    public String getSecretBase64() {
        return secretBase64;
    }

    public void setSecretBase64(
            String secretBase64
    ) {
        this.secretBase64 = secretBase64;
    }

    public Duration getAccessTokenTtl() {
        return accessTokenTtl;
    }

    public void setAccessTokenTtl(
            Duration accessTokenTtl
    ) {
        this.accessTokenTtl = accessTokenTtl;
    }
}