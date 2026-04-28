package com.gl.analyticsservice.controller;

import com.gl.analyticsservice.dto.PatientStatisticsDto;
import com.gl.analyticsservice.service.AnalyticsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Unit tests for {@link AnalyticsController}.
 *
 * Uses standalone MockMvc — no Spring Security filter chain is loaded,
 * so @PreAuthorize is bypassed.  Security integration would belong in a
 * separate @SpringBootTest slice.
 */
@ExtendWith(MockitoExtension.class)
class AnalyticsControllerTest {

    @Mock
    private AnalyticsService analyticsService;

    @InjectMocks
    private AnalyticsController analyticsController;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(analyticsController).build();
    }

    // ------------------------------------------------------------------ //
    //  GET /api/analytics/summary
    // ------------------------------------------------------------------ //

    @Test
    void getSummary_shouldReturn200WithStatistics() throws Exception {
        // Given
        PatientStatisticsDto dto = PatientStatisticsDto.builder()
                .totalPatients(200L)
                .todayRegistrations(10L)
                .thisMonthRegistrations(45L)
                .build();
        when(analyticsService.getStatistics()).thenReturn(dto);

        // When / Then
        mockMvc.perform(get("/api/analytics/summary")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalPatients").value(200))
                .andExpect(jsonPath("$.todayRegistrations").value(10))
                .andExpect(jsonPath("$.thisMonthRegistrations").value(45));

        verify(analyticsService, times(1)).getStatistics();
    }

    @Test
    void getSummary_shouldReturn200WithZeroStatistics() throws Exception {
        // Given — no data in DB
        PatientStatisticsDto dto = PatientStatisticsDto.builder()
                .totalPatients(0L)
                .todayRegistrations(0L)
                .thisMonthRegistrations(0L)
                .build();
        when(analyticsService.getStatistics()).thenReturn(dto);

        // When / Then
        mockMvc.perform(get("/api/analytics/summary")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalPatients").value(0))
                .andExpect(jsonPath("$.todayRegistrations").value(0))
                .andExpect(jsonPath("$.thisMonthRegistrations").value(0));
    }

    @Test
    void getSummary_shouldDelegateToAnalyticsService() throws Exception {
        // Given
        when(analyticsService.getStatistics())
                .thenReturn(PatientStatisticsDto.builder().build());

        // When
        mockMvc.perform(get("/api/analytics/summary"));

        // Then
        verify(analyticsService).getStatistics();
        verifyNoMoreInteractions(analyticsService);
    }
}
