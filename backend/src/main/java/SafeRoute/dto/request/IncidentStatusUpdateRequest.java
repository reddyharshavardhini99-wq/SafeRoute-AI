package SafeRoute.dto.request;

import SafeRoute.enums.IncidentStatus;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class IncidentStatusUpdateRequest {

    @NotNull(message = "Incident status is required")
    private IncidentStatus status;
}

