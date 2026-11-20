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
import SafeRoute.util.GeoUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class IncidentService {

    public static final int MAX_PAGE_SIZE = 100;
    public static final double MAX_RADIUS_KM = 50.0;

    private final IncidentRepository incidentRepository;
    private final UserRepository userRepository;

    @Transactional
    public IncidentResponse createIncident(IncidentRequest request, String currentUserEmail) {
        User user = getUserByEmail(currentUserEmail);

        Incident incident = Incident.builder()
                .title(request.getTitle().trim())
                .description(request.getDescription() != null ? request.getDescription().trim() : null)
                .incidentType(request.getIncidentType())
                .latitude(request.getLatitude())
                .longitude(request.getLongitude())
                .severity(request.getSeverity() != null ? request.getSeverity() : IncidentSeverity.MEDIUM)
                .status(IncidentStatus.REPORTED)
                .user(user)
                .build();

        Incident saved = incidentRepository.save(incident);
        return mapToResponse(saved, null);
    }

    public IncidentResponse getIncidentById(Long id) {
        Incident incident = incidentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Incident report not found with id: " + id));
        return mapToResponse(incident, null);
    }

    public PagedResponse<IncidentResponse> getAllIncidents(
            IncidentStatus status,
            IncidentType type,
            int page,
            int size,
            String sortBy,
            String sortDir) {

        // Enforce pagination constraints (page >= 0, 1 <= size <= 100)
        int clampedPage = Math.max(0, page);
        int clampedSize = Math.min(MAX_PAGE_SIZE, Math.max(1, size));

        Sort sort = sortDir.equalsIgnoreCase("desc") ?
                Sort.by(sortBy).descending() :
                Sort.by(sortBy).ascending();

        Pageable pageable = PageRequest.of(clampedPage, clampedSize, sort);
        Page<Incident> incidentPage;

        if (status != null && type != null) {
            incidentPage = incidentRepository.findByStatusAndIncidentType(status, type, pageable);
        } else if (status != null) {
            incidentPage = incidentRepository.findByStatus(status, pageable);
        } else if (type != null) {
            incidentPage = incidentRepository.findByIncidentType(type, pageable);
        } else {
            incidentPage = incidentRepository.findAll(pageable);
        }

        List<IncidentResponse> content = incidentPage.getContent().stream()
                .map(i -> mapToResponse(i, null))
                .toList();

        return PagedResponse.<IncidentResponse>builder()
                .content(content)
                .page(incidentPage.getNumber())
                .size(incidentPage.getSize())
                .totalElements(incidentPage.getTotalElements())
                .totalPages(incidentPage.getTotalPages())
                .last(incidentPage.isLast())
                .build();
    }

    public List<IncidentResponse> getNearbyIncidents(Double latitude, Double longitude, Double radiusKm) {
        // Validate coordinates
        if (latitude == null || latitude < -90.0 || latitude > 90.0) {
            throw new IllegalArgumentException("Latitude must be between -90.0 and 90.0 degrees");
        }
        if (longitude == null || longitude < -180.0 || longitude > 180.0) {
            throw new IllegalArgumentException("Longitude must be between -180.0 and 180.0 degrees");
        }
        // Validate radius (strictly > 0 and <= 50 km)
        if (radiusKm == null || radiusKm <= 0.0 || radiusKm > MAX_RADIUS_KM) {
            throw new IllegalArgumentException("Search radius must be greater than 0 and at most 50 km");
        }

        // Calculate bounding box handling antimeridian wrap
        double[] bbox = GeoUtils.calculateBoundingBox(latitude, longitude, radiusKm);
        double minLat = bbox[0];
        double maxLat = bbox[1];
        double minLon = bbox[2];
        double maxLon = bbox[3];

        List<Incident> candidates = incidentRepository.findCandidatesInBoundingBox(minLat, maxLat, minLon, maxLon);

        return candidates.stream()
                .map(inc -> {
                    double dist = GeoUtils.calculateDistance(latitude, longitude, inc.getLatitude(), inc.getLongitude());
                    return mapToResponse(inc, dist);
                })
                .filter(res -> res.getDistanceKm() <= radiusKm)
                .sorted(Comparator.comparing(IncidentResponse::getDistanceKm))
                .toList();
    }

    @Transactional
    public IncidentResponse updateIncidentStatus(Long id, IncidentStatus newStatus, String currentUserEmail) {
        Incident incident = incidentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Incident report not found with id: " + id));

        User currentUser = getUserByEmail(currentUserEmail);

        // Rule 1: Only ADMIN users can mark incidents as VERIFIED or DISMISSED
        if (newStatus == IncidentStatus.VERIFIED || newStatus == IncidentStatus.DISMISSED) {
            if (currentUser.getRole() != Role.ADMIN) {
                throw new AccessDeniedException("Only ADMIN users can mark incidents as " + newStatus);
            }
        } else {
            // Rule 2: For other status updates (e.g. RESOLVED, REPORTED), must be report owner or ADMIN
            boolean isOwner = incident.getUser().getId().equals(currentUser.getId());
            boolean isAdmin = currentUser.getRole() == Role.ADMIN;
            if (!isOwner && !isAdmin) {
                throw new AccessDeniedException("You do not have permission to modify this incident report");
            }
        }

        incident.setStatus(newStatus);
        Incident updated = incidentRepository.save(incident);
        return mapToResponse(updated, null);
    }

    @Transactional
    public void deleteIncident(Long id, String currentUserEmail) {
        Incident incident = incidentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Incident report not found with id: " + id));

        User currentUser = getUserByEmail(currentUserEmail);

        // Rule: Only report owner or ADMIN can delete
        boolean isOwner = incident.getUser().getId().equals(currentUser.getId());
        boolean isAdmin = currentUser.getRole() == Role.ADMIN;

        if (!isOwner && !isAdmin) {
            throw new AccessDeniedException("You do not have permission to delete this incident report");
        }

        incidentRepository.delete(incident);
    }

    private User getUserByEmail(String email) {
        return userRepository.findByEmail(email.toLowerCase().trim())
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + email));
    }

    private IncidentResponse mapToResponse(Incident incident, Double distanceKm) {
        return IncidentResponse.builder()
                .id(incident.getId())
                .title(incident.getTitle())
                .description(incident.getDescription())
                .incidentType(incident.getIncidentType())
                .latitude(incident.getLatitude())
                .longitude(incident.getLongitude())
                .severity(incident.getSeverity())
                .status(incident.getStatus())
                .createdAt(incident.getCreatedAt())
                .reporterId(incident.getUser() != null ? incident.getUser().getId() : null)
                .reporterName(incident.getUser() != null ? incident.getUser().getFullName() : null)
                .distanceKm(distanceKm)
                .build();
    }
}

