package SafeRoute.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class GeoUtilsTest {

    @Test
    @DisplayName("Should accurately calculate distance between coordinates using Haversine formula")
    void testCalculateDistance() {
        // Distance between New York (40.7128, -74.0060) and Philadelphia (39.9526, -75.1652) is ~129.6 km
        double distance = GeoUtils.calculateDistance(40.7128, -74.0060, 39.9526, -75.1652);
        assertTrue(distance >= 129.0 && distance <= 130.5, "Expected distance ~129.6 km, got: " + distance);
    }

    @Test
    @DisplayName("Should return 0 km for identical coordinates")
    void testCalculateDistance_SamePoint() {
        double distance = GeoUtils.calculateDistance(12.9716, 77.5946, 12.9716, 77.5946);
        assertEquals(0.0, distance);
    }

    @Test
    @DisplayName("Should calculate valid bounding box around coordinate for given radius")
    void testCalculateBoundingBox() {
        double lat = 12.9716;
        double lon = 77.5946;
        double radiusKm = 10.0;

        double[] bbox = GeoUtils.calculateBoundingBox(lat, lon, radiusKm);
        assertEquals(4, bbox.length);

        double minLat = bbox[0];
        double maxLat = bbox[1];
        double minLon = bbox[2];
        double maxLon = bbox[3];

        assertTrue(minLat < lat);
        assertTrue(maxLat > lat);
        assertTrue(minLon < lon);
        assertTrue(maxLon > lon);
    }

    @Test
    @DisplayName("Should correctly normalize longitude across antimeridian boundaries")
    void testNormalizeLongitude() {
        assertEquals(179.0, GeoUtils.normalizeLongitude(179.0));
        assertEquals(-179.0, GeoUtils.normalizeLongitude(-179.0));
        assertEquals(-179.0, GeoUtils.normalizeLongitude(181.0));
        assertEquals(179.0, GeoUtils.normalizeLongitude(-181.0));
    }
}

