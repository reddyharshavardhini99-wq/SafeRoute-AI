package SafeRoute.controller;

import SafeRoute.dto.request.IncidentRequest;
import SafeRoute.dto.request.IncidentStatusUpdateRequest;
import SafeRoute.dto.response.IncidentResponse;
import SafeRoute.dto.response.PagedResponse;
import SafeRoute.enums.IncidentSeverity;
import SafeRoute.enums.IncidentStatus;
import SafeRoute.enums.IncidentType;
import SafeRoute.exception.AccessDeniedException;
import SafeRoute.exception.GlobalExceptionHandler;
import SafeRoute.exception.ResourceNotFoundException;
import SafeRoute.service.IncidentService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.security.Principal;
import java.time.LocalDateTime;
import java.util.Collections;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class IncidentControllerTest {

    private MockMvc mockMvc;

    @Mock
    private IncidentService incidentService;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private IncidentController incidentController;

    private ObjectMapper objectMapper;
    private IncidentResponse sampleResponse;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        mockMvc = MockMvcBuilders.standaloneSetup(incidentController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        sampleResponse = IncidentResponse.builder()
                .id(1L)
                .title("Broken Sidewalk")
                .description("Deep hole in footpath")
                .incidentType(IncidentType.BROKEN_SIDEWALK)
                .latitude(12.9716)
                .longitude(77.5946)
                .severity(IncidentSeverity.MEDIUM)
                .status(IncidentStatus.REPORTED)
                .createdAt(LocalDateTime.now())
                .reporterId(10L)
                .reporterName("Alice")
                .build();
    }

    @Test
    @DisplayName("POST /api/v1/incidents should return 201 Created on valid input")
    void testCreateIncident_Success() throws Exception {
        IncidentRequest request = IncidentRequest.builder()
                .title("Broken Sidewalk")
                .description("Deep hole in footpath")
                .incidentType(IncidentType.BROKEN_SIDEWALK)
                .latitude(12.9716)
                .longitude(77.5946)
                .severity(IncidentSeverity.MEDIUM)
                .build();

        when(incidentService.createIncident(any(IncidentRequest.class), any())).thenReturn(sampleResponse);

        mockMvc.perform(post("/api/v1/incidents")
                        .principal(authentication)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.title").value("Broken Sidewalk"))
                .andExpect(jsonPath("$.incidentType").value("BROKEN_SIDEWALK"));
    }

    @Test
    @DisplayName("POST /api/v1/incidents should return 400 Bad Request on invalid coordinates")
    void testCreateIncident_InvalidCoordinates() throws Exception {
        IncidentRequest invalidRequest = IncidentRequest.builder()
                .title("Broken Sidewalk")
                .incidentType(IncidentType.BROKEN_SIDEWALK)
                .latitude(195.0) // Invalid: > 90
                .longitude(200.0) // Invalid: > 180
                .build();

        mockMvc.perform(post("/api/v1/incidents")
                        .principal(authentication)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.validationErrors").exists());
    }

    @Test
    @DisplayName("GET /api/v1/incidents should return paginated list")
    void testGetAllIncidents_Success() throws Exception {
        PagedResponse<IncidentResponse> pagedResponse = PagedResponse.<IncidentResponse>builder()
                .content(Collections.singletonList(sampleResponse))
                .page(0)
                .size(20)
                .totalElements(1)
                .totalPages(1)
                .last(true)
                .build();

        when(incidentService.getAllIncidents(any(), any(), anyInt(), anyInt(), anyString(), anyString()))
                .thenReturn(pagedResponse);

        mockMvc.perform(get("/api/v1/incidents"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].title").value("Broken Sidewalk"))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    @DisplayName("GET /api/v1/incidents/{id} should return 200 OK when found")
    void testGetIncidentById_Success() throws Exception {
        when(incidentService.getIncidentById(1L)).thenReturn(sampleResponse);

        mockMvc.perform(get("/api/v1/incidents/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.title").value("Broken Sidewalk"));
    }

    @Test
    @DisplayName("GET /api/v1/incidents/{id} should return 404 Not Found when missing")
    void testGetIncidentById_NotFound() throws Exception {
        when(incidentService.getIncidentById(99L))
                .thenThrow(new ResourceNotFoundException("Incident report not found with id: 99"));

        mockMvc.perform(get("/api/v1/incidents/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.message").value("Incident report not found with id: 99"));
    }

    @Test
    @DisplayName("GET /api/v1/incidents/nearby should return list with distance")
    void testGetNearbyIncidents_Success() throws Exception {
        sampleResponse.setDistanceKm(1.25);
        when(incidentService.getNearbyIncidents(12.9716, 77.5946, 5.0))
                .thenReturn(Collections.singletonList(sampleResponse));

        mockMvc.perform(get("/api/v1/incidents/nearby")
                        .param("latitude", "12.9716")
                        .param("longitude", "77.5946")
                        .param("radius", "5.0"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].distanceKm").value(1.25));
    }

    @Test
    @DisplayName("PATCH /api/v1/incidents/{id}/status should return 200 OK when authorized")
    void testUpdateStatus_Success() throws Exception {
        IncidentStatusUpdateRequest statusRequest = new IncidentStatusUpdateRequest(IncidentStatus.RESOLVED);
        sampleResponse.setStatus(IncidentStatus.RESOLVED);

        when(incidentService.updateIncidentStatus(eq(1L), eq(IncidentStatus.RESOLVED), any()))
                .thenReturn(sampleResponse);

        mockMvc.perform(patch("/api/v1/incidents/1/status")
                        .principal(authentication)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(statusRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("RESOLVED"));
    }

    @Test
    @DisplayName("PATCH /api/v1/incidents/{id}/status should return 403 Forbidden when unauthorized")
    void testUpdateStatus_Forbidden() throws Exception {
        IncidentStatusUpdateRequest statusRequest = new IncidentStatusUpdateRequest(IncidentStatus.VERIFIED);

        when(incidentService.updateIncidentStatus(eq(1L), eq(IncidentStatus.VERIFIED), any()))
                .thenThrow(new AccessDeniedException("Only ADMIN users can mark incidents as VERIFIED"));

        mockMvc.perform(patch("/api/v1/incidents/1/status")
                        .principal(authentication)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(statusRequest)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("Forbidden"))
                .andExpect(jsonPath("$.message").value("Only ADMIN users can mark incidents as VERIFIED"));
    }

    @Test
    @DisplayName("DELETE /api/v1/incidents/{id} should return 204 No Content when authorized")
    void testDeleteIncident_Success() throws Exception {
        doNothing().when(incidentService).deleteIncident(eq(1L), any());

        mockMvc.perform(delete("/api/v1/incidents/1")
                        .principal(authentication))
                .andExpect(status().isNoContent());

        verify(incidentService).deleteIncident(eq(1L), any());
    }

    @Test
    @DisplayName("DELETE /api/v1/incidents/{id} should return 403 Forbidden when unauthorized")
    void testDeleteIncident_Forbidden() throws Exception {
        doThrow(new AccessDeniedException("You do not have permission to delete this incident report"))
                .when(incidentService).deleteIncident(eq(1L), any());

        mockMvc.perform(delete("/api/v1/incidents/1")
                        .principal(authentication))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("Forbidden"));
    }
}

