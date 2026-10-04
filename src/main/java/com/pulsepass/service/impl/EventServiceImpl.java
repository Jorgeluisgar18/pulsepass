package com.pulsepass.service;

import com.pulsepass.domain.Artist;
import com.pulsepass.domain.Event;
import com.pulsepass.domain.EventStatus;
import com.pulsepass.domain.Venue;
import com.pulsepass.dto.request.CreateEventRequest;
import com.pulsepass.dto.response.EventResponse;
import com.pulsepass.dto.response.EventSummaryResponse;
import com.pulsepass.exception.BusinessRuleException;
import com.pulsepass.exception.DuplicateResourceException;
import com.pulsepass.exception.ResourceNotFoundException;
import com.pulsepass.mapper.EventMapper;
import com.pulsepass.repository.ArtistRepository;
import com.pulsepass.repository.EventRepository;
import com.pulsepass.repository.VenueRepository;
import com.pulsepass.service.EventService;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

@Service
public class EventServiceImpl implements EventService {

    private final EventRepository eventRepository;
    private final VenueRepository venueRepository;
    private final ArtistRepository artistRepository;
    private final EventMapper eventMapper;

    public EventServiceImpl(
            EventRepository eventRepository,
            VenueRepository venueRepository,
            ArtistRepository artistRepository,
            EventMapper eventMapper
    ) {
        this.eventRepository = eventRepository;
        this.venueRepository = venueRepository;
        this.artistRepository = artistRepository;
        this.eventMapper = eventMapper;
    }

    @Override
    @Transactional
    public EventResponse create(
            CreateEventRequest request
    ) {

        if (eventRepository.existsByEventCode(
                request.eventCode()
        )) {
            throw new DuplicateResourceException(
                    "Event code already exists: "
                            + request.eventCode()
            );
        }

        Venue venue = venueRepository
                .findByCode(request.venueCode())
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Venue not found: "
                                        + request.venueCode()
                        )
                );

        if (!venue.isActive()) {
            throw new BusinessRuleException(
                    "Venue is inactive: "
                            + venue.getCode()
            );
        }

        if (request.eventDate() == null
                || !request.eventDate()
                .isAfter(LocalDateTime.now())) {

            throw new BusinessRuleException(
                    "Event date must be in the future."
            );
        }

        if (request.minimumAge() == null
                || request.minimumAge() < 0) {

            throw new BusinessRuleException(
                    "Minimum age must be greater than or equal to zero."
            );
        }

        Event event = new Event(
                request.eventCode(),
                request.name(),
                request.description(),
                request.category(),
                EventStatus.DRAFT,
                request.eventDate(),
                request.minimumAge(),
                venue
        );

        Event savedEvent =
                eventRepository.save(event);

        return eventMapper.toResponse(savedEvent);
    }

    @Override
    @Transactional(readOnly = true)
    public EventResponse findByCode(
            String eventCode
    ) {

        Event event = findEvent(eventCode);

        return eventMapper.toResponse(event);
    }

    @Override
    @Transactional(readOnly = true)
    public List<EventSummaryResponse> findPublishedEvents() {

        return eventRepository
                .findByStatusOrderByEventDateAsc(
                        EventStatus.PUBLISHED
                )
                .stream()
                .map(eventMapper::toSummary)
                .toList();
    }

    @Override
    @Transactional
    public EventResponse publish(
            String eventCode
    ) {

        Event event = findEvent(eventCode);

        if (event.getStatus() != EventStatus.DRAFT) {
            throw new BusinessRuleException(
                    "Only DRAFT events can be published."
            );
        }

        if (!event.getEventDate()
                .isAfter(LocalDateTime.now())) {

            throw new BusinessRuleException(
                    "Event date must be in the future."
            );
        }

        if (!event.getVenue().isActive()) {
            throw new BusinessRuleException(
                    "Venue is inactive: "
                            + event.getVenue().getCode()
            );
        }

        event.changeStatus(
                EventStatus.PUBLISHED
        );

        Event savedEvent =
                eventRepository.save(event);

        return eventMapper.toResponse(savedEvent);
    }

    @Override
    @Transactional
    public EventResponse addArtist(
            String eventCode,
            Long artistId
    ) {

        Event event = findEvent(eventCode);

        Artist artist = artistRepository
                .findById(artistId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Artist not found: "
                                        + artistId
                        )
                );

        if (event.getStatus()
                == EventStatus.CANCELLED
                || event.getStatus()
                == EventStatus.FINISHED) {

            throw new BusinessRuleException(
                    "Artists cannot be added to "
                            + event.getStatus()
                            + " events."
            );
        }

        boolean alreadyAssociated =
                event.getArtists()
                        .stream()
                        .anyMatch(existingArtist ->
                                Objects.equals(
                                        existingArtist.getId(),
                                        artist.getId()
                                )
                        );

        if (alreadyAssociated) {
            throw new BusinessRuleException(
                    "Artist is already associated with event: "
                            + artist.getStageName()
            );
        }

        event.addArtist(artist);

        Event savedEvent =
                eventRepository.save(event);

        return eventMapper.toResponse(savedEvent);
    }

    @Override
    @Transactional(readOnly = true)
    public List<EventSummaryResponse> findByArtist(
            String stageName
    ) {

        return eventRepository
                .findByArtistStageName(stageName)
                .stream()
                .map(eventMapper::toSummary)
                .toList();
    }

    private Event findEvent(
            String eventCode
    ) {

        return eventRepository
                .findByEventCode(eventCode)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Event not found: "
                                        + eventCode
                        )
                );
    }
}