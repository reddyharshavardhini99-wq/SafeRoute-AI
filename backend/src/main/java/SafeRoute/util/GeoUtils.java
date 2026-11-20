package SafeRoute.util;

public final class GeoUtils {

    public static final double EARTH_RADIUS_KM = 6371.0;

    private GeoUtils() {
    }

    /**
     * Calculates the great-circle distance between two GPS coordinates using the Haversine formula.
     *
     * @param lat1 Latitude of point 1 in degrees
     * @param lon1 Longitude of point 1 in degrees
     * @param lat2 Latitude of point 2 in degrees
     * @param lon2 Longitude of point 2 in degrees
     * @return Distance in kilometers, rounded to 2 decimal places
     */
    public static double calculateDistance(double lat1, double lon1, double lat2, double lon2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);

        double rLat1 = Math.toRadians(lat1);
        double rLat2 = Math.toRadians(lat2);

        double a = Math.sin(dLat / 2.0) * Math.sin(dLat / 2.0)
                + Math.cos(rLat1) * Math.cos(rLat2)
                * Math.sin(dLon / 2.0) * Math.sin(dLon / 2.0);

        double c = 2.0 * Math.atan2(Math.sqrt(a), Math.sqrt(1.0 - a));
        double distance = EARTH_RADIUS_KM * c;

        return Math.round(distance * 100.0) / 100.0;
    }

    /**
     * Calculates a coordinate bounding box [minLat, maxLat, minLon, maxLon] around a center point for a given radius.
     * Handles antimeridian wrap-around where minLon > maxLon.
     */
    public static double[] calculateBoundingBox(double latitude, double longitude, double radiusKm) {
        double latDelta = Math.toDegrees(radiusKm / EARTH_RADIUS_KM);
        double minLat = Math.max(-90.0, latitude - latDelta);
        double maxLat = Math.min(90.0, latitude + latDelta);

        // Account for narrowing longitudes near poles
        double cosLat = Math.cos(Math.toRadians(latitude));
        double lonDelta;
        if (cosLat > 1e-6) {
            lonDelta = Math.toDegrees(radiusKm / (EARTH_RADIUS_KM * cosLat));
        } else {
            lonDelta = 180.0;
        }

        double minLon = normalizeLongitude(longitude - lonDelta);
        double maxLon = normalizeLongitude(longitude + lonDelta);

        return new double[]{minLat, maxLat, minLon, maxLon};
    }

    /**
     * Normalizes a longitude value to the range [-180, 180].
     */
    public static double normalizeLongitude(double lon) {
        while (lon > 180.0) {
            lon -= 360.0;
        }
        while (lon < -180.0) {
            lon += 360.0;
        }
        return lon;
    }
}

