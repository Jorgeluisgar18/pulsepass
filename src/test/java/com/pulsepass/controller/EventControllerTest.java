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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(EventController.class)
@Import(GlobalExceptionHandler.class)
class EventControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private EventService eventService;

    @MockitoBean
    private TicketService ticketService;

    @Test
    void shouldCreateEventAndReturn201() throws Exception {

        EventResponse response = new EventResponse(
                1L,
                "CMF-2026",
                "Caribbean Music Fest 2026",
                "Festival musical del Caribe",
                EventCategory.MUSIC,
                EventStatus.DRAFT,
                LocalDateTime.of(2026, 12, 15, 20, 0),
                18,
                null,
                "VEN-SMR-01",
                "Marina Convention Center",
                List.of()
        );

        when(eventService.create(any(CreateEventRequest.class)))
                .thenReturn(response);

        String requestBody = """
                {
                  "eventCode": "CMF-2026",
                  "name": "Caribbean Music Fest 2026",
                  "description": "Festival musical del Caribe",
                  "category": "MUSIC",
                  "eventDate": "2026-12-15T20:00:00",
                  "minimumAge": 18,
                  "venueCode": "VEN-SMR-01"
                }
                """;

        mockMvc.perform(
                        post("/api/events")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(requestBody)
                )
                .andExpect(status().isCreated())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.eventCode").value("CMF-2026"))
                .andExpect(jsonPath("$.name").value("Caribbean Music Fest 2026"))
                .andExpect(jsonPath("$.category").value("MUSIC"))
                .andExpect(jsonPath("$.status").value("DRAFT"))
                .andExpect(jsonPath("$.minimumAge").value(18))
                .andExpect(jsonPath("$.venueCode").value("VEN-SMR-01"));

        verify(eventService)
                .create(any(CreateEventRequest.class));
    }

    @Test
    void shouldReturn400WhenCreateEventRequestIsInvalid() throws Exception {

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
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(requestBody)
                )
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.details.eventCode").exists())
                .andExpect(jsonPath("$.details.name").exists())
                .andExpect(jsonPath("$.details.category").exists())
                .andExpect(jsonPath("$.details.eventDate").exists())
                .andExpect(jsonPath("$.details.minimumAge").exists())
                .andExpect(jsonPath("$.details.venueCode").exists());

        verify(eventService, never())
                .create(any(CreateEventRequest.class));
    }

    @Test
    void shouldReturnEventByCode() throws Exception {

        EventResponse response = new EventResponse(
                1L,
                "CMF-2026",
                "Caribbean Music Fest 2026",
                "Festival musical del Caribe",
                EventCategory.MUSIC,
                EventStatus.PUBLISHED,
                LocalDateTime.of(2026, 12, 15, 20, 0),
                18,
                null,
                "VEN-SMR-01",
                "Marina Convention Center",
                List.of()
        );

        when(eventService.findByCode("CMF-2026"))
                .thenReturn(response);

        mockMvc.perform(
                        get("/api/events/CMF-2026")
                )
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.eventCode").value("CMF-2026"))
                .andExpect(jsonPath("$.status").value("PUBLISHED"))
                .andExpect(jsonPath("$.venueCode").value("VEN-SMR-01"));

        verify(eventService)
                .findByCode("CMF-2026");
    }

    @Test
    void shouldReturn404WhenEventDoesNotExist() throws Exception {

        when(eventService.findByCode("EVT-404"))
                .thenThrow(
                        new ResourceNotFoundException(
                                "Event not found: EVT-404"
                        )
                );

        mockMvc.perform(
                        get("/api/events/EVT-404")
                )
                .andExpect(status().isNotFound())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(
                        jsonPath("$.message")
                                .value("Event not found: EVT-404")
                )
                .andExpect(jsonPath("$.details").isEmpty());

        verify(eventService)
                .findByCode("EVT-404");
    }

    @Test
    void shouldReturnPublishedEvents() throws Exception {

        EventSummaryResponse event1 = new EventSummaryResponse(
                1L,
                "CMF-2026",
                "Caribbean Music Fest 2026",
                EventCategory.MUSIC,
                EventStatus.PUBLISHED,
                LocalDateTime.of(2026, 12, 15, 20, 0),
                "VEN-SMR-01",
                "Marina Convention Center"
        );

        EventSummaryResponse event2 = new EventSummaryResponse(
                2L,
                "TECH-2026",
                "Caribbean Tech Conference",
                EventCategory.TECHNOLOGY,
                EventStatus.PUBLISHED,
                LocalDateTime.of(2026, 11, 20, 9, 0),
                "VEN-SMR-01",
                "Marina Convention Center"
        );

        when(eventService.findPublishedEvents())
                .thenReturn(List.of(event1, event2));

        mockMvc.perform(
                        get("/api/events/published")
                )
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].eventCode").value("CMF-2026"))
                .andExpect(jsonPath("$[0].status").value("PUBLISHED"))
                .andExpect(jsonPath("$[1].eventCode").value("TECH-2026"))
                .andExpect(jsonPath("$[1].status").value("PUBLISHED"));

        verify(eventService)
                .findPublishedEvents();
    }

    @Test
    void shouldPublishEvent() throws Exception {

        EventResponse response = new EventResponse(
                1L,
                "CMF-2026",
                "Caribbean Music Fest 2026",
                "Festival musical del Caribe",
                EventCategory.MUSIC,
                EventStatus.PUBLISHED,
                LocalDateTime.of(2026, 12, 15, 20, 0),
                18,
                null,
                "VEN-SMR-01",
                "Marina Convention Center",
                List.of()
        );

        when(eventService.publish("CMF-2026"))
                .thenReturn(response);

        mockMvc.perform(
                        patch("/api/events/CMF-2026/publish")
                )
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.eventCode").value("CMF-2026"))
                .andExpect(jsonPath("$.status").value("PUBLISHED"));

        verify(eventService)
                .publish("CMF-2026");
    }

    @Test
    void shouldReturn409WhenEventCannotBePublished() throws Exception {

        when(eventService.publish("CMF-2026"))
                .thenThrow(
                        new BusinessRuleException(
                                "Only DRAFT events can be published"
                        )
                );

        mockMvc.perform(
                        patch("/api/events/CMF-2026/publish")
                )
                .andExpect(status().isConflict())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.error").value("Conflict"))
                .andExpect(
                        jsonPath("$.message")
                                .value("Only DRAFT events can be published")
                );

        verify(eventService)
                .publish("CMF-2026");
    }

    @Test
    void shouldAddArtistToEvent() throws Exception {

        ArtistResponse artist = new ArtistResponse(
                1L,
                "Solar Beat",
                "Colombia",
                "Electronic",
                true
        );

        EventResponse response = new EventResponse(
                1L,
                "CMF-2026",
                "Caribbean Music Fest 2026",
                "Festival musical del Caribe",
                EventCategory.MUSIC,
                EventStatus.DRAFT,
                LocalDateTime.of(2026, 12, 15, 20, 0),
                18,
                null,
                "VEN-SMR-01",
                "Marina Convention Center",
                List.of(artist)
        );

        when(eventService.addArtist("CMF-2026", 1L))
                .thenReturn(response);

        mockMvc.perform(
                        post("/api/events/CMF-2026/artists/1")
                )
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.eventCode").value("CMF-2026"))
                .andExpect(jsonPath("$.artists.length()").value(1))
                .andExpect(jsonPath("$.artists[0].id").value(1))
                .andExpect(jsonPath("$.artists[0].stageName").value("Solar Beat"));

        verify(eventService)
                .addArtist("CMF-2026", 1L);
    }

    @Test
    void shouldReturnEventsByArtist() throws Exception {

        EventSummaryResponse response = new EventSummaryResponse(
                1L,
                "CMF-2026",
                "Caribbean Music Fest 2026",
                EventCategory.MUSIC,
                EventStatus.PUBLISHED,
                LocalDateTime.of(2026, 12, 15, 20, 0),
                "VEN-SMR-01",
                "Marina Convention Center"
        );

        when(eventService.findByArtist("Solar Beat"))
                .thenReturn(List.of(response));

        mockMvc.perform(
                        get("/api/events/by-artist")
                                .param("stageName", "Solar Beat")
                )
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].eventCode").value("CMF-2026"))
                .andExpect(jsonPath("$[0].status").value("PUBLISHED"));

        verify(eventService)
                .findByArtist("Solar Beat");
    }

    @Test
    void shouldReturnPaidTicketsByEvent() throws Exception {

        TicketResponse ticket = new TicketResponse(
                1L,
                "TKT-0001",
                TicketType.GENERAL,
                new BigDecimal("120000.00"),
                TicketStatus.PAID,
                LocalDateTime.of(2026, 10, 7, 12, 0),
                "andrea@example.com",
                "CMF-2026",
                "Caribbean Music Fest 2026"
        );

        when(ticketService.findPaidTicketsByEvent("CMF-2026"))
                .thenReturn(List.of(ticket));

        mockMvc.perform(
                        get("/api/events/CMF-2026/tickets/paid")
                )
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].ticketCode").value("TKT-0001"))
                .andExpect(jsonPath("$[0].status").value("PAID"))
                .andExpect(jsonPath("$[0].eventCode").value("CMF-2026"));

        verify(ticketService)
                .findPaidTicketsByEvent("CMF-2026");
    }
}