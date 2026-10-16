package com.pulsepass.service;

import com.pulsepass.domain.Event;
import com.pulsepass.domain.EventStatus;
import com.pulsepass.domain.Ticket;
import com.pulsepass.domain.User;
import com.pulsepass.exception.ResourceNotFoundException;
import com.pulsepass.security.SecurityPrincipal;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AuthorizationServiceTest {

    private final AuthorizationService authorizationService =
            new AuthorizationService();

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void shouldAllowUserToAccessOwnUserData() {

        authenticate(
                1L,
                "ROLE_USER"
        );

        User user =
                userWithId(1L);

        assertThatCode(() ->
                authorizationService
                        .requireSelf(user)
        ).doesNotThrowAnyException();
    }

    @Test
    void shouldRejectUserAccessingAnotherUser() {

        authenticate(
                1L,
                "ROLE_USER"
        );

        User anotherUser =
                userWithId(2L);

        assertThatThrownBy(() ->
                authorizationService
                        .requireSelf(
                                anotherUser
                        )
        ).isInstanceOf(
                AccessDeniedException.class
        );
    }

    @Test
    void shouldAllowAdminToAccessAnotherUser() {

        authenticate(
                99L,
                "ROLE_ADMIN"
        );

        User anotherUser =
                userWithId(2L);

        assertThatCode(() ->
                authorizationService
                        .requireSelfOrAdmin(
                                anotherUser
                        )
        ).doesNotThrowAnyException();
    }

    @Test
    void shouldAllowUserToReadOwnTicket() {

        authenticate(
                1L,
                "ROLE_USER"
        );

        User owner =
                userWithId(1L);

        Ticket ticket =
                ticketWithOwner(owner);

        assertThatCode(() ->
                authorizationService
                        .requireTicketReadAccess(
                                ticket
                        )
        ).doesNotThrowAnyException();
    }

    @Test
    void shouldRejectUserReadingAnotherUsersTicket() {

        authenticate(
                1L,
                "ROLE_USER"
        );

        User owner =
                userWithId(2L);

        Ticket ticket =
                ticketWithOwner(owner);

        assertThatThrownBy(() ->
                authorizationService
                        .requireTicketReadAccess(
                                ticket
                        )
        ).isInstanceOf(
                AccessDeniedException.class
        );
    }

    @Test
    void shouldAllowStaffToReadTicketOperationally() {

        authenticate(
                10L,
                "ROLE_STAFF"
        );

        User owner =
                userWithId(2L);

        Ticket ticket =
                ticketWithOwner(owner);

        assertThatCode(() ->
                authorizationService
                        .requireTicketReadAccess(
                                ticket
                        )
        ).doesNotThrowAnyException();
    }

    @Test
    void shouldAllowAdminToReadAnyTicket() {

        authenticate(
                99L,
                "ROLE_ADMIN"
        );

        User owner =
                userWithId(2L);

        Ticket ticket =
                ticketWithOwner(owner);

        assertThatCode(() ->
                authorizationService
                        .requireTicketReadAccess(
                                ticket
                        )
        ).doesNotThrowAnyException();
    }

    @Test
    void shouldAllowUserToCancelOwnTicket() {

        authenticate(
                1L,
                "ROLE_USER"
        );

        User owner =
                userWithId(1L);

        Ticket ticket =
                ticketWithOwner(owner);

        assertThatCode(() ->
                authorizationService
                        .requireTicketCancelAccess(
                                ticket
                        )
        ).doesNotThrowAnyException();
    }

    @Test
    void shouldRejectStaffCancellingAnotherUsersTicket() {

        authenticate(
                10L,
                "ROLE_STAFF"
        );

        User owner =
                userWithId(2L);

        Ticket ticket =
                ticketWithOwner(owner);

        assertThatThrownBy(() ->
                authorizationService
                        .requireTicketCancelAccess(
                                ticket
                        )
        ).isInstanceOf(
                AccessDeniedException.class
        );
    }

    @Test
    void shouldAllowAdminToCancelTicket() {

        authenticate(
                99L,
                "ROLE_ADMIN"
        );

        User owner =
                userWithId(2L);

        Ticket ticket =
                ticketWithOwner(owner);

        assertThatCode(() ->
                authorizationService
                        .requireTicketCancelAccess(
                                ticket
                        )
        ).doesNotThrowAnyException();
    }

    @Test
    void shouldAllowAnonymousToViewPublishedEvent() {

        Event event =
                eventWithStatus(
                        EventStatus.PUBLISHED
                );

        assertThatCode(() ->
                authorizationService
                        .requireEventVisible(
                                event
                        )
        ).doesNotThrowAnyException();
    }

    @Test
    void shouldHideDraftEventFromAnonymousUser() {

        Event event =
                eventWithStatus(
                        EventStatus.DRAFT
                );

        assertThatThrownBy(() ->
                authorizationService
                        .requireEventVisible(
                                event
                        )
        )
                .isInstanceOf(
                        ResourceNotFoundException.class
                )
                .hasMessageContaining(
                        "CMF-2026"
                );
    }

    @Test
    void shouldHideDraftEventFromRegularUser() {

        authenticate(
                1L,
                "ROLE_USER"
        );

        Event event =
                eventWithStatus(
                        EventStatus.DRAFT
                );

        assertThatThrownBy(() ->
                authorizationService
                        .requireEventVisible(
                                event
                        )
        ).isInstanceOf(
                ResourceNotFoundException.class
        );
    }

    @Test
    void shouldAllowStaffToViewDraftEvent() {

        authenticate(
                10L,
                "ROLE_STAFF"
        );

        Event event =
                eventWithStatus(
                        EventStatus.DRAFT
                );

        assertThatCode(() ->
                authorizationService
                        .requireEventVisible(
                                event
                        )
        ).doesNotThrowAnyException();

        assertThat(
                authorizationService
                        .canViewUnpublishedEvents()
        ).isTrue();
    }

    @Test
    void shouldAllowAdminToViewDraftEvent() {

        authenticate(
                99L,
                "ROLE_ADMIN"
        );

        Event event =
                eventWithStatus(
                        EventStatus.DRAFT
                );

        assertThatCode(() ->
                authorizationService
                        .requireEventVisible(
                                event
                        )
        ).doesNotThrowAnyException();

        assertThat(
                authorizationService
                        .canViewUnpublishedEvents()
        ).isTrue();
    }

    private void authenticate(
            Long userId,
            String authority
    ) {

        SecurityPrincipal principal =
                new SecurityPrincipal(
                        userId,
                        "test-user",
                        "test@pulsepass.com",
                        "$2a$10$testHash",
                        true,
                        List.of(
                                new SimpleGrantedAuthority(
                                        authority
                                )
                        )
                );

        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(
                        principal,
                        null,
                        principal.getAuthorities()
                );

        SecurityContext context =
                SecurityContextHolder
                        .createEmptyContext();

        context.setAuthentication(
                authentication
        );

        SecurityContextHolder.setContext(
                context
        );
    }

    private User userWithId(
            Long id
    ) {

        User user =
                mock(User.class);

        when(
                user.getId()
        ).thenReturn(id);

        return user;
    }

    private Ticket ticketWithOwner(
            User owner
    ) {

        Ticket ticket =
                mock(Ticket.class);

        when(
                ticket.getUser()
        ).thenReturn(owner);

        return ticket;
    }

    private Event eventWithStatus(
            EventStatus status
    ) {

        Event event =
                mock(Event.class);

        when(
                event.getStatus()
        ).thenReturn(status);

        when(
                event.getEventCode()
        ).thenReturn(
                "CMF-2026"
        );

        return event;
    }
}