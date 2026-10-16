package com.pulsepass.security;

import com.pulsepass.domain.User;
import com.pulsepass.domain.UserCredential;
import com.pulsepass.domain.UserRole;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

public class SecurityPrincipal implements UserDetails {

    private final Long userId;
    private final String username;
    private final String email;
    private final String passwordHash;
    private final boolean active;
    private final List<GrantedAuthority> authorities;

    public SecurityPrincipal(
            Long userId,
            String username,
            String email,
            String passwordHash,
            boolean active,
            List<GrantedAuthority> authorities
    ) {
        this.userId = userId;
        this.username = username;
        this.email = email;
        this.passwordHash = passwordHash;
        this.active = active;
        this.authorities = List.copyOf(authorities);
    }

    public static SecurityPrincipal from(
            UserCredential credential,
            List<UserRole> roles
    ) {

        User user = credential.getUser();

        List<GrantedAuthority> authorities =
                roles.stream()
                        .map(UserRole::getRole)
                        .map(role ->
                                new SimpleGrantedAuthority(
                                        "ROLE_" + role.name()
                                )
                        )
                        .map(GrantedAuthority.class::cast)
                        .toList();

        return new SecurityPrincipal(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                credential.getPasswordHash(),
                user.isActive(),
                authorities
        );
    }

    public Long getUserId() {
        return userId;
    }

    public String getEmail() {
        return email;
    }

    public boolean isActive() {
        return active;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getPassword() {
        return passwordHash;
    }

    @Override
    public String getUsername() {
        return username;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return active;
    }
}