package com.pulsepass.controller;

import com.pulsepass.domain.EventCategory;
import com.pulsepass.domain.EventStatus;
import com.pulsepass.domain.TicketStatus;
import com.pulsepass.domain.TicketType;

import com.pulsepass.dto.request.CreateEventRequest;
import com.pulsepass.dto.response.ArtistResponse;
import com.pulsepass.dto.response.EventResponse;
import com.pulsepass.dto.response.EventSummaryResponse;
import com.pulsepass.dto.response.TicketResponse;

import com.pulsepass.exception.BusinessRuleException;
import com.pulsepass.exception.GlobalExceptionHandler;
import com.pulsepass.exception.ResourceNotFoundException;

import com.pulsepass.security.CustomUserDetailsService;
import com.pulsepass.security.JwtAuthenticationFilter;
import com.pulsepass.security.JwtService;
import com.pulsepass.security.RestAccessDeniedHandler;
import com.pulsepass.security.RestAuthenticationEntryPoint;
import com.pulsepass.security.SecurityConfig;

import com.pulsepass.service.EventService;
import com.pulsepass.service.TicketService;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(EventController.class)
@Import({
        SecurityConfig.class,
        JwtAuthenticationFilter.class,
        RestAuthenticationEntryPoint.class,
        RestAccessDeniedHandler.class,
        GlobalExceptionHandler.class
})
class EventControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private EventService eventService;

    @MockitoBean
    private TicketService ticketService;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private CustomUserDetailsService userDetailsService;

    @Test
    void shouldCreateEventAndReturn201()
            throws Exception {

        when(
                eventService.create(
                        any(CreateEventRequest.class)
                )
        ).thenReturn(
                eventResponse(
                        EventStatus.DRAFT
                )
        );

        mockMvc.perform(
                        post("/api/events")
                                .with(
                                        user("admin")
                                                .roles("ADMIN")
                                )
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content(
                                        validEventJson()
                                )
                )
                .andExpect(
                        status().isCreated()
                )
                .andExpect(
                        content().contentTypeCompatibleWith(
                                MediaType.APPLICATION_JSON
                        )
                )
                .andExpect(
                        jsonPath("$.eventCode")
                                .value(
                                        "CMF-2027"
                                )
                )
                .andExpect(
                        jsonPath("$.status")
                                .value("DRAFT")
                );

        verify(eventService)
                .create(
                        any(CreateEventRequest.class)
                );
    }

    @Test
    void shouldReturn400WhenCreateEventRequestIsInvalid()
            throws Exception {

        String requestBody = """
                {
                  "eventCode": "",
                  "name": "",
                  "description": "Evento inválido",
                  "category": null,
                  "eventDate": null,
                  "minimumAge": -1,
                  "venueCode": ""
                }
                """;

        mockMvc.perform(
                        post("/api/events")
                                .with(
                                        user("admin")
                                                .roles("ADMIN")
                                )
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content(requestBody)
                )
                .andExpect(
                        status().isBadRequest()
                )
                .andExpect(
                        jsonPath("$.status")
                                .value(400)
                )
                .andExpect(
                        jsonPath("$.error")
                                .value(
                                        "Bad Request"
                                )
                )
                .andExpect(
                        jsonPath("$.message")
                                .value(
                                        "Validation failed"
                                )
                )
                .andExpect(
                        jsonPath("$.details.eventCode")
                                .exists()
                )
                .andExpect(
                        jsonPath("$.details.name")
                                .exists()
                )
                .andExpect(
                        jsonPath("$.details.category")
                                .exists()
                )
                .andExpect(
                        jsonPath("$.details.eventDate")
                                .exists()
                )
                .andExpect(
                        jsonPath("$.details.minimumAge")
                                .exists()
                )
                .andExpect(
                        jsonPath("$.details.venueCode")
                                .exists()
                );

        verify(
                eventService,
                never()
        ).create(
                any(CreateEventRequest.class)
        );
    }

    @Test
    void shouldReturnEventByCode()
            throws Exception {

        when(
                eventService.findByCode(
                        "CMF-2027"
                )
        ).thenReturn(
                eventResponse(
                        EventStatus.PUBLISHED
                )
        );

        mockMvc.perform(
                        get(
                                "/api/events/CMF-2027"
                        )
                )
                .andExpect(
                        status().isOk()
                )
                .andExpect(
                        jsonPath("$.eventCode")
                                .value(
                                        "CMF-2027"
                                )
                )
                .andExpect(
                        jsonPath("$.status")
                                .value(
                                        "PUBLISHED"
                                )
                );

        verify(eventService)
                .findByCode(
                        "CMF-2027"
                );
    }

    @Test
    void shouldReturn404WhenEventDoesNotExist()
            throws Exception {

        when(
                eventService.findByCode(
                        "EVT-404"
                )
        ).thenThrow(
                new ResourceNotFoundException(
                        "Event not found: EVT-404"
                )
        );

        mockMvc.perform(
                        get(
                                "/api/events/EVT-404"
                        )
                )
                .andExpect(
                        status().isNotFound()
                )
                .andExpect(
                        jsonPath("$.status")
                                .value(404)
                )
                .andExpect(
                        jsonPath("$.error")
                                .value(
                                        "Not Found"
                                )
                )
                .andExpect(
                        jsonPath("$.message")
                                .value(
                                        "Event not found: EVT-404"
                                )
                );

        verify(eventService)
                .findByCode(
                        "EVT-404"
                );
    }

    @Test
    void shouldReturnPublishedEvents()
            throws Exception {

        EventSummaryResponse event =
                new EventSummaryResponse(
                        1L,
                        "CMF-2027",
                        "Caribbean Music Fest 2027",
                        EventCategory.MUSIC,
                        EventStatus.PUBLISHED,
                        LocalDateTime.of(
                                2027,
                                12,
                                15,
                                20,
                                0
                        ),
                        "VEN-SMR-01",
                        "Marina Convention Center"
                );

        when(
                eventService.findPublishedEvents()
        ).thenReturn(
                List.of(event)
        );

        mockMvc.perform(
                        get(
                                "/api/events/published"
                        )
                )
                .andExpect(
                        status().isOk()
                )
                .andExpect(
                        jsonPath("$.length()")
                                .value(1)
                )
                .andExpect(
                        jsonPath("$[0].eventCode")
                                .value(
                                        "CMF-2027"
                                )
                )
                .andExpect(
                        jsonPath("$[0].status")
                                .value(
                                        "PUBLISHED"
                                )
                );

        verify(eventService)
                .findPublishedEvents();
    }

    @Test
    void shouldPublishEvent()
            throws Exception {

        when(
                eventService.publish(
                        "CMF-2027"
                )
        ).thenReturn(
                eventResponse(
                        EventStatus.PUBLISHED
                )
        );

        mockMvc.perform(
                        patch(
                                "/api/events/CMF-2027/publish"
                        )
                                .with(
                                        user("admin")
                                                .roles("ADMIN")
                                )
                )
                .andExpect(
                        status().isOk()
                )
                .andExpect(
                        jsonPath("$.eventCode")
                                .value(
                                        "CMF-2027"
                                )
                )
                .andExpect(
                        jsonPath("$.status")
                                .value(
                                        "PUBLISHED"
                                )
                );

        verify(eventService)
                .publish(
                        "CMF-2027"
                );
    }

    @Test
    void shouldReturn409WhenEventCannotBePublished()
            throws Exception {

        when(
                eventService.publish(
                        "CMF-2027"
                )
        ).thenThrow(
                new BusinessRuleException(
                        "Only DRAFT events can be published"
                )
        );

        mockMvc.perform(
                        patch(
                                "/api/events/CMF-2027/publish"
                        )
                                .with(
                                        user("admin")
                                                .roles("ADMIN")
                                )
                )
                .andExpect(
                        status().isConflict()
                )
                .andExpect(
                        jsonPath("$.status")
                                .value(409)
                )
                .andExpect(
                        jsonPath("$.error")
                                .value("Conflict")
                )
                .andExpect(
                        jsonPath("$.message")
                                .value(
                                        "Only DRAFT events can be published"
                                )
                );

        verify(eventService)
                .publish(
                        "CMF-2027"
                );
    }

    @Test
    void shouldAddArtistToEvent()
            throws Exception {

        ArtistResponse artist =
                new ArtistResponse(
                        1L,
                        "Solar Beat",
                        "Colombia",
                        "Electronic",
                        true
                );

        EventResponse response =
                new EventResponse(
                        1L,
                        "CMF-2027",
                        "Caribbean Music Fest 2027",
                        "Festival musical",
                        EventCategory.MUSIC,
                        EventStatus.DRAFT,
                        LocalDateTime.of(
                                2027,
                                12,
                                15,
                                20,
                                0
                        ),
                        18,
                        null,
                        "VEN-SMR-01",
                        "Marina Convention Center",
                        List.of(artist)
                );

        when(
                eventService.addArtist(
                        "CMF-2027",
                        1L
                )
        ).thenReturn(response);

        mockMvc.perform(
                        post(
                                "/api/events/CMF-2027/artists/1"
                        )
                                .with(
                                        user("admin")
                                                .roles("ADMIN")
                                )
                )
                .andExpect(
                        status().isOk()
                )
                .andExpect(
                        jsonPath("$.eventCode")
                                .value(
                                        "CMF-2027"
                                )
                )
                .andExpect(
                        jsonPath("$.artists.length()")
                                .value(1)
                )
                .andExpect(
                        jsonPath("$.artists[0].stageName")
                                .value(
                                        "Solar Beat"
                                )
                );

        verify(eventService)
                .addArtist(
                        "CMF-2027",
                        1L
                );
    }

    @Test
    void shouldReturnEventsByArtist()
            throws Exception {

        EventSummaryResponse response =
                new EventSummaryResponse(
                        1L,
                        "CMF-2027",
                        "Caribbean Music Fest 2027",
                        EventCategory.MUSIC,
                        EventStatus.PUBLISHED,
                        LocalDateTime.of(
                                2027,
                                12,
                                15,
                                20,
                                0
                        ),
                        "VEN-SMR-01",
                        "Marina Convention Center"
                );

        when(
                eventService.findByArtist(
                        "Solar Beat"
                )
        ).thenReturn(
                List.of(response)
        );

        mockMvc.perform(
                        get(
                                "/api/events/by-artist"
                        )
                                .param(
                                        "stageName",
                                        "Solar Beat"
                                )
                )
                .andExpect(
                        status().isOk()
                )
                .andExpect(
                        jsonPath("$.length()")
                                .value(1)
                )
                .andExpect(
                        jsonPath("$[0].status")
                                .value(
                                        "PUBLISHED"
                                )
                );

        verify(eventService)
                .findByArtist(
                        "Solar Beat"
                );
    }

    @Test
    void shouldReturnPaidTicketsByEvent()
            throws Exception {

        TicketResponse ticket =
                new TicketResponse(
                        1L,
                        "TKT-0001",
                        TicketType.GENERAL,
                        new BigDecimal(
                                "120000.00"
                        ),
                        TicketStatus.PAID,
                        LocalDateTime.of(
                                2026,
                                10,
                                15,
                                12,
                                0
                        ),
                        "andrea@example.com",
                        "CMF-2027",
                        "Caribbean Music Fest 2027"
                );

        when(
                ticketService.findPaidTicketsByEvent(
                        "CMF-2027"
                )
        ).thenReturn(
                List.of(ticket)
        );

        mockMvc.perform(
                        get(
                                "/api/events/CMF-2027/tickets/paid"
                        )
                                .with(
                                        user("laura")
                                                .roles("STAFF")
                                )
                )
                .andExpect(
                        status().isOk()
                )
                .andExpect(
                        jsonPath("$.length()")
                                .value(1)
                )
                .andExpect(
                        jsonPath("$[0].ticketCode")
                                .value(
                                        "TKT-0001"
                                )
                )
                .andExpect(
                        jsonPath("$[0].status")
                                .value("PAID")
                );

        verify(ticketService)
                .findPaidTicketsByEvent(
                        "CMF-2027"
                );
    }

    private String validEventJson() {

        return """
                {
                  "eventCode": "CMF-2027",
                  "name": "Caribbean Music Fest 2027",
                  "description": "Festival musical del Caribe",
                  "category": "MUSIC",
                  "eventDate": "2027-12-15T20:00:00",
                  "minimumAge": 18,
                  "venueCode": "VEN-SMR-01"
                }
                """;
    }

    private EventResponse eventResponse(
            EventStatus status
    ) {

        return new EventResponse(
                1L,
                "CMF-2027",
                "Caribbean Music Fest 2027",
                "Festival musical del Caribe",
                EventCategory.MUSIC,
                status,
                LocalDateTime.of(
                        2027,
                        12,
                        15,
                        20,
                        0
                ),
                18,
                null,
                "VEN-SMR-01",
                "Marina Convention Center",
                List.of()
        );
    }
}