package com.pulsepass.security;

import com.pulsepass.controller.ArtistController;
import com.pulsepass.controller.AuthController;
import com.pulsepass.controller.EventController;
import com.pulsepass.controller.TicketController;
import com.pulsepass.controller.UserController;
import com.pulsepass.controller.VenueController;

import com.pulsepass.domain.EventCategory;
import com.pulsepass.domain.EventStatus;
import com.pulsepass.domain.TicketStatus;
import com.pulsepass.domain.TicketType;

import com.pulsepass.dto.request.CreateEventRequest;
import com.pulsepass.dto.request.LoginRequest;
import com.pulsepass.dto.request.PurchaseTicketRequest;

import com.pulsepass.dto.response.AuthResponse;
import com.pulsepass.dto.response.EventResponse;
import com.pulsepass.dto.response.TicketResponse;
import com.pulsepass.dto.response.VenueResponse;

import com.pulsepass.exception.GlobalExceptionHandler;
import com.pulsepass.exception.ResourceNotFoundException;

import com.pulsepass.service.ArtistService;
import com.pulsepass.service.AuthService;
import com.pulsepass.service.EventService;
import com.pulsepass.service.TicketService;
import com.pulsepass.service.UserService;
import com.pulsepass.service.VenueService;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import javax.crypto.SecretKey;

import java.math.BigDecimal;

import java.time.Instant;
import java.time.LocalDateTime;

import java.util.Date;
import java.util.List;
import java.util.UUID;

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

@WebMvcTest(
        controllers = {
                VenueController.class,
                ArtistController.class,
                EventController.class,
                UserController.class,
                TicketController.class,
                AuthController.class
        }
)
@Import({
        SecurityConfig.class,
        JwtAuthenticationFilter.class,
        JwtService.class,
        RestAuthenticationEntryPoint.class,
        RestAccessDeniedHandler.class,
        GlobalExceptionHandler.class
})
class SecurityHttpTest {

    private static final String TEST_SECRET_BASE64 =
            "QUJDREVGR0hJSktMTU5PUFFSU1RVVldYWVoxMjM0NTY=";

    private static final String TEST_ISSUER =
            "pulsepass-api-test";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtService jwtService;

    @MockitoBean
    private CustomUserDetailsService userDetailsService;

    @MockitoBean
    private VenueService venueService;

    @MockitoBean
    private ArtistService artistService;

    @MockitoBean
    private EventService eventService;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private TicketService ticketService;

    @MockitoBean
    private AuthService authService;

    @Test
    void anonymousShouldAccessPublishedEvents()
            throws Exception {

        when(
                eventService.findPublishedEvents()
        ).thenReturn(
                List.of()
        );

        mockMvc.perform(
                        get(
                                "/api/events/published"
                        )
                )
                .andExpect(
                        status().isOk()
                );

        verify(eventService)
                .findPublishedEvents();
    }

    @Test
    void anonymousShouldAccessActiveVenues()
            throws Exception {

        when(
                venueService.findActiveVenues()
        ).thenReturn(
                List.of()
        );

        mockMvc.perform(
                        get(
                                "/api/venues/active"
                        )
                )
                .andExpect(
                        status().isOk()
                );

        verify(venueService)
                .findActiveVenues();
    }

    @Test
    void anonymousShouldAccessVenueByCode()
            throws Exception {

        VenueResponse response =
                new VenueResponse(
                        1L,
                        "VEN-SMR-01",
                        "Marina Convention Center",
                        "Santa Marta",
                        "Carrera 1 # 20-15",
                        5000,
                        true
                );

        when(
                venueService.findByCode(
                        "VEN-SMR-01"
                )
        ).thenReturn(response);

        mockMvc.perform(
                        get(
                                "/api/venues/VEN-SMR-01"
                        )
                )
                .andExpect(
                        status().isOk()
                )
                .andExpect(
                        jsonPath("$.code")
                                .value(
                                        "VEN-SMR-01"
                                )
                );
    }

    @Test
    void anonymousShouldNotCreateEvent()
            throws Exception {

        mockMvc.perform(
                        post(
                                "/api/events"
                        )
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content(
                                        validEventJson()
                                )
                )
                .andExpect(
                        status().isUnauthorized()
                )
                .andExpect(
                        jsonPath("$.status")
                                .value(401)
                );

        verify(
                eventService,
                never()
        ).create(
                any(CreateEventRequest.class)
        );
    }

    @Test
    void userShouldNotCreateEvent()
            throws Exception {

        mockMvc.perform(
                        post(
                                "/api/events"
                        )
                                .with(
                                        asUser()
                                )
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content(
                                        validEventJson()
                                )
                )
                .andExpect(
                        status().isForbidden()
                )
                .andExpect(
                        jsonPath("$.status")
                                .value(403)
                );

        verify(
                eventService,
                never()
        ).create(
                any(CreateEventRequest.class)
        );
    }

    @Test
    void adminShouldCreateEvent()
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
                        post(
                                "/api/events"
                        )
                                .with(
                                        asAdmin()
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
                        jsonPath("$.status")
                                .value("DRAFT")
                );

        verify(eventService)
                .create(
                        any(CreateEventRequest.class)
                );
    }

    @Test
    void anonymousShouldLoginAndReceiveBearer()
            throws Exception {

        when(
                authService.login(
                        any(LoginRequest.class)
                )
        ).thenReturn(
                new AuthResponse(
                        "signed.jwt.token",
                        "Bearer",
                        900L
                )
        );

        String requestBody = """
                {
                  "identifier": "andrea",
                  "password": "UnaFraseSeguraLarga2026!"
                }
                """;

        mockMvc.perform(
                        post(
                                "/api/auth/login"
                        )
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content(requestBody)
                )
                .andExpect(
                        status().isOk()
                )
                .andExpect(
                        jsonPath("$.accessToken")
                                .value(
                                        "signed.jwt.token"
                                )
                )
                .andExpect(
                        jsonPath("$.tokenType")
                                .value("Bearer")
                )
                .andExpect(
                        jsonPath("$.expiresIn")
                                .value(900)
                );
    }

    @Test
    void invalidLoginShouldReturn401()
            throws Exception {

        when(
                authService.login(
                        any(LoginRequest.class)
                )
        ).thenThrow(
                new BadCredentialsException(
                        "Bad credentials"
                )
        );

        String requestBody = """
                {
                  "identifier": "andrea",
                  "password": "incorrecta"
                }
                """;

        mockMvc.perform(
                        post(
                                "/api/auth/login"
                        )
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content(requestBody)
                )
                .andExpect(
                        status().isUnauthorized()
                )
                .andExpect(
                        jsonPath("$.status")
                                .value(401)
                )
                .andExpect(
                        jsonPath("$.message")
                                .value(
                                        "Invalid credentials"
                                )
                );
    }

    @Test
    void expiredJwtShouldReturn401()
            throws Exception {

        String token =
                expiredToken(
                        1L
                );

        mockMvc.perform(
                        get(
                                "/api/users/by-email"
                        )
                                .param(
                                        "email",
                                        "andrea@example.com"
                                )
                                .header(
                                        HttpHeaders.AUTHORIZATION,
                                        "Bearer " + token
                                )
                )
                .andExpect(
                        status().isUnauthorized()
                )
                .andExpect(
                        content().contentTypeCompatibleWith(
                                MediaType.APPLICATION_JSON
                        )
                )
                .andExpect(
                        jsonPath("$.status")
                                .value(401)
                );

        verify(
                userService,
                never()
        ).findByEmail(
                any(String.class)
        );
    }

    @Test
    void tamperedJwtShouldReturn401()
            throws Exception {

        String validToken =
                jwtService.generateToken(
                        principal(
                                1L,
                                true,
                                "ROLE_USER"
                        )
                );

        String tampered =
                tamperPayload(
                        validToken
                );

        mockMvc.perform(
                        get(
                                "/api/users/by-email"
                        )
                                .param(
                                        "email",
                                        "andrea@example.com"
                                )
                                .header(
                                        HttpHeaders.AUTHORIZATION,
                                        "Bearer " + tampered
                                )
                )
                .andExpect(
                        status().isUnauthorized()
                )
                .andExpect(
                        jsonPath("$.status")
                                .value(401)
                );

        verify(
                userService,
                never()
        ).findByEmail(
                any(String.class)
        );
    }

    @Test
    void userShouldReceive403ForAnotherUsersTicket()
            throws Exception {

        when(
                ticketService.findByCode(
                        "TKT-CARLOS"
                )
        ).thenThrow(
                new AccessDeniedException(
                        "Access denied"
                )
        );

        mockMvc.perform(
                        get(
                                "/api/tickets/TKT-CARLOS"
                        )
                                .with(
                                        asUser()
                                )
                )
                .andExpect(
                        status().isForbidden()
                )
                .andExpect(
                        jsonPath("$.status")
                                .value(403)
                );
    }

    @Test
    void userShouldNotPurchaseTicketForAnotherUser()
            throws Exception {

        when(
                ticketService.purchase(
                        any(PurchaseTicketRequest.class)
                )
        ).thenThrow(
                new AccessDeniedException(
                        "Access denied"
                )
        );

        String requestBody = """
                {
                  "userEmail": "carlos@example.com",
                  "eventCode": "CMF-2027",
                  "type": "GENERAL"
                }
                """;

        mockMvc.perform(
                        post(
                                "/api/tickets"
                        )
                                .with(
                                        asUser()
                                )
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content(requestBody)
                )
                .andExpect(
                        status().isForbidden()
                )
                .andExpect(
                        jsonPath("$.status")
                                .value(403)
                );
    }

    @Test
    void userShouldCancelOwnTicket()
            throws Exception {

        when(
                ticketService.cancel(
                        "TKT-0001"
                )
        ).thenReturn(
                ticketResponse(
                        TicketStatus.CANCELLED
                )
        );

        mockMvc.perform(
                        patch(
                                "/api/tickets/TKT-0001/cancel"
                        )
                                .with(
                                        asUser()
                                )
                )
                .andExpect(
                        status().isOk()
                )
                .andExpect(
                        jsonPath("$.status")
                                .value(
                                        "CANCELLED"
                                )
                );
    }

    @Test
    void userShouldNotMarkTicketAsUsed()
            throws Exception {

        mockMvc.perform(
                        patch(
                                "/api/tickets/TKT-0001/use"
                        )
                                .with(
                                        asUser()
                                )
                )
                .andExpect(
                        status().isForbidden()
                );

        verify(
                ticketService,
                never()
        ).markAsUsed(
                any(String.class)
        );
    }

    @Test
    void staffShouldMarkTicketAsUsed()
            throws Exception {

        when(
                ticketService.markAsUsed(
                        "TKT-0001"
                )
        ).thenReturn(
                ticketResponse(
                        TicketStatus.USED
                )
        );

        mockMvc.perform(
                        patch(
                                "/api/tickets/TKT-0001/use"
                        )
                                .with(
                                        asStaff()
                                )
                )
                .andExpect(
                        status().isOk()
                )
                .andExpect(
                        jsonPath("$.status")
                                .value("USED")
                );
    }

    @Test
    void adminShouldPublishEvent()
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
                                        asAdmin()
                                )
                )
                .andExpect(
                        status().isOk()
                )
                .andExpect(
                        jsonPath("$.status")
                                .value(
                                        "PUBLISHED"
                                )
                );
    }

    @Test
    void userShouldNotReadAnotherUsersData()
            throws Exception {

        when(
                userService.findByEmail(
                        "carlos@example.com"
                )
        ).thenThrow(
                new AccessDeniedException(
                        "Access denied"
                )
        );

        mockMvc.perform(
                        get(
                                "/api/users/by-email"
                        )
                                .with(
                                        asUser()
                                )
                                .param(
                                        "email",
                                        "carlos@example.com"
                                )
                )
                .andExpect(
                        status().isForbidden()
                )
                .andExpect(
                        jsonPath("$.status")
                                .value(403)
                );
    }

    @Test
    void inactiveUserWithValidJwtShouldReturn401()
            throws Exception {

        String token =
                jwtService.generateToken(
                        principal(
                                5L,
                                true,
                                "ROLE_USER"
                        )
                );

        when(
                userDetailsService.loadUserById(
                        5L
                )
        ).thenReturn(
                principal(
                        5L,
                        false,
                        "ROLE_USER"
                )
        );

        mockMvc.perform(
                        get(
                                "/api/users/by-email"
                        )
                                .param(
                                        "email",
                                        "inactive@example.com"
                                )
                                .header(
                                        HttpHeaders.AUTHORIZATION,
                                        "Bearer " + token
                                )
                )
                .andExpect(
                        status().isUnauthorized()
                )
                .andExpect(
                        jsonPath("$.status")
                                .value(401)
                );
    }

    @Test
    void anonymousShouldNotReceiveDraftEvent()
            throws Exception {

        when(
                eventService.findByCode(
                        "DRAFT-001"
                )
        ).thenThrow(
                new ResourceNotFoundException(
                        "Event not found: DRAFT-001"
                )
        );

        mockMvc.perform(
                        get(
                                "/api/events/DRAFT-001"
                        )
                )
                .andExpect(
                        status().isNotFound()
                )
                .andExpect(
                        jsonPath("$.status")
                                .value(404)
                );
    }

    @Test
    void authorizedInvalidRequestShouldReturn400()
            throws Exception {

        String requestBody = """
                {
                  "eventCode": "",
                  "name": "",
                  "description": "Invalid",
                  "category": null,
                  "eventDate": null,
                  "minimumAge": -1,
                  "venueCode": ""
                }
                """;

        mockMvc.perform(
                        post(
                                "/api/events"
                        )
                                .with(
                                        asAdmin()
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
                );

        verify(
                eventService,
                never()
        ).create(
                any(CreateEventRequest.class)
        );
    }

    @Test
    void authenticationErrorShouldNotExposeSecrets()
            throws Exception {

        when(
                authService.login(
                        any(LoginRequest.class)
                )
        ).thenThrow(
                new BadCredentialsException(
                        "Internal authentication details"
                )
        );

        String requestBody = """
                {
                  "identifier": "andrea",
                  "password": "super-secret-value"
                }
                """;

        mockMvc.perform(
                        post(
                                "/api/auth/login"
                        )
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content(requestBody)
                )
                .andExpect(
                        status().isUnauthorized()
                )
                .andExpect(
                        jsonPath("$.message")
                                .value(
                                        "Invalid credentials"
                                )
                )
                .andExpect(
                        jsonPath("$.password")
                                .doesNotExist()
                )
                .andExpect(
                        jsonPath("$.passwordHash")
                                .doesNotExist()
                )
                .andExpect(
                        jsonPath("$.trace")
                                .doesNotExist()
                );
    }

    @Test
    void userShouldNotPublishEvent()
            throws Exception {

        mockMvc.perform(
                        patch(
                                "/api/events/CMF-2027/publish"
                        )
                                .with(
                                        asUser()
                                )
                )
                .andExpect(
                        status().isForbidden()
                );

        verify(
                eventService,
                never()
        ).publish(
                any(String.class)
        );
    }

    @Test
    void userShouldNotAddArtistToEvent()
            throws Exception {

        mockMvc.perform(
                        post(
                                "/api/events/CMF-2027/artists/1"
                        )
                                .with(
                                        asUser()
                                )
                )
                .andExpect(
                        status().isForbidden()
                );

        verify(
                eventService,
                never()
        ).addArtist(
                any(String.class),
                any(Long.class)
        );
    }

    @Test
    void adminShouldAccessPaidTicketsByEvent()
            throws Exception {

        when(
                ticketService.findPaidTicketsByEvent(
                        "CMF-2027"
                )
        ).thenReturn(
                List.of()
        );

        mockMvc.perform(
                        get(
                                "/api/events/CMF-2027/tickets/paid"
                        )
                                .with(
                                        asAdmin()
                                )
                )
                .andExpect(
                        status().isOk()
                );

        verify(ticketService)
                .findPaidTicketsByEvent(
                        "CMF-2027"
                );
    }

    @Test
    void anonymousShouldNotAccessUserData()
            throws Exception {

        mockMvc.perform(
                        get(
                                "/api/users/by-email"
                        )
                                .param(
                                        "email",
                                        "andrea@example.com"
                                )
                )
                .andExpect(
                        status().isUnauthorized()
                );

        verify(
                userService,
                never()
        ).findByEmail(
                any(String.class)
        );
    }

    @Test
    void anonymousShouldNotPurchaseTicket()
            throws Exception {

        mockMvc.perform(
                        post(
                                "/api/tickets"
                        )
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content(
                                        validPurchaseJson()
                                )
                )
                .andExpect(
                        status().isUnauthorized()
                );

        verify(
                ticketService,
                never()
        ).purchase(
                any(PurchaseTicketRequest.class)
        );
    }

    private RequestPostProcessor asUser() {

        return user("andrea")
                .roles("USER");
    }

    private RequestPostProcessor asStaff() {

        return user("laura")
                .roles("STAFF");
    }

    private RequestPostProcessor asAdmin() {

        return user("admin")
                .roles("ADMIN");
    }

    private SecurityPrincipal principal(
            Long userId,
            boolean active,
            String authority
    ) {

        return new SecurityPrincipal(
                userId,
                "andrea",
                "andrea@example.com",
                "$2a$10$testHash",
                active,
                List.of(
                        new SimpleGrantedAuthority(
                                authority
                        )
                )
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

    private String validPurchaseJson() {

        return """
                {
                  "userEmail": "andrea@example.com",
                  "eventCode": "CMF-2027",
                  "type": "GENERAL"
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

    private TicketResponse ticketResponse(
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
                        20,
                        0
                ),
                "andrea@example.com",
                "CMF-2027",
                "Caribbean Music Fest 2027"
        );
    }

    private String expiredToken(
            Long userId
    ) {

        SecretKey key =
                Keys.hmacShaKeyFor(
                        Decoders.BASE64.decode(
                                TEST_SECRET_BASE64
                        )
                );

        Instant now =
                Instant.now();

        return Jwts.builder()
                .subject(
                        userId.toString()
                )
                .issuer(
                        TEST_ISSUER
                )
                .issuedAt(
                        Date.from(
                                now.minusSeconds(120)
                        )
                )
                .expiration(
                        Date.from(
                                now.minusSeconds(60)
                        )
                )
                .id(
                        UUID.randomUUID()
                                .toString()
                )
                .signWith(
                        key,
                        Jwts.SIG.HS256
                )
                .compact();
    }

    private String tamperPayload(
            String token
    ) {

        String[] parts =
                token.split("\\.");

        String payload =
                parts[1];

        char original =
                payload.charAt(0);

        char replacement =
                original == 'A'
                        ? 'B'
                        : 'A';

        parts[1] =
                replacement
                        + payload.substring(1);

        return String.join(
                ".",
                parts
        );
    }
}