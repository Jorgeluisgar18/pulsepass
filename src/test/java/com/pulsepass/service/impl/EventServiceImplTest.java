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

    @InjectMocks
    private EventServiceImpl eventService;


    // ---------------------------------------------------------
    // TEST-EVENT-001
    // Evento existente -> retorna DTO
    // ---------------------------------------------------------

    @Test
    void shouldReturnEventWhenEventExists() {

        Venue venue = activeVenue();

        Event event = new Event(
                "CMF-2026",
                "Caribbean Music Fest 2026",
                "Festival musical",
                EventCategory.MUSIC,
                EventStatus.DRAFT,
                futureDate(),
                18,
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
                eventMapper.toResponse(event)
        ).thenReturn(
                expectedResponse
        );

        EventResponse result =
                eventService.findByCode(
                        "CMF-2026"
                );

        assertThat(result)
                .isEqualTo(expectedResponse);

        verify(eventRepository)
                .findByEventCode(
                        "CMF-2026"
                );

        verify(eventMapper)
                .toResponse(event);
    }


    // ---------------------------------------------------------
    // TEST-EVENT-002
    // Evento inexistente -> ResourceNotFoundException
    // ---------------------------------------------------------

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

        verify(eventMapper, never())
                .toResponse(any(Event.class));
    }


    // ---------------------------------------------------------
    // TEST-EVENT-003
    // Crear evento válido -> save ejecutado
    // ---------------------------------------------------------

    @Test
    void shouldCreateValidEvent() {

        Venue venue = activeVenue();

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
                eventService.create(request);

        ArgumentCaptor<Event> captor =
                ArgumentCaptor.forClass(
                        Event.class
                );

        verify(eventRepository)
                .save(captor.capture());

        Event savedEvent =
                captor.getValue();

        assertThat(result)
                .isEqualTo(expectedResponse);

        assertThat(savedEvent.getEventCode())
                .isEqualTo("CMF-2026");

        assertThat(savedEvent.getStatus())
                .isEqualTo(EventStatus.DRAFT);

        assertThat(savedEvent.getVenue())
                .isSameAs(venue);

        assertThat(savedEvent.getMinimumAge())
                .isEqualTo(18);
    }


    // ---------------------------------------------------------
    // TEST-EVENT-004
    // Venue inexistente -> error y save nunca ejecutado
    // ---------------------------------------------------------

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
                eventService.create(request)
        )
                .isInstanceOf(
                        ResourceNotFoundException.class
                )
                .hasMessageContaining(
                        request.venueCode()
                );

        verify(eventRepository, never())
                .save(any(Event.class));

        verify(eventMapper, never())
                .toResponse(any(Event.class));
    }


    // ---------------------------------------------------------
    // TEST-EVENT-005
    // Venue inactivo -> BusinessRuleException
    // ---------------------------------------------------------

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
                Optional.of(inactiveVenue)
        );

        assertThatThrownBy(() ->
                eventService.create(request)
        )
                .isInstanceOf(
                        BusinessRuleException.class
                )
                .hasMessageContaining(
                        "inactive"
                );

        verify(eventRepository, never())
                .save(any(Event.class));
    }


    // ---------------------------------------------------------
    // TEST-EVENT-006
    // Fecha pasada -> BusinessRuleException
    // ---------------------------------------------------------

    @Test
    void shouldNotCreateEventWithPastDate() {

        Venue venue = activeVenue();

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
                eventService.create(request)
        )
                .isInstanceOf(
                        BusinessRuleException.class
                )
                .hasMessageContaining(
                        "future"
                );

        verify(eventRepository, never())
                .save(any(Event.class));
    }


    // ---------------------------------------------------------
    // TEST-EVENT-007
    // Publicar DRAFT válido -> PUBLISHED
    // ---------------------------------------------------------

    @Test
    void shouldPublishDraftEvent() {

        Venue venue = activeVenue();

        Event event = new Event(
                "CMF-2026",
                "Caribbean Music Fest 2026",
                "Festival musical",
                EventCategory.MUSIC,
                EventStatus.DRAFT,
                futureDate(),
                18,
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
                eventRepository.save(event)
        ).thenReturn(event);

        when(
                eventMapper.toResponse(event)
        ).thenReturn(
                expectedResponse
        );

        EventResponse result =
                eventService.publish(
                        "CMF-2026"
                );

        assertThat(event.getStatus())
                .isEqualTo(
                        EventStatus.PUBLISHED
                );

        assertThat(result)
                .isEqualTo(
                        expectedResponse
                );

        verify(eventRepository)
                .save(event);
    }


    // ---------------------------------------------------------
    // TEST-EVENT-008
    // Publicar CANCELLED -> error y no persistir
    // ---------------------------------------------------------

    @Test
    void shouldNotPublishCancelledEvent() {

        Venue venue = activeVenue();

        Event event = new Event(
                "CMF-2026",
                "Caribbean Music Fest 2026",
                "Festival musical",
                EventCategory.MUSIC,
                EventStatus.CANCELLED,
                futureDate(),
                18,
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

        verify(eventRepository, never())
                .save(any(Event.class));

        verify(eventMapper, never())
                .toResponse(any(Event.class));
    }


    // ---------------------------------------------------------
    // BR-EVENT-010
    // Asociar artista válido
    // ---------------------------------------------------------

    @Test
    void shouldAddArtistToEvent() {

        Venue venue = activeVenue();

        Event event = new Event(
                "CMF-2026",
                "Caribbean Music Fest 2026",
                "Festival musical",
                EventCategory.MUSIC,
                EventStatus.DRAFT,
                futureDate(),
                18,
                venue
        );

        Artist artist = new Artist(
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
                artistRepository.findById(1L)
        ).thenReturn(
                Optional.of(artist)
        );

        when(
                eventRepository.save(event)
        ).thenReturn(event);

        when(
                eventMapper.toResponse(event)
        ).thenReturn(
                expectedResponse
        );

        EventResponse result =
                eventService.addArtist(
                        "CMF-2026",
                        1L
                );

        assertThat(event.getArtists())
                .contains(artist);

        assertThat(result)
                .isEqualTo(expectedResponse);

        verify(eventRepository)
                .save(event);
    }


    // ---------------------------------------------------------
    // BR-EVENT-010
    // No asociar dos veces el mismo artista
    // ---------------------------------------------------------

    @Test
    void shouldRejectDuplicatedArtistAssociation() {

        Venue venue = activeVenue();

        Event event = new Event(
                "CMF-2026",
                "Caribbean Music Fest 2026",
                "Festival musical",
                EventCategory.MUSIC,
                EventStatus.DRAFT,
                futureDate(),
                18,
                venue
        );

        Artist artist = new Artist(
                "Solar Beat",
                "Colombia",
                "Electronic",
                true
        );

        event.addArtist(artist);

        when(
                eventRepository.findByEventCode(
                        "CMF-2026"
                )
        ).thenReturn(
                Optional.of(event)
        );

        when(
                artistRepository.findById(1L)
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

        verify(eventRepository, never())
                .save(any(Event.class));
    }


    // ---------------------------------------------------------
    // Consultar eventos PUBLISHED
    // ---------------------------------------------------------

    @Test
    void shouldReturnPublishedEvents() {

        Venue venue = activeVenue();

        Event first = new Event(
                "EVT-001",
                "Evento Uno",
                "Descripción",
                EventCategory.MUSIC,
                EventStatus.PUBLISHED,
                futureDate(),
                18,
                venue
        );

        Event second = new Event(
                "EVT-002",
                "Evento Dos",
                "Descripción",
                EventCategory.CULTURE,
                EventStatus.PUBLISHED,
                futureDate().plusDays(1),
                0,
                venue
        );

        EventSummaryResponse firstResponse =
                new EventSummaryResponse(
                        null,
                        "EVT-001",
                        "Evento Uno",
                        EventCategory.MUSIC,
                        EventStatus.PUBLISHED,
                        first.getEventDate(),
                        "VEN-SMR-01",
                        "Marina Convention Center"
                );

        EventSummaryResponse secondResponse =
                new EventSummaryResponse(
                        null,
                        "EVT-002",
                        "Evento Dos",
                        EventCategory.CULTURE,
                        EventStatus.PUBLISHED,
                        second.getEventDate(),
                        "VEN-SMR-01",
                        "Marina Convention Center"
                );

        when(
                eventRepository
                        .findByStatusOrderByEventDateAsc(
                                EventStatus.PUBLISHED
                        )
        ).thenReturn(
                List.of(first, second)
        );

        when(
                eventMapper.toSummary(first)
        ).thenReturn(firstResponse);

        when(
                eventMapper.toSummary(second)
        ).thenReturn(secondResponse);

        List<EventSummaryResponse> result =
                eventService
                        .findPublishedEvents();

        assertThat(result)
                .containsExactly(
                        firstResponse,
                        secondResponse
                );

        verify(eventRepository)
                .findByStatusOrderByEventDateAsc(
                        EventStatus.PUBLISHED
                );
    }


    // ---------------------------------------------------------
    // Consultar eventos por artista
    // ---------------------------------------------------------

    @Test
    void shouldReturnEventsByArtist() {

        Venue venue = activeVenue();

        Event event = new Event(
                "CMF-2026",
                "Caribbean Music Fest 2026",
                "Festival musical",
                EventCategory.MUSIC,
                EventStatus.PUBLISHED,
                futureDate(),
                18,
                venue
        );

        EventSummaryResponse summary =
                new EventSummaryResponse(
                        null,
                        "CMF-2026",
                        "Caribbean Music Fest 2026",
                        EventCategory.MUSIC,
                        EventStatus.PUBLISHED,
                        event.getEventDate(),
                        "VEN-SMR-01",
                        "Marina Convention Center"
                );

        when(
                eventRepository.findByArtistStageName(
                        "Solar Beat"
                )
        ).thenReturn(
                List.of(event)
        );

        when(
                eventMapper.toSummary(event)
        ).thenReturn(summary);

        List<EventSummaryResponse> result =
                eventService.findByArtist(
                        "Solar Beat"
                );

        assertThat(result)
                .containsExactly(summary);

        verify(eventRepository)
                .findByArtistStageName(
                        "Solar Beat"
                );
    }


    // ---------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------

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
}