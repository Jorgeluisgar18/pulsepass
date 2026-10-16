package com.pulsepass.service;

import com.pulsepass.domain.Event;
import com.pulsepass.domain.EventStatus;
import com.pulsepass.domain.Role;
import com.pulsepass.domain.Ticket;
import com.pulsepass.domain.User;
import com.pulsepass.exception.ResourceNotFoundException;
import com.pulsepass.security.SecurityPrincipal;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.Objects;

@Service
public class AuthorizationService {

    public void requireSelf(
            User requestedUser
    ) {

        SecurityPrincipal principal =
                requirePrincipal();

        if (!Objects.equals(
                principal.getUserId(),
                requestedUser.getId()
        )) {
            throw forbidden();
        }
    }

    public void requireSelfOrAdmin(
            User requestedUser
    ) {

        if (hasRole(Role.ADMIN)) {
            return;
        }

        requireSelf(
                requestedUser
        );
    }

    public void requireTicketReadAccess(
            Ticket ticket
    ) {

        if (hasAnyRole(
                Role.STAFF,
                Role.ADMIN
        )) {
            return;
        }

        requireSelf(
                ticket.getUser()
        );
    }

    public void requireTicketCancelAccess(
            Ticket ticket
    ) {

        if (hasRole(Role.ADMIN)) {
            return;
        }

        requireSelf(
                ticket.getUser()
        );
    }

    public void requireEventVisible(
            Event event
    ) {

        if (event.getStatus()
                == EventStatus.PUBLISHED) {
            return;
        }

        if (canViewUnpublishedEvents()) {
            return;
        }

        /*
         * Para visitantes y USER ocultamos incluso
         * la existencia del borrador.
         */
        throw new ResourceNotFoundException(
                "Event not found: "
                        + event.getEventCode()
        );
    }

    public boolean canViewUnpublishedEvents() {

        return hasAnyRole(
                Role.STAFF,
                Role.ADMIN
        );
    }

    public boolean hasRole(
            Role role
    ) {

        SecurityPrincipal principal =
                currentPrincipalOrNull();

        if (principal == null) {
            return false;
        }

        String requiredAuthority =
                "ROLE_" + role.name();

        return principal
                .getAuthorities()
                .stream()
                .map(
                        GrantedAuthority::getAuthority
                )
                .anyMatch(
                        requiredAuthority::equals
                );
    }

    public boolean hasAnyRole(
            Role... roles
    ) {

        return Arrays.stream(roles)
                .anyMatch(this::hasRole);
    }

    private SecurityPrincipal requirePrincipal() {

        SecurityPrincipal principal =
                currentPrincipalOrNull();

        if (principal == null) {
            throw forbidden();
        }

        return principal;
    }

    private SecurityPrincipal currentPrincipalOrNull() {

        Authentication authentication =
                SecurityContextHolder
                        .getContext()
                        .getAuthentication();

        if (authentication == null
                || !authentication.isAuthenticated()) {
            return null;
        }

        Object principal =
                authentication.getPrincipal();

        if (!(principal
                instanceof SecurityPrincipal securityPrincipal)) {
            return null;
        }

        return securityPrincipal;
    }

    private AccessDeniedException forbidden() {

        return new AccessDeniedException(
                "Access denied"
        );
    }
}