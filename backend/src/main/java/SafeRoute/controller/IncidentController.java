package SafeRoute.controller;

import SafeRoute.dto.request.IncidentRequest;
import SafeRoute.dto.request.IncidentStatusUpdateRequest;
import SafeRoute.dto.response.IncidentResponse;
import SafeRoute.dto.response.PagedResponse;
import SafeRoute.enums.IncidentStatus;
import SafeRoute.enums.IncidentType;
import SafeRoute.service.IncidentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/incidents")
@RequiredArgsConstructor
public class IncidentController {

    private final IncidentService incidentService;

    @PostMapping
    public ResponseEntity<IncidentResponse> createIncident(
            @Valid @RequestBody IncidentRequest request,
            Authentication authentication) {

        IncidentResponse response = incidentService.createIncident(request, authentication.getName());
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @GetMapping
    public ResponseEntity<PagedResponse<IncidentResponse>> getAllIncidents(
            @RequestParam(required = false) IncidentStatus status,
            @RequestParam(required = false) IncidentType type,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {

        PagedResponse<IncidentResponse> response = incidentService.getAllIncidents(
                status, type, page, size, sortBy, sortDir);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<IncidentResponse> getIncidentById(@PathVariable Long id) {
        IncidentResponse response = incidentService.getIncidentById(id);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/nearby")
    public ResponseEntity<List<IncidentResponse>> getNearbyIncidents(
            @RequestParam Double latitude,
            @RequestParam Double longitude,
            @RequestParam(defaultValue = "5.0") Double radius) {

        List<IncidentResponse> response = incidentService.getNearbyIncidents(latitude, longitude, radius);
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<IncidentResponse> updateIncidentStatus(
            @PathVariable Long id,
            @Valid @RequestBody IncidentStatusUpdateRequest request,
            Authentication authentication) {

        IncidentResponse response = incidentService.updateIncidentStatus(
                id, request.getStatus(), authentication.getName());
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteIncident(
            @PathVariable Long id,
            Authentication authentication) {

        incidentService.deleteIncident(id, authentication.getName());
        return ResponseEntity.noContent().build();
    }
}

