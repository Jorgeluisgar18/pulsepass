package com.pulsepass.controller;

import com.pulsepass.domain.TicketStatus;
import com.pulsepass.domain.TicketType;
import com.pulsepass.dto.request.PurchaseTicketRequest;
import com.pulsepass.dto.response.TicketResponse;
import com.pulsepass.exception.BusinessRuleException;
import com.pulsepass.exception.GlobalExceptionHandler;
import com.pulsepass.exception.ResourceNotFoundException;
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

@WebMvcTest(TicketController.class)
@Import(GlobalExceptionHandler.class)
class TicketControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TicketService ticketService;

    @Test
    void shouldPurchaseTicketAndReturn201() throws Exception {

        TicketResponse response = paidTicket();

        when(ticketService.purchase(any(PurchaseTicketRequest.class)))
                .thenReturn(response);

        String requestBody = """
                {
                  "userEmail": "andrea@example.com",
                  "eventCode": "CMF-2026",
                  "type": "GENERAL"
                }
                """;

        mockMvc.perform(
                        post("/api/tickets")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(requestBody)
                )
                .andExpect(status().isCreated())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.ticketCode").value("TKT-0001"))
                .andExpect(jsonPath("$.type").value("GENERAL"))
                .andExpect(jsonPath("$.status").value("PAID"))
                .andExpect(jsonPath("$.userEmail").value("andrea@example.com"))
                .andExpect(jsonPath("$.eventCode").value("CMF-2026"));

        verify(ticketService)
                .purchase(any(PurchaseTicketRequest.class));
    }

    @Test
    void shouldReturn400WhenPurchaseRequestIsInvalid() throws Exception {

        String requestBody = """
                {
                  "userEmail": "correo-invalido",
                  "eventCode": "",
                  "type": null
                }
                """;

        mockMvc.perform(
                        post("/api/tickets")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(requestBody)
                )
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.details.userEmail").exists())
                .andExpect(jsonPath("$.details.eventCode").exists())
                .andExpect(jsonPath("$.details.type").exists());

        verify(ticketService, never())
                .purchase(any(PurchaseTicketRequest.class));
    }

    @Test
    void shouldReturn404WhenUserDoesNotExist() throws Exception {

        when(ticketService.purchase(any(PurchaseTicketRequest.class)))
                .thenThrow(
                        new ResourceNotFoundException(
                                "User not found: missing@example.com"
                        )
                );

        String requestBody = """
                {
                  "userEmail": "missing@example.com",
                  "eventCode": "CMF-2026",
                  "type": "GENERAL"
                }
                """;

        mockMvc.perform(
                        post("/api/tickets")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(requestBody)
                )
                .andExpect(status().isNotFound())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(
                        jsonPath("$.message")
                                .value("User not found: missing@example.com")
                );

        verify(ticketService)
                .purchase(any(PurchaseTicketRequest.class));
    }

    @Test
    void shouldReturn409WhenPurchaseViolatesBusinessRule() throws Exception {

        when(ticketService.purchase(any(PurchaseTicketRequest.class)))
                .thenThrow(
                        new BusinessRuleException(
                                "User does not meet minimum age."
                        )
                );

        String requestBody = """
                {
                  "userEmail": "laura@example.com",
                  "eventCode": "CMF-2026",
                  "type": "GENERAL"
                }
                """;

        mockMvc.perform(
                        post("/api/tickets")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(requestBody)
                )
                .andExpect(status().isConflict())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.error").value("Conflict"))
                .andExpect(
                        jsonPath("$.message")
                                .value("User does not meet minimum age.")
                );

        verify(ticketService)
                .purchase(any(PurchaseTicketRequest.class));
    }

    @Test
    void shouldReturnTicketByCode() throws Exception {

        when(ticketService.findByCode("TKT-0001"))
                .thenReturn(paidTicket());

        mockMvc.perform(
                        get("/api/tickets/TKT-0001")
                )
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.ticketCode").value("TKT-0001"))
                .andExpect(jsonPath("$.status").value("PAID"))
                .andExpect(jsonPath("$.eventCode").value("CMF-2026"));

        verify(ticketService)
                .findByCode("TKT-0001");
    }

    @Test
    void shouldReturnTicketsByUser() throws Exception {

        when(ticketService.findByUserEmail("andrea@example.com"))
                .thenReturn(List.of(paidTicket()));

        mockMvc.perform(
                        get("/api/tickets/by-user")
                                .param("email", "andrea@example.com")
                )
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].ticketCode").value("TKT-0001"))
                .andExpect(jsonPath("$[0].userEmail").value("andrea@example.com"));

        verify(ticketService)
                .findByUserEmail("andrea@example.com");
    }

    @Test
    void shouldCancelTicket() throws Exception {

        TicketResponse cancelled = ticketWithStatus(
                TicketStatus.CANCELLED
        );

        when(ticketService.cancel("TKT-0001"))
                .thenReturn(cancelled);

        mockMvc.perform(
                        patch("/api/tickets/TKT-0001/cancel")
                )
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.ticketCode").value("TKT-0001"))
                .andExpect(jsonPath("$.status").value("CANCELLED"));

        verify(ticketService)
                .cancel("TKT-0001");
    }

    @Test
    void shouldReturn409WhenTicketCannotBeCancelled() throws Exception {

        when(ticketService.cancel("TKT-0001"))
                .thenThrow(
                        new BusinessRuleException(
                                "Only PAID tickets can be cancelled."
                        )
                );

        mockMvc.perform(
                        patch("/api/tickets/TKT-0001/cancel")
                )
                .andExpect(status().isConflict())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(
                        jsonPath("$.message")
                                .value("Only PAID tickets can be cancelled.")
                );

        verify(ticketService)
                .cancel("TKT-0001");
    }

    @Test
    void shouldMarkTicketAsUsed() throws Exception {

        TicketResponse used = ticketWithStatus(
                TicketStatus.USED
        );

        when(ticketService.markAsUsed("TKT-0001"))
                .thenReturn(used);

        mockMvc.perform(
                        patch("/api/tickets/TKT-0001/use")
                )
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.ticketCode").value("TKT-0001"))
                .andExpect(jsonPath("$.status").value("USED"));

        verify(ticketService)
                .markAsUsed("TKT-0001");
    }

    @Test
    void shouldReturn409WhenTicketCannotBeUsed() throws Exception {

        when(ticketService.markAsUsed("TKT-0001"))
                .thenThrow(
                        new BusinessRuleException(
                                "Only PAID tickets can be marked as used."
                        )
                );

        mockMvc.perform(
                        patch("/api/tickets/TKT-0001/use")
                )
                .andExpect(status().isConflict())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(
                        jsonPath("$.message")
                                .value("Only PAID tickets can be marked as used.")
                );

        verify(ticketService)
                .markAsUsed("TKT-0001");
    }

    @Test
    void shouldReturn400WhenTicketTypeIsInvalid() throws Exception {

        String requestBody = """
                {
                  "userEmail": "andrea@example.com",
                  "eventCode": "CMF-2026",
                  "type": "PLATINUM"
                }
                """;

        mockMvc.perform(
                        post("/api/tickets")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(requestBody)
                )
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(
                        jsonPath("$.message")
                                .value("Malformed or invalid JSON request")
                )
                .andExpect(jsonPath("$.details.body").exists());

        verify(ticketService, never())
                .purchase(any(PurchaseTicketRequest.class));
    }

    @Test
    void shouldReturn500WhenUnexpectedErrorOccurs() throws Exception {

        when(ticketService.findByCode("TKT-ERROR"))
                .thenThrow(
                        new RuntimeException(
                                "Internal failure"
                        )
                );

        mockMvc.perform(
                        get("/api/tickets/TKT-ERROR")
                )
                .andExpect(status().isInternalServerError())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(500))
                .andExpect(
                        jsonPath("$.error")
                                .value("Internal Server Error")
                )
                .andExpect(
                        jsonPath("$.message")
                                .value("An unexpected error occurred")
                )
                .andExpect(jsonPath("$.details").isEmpty());

        verify(ticketService)
                .findByCode("TKT-ERROR");
    }

    private TicketResponse paidTicket() {
        return ticketWithStatus(
                TicketStatus.PAID
        );
    }

    private TicketResponse ticketWithStatus(
            TicketStatus status
    ) {
        return new TicketResponse(
                1L,
                "TKT-0001",
                TicketType.GENERAL,
                new BigDecimal("120000.00"),
                status,
                LocalDateTime.of(2026, 10, 7, 12, 0),
                "andrea@example.com",
                "CMF-2026",
                "Caribbean Music Fest 2026"
        );
    }
}