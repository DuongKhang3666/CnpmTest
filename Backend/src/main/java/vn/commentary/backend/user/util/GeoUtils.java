package vn.commentary.backend.user.util;

/** Tiện ích địa lý cho thuyết minh tự động theo GPS. */
public final class GeoUtils {
    private static final double EARTH_RADIUS_METERS = 6_371_000d;

    private GeoUtils() {
    }

    /** Khoảng cách đường tròn lớn (haversine) giữa hai toạ độ, đơn vị mét. */
    public static double distanceMeters(double lat1, double lon1, double lat2, double lon2) {
        double phi1 = Math.toRadians(lat1);
        double phi2 = Math.toRadians(lat2);
        double dPhi = Math.toRadians(lat2 - lat1);
        double dLambda = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dPhi / 2) * Math.sin(dPhi / 2)
                + Math.cos(phi1) * Math.cos(phi2) * Math.sin(dLambda / 2) * Math.sin(dLambda / 2);
        return 2 * EARTH_RADIUS_METERS * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    }

    public static boolean isValidLatLng(double lat, double lng) {
        return !Double.isNaN(lat) && !Double.isNaN(lng)
                && lat >= -90 && lat <= 90 && lng >= -180 && lng <= 180;
    }
}
