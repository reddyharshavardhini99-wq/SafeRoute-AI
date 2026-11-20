package SafeRoute.repository;

import SafeRoute.entity.Incident;
import SafeRoute.enums.IncidentStatus;
import SafeRoute.enums.IncidentType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface IncidentRepository extends JpaRepository<Incident, Long> {

    List<Incident> findByStatus(IncidentStatus status);

    List<Incident> findByIncidentType(IncidentType incidentType);

    List<Incident> findByUserId(Long userId);

    Page<Incident> findByStatus(IncidentStatus status, Pageable pageable);

    Page<Incident> findByIncidentType(IncidentType incidentType, Pageable pageable);

    Page<Incident> findByStatusAndIncidentType(IncidentStatus status, IncidentType incidentType, Pageable pageable);

    /**
     * Finds incidents within a coordinate bounding box.
     * Supports antimeridian wrap-around when minLon > maxLon.
     */
    @Query("SELECT i FROM Incident i WHERE i.latitude BETWEEN :minLat AND :maxLat AND " +
            "((:minLon <= :maxLon AND i.longitude BETWEEN :minLon AND :maxLon) OR " +
            "(:minLon > :maxLon AND (i.longitude >= :minLon OR i.longitude <= :maxLon)))")
    List<Incident> findCandidatesInBoundingBox(
            @Param("minLat") Double minLat,
            @Param("maxLat") Double maxLat,
            @Param("minLon") Double minLon,
            @Param("maxLon") Double maxLon
    );
}
