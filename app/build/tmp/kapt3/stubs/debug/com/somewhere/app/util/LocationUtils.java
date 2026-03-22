package com.somewhere.app.util;

import kotlin.math.*;

/**
 * Distance and bearing calculations for GPS coordinates.
 */
@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000,\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0002\b\u0002\n\u0002\u0010\u0007\n\u0002\b\u0002\n\u0002\u0010\b\n\u0002\b\u0005\n\u0002\u0010\u0006\n\u0002\b\u0004\n\u0002\u0010\u000e\n\u0002\b\u0003\b\u00c7\u0002\u0018\u00002\u00020\u0001B\u0007\b\u0002\u00a2\u0006\u0002\u0010\u0002J\u0016\u0010\t\u001a\u00020\u00042\u0006\u0010\n\u001a\u00020\u00042\u0006\u0010\u000b\u001a\u00020\u0004J&\u0010\u000b\u001a\u00020\u00042\u0006\u0010\f\u001a\u00020\r2\u0006\u0010\u000e\u001a\u00020\r2\u0006\u0010\u000f\u001a\u00020\r2\u0006\u0010\u0010\u001a\u00020\rJ\u000e\u0010\u0011\u001a\u00020\u00122\u0006\u0010\u0013\u001a\u00020\u0004J&\u0010\u0014\u001a\u00020\u00042\u0006\u0010\f\u001a\u00020\r2\u0006\u0010\u000e\u001a\u00020\r2\u0006\u0010\u000f\u001a\u00020\r2\u0006\u0010\u0010\u001a\u00020\rR\u000e\u0010\u0003\u001a\u00020\u0004X\u0086T\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0005\u001a\u00020\u0004X\u0086T\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0006\u001a\u00020\u0007X\u0086T\u00a2\u0006\u0002\n\u0000R\u000e\u0010\b\u001a\u00020\u0004X\u0086T\u00a2\u0006\u0002\n\u0000\u00a8\u0006\u0015"}, d2 = {"Lcom/somewhere/app/util/LocationUtils;", "", "()V", "ACCURACY_WARNING_METERS", "", "DISCOVERY_RADIUS", "MAX_VISIBLE", "", "UNLOCK_RADIUS", "angleDifference", "heading", "bearing", "lat1", "", "lon1", "lat2", "lon2", "formatDistance", "", "meters", "haversineDistance", "app_debug"})
public final class LocationUtils {
    public static final float DISCOVERY_RADIUS = 15.0F;
    public static final float UNLOCK_RADIUS = 8.0F;
    public static final int MAX_VISIBLE = 3;
    public static final float ACCURACY_WARNING_METERS = 35.0F;
    @org.jetbrains.annotations.NotNull()
    public static final com.somewhere.app.util.LocationUtils INSTANCE = null;
    
    private LocationUtils() {
        super();
    }
    
    /**
     * Haversine distance between two GPS points in meters.
     */
    public final float haversineDistance(double lat1, double lon1, double lat2, double lon2) {
        return 0.0F;
    }
    
    /**
     * Bearing from point 1 to point 2 in degrees (0 = north, clockwise).
     * Used to position overlay cards relative to device heading.
     */
    public final float bearing(double lat1, double lon1, double lat2, double lon2) {
        return 0.0F;
    }
    
    /**
     * Angular difference between device heading and drop bearing,
     * normalized to [-180, 180]. Used for horizontal overlay positioning.
     */
    public final float angleDifference(float heading, float bearing) {
        return 0.0F;
    }
    
    /**
     * Format distance for display: "4m away", "12m away"
     */
    @org.jetbrains.annotations.NotNull()
    public final java.lang.String formatDistance(float meters) {
        return null;
    }
}