package com.pulsepass.controller;

import com.pulsepass.domain.TicketStatus;
import com.pulsepass.domain.TicketType;

import com.pulsepass.dto.request.PurchaseTicketRequest;
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

@WebMvcTest(TicketController.class)
@Import({
        SecurityConfig.class,
        JwtAuthenticationFilter.class,
        RestAuthenticationEntryPoint.class,
        RestAccessDeniedHandler.class,
        GlobalExceptionHandler.class
})
class TicketControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TicketService ticketService;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private CustomUserDetailsService userDetailsService;

    @Test
    void shouldPurchaseTicketAndReturn201()
            throws Exception {

        when(
                ticketService.purchase(
                        any(PurchaseTicketRequest.class)
                )
        ).thenReturn(
                paidTicket()
        );

        mockMvc.perform(
                        post("/api/tickets")
                                .with(
                                        user("andrea")
                                                .roles("USER")
                                )
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content(
                                        validPurchaseJson()
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
                        jsonPath("$.ticketCode")
                                .value(
                                        "TKT-0001"
                                )
                )
                .andExpect(
                        jsonPath("$.type")
                                .value("GENERAL")
                )
                .andExpect(
                        jsonPath("$.status")
                                .value("PAID")
                )
                .andExpect(
                        jsonPath("$.userEmail")
                                .value(
                                        "andrea@example.com"
                                )
                )
                .andExpect(
                        jsonPath("$.eventCode")
                                .value(
                                        "CMF-2027"
                                )
                );

        verify(ticketService)
                .purchase(
                        any(PurchaseTicketRequest.class)
                );
    }

    @Test
    void shouldReturn400WhenPurchaseRequestIsInvalid()
            throws Exception {

        String requestBody = """
                {
                  "userEmail": "correo-invalido",
                  "eventCode": "",
                  "type": null
                }
                """;

        mockMvc.perform(
                        post("/api/tickets")
                                .with(
                                        user("andrea")
                                                .roles("USER")
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
                        jsonPath("$.details.userEmail")
                                .exists()
                )
                .andExpect(
                        jsonPath("$.details.eventCode")
                                .exists()
                )
                .andExpect(
                        jsonPath("$.details.type")
                                .exists()
                );

        verify(
                ticketService,
                never()
        ).purchase(
                any(PurchaseTicketRequest.class)
        );
    }

    @Test
    void shouldReturn404WhenUserDoesNotExist()
            throws Exception {

        when(
                ticketService.purchase(
                        any(PurchaseTicketRequest.class)
                )
        ).thenThrow(
                new ResourceNotFoundException(
                        "User not found: missing@example.com"
                )
        );

        String requestBody = """
                {
                  "userEmail": "missing@example.com",
                  "eventCode": "CMF-2027",
                  "type": "GENERAL"
                }
                """;

        mockMvc.perform(
                        post("/api/tickets")
                                .with(
                                        user("andrea")
                                                .roles("USER")
                                )
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content(requestBody)
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
                                        "User not found: missing@example.com"
                                )
                );
    }

    @Test
    void shouldReturn409WhenPurchaseViolatesBusinessRule()
            throws Exception {

        when(
                ticketService.purchase(
                        any(PurchaseTicketRequest.class)
                )
        ).thenThrow(
                new BusinessRuleException(
                        "User does not meet minimum age."
                )
        );

        mockMvc.perform(
                        post("/api/tickets")
                                .with(
                                        user("andrea")
                                                .roles("USER")
                                )
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content(
                                        validPurchaseJson()
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
                        jsonPath("$.message")
                                .value(
                                        "User does not meet minimum age."
                                )
                );
    }

    @Test
    void shouldReturnTicketByCode()
            throws Exception {

        when(
                ticketService.findByCode(
                        "TKT-0001"
                )
        ).thenReturn(
                paidTicket()
        );

        mockMvc.perform(
                        get(
                                "/api/tickets/TKT-0001"
                        )
                                .with(
                                        user("andrea")
                                                .roles("USER")
                                )
                )
                .andExpect(
                        status().isOk()
                )
                .andExpect(
                        jsonPath("$.ticketCode")
                                .value(
                                        "TKT-0001"
                                )
                )
                .andExpect(
                        jsonPath("$.status")
                                .value("PAID")
                )
                .andExpect(
                        jsonPath("$.eventCode")
                                .value(
                                        "CMF-2027"
                                )
                );

        verify(ticketService)
                .findByCode(
                        "TKT-0001"
                );
    }

    @Test
    void shouldReturnTicketsByUser()
            throws Exception {

        when(
                ticketService.findByUserEmail(
                        "andrea@example.com"
                )
        ).thenReturn(
                List.of(
                        paidTicket()
                )
        );

        mockMvc.perform(
                        get(
                                "/api/tickets/by-user"
                        )
                                .with(
                                        user("andrea")
                                                .roles("USER")
                                )
                                .param(
                                        "email",
                                        "andrea@example.com"
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
                        jsonPath("$[0].userEmail")
                                .value(
                                        "andrea@example.com"
                                )
                );

        verify(ticketService)
                .findByUserEmail(
                        "andrea@example.com"
                );
    }

    @Test
    void shouldCancelTicket()
            throws Exception {

        when(
                ticketService.cancel(
                        "TKT-0001"
                )
        ).thenReturn(
                ticketWithStatus(
                        TicketStatus.CANCELLED
                )
        );

        mockMvc.perform(
                        patch(
                                "/api/tickets/TKT-0001/cancel"
                        )
                                .with(
                                        user("andrea")
                                                .roles("USER")
                                )
                )
                .andExpect(
                        status().isOk()
                )
                .andExpect(
                        jsonPath("$.ticketCode")
                                .value(
                                        "TKT-0001"
                                )
                )
                .andExpect(
                        jsonPath("$.status")
                                .value(
                                        "CANCELLED"
                                )
                );

        verify(ticketService)
                .cancel(
                        "TKT-0001"
                );
    }

    @Test
    void shouldReturn409WhenTicketCannotBeCancelled()
            throws Exception {

        when(
                ticketService.cancel(
                        "TKT-0001"
                )
        ).thenThrow(
                new BusinessRuleException(
                        "Only PAID tickets can be cancelled."
                )
        );

        mockMvc.perform(
                        patch(
                                "/api/tickets/TKT-0001/cancel"
                        )
                                .with(
                                        user("andrea")
                                                .roles("USER")
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
                        jsonPath("$.message")
                                .value(
                                        "Only PAID tickets can be cancelled."
                                )
                );

        verify(ticketService)
                .cancel(
                        "TKT-0001"
                );
    }

    @Test
    void shouldMarkTicketAsUsed()
            throws Exception {

        when(
                ticketService.markAsUsed(
                        "TKT-0001"
                )
        ).thenReturn(
                ticketWithStatus(
                        TicketStatus.USED
                )
        );

        mockMvc.perform(
                        patch(
                                "/api/tickets/TKT-0001/use"
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
                        jsonPath("$.ticketCode")
                                .value(
                                        "TKT-0001"
                                )
                )
                .andExpect(
                        jsonPath("$.status")
                                .value("USED")
                );

        verify(ticketService)
                .markAsUsed(
                        "TKT-0001"
                );
    }

    @Test
    void shouldReturn409WhenTicketCannotBeUsed()
            throws Exception {

        when(
                ticketService.markAsUsed(
                        "TKT-0001"
                )
        ).thenThrow(
                new BusinessRuleException(
                        "Only PAID tickets can be marked as used."
                )
        );

        mockMvc.perform(
                        patch(
                                "/api/tickets/TKT-0001/use"
                        )
                                .with(
                                        user("laura")
                                                .roles("STAFF")
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
                        jsonPath("$.message")
                                .value(
                                        "Only PAID tickets can be marked as used."
                                )
                );

        verify(ticketService)
                .markAsUsed(
                        "TKT-0001"
                );
    }

    @Test
    void shouldReturn400WhenTicketTypeIsInvalid()
            throws Exception {

        String requestBody = """
                {
                  "userEmail": "andrea@example.com",
                  "eventCode": "CMF-2027",
                  "type": "PLATINUM"
                }
                """;

        mockMvc.perform(
                        post("/api/tickets")
                                .with(
                                        user("andrea")
                                                .roles("USER")
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
                                        "Malformed or invalid JSON request"
                                )
                )
                .andExpect(
                        jsonPath("$.details.body")
                                .exists()
                );

        verify(
                ticketService,
                never()
        ).purchase(
                any(PurchaseTicketRequest.class)
        );
    }

    @Test
    void shouldReturn500WhenUnexpectedErrorOccurs()
            throws Exception {

        when(
                ticketService.findByCode(
                        "TKT-ERROR"
                )
        ).thenThrow(
                new RuntimeException(
                        "Internal failure"
                )
        );

        mockMvc.perform(
                        get(
                                "/api/tickets/TKT-ERROR"
                        )
                                .with(
                                        user("andrea")
                                                .roles("USER")
                                )
                )
                .andExpect(
                        status().isInternalServerError()
                )
                .andExpect(
                        jsonPath("$.status")
                                .value(500)
                )
                .andExpect(
                        jsonPath("$.error")
                                .value(
                                        "Internal Server Error"
                                )
                )
                .andExpect(
                        jsonPath("$.message")
                                .value(
                                        "An unexpected error occurred"
                                )
                )
                .andExpect(
                        jsonPath("$.details")
                                .isEmpty()
                );

        verify(ticketService)
                .findByCode(
                        "TKT-ERROR"
                );
    }

    private String validPurchaseJson() {

        return """
                {
                  "userEmail": "andrea@example.com",
                  "eventCode": "CMF-2027",
                  "type": "GENERAL"
                }
                """;
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
                new BigDecimal(
                        "120000.00"
                ),
                status,
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
    }
}