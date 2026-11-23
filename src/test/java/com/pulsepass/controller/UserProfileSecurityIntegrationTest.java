package com.pulsepass.controller;

import com.jayway.jsonpath.JsonPath;
import com.pulsepass.domain.User;
import com.pulsepass.domain.UserProfile;
import com.pulsepass.exception.GlobalExceptionHandler;
import com.pulsepass.mapper.UserMapper;
import com.pulsepass.mapper.UserMapperImpl;
import com.pulsepass.repository.UserProfileRepository;
import com.pulsepass.repository.UserRepository;
import com.pulsepass.security.CustomUserDetailsService;
import com.pulsepass.security.JwtAuthenticationFilter;
import com.pulsepass.security.JwtService;
import com.pulsepass.security.RestAccessDeniedHandler;
import com.pulsepass.security.RestAuthenticationEntryPoint;
import com.pulsepass.security.SecurityConfig;
import com.pulsepass.security.SecurityPrincipal;
import com.pulsepass.service.AuthService;
import com.pulsepass.service.AuthorizationService;
import com.pulsepass.service.impl.UserServiceImpl;
import jakarta.persistence.EntityManagerFactory;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import javax.sql.DataSource;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = UserController.class, properties = {
        "spring.datasource.url=jdbc:postgresql://127.0.0.1:1/no_database_allowed",
        "spring.flyway.enabled=false",
        "spring.sql.init.mode=never",
        "spring.jpa.hibernate.ddl-auto=none"
})
@Import({
        SecurityConfig.class,
        JwtAuthenticationFilter.class,
        RestAuthenticationEntryPoint.class,
        RestAccessDeniedHandler.class,
        GlobalExceptionHandler.class,
        UserServiceImpl.class,
        AuthorizationService.class,
        UserMapperImpl.class
})
class UserProfileSecurityIntegrationTest {

    private static final long OWNER_ID = 11L;
    private static final long OTHER_ID = 12L;
    private static final String TEST_HASH = "unused-principal-test-hash";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ApplicationContext context;

    @MockitoBean
    private UserRepository userRepository;

    @MockitoBean
    private UserProfileRepository userProfileRepository;

    @MockitoBean
    private AuthService authService;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private CustomUserDetailsService userDetailsService;

    @MockitoSpyBean
    private UserMapper userMapper;

    private User owner;
    private User other;

    @BeforeEach
    void setUp() {
        assertNoPersistenceInfrastructure();
        owner = profile(OWNER_ID, "profile-owner", "owner@example.test",
                "OwnerFirst", "OwnerLast", "+573000000011", "OwnerCity",
                LocalDate.of(2000, 1, 2));
        other = profile(OTHER_ID, "private-target", "private@example.test",
                "PrivateFirst", "PrivateLast", "+573000000012", "PrivateCity",
                LocalDate.of(1990, 5, 7));
    }

    @Test
    void contextDoesNotInitializeDataSourceJpaOrFlyway() {
        assertNoPersistenceInfrastructure();
    }

    @ParameterizedTest
    @ValueSource(strings = {"email", "username"})
    void staffCanReadOwnProfile(String lookup) throws Exception {
        stubLookup(lookup, owner);

        ResultActions result = mockMvc.perform(
                request(lookup, owner).with(user(principal("STAFF"))));

        assertAuthorizedProfile(result, owner);
        verifyLookup(lookup, owner);
        verify(userMapper).toResponse(owner);
        verifyNoInteractions(userProfileRepository);
    }

    @ParameterizedTest
    @ValueSource(strings = {"email", "username"})
    void userCanReadOwnProfile(String lookup) throws Exception {
        stubLookup(lookup, owner);

        ResultActions result = mockMvc.perform(
                request(lookup, owner).with(user(principal("USER"))));

        assertAuthorizedProfile(result, owner);
        verifyLookup(lookup, owner);
        verify(userMapper).toResponse(owner);
    }

    @ParameterizedTest
    @CsvSource({
            "STAFF,email",
            "STAFF,username",
            "USER,email",
            "USER,username"
    })
    void ownershipDenialDoesNotMapOrExposeOtherProfile(String role, String lookup)
            throws Exception {
        stubLookup(lookup, other);

        MvcResult result = mockMvc.perform(
                        request(lookup, other).with(user(principal(role))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.details").isEmpty())
                .andReturn();

        verifyLookup(lookup, other);
        verify(userMapper, never()).toResponse(any(User.class));
        assertNoProfileData(result);
        verifyNoInteractions(userProfileRepository);
    }

    @ParameterizedTest
    @ValueSource(strings = {"email", "username"})
    void adminKeepsExistingAccessToOtherProfile(String lookup) throws Exception {
        stubLookup(lookup, other);

        ResultActions result = mockMvc.perform(
                request(lookup, other).with(user(principal("ADMIN"))));

        assertAuthorizedProfile(result, other);
        verifyLookup(lookup, other);
        verify(userMapper).toResponse(other);
    }

    @ParameterizedTest
    @ValueSource(strings = {"email", "username"})
    void anonymousCannotReadProfiles(String lookup) throws Exception {
        MvcResult result = mockMvc.perform(request(lookup, other))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andReturn();

        assertNoProfileData(result);
        verifyNoInteractions(userRepository, userProfileRepository, userMapper);
    }

    private void assertNoPersistenceInfrastructure() {
        assertThat(context.getBeansOfType(DataSource.class)).isEmpty();
        assertThat(context.getBeansOfType(EntityManagerFactory.class)).isEmpty();
        assertThat(context.getBeansOfType(Flyway.class)).isEmpty();
    }

    private User profile(long id, String username, String email,
                         String firstName, String lastName, String phone,
                         String city, LocalDate birthDate) {
        User user = new User(username, email, true);
        ReflectionTestUtils.setField(user, "id", id);
        user.assignProfile(new UserProfile(firstName, lastName, phone, city, birthDate));
        return user;
    }

    private SecurityPrincipal principal(String role) {
        SecurityPrincipal principal = new SecurityPrincipal(
                OWNER_ID, owner.getUsername(), owner.getEmail(), TEST_HASH, true,
                List.of(new SimpleGrantedAuthority("ROLE_" + role)));

        assertThat(principal.getAuthorities())
                .extracting(GrantedAuthority::getAuthority)
                .containsExactly("ROLE_" + role);
        return principal;
    }

    private MockHttpServletRequestBuilder request(String lookup, User target) {
        return get("/api/users/by-" + lookup).param(lookup, identifier(lookup, target));
    }

    private String identifier(String lookup, User target) {
        return "email".equals(lookup) ? target.getEmail() : target.getUsername();
    }

    private void stubLookup(String lookup, User target) {
        if ("email".equals(lookup)) {
            when(userRepository.findByEmailIgnoreCase(target.getEmail()))
                    .thenReturn(Optional.of(target));
        } else {
            when(userRepository.findByUsername(target.getUsername()))
                    .thenReturn(Optional.of(target));
        }
    }

    private void verifyLookup(String lookup, User target) {
        if ("email".equals(lookup)) {
            verify(userRepository).findByEmailIgnoreCase(target.getEmail());
        } else {
            verify(userRepository).findByUsername(target.getUsername());
        }
    }

    private void assertAuthorizedProfile(ResultActions result, User target)
            throws Exception {
        MvcResult response = result.andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(target.getId().intValue()))
                .andExpect(jsonPath("$.username").value(target.getUsername()))
                .andExpect(jsonPath("$.email").value(target.getEmail()))
                .andExpect(jsonPath("$.active").value(true))
                .andExpect(jsonPath("$.firstName").value(target.getProfile().getFirstName()))
                .andExpect(jsonPath("$.lastName").value(target.getProfile().getLastName()))
                .andExpect(jsonPath("$.phone").value(target.getProfile().getPhone()))
                .andExpect(jsonPath("$.city").value(target.getProfile().getCity()))
                .andExpect(jsonPath("$.birthDate").value(target.getProfile().getBirthDate().toString()))
                .andReturn();

        String body = response.getResponse().getContentAsString();
        Map<String, Object> json = JsonPath.read(body, "$");
        assertThat(json.keySet()).containsExactlyInAnyOrder(
                "id", "username", "email", "active", "firstName", "lastName",
                "phone", "city", "birthDate");
        assertThat(body).doesNotContain(TEST_HASH, "password", "passwordHash",
                "credentials", "accessToken");
    }

    private void assertNoProfileData(MvcResult result) throws Exception {
        String body = result.getResponse().getContentAsString();
        Map<String, Object> json = JsonPath.read(body, "$");
        assertThat(json.keySet()).containsExactlyInAnyOrder(
                "timestamp", "status", "error", "message", "details");
        assertThat(body).doesNotContain(
                other.getUsername(), other.getEmail(),
                other.getProfile().getFirstName(), other.getProfile().getLastName(),
                other.getProfile().getPhone(), other.getProfile().getCity(),
                other.getProfile().getBirthDate().toString(), TEST_HASH);
    }
}
