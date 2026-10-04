package com.pulsepass.service.impl;

import com.pulsepass.domain.Event;
import com.pulsepass.domain.EventStatus;
import com.pulsepass.domain.Ticket;
import com.pulsepass.domain.TicketStatus;
import com.pulsepass.domain.User;
import com.pulsepass.domain.UserProfile;
import com.pulsepass.dto.request.PurchaseTicketRequest;
import com.pulsepass.dto.response.TicketResponse;
import com.pulsepass.exception.BusinessRuleException;
import com.pulsepass.exception.ResourceNotFoundException;
import com.pulsepass.mapper.TicketMapper;
import com.pulsepass.repository.EventRepository;
import com.pulsepass.repository.TicketRepository;
import com.pulsepass.repository.UserRepository;
import com.pulsepass.service.TicketService;
import com.pulsepass.service.pricing.TicketPricingPolicy;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
import java.util.List;
import java.util.UUID;

@Service
public class TicketServiceImpl implements TicketService {

    private final TicketRepository ticketRepository;
    private final UserRepository userRepository;
    private final EventRepository eventRepository;
    private final TicketMapper ticketMapper;
    private final TicketPricingPolicy ticketPricingPolicy;

    public TicketServiceImpl(
            TicketRepository ticketRepository,
            UserRepository userRepository,
            EventRepository eventRepository,
            TicketMapper ticketMapper,
            TicketPricingPolicy ticketPricingPolicy
    ) {
        this.ticketRepository = ticketRepository;
        this.userRepository = userRepository;
        this.eventRepository = eventRepository;
        this.ticketMapper = ticketMapper;
        this.ticketPricingPolicy = ticketPricingPolicy;
    }

    @Override
    @Transactional
    public TicketResponse purchase(
            PurchaseTicketRequest request
    ) {

        User user = userRepository
                .findByEmailIgnoreCase(
                        request.userEmail()
                )
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "User not found: "
                                        + request.userEmail()
                        )
                );

        if (!user.isActive()) {
            throw new BusinessRuleException(
                    "User is inactive: "
                            + user.getEmail()
            );
        }

        Event event = eventRepository
                .findByEventCode(
                        request.eventCode()
                )
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Event not found: "
                                        + request.eventCode()
                        )
                );

        validateEventForPurchase(event);

        validateMinimumAge(
                user,
                event
        );

        long paidTickets =
                ticketRepository
                        .countByEventEventCodeAndStatus(
                                event.getEventCode(),
                                TicketStatus.PAID
                        );

        int capacity =
                event.getVenue().getCapacity();

        if (paidTickets >= capacity) {
            throw new BusinessRuleException(
                    "Event has no available capacity: "
                            + event.getEventCode()
            );
        }

        BigDecimal price =
                ticketPricingPolicy.calculate(
                        request.type()
                );

        if (price.signum() < 0) {
            throw new BusinessRuleException(
                    "Ticket price cannot be negative."
            );
        }

        Ticket ticket = new Ticket(
                generateTicketCode(),
                request.type(),
                price,
                TicketStatus.PAID,
                LocalDateTime.now(),
                user,
                event
        );

        Ticket savedTicket =
                ticketRepository.save(ticket);

        if (paidTickets + 1 == capacity) {

            event.changeStatus(
                    EventStatus.SOLD_OUT
            );

            eventRepository.save(event);
        }

        return ticketMapper.toResponse(
                savedTicket
        );
    }

    @Override
    @Transactional(readOnly = true)
    public TicketResponse findByCode(
            String ticketCode
    ) {

        Ticket ticket =
                findTicket(ticketCode);

        return ticketMapper.toResponse(ticket);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TicketResponse> findByUserEmail(
            String email
    ) {

        return ticketRepository
                .findByUserEmailIgnoreCaseOrderByPurchaseDateDesc(
                        email
                )
                .stream()
                .map(ticketMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<TicketResponse> findPaidTicketsByEvent(
            String eventCode
    ) {

        return ticketRepository
                .findByEventEventCodeAndStatus(
                        eventCode,
                        TicketStatus.PAID
                )
                .stream()
                .map(ticketMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional
    public TicketResponse cancel(
            String ticketCode
    ) {

        Ticket ticket =
                findTicket(ticketCode);

        if (ticket.getStatus()
                != TicketStatus.PAID) {

            throw new BusinessRuleException(
                    "Only PAID tickets can be cancelled."
            );
        }

        if (LocalDateTime.now()
                .isAfter(
                        ticket.getEvent()
                                .getEventDate()
                )) {

            throw new BusinessRuleException(
                    "Ticket cannot be cancelled after the event date."
            );
        }

        ticket.changeStatus(
                TicketStatus.CANCELLED
        );

        Ticket savedTicket =
                ticketRepository.save(ticket);

        return ticketMapper.toResponse(
                savedTicket
        );
    }

    @Override
    @Transactional
    public TicketResponse markAsUsed(
            String ticketCode
    ) {

        Ticket ticket =
                findTicket(ticketCode);

        if (ticket.getStatus()
                != TicketStatus.PAID) {

            throw new BusinessRuleException(
                    "Only PAID tickets can be marked as used."
            );
        }

        ticket.changeStatus(
                TicketStatus.USED
        );

        Ticket savedTicket =
                ticketRepository.save(ticket);

        return ticketMapper.toResponse(
                savedTicket
        );
    }

    private Ticket findTicket(
            String ticketCode
    ) {

        return ticketRepository
                .findByTicketCode(ticketCode)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Ticket not found: "
                                        + ticketCode
                        )
                );
    }

    private void validateEventForPurchase(
            Event event
    ) {

        if (event.getStatus()
                != EventStatus.PUBLISHED) {

            throw new BusinessRuleException(
                    "Tickets can only be purchased for PUBLISHED events."
            );
        }

        if (!event.getEventDate()
                .isAfter(LocalDateTime.now())) {

            throw new BusinessRuleException(
                    "Cannot purchase tickets for an event that already occurred."
            );
        }
    }

    private void validateMinimumAge(
            User user,
            Event event
    ) {

        Integer minimumAge =
                event.getMinimumAge();

        if (minimumAge == null
                || minimumAge == 0) {
            return;
        }

        UserProfile profile =
                user.getProfile();

        if (profile == null
                || profile.getBirthDate() == null) {

            throw new BusinessRuleException(
                    "User birth date is required to validate minimum age."
            );
        }

        LocalDate eventDate =
                event.getEventDate()
                        .toLocalDate();

        int ageAtEvent =
                Period.between(
                        profile.getBirthDate(),
                        eventDate
                ).getYears();

        if (ageAtEvent < minimumAge) {

            throw new BusinessRuleException(
                    "User does not meet minimum age."
            );
        }
    }

    private String generateTicketCode() {

        return "TKT-"
                + UUID.randomUUID()
                .toString()
                .toUpperCase();
    }
}