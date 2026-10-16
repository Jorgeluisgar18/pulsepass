package com.pulsepass.service.impl;

import com.pulsepass.domain.Event;
import com.pulsepass.domain.EventCategory;
import com.pulsepass.domain.EventStatus;
import com.pulsepass.domain.Ticket;
import com.pulsepass.domain.TicketStatus;
import com.pulsepass.domain.TicketType;
import com.pulsepass.domain.User;
import com.pulsepass.domain.UserProfile;
import com.pulsepass.domain.Venue;

import com.pulsepass.dto.request.PurchaseTicketRequest;
import com.pulsepass.dto.response.TicketResponse;

import com.pulsepass.exception.BusinessRuleException;
import com.pulsepass.exception.ResourceNotFoundException;

import com.pulsepass.mapper.TicketMapper;

import com.pulsepass.repository.EventRepository;
import com.pulsepass.repository.TicketRepository;
import com.pulsepass.repository.UserRepository;

import com.pulsepass.service.AuthorizationService;
import com.pulsepass.service.pricing.TicketPricingPolicy;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.security.access.AccessDeniedException;

import java.math.BigDecimal;

import java.time.LocalDate;
import java.time.LocalDateTime;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TicketServiceImplTest {

    @Mock
    private TicketRepository ticketRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private EventRepository eventRepository;

    @Mock
    private TicketMapper ticketMapper;

    @Mock
    private TicketPricingPolicy ticketPricingPolicy;

    @Mock
    private AuthorizationService authorizationService;

    @InjectMocks
    private TicketServiceImpl ticketService;

    @Test
    void shouldPurchaseTicketSuccessfully() {

        User user =
                activeAdultUser();

        Event event =
                eventWithStatus(
                        EventStatus.PUBLISHED,
                        18,
                        3
                );

        PurchaseTicketRequest request =
                validRequest();

        BigDecimal price =
                new BigDecimal(
                        "120000.00"
                );

        TicketResponse expectedResponse =
                ticketResponse(
                        TicketStatus.PAID,
                        price
                );

        when(
                userRepository.findByEmailIgnoreCase(
                        request.userEmail()
                )
        ).thenReturn(
                Optional.of(user)
        );

        when(
                eventRepository.findByEventCode(
                        request.eventCode()
                )
        ).thenReturn(
                Optional.of(event)
        );

        when(
                ticketRepository
                        .countByEventEventCodeAndStatus(
                                eq("CMF-2026"),
                                eq(TicketStatus.PAID)
                        )
        ).thenReturn(
                0L
        );

        when(
                ticketPricingPolicy.calculate(
                        TicketType.GENERAL
                )
        ).thenReturn(
                price
        );

        when(
                ticketRepository.save(
                        any(Ticket.class)
                )
        ).thenAnswer(invocation ->
                invocation.getArgument(
                        0,
                        Ticket.class
                )
        );

        when(
                ticketMapper.toResponse(
                        any(Ticket.class)
                )
        ).thenReturn(
                expectedResponse
        );

        TicketResponse result =
                ticketService.purchase(
                        request
                );

        ArgumentCaptor<Ticket> captor =
                ArgumentCaptor.forClass(
                        Ticket.class
                );

        verify(ticketRepository)
                .save(
                        captor.capture()
                );

        Ticket savedTicket =
                captor.getValue();

        assertThat(result)
                .isEqualTo(
                        expectedResponse
                );

        assertThat(
                savedTicket.getStatus()
        ).isEqualTo(
                TicketStatus.PAID
        );

        assertThat(
                savedTicket.getType()
        ).isEqualTo(
                TicketType.GENERAL
        );

        assertThat(
                savedTicket.getPrice()
        ).isEqualByComparingTo(
                price
        );

        assertThat(
                savedTicket.getUser()
        ).isSameAs(
                user
        );

        assertThat(
                savedTicket.getEvent()
        ).isSameAs(
                event
        );

        assertThat(
                savedTicket.getTicketCode()
        ).startsWith(
                "TKT-"
        );

        verify(authorizationService)
                .requireSelf(
                        user
                );

        verify(
                eventRepository,
                never()
        ).save(
                any(Event.class)
        );
    }

    @Test
    void shouldRejectPurchaseWhenUserDoesNotExist() {

        PurchaseTicketRequest request =
                validRequest();

        when(
                userRepository.findByEmailIgnoreCase(
                        request.userEmail()
                )
        ).thenReturn(
                Optional.empty()
        );

        assertThatThrownBy(() ->
                ticketService.purchase(
                        request
                )
        )
                .isInstanceOf(
                        ResourceNotFoundException.class
                )
                .hasMessageContaining(
                        request.userEmail()
                );

        verify(
                authorizationService,
                never()
        ).requireSelf(
                any(User.class)
        );

        verify(
                eventRepository,
                never()
        ).findByEventCode(
                anyString()
        );

        verify(
                ticketRepository,
                never()
        ).save(
                any(Ticket.class)
        );
    }

    @Test
    void shouldRejectPurchaseForAnotherUser() {

        User user =
                activeAdultUser();

        PurchaseTicketRequest request =
                validRequest();

        when(
                userRepository.findByEmailIgnoreCase(
                        request.userEmail()
                )
        ).thenReturn(
                Optional.of(user)
        );

        doThrow(
                new AccessDeniedException(
                        "Access denied"
                )
        )
                .when(
                        authorizationService
                )
                .requireSelf(
                        user
                );

        assertThatThrownBy(() ->
                ticketService.purchase(
                        request
                )
        ).isInstanceOf(
                AccessDeniedException.class
        );

        verify(
                eventRepository,
                never()
        ).findByEventCode(
                anyString()
        );

        verify(
                ticketRepository,
                never()
        ).save(
                any(Ticket.class)
        );
    }

    @Test
    void shouldRejectPurchaseWhenUserIsInactive() {

        User user =
                user(
                        false,
                        LocalDate.now()
                                .minusYears(25)
                );

        PurchaseTicketRequest request =
                validRequest();

        when(
                userRepository.findByEmailIgnoreCase(
                        request.userEmail()
                )
        ).thenReturn(
                Optional.of(user)
        );

        assertThatThrownBy(() ->
                ticketService.purchase(
                        request
                )
        )
                .isInstanceOf(
                        BusinessRuleException.class
                )
                .hasMessageContaining(
                        "inactive"
                );

        verify(
                eventRepository,
                never()
        ).findByEventCode(
                anyString()
        );
    }

    @Test
    void shouldRejectPurchaseForDraftEvent() {

        User user =
                activeAdultUser();

        Event event =
                eventWithStatus(
                        EventStatus.DRAFT,
                        18,
                        3
                );

        PurchaseTicketRequest request =
                validRequest();

        when(
                userRepository.findByEmailIgnoreCase(
                        request.userEmail()
                )
        ).thenReturn(
                Optional.of(user)
        );

        when(
                eventRepository.findByEventCode(
                        request.eventCode()
                )
        ).thenReturn(
                Optional.of(event)
        );

        assertThatThrownBy(() ->
                ticketService.purchase(
                        request
                )
        )
                .isInstanceOf(
                        BusinessRuleException.class
                )
                .hasMessageContaining(
                        "PUBLISHED"
                );

        verify(
                ticketRepository,
                never()
        ).save(
                any(Ticket.class)
        );
    }

    @Test
    void shouldRejectPurchaseForCancelledEvent() {

        User user =
                activeAdultUser();

        Event event =
                eventWithStatus(
                        EventStatus.CANCELLED,
                        18,
                        3
                );

        PurchaseTicketRequest request =
                validRequest();

        when(
                userRepository.findByEmailIgnoreCase(
                        request.userEmail()
                )
        ).thenReturn(
                Optional.of(user)
        );

        when(
                eventRepository.findByEventCode(
                        request.eventCode()
                )
        ).thenReturn(
                Optional.of(event)
        );

        assertThatThrownBy(() ->
                ticketService.purchase(
                        request
                )
        )
                .isInstanceOf(
                        BusinessRuleException.class
                )
                .hasMessageContaining(
                        "PUBLISHED"
                );

        verify(
                ticketRepository,
                never()
        ).save(
                any(Ticket.class)
        );
    }

    @Test
    void shouldRejectUnderageUser() {

        Event event =
                eventWithStatus(
                        EventStatus.PUBLISHED,
                        18,
                        3
                );

        LocalDate birthDate =
                event.getEventDate()
                        .toLocalDate()
                        .minusYears(17);

        User underageUser =
                user(
                        true,
                        birthDate
                );

        PurchaseTicketRequest request =
                validRequest();

        when(
                userRepository.findByEmailIgnoreCase(
                        request.userEmail()
                )
        ).thenReturn(
                Optional.of(
                        underageUser
                )
        );

        when(
                eventRepository.findByEventCode(
                        request.eventCode()
                )
        ).thenReturn(
                Optional.of(event)
        );

        assertThatThrownBy(() ->
                ticketService.purchase(
                        request
                )
        )
                .isInstanceOf(
                        BusinessRuleException.class
                )
                .hasMessageContaining(
                        "minimum age"
                );

        verify(
                ticketRepository,
                never()
        ).save(
                any(Ticket.class)
        );
    }

    @Test
    void shouldRejectPurchaseWhenEventHasNoCapacity() {

        User user =
                activeAdultUser();

        Event event =
                eventWithStatus(
                        EventStatus.PUBLISHED,
                        18,
                        3
                );

        PurchaseTicketRequest request =
                validRequest();

        when(
                userRepository.findByEmailIgnoreCase(
                        request.userEmail()
                )
        ).thenReturn(
                Optional.of(user)
        );

        when(
                eventRepository.findByEventCode(
                        request.eventCode()
                )
        ).thenReturn(
                Optional.of(event)
        );

        when(
                ticketRepository
                        .countByEventEventCodeAndStatus(
                                "CMF-2026",
                                TicketStatus.PAID
                        )
        ).thenReturn(
                3L
        );

        assertThatThrownBy(() ->
                ticketService.purchase(
                        request
                )
        )
                .isInstanceOf(
                        BusinessRuleException.class
                )
                .hasMessageContaining(
                        "capacity"
                );

        verify(
                ticketPricingPolicy,
                never()
        ).calculate(
                any(TicketType.class)
        );

        verify(
                ticketRepository,
                never()
        ).save(
                any(Ticket.class)
        );
    }

    @Test
    void shouldMarkEventSoldOutWhenLastTicketIsPurchased() {

        User user =
                activeAdultUser();

        Event event =
                eventWithStatus(
                        EventStatus.PUBLISHED,
                        18,
                        3
                );

        PurchaseTicketRequest request =
                validRequest();

        BigDecimal price =
                new BigDecimal(
                        "120000.00"
                );

        TicketResponse expectedResponse =
                ticketResponse(
                        TicketStatus.PAID,
                        price
                );

        when(
                userRepository.findByEmailIgnoreCase(
                        request.userEmail()
                )
        ).thenReturn(
                Optional.of(user)
        );

        when(
                eventRepository.findByEventCode(
                        request.eventCode()
                )
        ).thenReturn(
                Optional.of(event)
        );

        when(
                ticketRepository
                        .countByEventEventCodeAndStatus(
                                "CMF-2026",
                                TicketStatus.PAID
                        )
        ).thenReturn(
                2L
        );

        when(
                ticketPricingPolicy.calculate(
                        TicketType.GENERAL
                )
        ).thenReturn(
                price
        );

        when(
                ticketRepository.save(
                        any(Ticket.class)
                )
        ).thenAnswer(invocation ->
                invocation.getArgument(
                        0,
                        Ticket.class
                )
        );

        when(
                eventRepository.save(
                        event
                )
        ).thenReturn(
                event
        );

        when(
                ticketMapper.toResponse(
                        any(Ticket.class)
                )
        ).thenReturn(
                expectedResponse
        );

        TicketResponse result =
                ticketService.purchase(
                        request
                );

        assertThat(result)
                .isEqualTo(
                        expectedResponse
                );

        assertThat(
                event.getStatus()
        ).isEqualTo(
                EventStatus.SOLD_OUT
        );

        verify(eventRepository)
                .save(
                        event
                );
    }

    @Test
    void shouldFindTicketByCodeWhenAuthorized() {

        Event event =
                eventWithStatus(
                        EventStatus.PUBLISHED,
                        18,
                        3
                );

        Ticket ticket =
                ticket(
                        event,
                        TicketStatus.PAID
                );

        TicketResponse response =
                ticketResponse(
                        TicketStatus.PAID,
                        ticket.getPrice()
                );

        when(
                ticketRepository.findByTicketCode(
                        "TKT-001"
                )
        ).thenReturn(
                Optional.of(ticket)
        );

        when(
                ticketMapper.toResponse(
                        ticket
                )
        ).thenReturn(
                response
        );

        TicketResponse result =
                ticketService.findByCode(
                        "TKT-001"
                );

        assertThat(result)
                .isEqualTo(
                        response
                );

        verify(authorizationService)
                .requireTicketReadAccess(
                        ticket
                );
    }

    @Test
    void shouldRejectReadingAnotherUsersTicket() {

        Event event =
                eventWithStatus(
                        EventStatus.PUBLISHED,
                        18,
                        3
                );

        Ticket ticket =
                ticket(
                        event,
                        TicketStatus.PAID
                );

        when(
                ticketRepository.findByTicketCode(
                        "TKT-001"
                )
        ).thenReturn(
                Optional.of(ticket)
        );

        doThrow(
                new AccessDeniedException(
                        "Access denied"
                )
        )
                .when(
                        authorizationService
                )
                .requireTicketReadAccess(
                        ticket
                );

        assertThatThrownBy(() ->
                ticketService.findByCode(
                        "TKT-001"
                )
        ).isInstanceOf(
                AccessDeniedException.class
        );

        verify(
                ticketMapper,
                never()
        ).toResponse(
                any(Ticket.class)
        );
    }

    @Test
    void shouldFindTicketsByUserEmailWhenAuthorized() {

        User user =
                activeAdultUser();

        Event event =
                eventWithStatus(
                        EventStatus.PUBLISHED,
                        18,
                        3
                );

        Ticket ticket =
                ticket(
                        event,
                        TicketStatus.PAID
                );

        TicketResponse response =
                ticketResponse(
                        TicketStatus.PAID,
                        ticket.getPrice()
                );

        when(
                userRepository.findByEmailIgnoreCase(
                        "andrea@email.com"
                )
        ).thenReturn(
                Optional.of(user)
        );

        when(
                ticketRepository
                        .findByUserEmailIgnoreCaseOrderByPurchaseDateDesc(
                                "andrea@email.com"
                        )
        ).thenReturn(
                List.of(ticket)
        );

        when(
                ticketMapper.toResponse(
                        ticket
                )
        ).thenReturn(
                response
        );

        List<TicketResponse> result =
                ticketService.findByUserEmail(
                        "andrea@email.com"
                );

        assertThat(result)
                .containsExactly(
                        response
                );

        verify(authorizationService)
                .requireSelfOrAdmin(
                        user
                );
    }

    @Test
    void shouldCancelPaidTicket() {

        Event event =
                eventWithStatus(
                        EventStatus.PUBLISHED,
                        18,
                        3
                );

        Ticket ticket =
                ticket(
                        event,
                        TicketStatus.PAID
                );

        TicketResponse expectedResponse =
                ticketResponse(
                        TicketStatus.CANCELLED,
                        ticket.getPrice()
                );

        when(
                ticketRepository.findByTicketCode(
                        "TKT-001"
                )
        ).thenReturn(
                Optional.of(ticket)
        );

        when(
                ticketRepository.save(
                        ticket
                )
        ).thenReturn(
                ticket
        );

        when(
                ticketMapper.toResponse(
                        ticket
                )
        ).thenReturn(
                expectedResponse
        );

        TicketResponse result =
                ticketService.cancel(
                        "TKT-001"
                );

        assertThat(
                ticket.getStatus()
        ).isEqualTo(
                TicketStatus.CANCELLED
        );

        assertThat(result)
                .isEqualTo(
                        expectedResponse
                );

        verify(authorizationService)
                .requireTicketCancelAccess(
                        ticket
                );

        verify(ticketRepository)
                .save(
                        ticket
                );
    }

    @Test
    void shouldRejectCancellationOfAnotherUsersTicketBeforeMutation() {

        Event event =
                eventWithStatus(
                        EventStatus.PUBLISHED,
                        18,
                        3
                );

        Ticket ticket =
                ticket(
                        event,
                        TicketStatus.PAID
                );

        when(
                ticketRepository.findByTicketCode(
                        "TKT-001"
                )
        ).thenReturn(
                Optional.of(ticket)
        );

        doThrow(
                new AccessDeniedException(
                        "Access denied"
                )
        )
                .when(
                        authorizationService
                )
                .requireTicketCancelAccess(
                        ticket
                );

        assertThatThrownBy(() ->
                ticketService.cancel(
                        "TKT-001"
                )
        ).isInstanceOf(
                AccessDeniedException.class
        );

        assertThat(
                ticket.getStatus()
        ).isEqualTo(
                TicketStatus.PAID
        );

        verify(
                ticketRepository,
                never()
        ).save(
                any(Ticket.class)
        );
    }

    @Test
    void shouldRejectCancellationOfUsedTicket() {

        Event event =
                eventWithStatus(
                        EventStatus.PUBLISHED,
                        18,
                        3
                );

        Ticket ticket =
                ticket(
                        event,
                        TicketStatus.USED
                );

        when(
                ticketRepository.findByTicketCode(
                        "TKT-001"
                )
        ).thenReturn(
                Optional.of(ticket)
        );

        assertThatThrownBy(() ->
                ticketService.cancel(
                        "TKT-001"
                )
        )
                .isInstanceOf(
                        BusinessRuleException.class
                )
                .hasMessageContaining(
                        "PAID"
                );

        verify(
                ticketRepository,
                never()
        ).save(
                any(Ticket.class)
        );
    }

    @Test
    void shouldMarkPaidTicketAsUsed() {

        Event event =
                eventWithStatus(
                        EventStatus.PUBLISHED,
                        18,
                        3
                );

        Ticket ticket =
                ticket(
                        event,
                        TicketStatus.PAID
                );

        TicketResponse expectedResponse =
                ticketResponse(
                        TicketStatus.USED,
                        ticket.getPrice()
                );

        when(
                ticketRepository.findByTicketCode(
                        "TKT-001"
                )
        ).thenReturn(
                Optional.of(ticket)
        );

        when(
                ticketRepository.save(
                        ticket
                )
        ).thenReturn(
                ticket
        );

        when(
                ticketMapper.toResponse(
                        ticket
                )
        ).thenReturn(
                expectedResponse
        );

        TicketResponse result =
                ticketService.markAsUsed(
                        "TKT-001"
                );

        assertThat(
                ticket.getStatus()
        ).isEqualTo(
                TicketStatus.USED
        );

        assertThat(result)
                .isEqualTo(
                        expectedResponse
                );
    }

    @Test
    void shouldRejectUsingCancelledTicket() {

        Event event =
                eventWithStatus(
                        EventStatus.PUBLISHED,
                        18,
                        3
                );

        Ticket ticket =
                ticket(
                        event,
                        TicketStatus.CANCELLED
                );

        when(
                ticketRepository.findByTicketCode(
                        "TKT-001"
                )
        ).thenReturn(
                Optional.of(ticket)
        );

        assertThatThrownBy(() ->
                ticketService.markAsUsed(
                        "TKT-001"
                )
        )
                .isInstanceOf(
                        BusinessRuleException.class
                )
                .hasMessageContaining(
                        "PAID"
                );

        verify(
                ticketRepository,
                never()
        ).save(
                any(Ticket.class)
        );
    }

    private PurchaseTicketRequest validRequest() {

        return new PurchaseTicketRequest(
                "andrea@email.com",
                "CMF-2026",
                TicketType.GENERAL
        );
    }

    private User activeAdultUser() {

        return user(
                true,
                LocalDate.now()
                        .minusYears(25)
        );
    }

    private User user(
            boolean active,
            LocalDate birthDate
    ) {

        User user =
                new User(
                        "andrea",
                        "andrea@email.com",
                        active
                );

        UserProfile profile =
                new UserProfile(
                        "Andrea",
                        "Martinez",
                        "3001112233",
                        "Santa Marta",
                        birthDate
                );

        user.assignProfile(
                profile
        );

        return user;
    }

    private Event eventWithStatus(
            EventStatus status,
            int minimumAge,
            int capacity
    ) {

        Venue venue =
                new Venue(
                        "VEN-SMR-01",
                        "Marina Convention Center",
                        "Santa Marta",
                        "Avenida del Libertador",
                        capacity,
                        true
                );

        return new Event(
                "CMF-2026",
                "Caribbean Music Fest 2026",
                "Festival musical",
                EventCategory.MUSIC,
                status,
                LocalDateTime.now()
                        .plusMonths(6),
                minimumAge,
                venue
        );
    }

    private Ticket ticket(
            Event event,
            TicketStatus status
    ) {

        return new Ticket(
                "TKT-001",
                TicketType.GENERAL,
                new BigDecimal(
                        "120000.00"
                ),
                status,
                LocalDateTime.now(),
                activeAdultUser(),
                event
        );
    }

    private TicketResponse ticketResponse(
            TicketStatus status,
            BigDecimal price
    ) {

        return new TicketResponse(
                null,
                "TKT-001",
                TicketType.GENERAL,
                price,
                status,
                LocalDateTime.now(),
                "andrea@email.com",
                "CMF-2026",
                "Caribbean Music Fest 2026"
        );
    }
}