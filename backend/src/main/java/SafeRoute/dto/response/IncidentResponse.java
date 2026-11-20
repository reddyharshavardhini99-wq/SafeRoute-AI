package SafeRoute.dto.response;

import SafeRoute.enums.IncidentSeverity;
import SafeRoute.enums.IncidentStatus;
import SafeRoute.enums.IncidentType;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class IncidentResponse {

    private Long id;
    private String title;
    private String description;
    private IncidentType incidentType;
    private Double latitude;
    private Double longitude;
    private IncidentSeverity severity;
    private IncidentStatus status;
    private LocalDateTime createdAt;

    private Long reporterId;
    private String reporterName;

    // Populated for nearby searches
    private Double distanceKm;
}

