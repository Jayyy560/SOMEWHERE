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
 * Represents a discovered drop with its spatial metadata.
 */
@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000*\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\u0007\n\u0000\n\u0002\u0010\u000b\n\u0002\b\u0014\n\u0002\u0010\b\n\u0000\n\u0002\u0010\u000e\n\u0000\b\u0087\b\u0018\u00002\u00020\u0001B9\u0012\u0006\u0010\u0002\u001a\u00020\u0003\u0012\u0006\u0010\u0004\u001a\u00020\u0005\u0012\u0006\u0010\u0006\u001a\u00020\u0007\u0012\u0006\u0010\b\u001a\u00020\u0005\u0012\b\b\u0002\u0010\t\u001a\u00020\u0007\u0012\b\b\u0002\u0010\n\u001a\u00020\u0007\u00a2\u0006\u0002\u0010\u000bJ\t\u0010\u0012\u001a\u00020\u0003H\u00c6\u0003J\t\u0010\u0013\u001a\u00020\u0005H\u00c6\u0003J\t\u0010\u0014\u001a\u00020\u0007H\u00c6\u0003J\t\u0010\u0015\u001a\u00020\u0005H\u00c6\u0003J\t\u0010\u0016\u001a\u00020\u0007H\u00c6\u0003J\t\u0010\u0017\u001a\u00020\u0007H\u00c6\u0003JE\u0010\u0018\u001a\u00020\u00002\b\b\u0002\u0010\u0002\u001a\u00020\u00032\b\b\u0002\u0010\u0004\u001a\u00020\u00052\b\b\u0002\u0010\u0006\u001a\u00020\u00072\b\b\u0002\u0010\b\u001a\u00020\u00052\b\b\u0002\u0010\t\u001a\u00020\u00072\b\b\u0002\u0010\n\u001a\u00020\u0007H\u00c6\u0001J\u0013\u0010\u0019\u001a\u00020\u00072\b\u0010\u001a\u001a\u0004\u0018\u00010\u0001H\u00d6\u0003J\t\u0010\u001b\u001a\u00020\u001cH\u00d6\u0001J\t\u0010\u001d\u001a\u00020\u001eH\u00d6\u0001R\u0011\u0010\b\u001a\u00020\u0005\u00a2\u0006\b\n\u0000\u001a\u0004\b\f\u0010\rR\u0011\u0010\u0004\u001a\u00020\u0005\u00a2\u0006\b\n\u0000\u001a\u0004\b\u000e\u0010\rR\u0011\u0010\u0002\u001a\u00020\u0003\u00a2\u0006\b\n\u0000\u001a\u0004\b\u000f\u0010\u0010R\u0011\u0010\n\u001a\u00020\u0007\u00a2\u0006\b\n\u0000\u001a\u0004\b\n\u0010\u0011R\u0011\u0010\t\u001a\u00020\u0007\u00a2\u0006\b\n\u0000\u001a\u0004\b\t\u0010\u0011R\u0011\u0010\u0006\u001a\u00020\u0007\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0006\u0010\u0011\u00a8\u0006\u001f"}, d2 = {"Lcom/somewhere/app/viewmodel/DiscoveredDrop;", "", "drop", "Lcom/somewhere/app/data/model/Drop;", "distanceMeters", "", "isUnlocked", "", "angleDegrees", "isPrimary", "isNewlyDiscovered", "(Lcom/somewhere/app/data/model/Drop;FZFZZ)V", "getAngleDegrees", "()F", "getDistanceMeters", "getDrop", "()Lcom/somewhere/app/data/model/Drop;", "()Z", "component1", "component2", "component3", "component4", "component5", "component6", "copy", "equals", "other", "hashCode", "", "toString", "", "app_debug"})
public final class DiscoveredDrop {
    @org.jetbrains.annotations.NotNull()
    private final com.somewhere.app.data.model.Drop drop = null;
    private final float distanceMeters = 0.0F;
    private final boolean isUnlocked = false;
    private final float angleDegrees = 0.0F;
    private final boolean isPrimary = false;
    private final boolean isNewlyDiscovered = false;
    
    @org.jetbrains.annotations.NotNull()
    public final com.somewhere.app.data.model.Drop component1() {
        return null;
    }
    
    public final float component2() {
        return 0.0F;
    }
    
    public final boolean component3() {
        return false;
    }
    
    public final float component4() {
        return 0.0F;
    }
    
    public final boolean component5() {
        return false;
    }
    
    public final boolean component6() {
        return false;
    }
    
    @org.jetbrains.annotations.NotNull()
    public final com.somewhere.app.viewmodel.DiscoveredDrop copy(@org.jetbrains.annotations.NotNull()
    com.somewhere.app.data.model.Drop drop, float distanceMeters, boolean isUnlocked, float angleDegrees, boolean isPrimary, boolean isNewlyDiscovered) {
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
    
    public DiscoveredDrop(@org.jetbrains.annotations.NotNull()
    com.somewhere.app.data.model.Drop drop, float distanceMeters, boolean isUnlocked, float angleDegrees, boolean isPrimary, boolean isNewlyDiscovered) {
        super();
    }
    
    @org.jetbrains.annotations.NotNull()
    public final com.somewhere.app.data.model.Drop getDrop() {
        return null;
    }
    
    public final float getDistanceMeters() {
        return 0.0F;
    }
    
    public final boolean isUnlocked() {
        return false;
    }
    
    public final float getAngleDegrees() {
        return 0.0F;
    }
    
    public final boolean isPrimary() {
        return false;
    }
    
    public final boolean isNewlyDiscovered() {
        return false;
    }
}