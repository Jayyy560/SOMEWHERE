package com.somewhere.app.di;

import android.content.Context;
import android.hardware.SensorManager;
import androidx.room.Room;
import com.somewhere.app.data.local.AppDatabase;
import com.somewhere.app.data.local.DropDao;
import com.somewhere.app.data.repository.DropRepository;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import dagger.Module;
import dagger.Provides;
import dagger.hilt.InstallIn;
import dagger.hilt.android.qualifiers.ApplicationContext;
import dagger.hilt.components.SingletonComponent;
import javax.inject.Singleton;

@dagger.Module()
@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u00004\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\b\u00c7\u0002\u0018\u00002\u00020\u0001B\u0007\b\u0002\u00a2\u0006\u0002\u0010\u0002J\u0012\u0010\u0003\u001a\u00020\u00042\b\b\u0001\u0010\u0005\u001a\u00020\u0006H\u0007J\u0010\u0010\u0007\u001a\u00020\b2\u0006\u0010\t\u001a\u00020\u0004H\u0007J\u001a\u0010\n\u001a\u00020\u000b2\b\b\u0001\u0010\u0005\u001a\u00020\u00062\u0006\u0010\f\u001a\u00020\bH\u0007J\u0012\u0010\r\u001a\u00020\u000e2\b\b\u0001\u0010\u0005\u001a\u00020\u0006H\u0007J\u0012\u0010\u000f\u001a\u00020\u00102\b\b\u0001\u0010\u0005\u001a\u00020\u0006H\u0007\u00a8\u0006\u0011"}, d2 = {"Lcom/somewhere/app/di/AppModule;", "", "()V", "provideDatabase", "Lcom/somewhere/app/data/local/AppDatabase;", "context", "Landroid/content/Context;", "provideDropDao", "Lcom/somewhere/app/data/local/DropDao;", "database", "provideDropRepository", "Lcom/somewhere/app/data/repository/DropRepository;", "dao", "provideFusedLocationClient", "Lcom/google/android/gms/location/FusedLocationProviderClient;", "provideSensorManager", "Landroid/hardware/SensorManager;", "app_debug"})
@dagger.hilt.InstallIn(value = {dagger.hilt.components.SingletonComponent.class})
public final class AppModule {
    @org.jetbrains.annotations.NotNull()
    public static final com.somewhere.app.di.AppModule INSTANCE = null;
    
    private AppModule() {
        super();
    }
    
    @dagger.Provides()
    @javax.inject.Singleton()
    @org.jetbrains.annotations.NotNull()
    public final com.somewhere.app.data.local.AppDatabase provideDatabase(@dagger.hilt.android.qualifiers.ApplicationContext()
    @org.jetbrains.annotations.NotNull()
    android.content.Context context) {
        return null;
    }
    
    @dagger.Provides()
    @org.jetbrains.annotations.NotNull()
    public final com.somewhere.app.data.local.DropDao provideDropDao(@org.jetbrains.annotations.NotNull()
    com.somewhere.app.data.local.AppDatabase database) {
        return null;
    }
    
    @dagger.Provides()
    @javax.inject.Singleton()
    @org.jetbrains.annotations.NotNull()
    public final com.somewhere.app.data.repository.DropRepository provideDropRepository(@dagger.hilt.android.qualifiers.ApplicationContext()
    @org.jetbrains.annotations.NotNull()
    android.content.Context context, @org.jetbrains.annotations.NotNull()
    com.somewhere.app.data.local.DropDao dao) {
        return null;
    }
    
    @dagger.Provides()
    @org.jetbrains.annotations.NotNull()
    public final android.hardware.SensorManager provideSensorManager(@dagger.hilt.android.qualifiers.ApplicationContext()
    @org.jetbrains.annotations.NotNull()
    android.content.Context context) {
        return null;
    }
    
    @dagger.Provides()
    @org.jetbrains.annotations.NotNull()
    public final com.google.android.gms.location.FusedLocationProviderClient provideFusedLocationClient(@dagger.hilt.android.qualifiers.ApplicationContext()
    @org.jetbrains.annotations.NotNull()
    android.content.Context context) {
        return null;
    }
}