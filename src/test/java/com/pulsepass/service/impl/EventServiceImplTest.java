package com.pulsepass.service.impl;

import com.pulsepass.domain.Artist;
import com.pulsepass.domain.Event;
import com.pulsepass.domain.EventCategory;
import com.pulsepass.domain.EventStatus;
import com.pulsepass.domain.Venue;

import com.pulsepass.dto.request.CreateEventRequest;
import com.pulsepass.dto.response.EventResponse;
import com.pulsepass.dto.response.EventSummaryResponse;

import com.pulsepass.exception.BusinessRuleException;
import com.pulsepass.exception.ResourceNotFoundException;

import com.pulsepass.mapper.EventMapper;

import com.pulsepass.repository.ArtistRepository;
import com.pulsepass.repository.EventRepository;
import com.pulsepass.repository.VenueRepository;

import com.pulsepass.service.AuthorizationService;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import static org.mockito.ArgumentMatchers.any;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EventServiceImplTest {

    @Mock
    private EventRepository eventRepository;

    @Mock
    private VenueRepository venueRepository;

    @Mock
    private ArtistRepository artistRepository;

    @Mock
    private EventMapper eventMapper;

    @Mock
    private AuthorizationService authorizationService;

    @InjectMocks
    private EventServiceImpl eventService;

    @Test
    void shouldReturnEventWhenEventExistsAndIsVisible() {

        Venue venue =
                activeVenue();

        Event event =
                event(
                        "CMF-2026",
                        EventStatus.DRAFT,
                        venue
                );

        EventResponse expectedResponse =
                eventResponse(
                        EventStatus.DRAFT
                );

        when(
                eventRepository.findByEventCode(
                        "CMF-2026"
                )
        ).thenReturn(
                Optional.of(event)
        );

        when(
                eventMapper.toResponse(
                        event
                )
        ).thenReturn(
                expectedResponse
        );

        EventResponse result =
                eventService.findByCode(
                        "CMF-2026"
                );

        assertThat(result)
                .isEqualTo(
                        expectedResponse
                );

        verify(authorizationService)
                .requireEventVisible(
                        event
                );

        verify(eventMapper)
                .toResponse(
                        event
                );
    }

    @Test
    void shouldHideDraftEventWhenAuthorizationDeniesAccess() {

        Venue venue =
                activeVenue();

        Event event =
                event(
                        "CMF-2026",
                        EventStatus.DRAFT,
                        venue
                );

        when(
                eventRepository.findByEventCode(
                        "CMF-2026"
                )
        ).thenReturn(
                Optional.of(event)
        );

        doThrow(
                new ResourceNotFoundException(
                        "Event not found: CMF-2026"
                )
        )
                .when(
                        authorizationService
                )
                .requireEventVisible(
                        event
                );

        assertThatThrownBy(() ->
                eventService.findByCode(
                        "CMF-2026"
                )
        )
                .isInstanceOf(
                        ResourceNotFoundException.class
                )
                .hasMessageContaining(
                        "CMF-2026"
                );

        verify(
                eventMapper,
                never()
        ).toResponse(
                any(Event.class)
        );
    }

    @Test
    void shouldThrowWhenEventDoesNotExist() {

        when(
                eventRepository.findByEventCode(
                        "UNKNOWN"
                )
        ).thenReturn(
                Optional.empty()
        );

        assertThatThrownBy(() ->
                eventService.findByCode(
                        "UNKNOWN"
                )
        )
                .isInstanceOf(
                        ResourceNotFoundException.class
                )
                .hasMessageContaining(
                        "UNKNOWN"
                );

        verify(
                authorizationService,
                never()
        ).requireEventVisible(
                any(Event.class)
        );

        verify(
                eventMapper,
                never()
        ).toResponse(
                any(Event.class)
        );
    }

    @Test
    void shouldCreateValidEvent() {

        Venue venue =
                activeVenue();

        CreateEventRequest request =
                validCreateRequest();

        EventResponse expectedResponse =
                eventResponse(
                        EventStatus.DRAFT
                );

        when(
                eventRepository.existsByEventCode(
                        request.eventCode()
                )
        ).thenReturn(false);

        when(
                venueRepository.findByCode(
                        request.venueCode()
                )
        ).thenReturn(
                Optional.of(venue)
        );

        when(
                eventRepository.save(
                        any(Event.class)
                )
        ).thenAnswer(invocation ->
                invocation.getArgument(
                        0,
                        Event.class
                )
        );

        when(
                eventMapper.toResponse(
                        any(Event.class)
                )
        ).thenReturn(
                expectedResponse
        );

        EventResponse result =
                eventService.create(
                        request
                );

        ArgumentCaptor<Event> captor =
                ArgumentCaptor.forClass(
                        Event.class
                );

        verify(eventRepository)
                .save(
                        captor.capture()
                );

        Event savedEvent =
                captor.getValue();

        assertThat(result)
                .isEqualTo(
                        expectedResponse
                );

        assertThat(
                savedEvent.getEventCode()
        ).isEqualTo(
                "CMF-2026"
        );

        assertThat(
                savedEvent.getStatus()
        ).isEqualTo(
                EventStatus.DRAFT
        );

        assertThat(
                savedEvent.getVenue()
        ).isSameAs(
                venue
        );
    }

    @Test
    void shouldNotCreateEventWhenVenueDoesNotExist() {

        CreateEventRequest request =
                validCreateRequest();

        when(
                eventRepository.existsByEventCode(
                        request.eventCode()
                )
        ).thenReturn(false);

        when(
                venueRepository.findByCode(
                        request.venueCode()
                )
        ).thenReturn(
                Optional.empty()
        );

        assertThatThrownBy(() ->
                eventService.create(
                        request
                )
        )
                .isInstanceOf(
                        ResourceNotFoundException.class
                )
                .hasMessageContaining(
                        request.venueCode()
                );

        verify(
                eventRepository,
                never()
        ).save(
                any(Event.class)
        );
    }

    @Test
    void shouldNotCreateEventWhenVenueIsInactive() {

        Venue inactiveVenue =
                new Venue(
                        "VEN-SMR-01",
                        "Marina Convention Center",
                        "Santa Marta",
                        "Avenida del Libertador",
                        5000,
                        false
                );

        CreateEventRequest request =
                validCreateRequest();

        when(
                eventRepository.existsByEventCode(
                        request.eventCode()
                )
        ).thenReturn(false);

        when(
                venueRepository.findByCode(
                        request.venueCode()
                )
        ).thenReturn(
                Optional.of(
                        inactiveVenue
                )
        );

        assertThatThrownBy(() ->
                eventService.create(
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
        ).save(
                any(Event.class)
        );
    }

    @Test
    void shouldNotCreateEventWithPastDate() {

        Venue venue =
                activeVenue();

        CreateEventRequest request =
                new CreateEventRequest(
                        "CMF-2026",
                        "Caribbean Music Fest 2026",
                        "Festival musical",
                        EventCategory.MUSIC,
                        LocalDateTime.now()
                                .minusDays(1),
                        18,
                        "VEN-SMR-01"
                );

        when(
                eventRepository.existsByEventCode(
                        request.eventCode()
                )
        ).thenReturn(false);

        when(
                venueRepository.findByCode(
                        request.venueCode()
                )
        ).thenReturn(
                Optional.of(venue)
        );

        assertThatThrownBy(() ->
                eventService.create(
                        request
                )
        )
                .isInstanceOf(
                        BusinessRuleException.class
                )
                .hasMessageContaining(
                        "future"
                );

        verify(
                eventRepository,
                never()
        ).save(
                any(Event.class)
        );
    }

    @Test
    void shouldPublishDraftEvent() {

        Venue venue =
                activeVenue();

        Event event =
                event(
                        "CMF-2026",
                        EventStatus.DRAFT,
                        venue
                );

        EventResponse expectedResponse =
                eventResponse(
                        EventStatus.PUBLISHED
                );

        when(
                eventRepository.findByEventCode(
                        "CMF-2026"
                )
        ).thenReturn(
                Optional.of(event)
        );

        when(
                eventRepository.save(
                        event
                )
        ).thenReturn(
                event
        );

        when(
                eventMapper.toResponse(
                        event
                )
        ).thenReturn(
                expectedResponse
        );

        EventResponse result =
                eventService.publish(
                        "CMF-2026"
                );

        assertThat(
                event.getStatus()
        ).isEqualTo(
                EventStatus.PUBLISHED
        );

        assertThat(result)
                .isEqualTo(
                        expectedResponse
                );
    }

    @Test
    void shouldNotPublishCancelledEvent() {

        Venue venue =
                activeVenue();

        Event event =
                event(
                        "CMF-2026",
                        EventStatus.CANCELLED,
                        venue
                );

        when(
                eventRepository.findByEventCode(
                        "CMF-2026"
                )
        ).thenReturn(
                Optional.of(event)
        );

        assertThatThrownBy(() ->
                eventService.publish(
                        "CMF-2026"
                )
        )
                .isInstanceOf(
                        BusinessRuleException.class
                )
                .hasMessageContaining(
                        "DRAFT"
                );

        verify(
                eventRepository,
                never()
        ).save(
                any(Event.class)
        );
    }

    @Test
    void shouldAddArtistToEvent() {

        Venue venue =
                activeVenue();

        Event event =
                event(
                        "CMF-2026",
                        EventStatus.DRAFT,
                        venue
                );

        Artist artist =
                new Artist(
                        "Solar Beat",
                        "Colombia",
                        "Electronic",
                        true
                );

        EventResponse expectedResponse =
                eventResponse(
                        EventStatus.DRAFT
                );

        when(
                eventRepository.findByEventCode(
                        "CMF-2026"
                )
        ).thenReturn(
                Optional.of(event)
        );

        when(
                artistRepository.findById(
                        1L
                )
        ).thenReturn(
                Optional.of(artist)
        );

        when(
                eventRepository.save(
                        event
                )
        ).thenReturn(
                event
        );

        when(
                eventMapper.toResponse(
                        event
                )
        ).thenReturn(
                expectedResponse
        );

        EventResponse result =
                eventService.addArtist(
                        "CMF-2026",
                        1L
                );

        assertThat(
                event.getArtists()
        ).contains(
                artist
        );

        assertThat(result)
                .isEqualTo(
                        expectedResponse
                );
    }

    @Test
    void shouldRejectDuplicatedArtistAssociation() {

        Venue venue =
                activeVenue();

        Event event =
                event(
                        "CMF-2026",
                        EventStatus.DRAFT,
                        venue
                );

        Artist artist =
                new Artist(
                        "Solar Beat",
                        "Colombia",
                        "Electronic",
                        true
                );

        event.addArtist(
                artist
        );

        when(
                eventRepository.findByEventCode(
                        "CMF-2026"
                )
        ).thenReturn(
                Optional.of(event)
        );

        when(
                artistRepository.findById(
                        1L
                )
        ).thenReturn(
                Optional.of(artist)
        );

        assertThatThrownBy(() ->
                eventService.addArtist(
                        "CMF-2026",
                        1L
                )
        )
                .isInstanceOf(
                        BusinessRuleException.class
                )
                .hasMessageContaining(
                        "already associated"
                );

        verify(
                eventRepository,
                never()
        ).save(
                any(Event.class)
        );
    }

    @Test
    void shouldReturnPublishedEvents() {

        Venue venue =
                activeVenue();

        Event first =
                event(
                        "EVT-001",
                        EventStatus.PUBLISHED,
                        venue
                );

        Event second =
                event(
                        "EVT-002",
                        EventStatus.PUBLISHED,
                        venue
                );

        EventSummaryResponse firstResponse =
                summary(
                        first
                );

        EventSummaryResponse secondResponse =
                summary(
                        second
                );

        when(
                eventRepository
                        .findByStatusOrderByEventDateAsc(
                                EventStatus.PUBLISHED
                        )
        ).thenReturn(
                List.of(
                        first,
                        second
                )
        );

        when(
                eventMapper.toSummary(
                        first
                )
        ).thenReturn(
                firstResponse
        );

        when(
                eventMapper.toSummary(
                        second
                )
        ).thenReturn(
                secondResponse
        );

        List<EventSummaryResponse> result =
                eventService
                        .findPublishedEvents();

        assertThat(result)
                .containsExactly(
                        firstResponse,
                        secondResponse
                );
    }

    @Test
    void shouldReturnPublishedEventsByArtistForPublicAccess() {

        Venue venue =
                activeVenue();

        Event published =
                event(
                        "PUB-001",
                        EventStatus.PUBLISHED,
                        venue
                );

        Event draft =
                event(
                        "DRAFT-001",
                        EventStatus.DRAFT,
                        venue
                );

        EventSummaryResponse publishedResponse =
                summary(
                        published
                );

        when(
                eventRepository.findByArtistStageName(
                        "Solar Beat"
                )
        ).thenReturn(
                List.of(
                        published,
                        draft
                )
        );

        when(
                authorizationService
                        .canViewUnpublishedEvents()
        ).thenReturn(
                false
        );

        when(
                eventMapper.toSummary(
                        published
                )
        ).thenReturn(
                publishedResponse
        );

        List<EventSummaryResponse> result =
                eventService.findByArtist(
                        "Solar Beat"
                );

        assertThat(result)
                .containsExactly(
                        publishedResponse
                );

        verify(
                eventMapper,
                never()
        ).toSummary(
                draft
        );
    }

    @Test
    void shouldReturnAllArtistEventsForPrivilegedUser() {

        Venue venue =
                activeVenue();

        Event published =
                event(
                        "PUB-001",
                        EventStatus.PUBLISHED,
                        venue
                );

        Event draft =
                event(
                        "DRAFT-001",
                        EventStatus.DRAFT,
                        venue
                );

        EventSummaryResponse publishedResponse =
                summary(
                        published
                );

        EventSummaryResponse draftResponse =
                summary(
                        draft
                );

        when(
                eventRepository.findByArtistStageName(
                        "Solar Beat"
                )
        ).thenReturn(
                List.of(
                        published,
                        draft
                )
        );

        when(
                authorizationService
                        .canViewUnpublishedEvents()
        ).thenReturn(
                true
        );

        when(
                eventMapper.toSummary(
                        published
                )
        ).thenReturn(
                publishedResponse
        );

        when(
                eventMapper.toSummary(
                        draft
                )
        ).thenReturn(
                draftResponse
        );

        List<EventSummaryResponse> result =
                eventService.findByArtist(
                        "Solar Beat"
                );

        assertThat(result)
                .containsExactly(
                        publishedResponse,
                        draftResponse
                );
    }

    private Venue activeVenue() {

        return new Venue(
                "VEN-SMR-01",
                "Marina Convention Center",
                "Santa Marta",
                "Avenida del Libertador",
                3,
                true
        );
    }

    private LocalDateTime futureDate() {

        return LocalDateTime.now()
                .plusMonths(6);
    }

    private Event event(
            String eventCode,
            EventStatus status,
            Venue venue
    ) {

        return new Event(
                eventCode,
                "Caribbean Music Fest 2026",
                "Festival musical",
                EventCategory.MUSIC,
                status,
                futureDate(),
                18,
                venue
        );
    }

    private CreateEventRequest validCreateRequest() {

        return new CreateEventRequest(
                "CMF-2026",
                "Caribbean Music Fest 2026",
                "Festival musical",
                EventCategory.MUSIC,
                futureDate(),
                18,
                "VEN-SMR-01"
        );
    }

    private EventResponse eventResponse(
            EventStatus status
    ) {

        return new EventResponse(
                null,
                "CMF-2026",
                "Caribbean Music Fest 2026",
                "Festival musical",
                EventCategory.MUSIC,
                status,
                futureDate(),
                18,
                null,
                "VEN-SMR-01",
                "Marina Convention Center",
                List.of()
        );
    }

    private EventSummaryResponse summary(
            Event event
    ) {

        return new EventSummaryResponse(
                null,
                event.getEventCode(),
                event.getName(),
                event.getCategory(),
                event.getStatus(),
                event.getEventDate(),
                event.getVenue().getCode(),
                event.getVenue().getName()
        );
    }
}