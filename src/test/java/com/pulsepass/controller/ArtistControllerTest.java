package com.pulsepass.controller;

import com.pulsepass.dto.response.ArtistResponse;
import com.pulsepass.exception.GlobalExceptionHandler;
import com.pulsepass.exception.ResourceNotFoundException;
import com.pulsepass.service.ArtistService;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ArtistController.class)
@Import(GlobalExceptionHandler.class)
class ArtistControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ArtistService artistService;

    @Test
    void shouldReturnArtistById() throws Exception {

        ArtistResponse response = new ArtistResponse(
                1L,
                "Solar Beat",
                "Colombia",
                "Electronic",
                true
        );

        when(artistService.findById(1L))
                .thenReturn(response);

        mockMvc.perform(
                        get("/api/artists/1")
                )
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.stageName").value("Solar Beat"))
                .andExpect(jsonPath("$.country").value("Colombia"))
                .andExpect(jsonPath("$.active").value(true));

        verify(artistService)
                .findById(1L);
    }

    @Test
    void shouldReturn404WhenArtistDoesNotExist() throws Exception {

        when(artistService.findById(99L))
                .thenThrow(
                        new ResourceNotFoundException(
                                "Artist not found: 99"
                        )
                );

        mockMvc.perform(
                        get("/api/artists/99")
                )
                .andExpect(status().isNotFound())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(
                        jsonPath("$.message")
                                .value("Artist not found: 99")
                );

        verify(artistService)
                .findById(99L);
    }

    @Test
    void shouldReturnArtistByStageName() throws Exception {

        ArtistResponse response = new ArtistResponse(
                1L,
                "Solar Beat",
                "Colombia",
                "Electronic",
                true
        );

        when(artistService.findByStageName("Solar Beat"))
                .thenReturn(response);

        mockMvc.perform(
                        get("/api/artists/by-stage-name")
                                .param("stageName", "Solar Beat")
                )
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.stageName").value("Solar Beat"))
                .andExpect(jsonPath("$.active").value(true));

        verify(artistService)
                .findByStageName("Solar Beat");
    }

    @Test
    void shouldReturnActiveArtists() throws Exception {

        ArtistResponse artist1 = new ArtistResponse(
                1L,
                "Solar Beat",
                "Colombia",
                "Electronic",
                true
        );

        ArtistResponse artist2 = new ArtistResponse(
                2L,
                "Neon Waves",
                "Colombia",
                "Pop",
                true
        );

        when(artistService.findActiveArtists())
                .thenReturn(List.of(artist1, artist2));

        mockMvc.perform(
                        get("/api/artists/active")
                )
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].stageName").value("Solar Beat"))
                .andExpect(jsonPath("$[1].stageName").value("Neon Waves"))
                .andExpect(jsonPath("$[0].active").value(true))
                .andExpect(jsonPath("$[1].active").value(true));

        verify(artistService)
                .findActiveArtists();
    }
}