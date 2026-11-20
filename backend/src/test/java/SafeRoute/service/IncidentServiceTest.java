package SafeRoute.service;

import SafeRoute.dto.request.IncidentRequest;
import SafeRoute.dto.response.IncidentResponse;
import SafeRoute.dto.response.PagedResponse;
import SafeRoute.entity.Incident;
import SafeRoute.entity.User;
import SafeRoute.enums.IncidentSeverity;
import SafeRoute.enums.IncidentStatus;
import SafeRoute.enums.IncidentType;
import SafeRoute.enums.Role;
import SafeRoute.exception.AccessDeniedException;
import SafeRoute.exception.ResourceNotFoundException;
import SafeRoute.repository.IncidentRepository;
import SafeRoute.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class IncidentServiceTest {

    @Mock
    private IncidentRepository incidentRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private IncidentService incidentService;

    private User regularUser;
    private User otherUser;
    private User adminUser;
    private Incident testIncident;
    private IncidentRequest incidentRequest;

    @BeforeEach
    void setUp() {
        regularUser = User.builder()
                .id(1L)
                .fullName("Regular User")
                .email("user@example.com")
                .role(Role.USER)
                .build();

        otherUser = User.builder()
                .id(2L)
                .fullName("Other User")
                .email("other@example.com")
                .role(Role.USER)
                .build();

        adminUser = User.builder()
                .id(99L)
                .fullName("Admin User")
                .email("admin@example.com")
                .role(Role.ADMIN)
                .build();

        testIncident = Incident.builder()
                .id(10L)
                .title("Dark Alleyway")
                .description("No streetlights working")
                .incidentType(IncidentType.POOR_LIGHTING)
                .latitude(12.9716)
                .longitude(77.5946)
                .severity(IncidentSeverity.HIGH)
                .status(IncidentStatus.REPORTED)
                .createdAt(LocalDateTime.now())
                .user(regularUser)
                .build();

        incidentRequest = IncidentRequest.builder()
                .title("Dark Alleyway")
                .description("No streetlights working")
                .incidentType(IncidentType.POOR_LIGHTING)
                .latitude(12.9716)
                .longitude(77.5946)
                .severity(IncidentSeverity.HIGH)
                .build();
    }

    @Test
    @DisplayName("Should create incident deriving ownership from authenticated user email")
    void testCreateIncident_Success() {
        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(regularUser));
        when(incidentRepository.save(any(Incident.class))).thenReturn(testIncident);

        IncidentResponse response = incidentService.createIncident(incidentRequest, "user@example.com");

        assertNotNull(response);
        assertEquals(10L, response.getId());
        assertEquals("Dark Alleyway", response.getTitle());
        assertEquals(1L, response.getReporterId());
        assertEquals("Regular User", response.getReporterName());
        assertEquals(IncidentStatus.REPORTED, response.getStatus());

        verify(incidentRepository).save(any(Incident.class));
    }

    @Test
    @DisplayName("Should fetch incident by ID")
    void testGetIncidentById_Success() {
        when(incidentRepository.findById(10L)).thenReturn(Optional.of(testIncident));

        IncidentResponse response = incidentService.getIncidentById(10L);

        assertNotNull(response);
        assertEquals(10L, response.getId());
        assertEquals("Dark Alleyway", response.getTitle());
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when incident ID does not exist")
    void testGetIncidentById_NotFound() {
        when(incidentRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> incidentService.getIncidentById(999L));
    }

    @Test
    @DisplayName("Should clamp pagination size to maximum 100")
    void testGetAllIncidents_ClampsSizeTo100() {
        Page<Incident> page = new PageImpl<>(Collections.singletonList(testIncident));
        when(incidentRepository.findAll(any(Pageable.class))).thenReturn(page);

        PagedResponse<IncidentResponse> response = incidentService.getAllIncidents(
                null, null, 0, 500, "createdAt", "desc");

        assertNotNull(response);
        assertEquals(1, response.getContent().size());

        org.mockito.ArgumentCaptor<Pageable> captor = org.mockito.ArgumentCaptor.forClass(Pageable.class);
        verify(incidentRepository).findAll(captor.capture());
        assertEquals(100, captor.getValue().getPageSize());
    }

    @Test
    @DisplayName("Should find nearby incidents within radius and calculate distance")
    void testGetNearbyIncidents_Success() {
        // Center: 12.9716, 77.5946. testIncident is exactly at center (dist = 0.0)
        when(incidentRepository.findCandidatesInBoundingBox(anyDouble(), anyDouble(), anyDouble(), anyDouble()))
                .thenReturn(Collections.singletonList(testIncident));

        List<IncidentResponse> nearby = incidentService.getNearbyIncidents(12.9716, 77.5946, 5.0);

        assertNotNull(nearby);
        assertEquals(1, nearby.size());
        assertEquals(0.0, nearby.get(0).getDistanceKm());
    }

    @Test
    @DisplayName("Should reject invalid nearby radius <= 0 or > 50 km")
    void testGetNearbyIncidents_InvalidRadius() {
        assertThrows(IllegalArgumentException.class, () -> incidentService.getNearbyIncidents(12.9716, 77.5946, 0.0));
        assertThrows(IllegalArgumentException.class, () -> incidentService.getNearbyIncidents(12.9716, 77.5946, -5.0));
        assertThrows(IllegalArgumentException.class, () -> incidentService.getNearbyIncidents(12.9716, 77.5946, 55.0));
    }

    @Test
    @DisplayName("Only ADMIN can mark incident as VERIFIED")
    void testUpdateStatus_AdminCanVerify() {
        when(incidentRepository.findById(10L)).thenReturn(Optional.of(testIncident));
        when(userRepository.findByEmail("admin@example.com")).thenReturn(Optional.of(adminUser));
        when(incidentRepository.save(any(Incident.class))).thenReturn(testIncident);

        IncidentResponse response = incidentService.updateIncidentStatus(10L, IncidentStatus.VERIFIED, "admin@example.com");

        assertNotNull(response);
        verify(incidentRepository).save(argThat(i -> i.getStatus() == IncidentStatus.VERIFIED));
    }

    @Test
    @DisplayName("Regular user cannot mark incident as VERIFIED or DISMISSED")
    void testUpdateStatus_RegularUserCannotVerify() {
        when(incidentRepository.findById(10L)).thenReturn(Optional.of(testIncident));
        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(regularUser));

        assertThrows(AccessDeniedException.class, () ->
                incidentService.updateIncidentStatus(10L, IncidentStatus.VERIFIED, "user@example.com"));

        assertThrows(AccessDeniedException.class, () ->
                incidentService.updateIncidentStatus(10L, IncidentStatus.DISMISSED, "user@example.com"));

        verify(incidentRepository, never()).save(any(Incident.class));
    }

    @Test
    @DisplayName("Report owner can mark incident as RESOLVED")
    void testUpdateStatus_OwnerCanResolve() {
        when(incidentRepository.findById(10L)).thenReturn(Optional.of(testIncident));
        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(regularUser));
        when(incidentRepository.save(any(Incident.class))).thenReturn(testIncident);

        IncidentResponse response = incidentService.updateIncidentStatus(10L, IncidentStatus.RESOLVED, "user@example.com");

        assertNotNull(response);
        verify(incidentRepository).save(argThat(i -> i.getStatus() == IncidentStatus.RESOLVED));
    }

    @Test
    @DisplayName("Non-owner non-admin cannot update incident status")
    void testUpdateStatus_NonOwnerCannotUpdate() {
        when(incidentRepository.findById(10L)).thenReturn(Optional.of(testIncident));
        when(userRepository.findByEmail("other@example.com")).thenReturn(Optional.of(otherUser));

        assertThrows(AccessDeniedException.class, () ->
                incidentService.updateIncidentStatus(10L, IncidentStatus.RESOLVED, "other@example.com"));

        verify(incidentRepository, never()).save(any(Incident.class));
    }

    @Test
    @DisplayName("Report owner can delete their own incident")
    void testDeleteIncident_OwnerCanDelete() {
        when(incidentRepository.findById(10L)).thenReturn(Optional.of(testIncident));
        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(regularUser));

        incidentService.deleteIncident(10L, "user@example.com");

        verify(incidentRepository).delete(testIncident);
    }

    @Test
    @DisplayName("Admin can delete any incident")
    void testDeleteIncident_AdminCanDelete() {
        when(incidentRepository.findById(10L)).thenReturn(Optional.of(testIncident));
        when(userRepository.findByEmail("admin@example.com")).thenReturn(Optional.of(adminUser));

        incidentService.deleteIncident(10L, "admin@example.com");

        verify(incidentRepository).delete(testIncident);
    }

    @Test
    @DisplayName("Non-owner non-admin cannot delete incident")
    void testDeleteIncident_NonOwnerCannotDelete() {
        when(incidentRepository.findById(10L)).thenReturn(Optional.of(testIncident));
        when(userRepository.findByEmail("other@example.com")).thenReturn(Optional.of(otherUser));

        assertThrows(AccessDeniedException.class, () ->
                incidentService.deleteIncident(10L, "other@example.com"));

        verify(incidentRepository, never()).delete(any(Incident.class));
    }
}

