package com.somewhere.app.viewmodel;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Looper;
import androidx.lifecycle.ViewModel;
import com.google.android.gms.location.*;
import com.somewhere.app.data.model.Drop;
import com.somewhere.app.data.repository.DropRepository;
import com.somewhere.app.util.LocationUtils;
import dagger.hilt.android.lifecycle.HiltViewModel;
import kotlinx.coroutines.flow.StateFlow;
import javax.inject.Inject;

/**
 * ViewModel for the Discovery screen.
 * Combines location updates + compass heading to produce spatially positioned overlays.
 */
@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000d\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\u0007\n\u0000\n\u0002\u0010#\n\u0002\u0010\u000e\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0003\n\u0002\u0010\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0010\u0006\n\u0002\b\n\b\u0007\u0018\u00002\u00020\u0001:\u0001)B\u001f\b\u0007\u0012\u0006\u0010\u0002\u001a\u00020\u0003\u0012\u0006\u0010\u0004\u001a\u00020\u0005\u0012\u0006\u0010\u0006\u001a\u00020\u0007\u00a2\u0006\u0002\u0010\bJ\u0006\u0010\u0019\u001a\u00020\u001aJ\u000e\u0010\u001b\u001a\u00020\u001a2\u0006\u0010\u001c\u001a\u00020\u001dJ\u0018\u0010\u001e\u001a\u00020\u001a2\u0006\u0010\u001f\u001a\u00020 2\u0006\u0010!\u001a\u00020 H\u0002J\b\u0010\"\u001a\u00020\u001aH\u0014J\u000e\u0010#\u001a\u00020\u001a2\u0006\u0010\u001c\u001a\u00020\u001dJ\u0010\u0010$\u001a\u00020\u001a2\b\u0010%\u001a\u0004\u0018\u00010\u001dJ\u0006\u0010&\u001a\u00020\u001aJ\u0006\u0010\'\u001a\u00020\u001aJ\b\u0010(\u001a\u00020\u001aH\u0002R\u0014\u0010\t\u001a\b\u0012\u0004\u0012\u00020\u000b0\nX\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\f\u001a\u00020\rX\u0082\u000e\u00a2\u0006\u0002\n\u0000R\u0014\u0010\u000e\u001a\b\u0012\u0004\u0012\u00020\u00100\u000fX\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0006\u001a\u00020\u0007X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0011\u001a\u00020\u0012X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0002\u001a\u00020\u0003X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0013\u001a\u00020\u0014X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0004\u001a\u00020\u0005X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u0017\u0010\u0015\u001a\b\u0012\u0004\u0012\u00020\u000b0\u0016\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0017\u0010\u0018\u00a8\u0006*"}, d2 = {"Lcom/somewhere/app/viewmodel/DiscoveryViewModel;", "Landroidx/lifecycle/ViewModel;", "repository", "Lcom/somewhere/app/data/repository/DropRepository;", "sensorManager", "Landroid/hardware/SensorManager;", "fusedLocationClient", "Lcom/google/android/gms/location/FusedLocationProviderClient;", "(Lcom/somewhere/app/data/repository/DropRepository;Landroid/hardware/SensorManager;Lcom/google/android/gms/location/FusedLocationProviderClient;)V", "_uiState", "Lkotlinx/coroutines/flow/MutableStateFlow;", "Lcom/somewhere/app/viewmodel/DiscoveryViewModel$DiscoveryUiState;", "currentHeading", "", "discoveredIds", "", "", "locationCallback", "Lcom/google/android/gms/location/LocationCallback;", "sensorListener", "Landroid/hardware/SensorEventListener;", "uiState", "Lkotlinx/coroutines/flow/StateFlow;", "getUiState", "()Lkotlinx/coroutines/flow/StateFlow;", "clearMessage", "", "deleteDrop", "item", "Lcom/somewhere/app/viewmodel/DiscoveredDrop;", "fetchNearbyDrops", "lat", "", "lon", "onCleared", "reportDrop", "selectDrop", "drop", "startTracking", "stopTracking", "updateOverlayPositions", "DiscoveryUiState", "app_debug"})
@dagger.hilt.android.lifecycle.HiltViewModel()
public final class DiscoveryViewModel extends androidx.lifecycle.ViewModel {
    @org.jetbrains.annotations.NotNull()
    private final com.somewhere.app.data.repository.DropRepository repository = null;
    @org.jetbrains.annotations.NotNull()
    private final android.hardware.SensorManager sensorManager = null;
    @org.jetbrains.annotations.NotNull()
    private final com.google.android.gms.location.FusedLocationProviderClient fusedLocationClient = null;
    @org.jetbrains.annotations.NotNull()
    private final kotlinx.coroutines.flow.MutableStateFlow<com.somewhere.app.viewmodel.DiscoveryViewModel.DiscoveryUiState> _uiState = null;
    @org.jetbrains.annotations.NotNull()
    private final kotlinx.coroutines.flow.StateFlow<com.somewhere.app.viewmodel.DiscoveryViewModel.DiscoveryUiState> uiState = null;
    @org.jetbrains.annotations.NotNull()
    private final java.util.Set<java.lang.String> discoveredIds = null;
    private float currentHeading = 0.0F;
    @org.jetbrains.annotations.NotNull()
    private final android.hardware.SensorEventListener sensorListener = null;
    @org.jetbrains.annotations.NotNull()
    private final com.google.android.gms.location.LocationCallback locationCallback = null;
    
    @javax.inject.Inject()
    public DiscoveryViewModel(@org.jetbrains.annotations.NotNull()
    com.somewhere.app.data.repository.DropRepository repository, @org.jetbrains.annotations.NotNull()
    android.hardware.SensorManager sensorManager, @org.jetbrains.annotations.NotNull()
    com.google.android.gms.location.FusedLocationProviderClient fusedLocationClient) {
        super();
    }
    
    @org.jetbrains.annotations.NotNull()
    public final kotlinx.coroutines.flow.StateFlow<com.somewhere.app.viewmodel.DiscoveryViewModel.DiscoveryUiState> getUiState() {
        return null;
    }
    
    public final void startTracking() {
    }
    
    public final void stopTracking() {
    }
    
    public final void selectDrop(@org.jetbrains.annotations.Nullable()
    com.somewhere.app.viewmodel.DiscoveredDrop drop) {
    }
    
    public final void clearMessage() {
    }
    
    public final void deleteDrop(@org.jetbrains.annotations.NotNull()
    com.somewhere.app.viewmodel.DiscoveredDrop item) {
    }
    
    public final void reportDrop(@org.jetbrains.annotations.NotNull()
    com.somewhere.app.viewmodel.DiscoveredDrop item) {
    }
    
    private final void fetchNearbyDrops(double lat, double lon) {
    }
    
    private final void updateOverlayPositions() {
    }
    
    @java.lang.Override()
    protected void onCleared() {
    }
    
    @kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000:\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0000\n\u0002\u0010 \n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\u0007\n\u0000\n\u0002\u0010\u0006\n\u0002\b\u0002\n\u0002\u0010\u000b\n\u0002\b\u0003\n\u0002\u0010\u000e\n\u0002\b\u001e\n\u0002\u0010\b\n\u0002\b\u0002\b\u0087\b\u0018\u00002\u00020\u0001Ba\u0012\u000e\b\u0002\u0010\u0002\u001a\b\u0012\u0004\u0012\u00020\u00040\u0003\u0012\b\b\u0002\u0010\u0005\u001a\u00020\u0006\u0012\b\b\u0002\u0010\u0007\u001a\u00020\b\u0012\b\b\u0002\u0010\t\u001a\u00020\b\u0012\b\b\u0002\u0010\n\u001a\u00020\u000b\u0012\n\b\u0002\u0010\f\u001a\u0004\u0018\u00010\u0006\u0012\n\b\u0002\u0010\r\u001a\u0004\u0018\u00010\u0004\u0012\n\b\u0002\u0010\u000e\u001a\u0004\u0018\u00010\u000f\u00a2\u0006\u0002\u0010\u0010J\u000f\u0010!\u001a\b\u0012\u0004\u0012\u00020\u00040\u0003H\u00c6\u0003J\t\u0010\"\u001a\u00020\u0006H\u00c6\u0003J\t\u0010#\u001a\u00020\bH\u00c6\u0003J\t\u0010$\u001a\u00020\bH\u00c6\u0003J\t\u0010%\u001a\u00020\u000bH\u00c6\u0003J\u0010\u0010&\u001a\u0004\u0018\u00010\u0006H\u00c6\u0003\u00a2\u0006\u0002\u0010\u0016J\u000b\u0010\'\u001a\u0004\u0018\u00010\u0004H\u00c6\u0003J\u000b\u0010(\u001a\u0004\u0018\u00010\u000fH\u00c6\u0003Jj\u0010)\u001a\u00020\u00002\u000e\b\u0002\u0010\u0002\u001a\b\u0012\u0004\u0012\u00020\u00040\u00032\b\b\u0002\u0010\u0005\u001a\u00020\u00062\b\b\u0002\u0010\u0007\u001a\u00020\b2\b\b\u0002\u0010\t\u001a\u00020\b2\b\b\u0002\u0010\n\u001a\u00020\u000b2\n\b\u0002\u0010\f\u001a\u0004\u0018\u00010\u00062\n\b\u0002\u0010\r\u001a\u0004\u0018\u00010\u00042\n\b\u0002\u0010\u000e\u001a\u0004\u0018\u00010\u000fH\u00c6\u0001\u00a2\u0006\u0002\u0010*J\u0013\u0010+\u001a\u00020\u000b2\b\u0010,\u001a\u0004\u0018\u00010\u0001H\u00d6\u0003J\t\u0010-\u001a\u00020.H\u00d6\u0001J\t\u0010/\u001a\u00020\u000fH\u00d6\u0001R\u0011\u0010\n\u001a\u00020\u000b\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0011\u0010\u0012R\u0011\u0010\u0005\u001a\u00020\u0006\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0013\u0010\u0014R\u0015\u0010\f\u001a\u0004\u0018\u00010\u0006\u00a2\u0006\n\n\u0002\u0010\u0017\u001a\u0004\b\u0015\u0010\u0016R\u0013\u0010\u000e\u001a\u0004\u0018\u00010\u000f\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0018\u0010\u0019R\u0017\u0010\u0002\u001a\b\u0012\u0004\u0012\u00020\u00040\u0003\u00a2\u0006\b\n\u0000\u001a\u0004\b\u001a\u0010\u001bR\u0013\u0010\r\u001a\u0004\u0018\u00010\u0004\u00a2\u0006\b\n\u0000\u001a\u0004\b\u001c\u0010\u001dR\u0011\u0010\u0007\u001a\u00020\b\u00a2\u0006\b\n\u0000\u001a\u0004\b\u001e\u0010\u001fR\u0011\u0010\t\u001a\u00020\b\u00a2\u0006\b\n\u0000\u001a\u0004\b \u0010\u001f\u00a8\u00060"}, d2 = {"Lcom/somewhere/app/viewmodel/DiscoveryViewModel$DiscoveryUiState;", "", "nearbyDrops", "", "Lcom/somewhere/app/viewmodel/DiscoveredDrop;", "heading", "", "userLat", "", "userLon", "hasLocation", "", "locationAccuracyMeters", "selectedDrop", "message", "", "(Ljava/util/List;FDDZLjava/lang/Float;Lcom/somewhere/app/viewmodel/DiscoveredDrop;Ljava/lang/String;)V", "getHasLocation", "()Z", "getHeading", "()F", "getLocationAccuracyMeters", "()Ljava/lang/Float;", "Ljava/lang/Float;", "getMessage", "()Ljava/lang/String;", "getNearbyDrops", "()Ljava/util/List;", "getSelectedDrop", "()Lcom/somewhere/app/viewmodel/DiscoveredDrop;", "getUserLat", "()D", "getUserLon", "component1", "component2", "component3", "component4", "component5", "component6", "component7", "component8", "copy", "(Ljava/util/List;FDDZLjava/lang/Float;Lcom/somewhere/app/viewmodel/DiscoveredDrop;Ljava/lang/String;)Lcom/somewhere/app/viewmodel/DiscoveryViewModel$DiscoveryUiState;", "equals", "other", "hashCode", "", "toString", "app_debug"})
    public static final class DiscoveryUiState {
        @org.jetbrains.annotations.NotNull()
        private final java.util.List<com.somewhere.app.viewmodel.DiscoveredDrop> nearbyDrops = null;
        private final float heading = 0.0F;
        private final double userLat = 0.0;
        private final double userLon = 0.0;
        private final boolean hasLocation = false;
        @org.jetbrains.annotations.Nullable()
        private final java.lang.Float locationAccuracyMeters = null;
        @org.jetbrains.annotations.Nullable()
        private final com.somewhere.app.viewmodel.DiscoveredDrop selectedDrop = null;
        @org.jetbrains.annotations.Nullable()
        private final java.lang.String message = null;
        
        @org.jetbrains.annotations.NotNull()
        public final java.util.List<com.somewhere.app.viewmodel.DiscoveredDrop> component1() {
            return null;
        }
        
        public final float component2() {
            return 0.0F;
        }
        
        public final double component3() {
            return 0.0;
        }
        
        public final double component4() {
            return 0.0;
        }
        
        public final boolean component5() {
            return false;
        }
        
        @org.jetbrains.annotations.Nullable()
        public final java.lang.Float component6() {
            return null;
        }
        
        @org.jetbrains.annotations.Nullable()
        public final com.somewhere.app.viewmodel.DiscoveredDrop component7() {
            return null;
        }
        
        @org.jetbrains.annotations.Nullable()
        public final java.lang.String component8() {
            return null;
        }
        
        @org.jetbrains.annotations.NotNull()
        public final com.somewhere.app.viewmodel.DiscoveryViewModel.DiscoveryUiState copy(@org.jetbrains.annotations.NotNull()
        java.util.List<com.somewhere.app.viewmodel.DiscoveredDrop> nearbyDrops, float heading, double userLat, double userLon, boolean hasLocation, @org.jetbrains.annotations.Nullable()
        java.lang.Float locationAccuracyMeters, @org.jetbrains.annotations.Nullable()
        com.somewhere.app.viewmodel.DiscoveredDrop selectedDrop, @org.jetbrains.annotations.Nullable()
        java.lang.String message) {
            return null;
        }
        
        @java.lang.Override()
        public boolean equals(@org.jetbrains.annotations.Nullable()
        java.lang.Object other) {
            return false;
        }
        
        @java.lang.Override()
        public int hashCode() {
            return 0;
        }
        
        @java.lang.Override()
        @org.jetbrains.annotations.NotNull()
        public java.lang.String toString() {
            return null;
        }
        
        public DiscoveryUiState(@org.jetbrains.annotations.NotNull()
        java.util.List<com.somewhere.app.viewmodel.DiscoveredDrop> nearbyDrops, float heading, double userLat, double userLon, boolean hasLocation, @org.jetbrains.annotations.Nullable()
        java.lang.Float locationAccuracyMeters, @org.jetbrains.annotations.Nullable()
        com.somewhere.app.viewmodel.DiscoveredDrop selectedDrop, @org.jetbrains.annotations.Nullable()
        java.lang.String message) {
            super();
        }
        
        @org.jetbrains.annotations.NotNull()
        public final java.util.List<com.somewhere.app.viewmodel.DiscoveredDrop> getNearbyDrops() {
            return null;
        }
        
        public final float getHeading() {
            return 0.0F;
        }
        
        public final double getUserLat() {
            return 0.0;
        }
        
        public final double getUserLon() {
            return 0.0;
        }
        
        public final boolean getHasLocation() {
            return false;
        }
        
        @org.jetbrains.annotations.Nullable()
        public final java.lang.Float getLocationAccuracyMeters() {
            return null;
        }
        
        @org.jetbrains.annotations.Nullable()
        public final com.somewhere.app.viewmodel.DiscoveredDrop getSelectedDrop() {
            return null;
        }
        
        @org.jetbrains.annotations.Nullable()
        public final java.lang.String getMessage() {
            return null;
        }
        
        public DiscoveryUiState() {
            super();
        }
    }
}