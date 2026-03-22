package com.somewhere.app.data.repository;

import android.content.Context;
import android.net.Uri;
import com.somewhere.app.data.local.DropDao;
import com.somewhere.app.data.model.Drop;
import com.somewhere.app.util.LocationUtils;
import kotlinx.coroutines.flow.Flow;
import java.io.File;
import java.util.UUID;

/**
 * Single source of truth for Drop operations.
 * Handles ID generation, timestamp, and precise Haversine distance filtering.
 */
@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000R\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\u0010 \n\u0002\u0018\u0002\n\u0002\b\u0003\n\u0002\u0010\u0002\n\u0002\b\u0007\n\u0002\u0010\u000e\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\u0010\u0007\n\u0000\n\u0002\u0010\u0006\n\u0002\b\u0003\n\u0002\u0010\b\n\u0002\b\b\b\u0007\u0018\u00002\u00020\u0001B\u0015\u0012\u0006\u0010\u0002\u001a\u00020\u0003\u0012\u0006\u0010\u0004\u001a\u00020\u0005\u00a2\u0006\u0002\u0010\u0006J\u000e\u0010\r\u001a\u00020\u000eH\u0086@\u00a2\u0006\u0002\u0010\u000fJ\u000e\u0010\u0010\u001a\u00020\u000eH\u0086@\u00a2\u0006\u0002\u0010\u000fJ\u0016\u0010\u0011\u001a\u00020\u000e2\u0006\u0010\u0012\u001a\u00020\nH\u0086@\u00a2\u0006\u0002\u0010\u0013J\u0010\u0010\u0014\u001a\u00020\u000e2\u0006\u0010\u0015\u001a\u00020\u0016H\u0002J\u000e\u0010\u0017\u001a\u00020\u000e2\u0006\u0010\u0015\u001a\u00020\u0016JD\u0010\u0018\u001a\u0014\u0012\u0010\u0012\u000e\u0012\u0004\u0012\u00020\n\u0012\u0004\u0012\u00020\u001a0\u00190\t2\u0006\u0010\u001b\u001a\u00020\u001c2\u0006\u0010\u001d\u001a\u00020\u001c2\b\b\u0002\u0010\u001e\u001a\u00020\u001a2\b\b\u0002\u0010\u001f\u001a\u00020 H\u0086@\u00a2\u0006\u0002\u0010!J.\u0010\"\u001a\u00020\n2\u0006\u0010#\u001a\u00020\u00162\u0006\u0010$\u001a\u00020\u00162\u0006\u0010%\u001a\u00020\u001c2\u0006\u0010&\u001a\u00020\u001cH\u0086@\u00a2\u0006\u0002\u0010\'R\u001d\u0010\u0007\u001a\u000e\u0012\n\u0012\b\u0012\u0004\u0012\u00020\n0\t0\b\u00a2\u0006\b\n\u0000\u001a\u0004\b\u000b\u0010\fR\u000e\u0010\u0002\u001a\u00020\u0003X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0004\u001a\u00020\u0005X\u0082\u0004\u00a2\u0006\u0002\n\u0000\u00a8\u0006("}, d2 = {"Lcom/somewhere/app/data/repository/DropRepository;", "", "context", "Landroid/content/Context;", "dao", "Lcom/somewhere/app/data/local/DropDao;", "(Landroid/content/Context;Lcom/somewhere/app/data/local/DropDao;)V", "allDrops", "Lkotlinx/coroutines/flow/Flow;", "", "Lcom/somewhere/app/data/model/Drop;", "getAllDrops", "()Lkotlinx/coroutines/flow/Flow;", "cleanupOrphanImages", "", "(Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "deleteAllDrops", "deleteDrop", "drop", "(Lcom/somewhere/app/data/model/Drop;Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "deleteImageIfLocal", "path", "", "deleteLocalImage", "getDropsNear", "Lkotlin/Pair;", "", "lat", "", "lon", "radiusMeters", "maxItems", "", "(DDFILkotlin/coroutines/Continuation;)Ljava/lang/Object;", "saveDrop", "text", "imagePath", "latitude", "longitude", "(Ljava/lang/String;Ljava/lang/String;DDLkotlin/coroutines/Continuation;)Ljava/lang/Object;", "app_debug"})
public final class DropRepository {
    @org.jetbrains.annotations.NotNull()
    private final android.content.Context context = null;
    @org.jetbrains.annotations.NotNull()
    private final com.somewhere.app.data.local.DropDao dao = null;
    @org.jetbrains.annotations.NotNull()
    private final kotlinx.coroutines.flow.Flow<java.util.List<com.somewhere.app.data.model.Drop>> allDrops = null;
    
    public DropRepository(@org.jetbrains.annotations.NotNull()
    android.content.Context context, @org.jetbrains.annotations.NotNull()
    com.somewhere.app.data.local.DropDao dao) {
        super();
    }
    
    @org.jetbrains.annotations.NotNull()
    public final kotlinx.coroutines.flow.Flow<java.util.List<com.somewhere.app.data.model.Drop>> getAllDrops() {
        return null;
    }
    
    @org.jetbrains.annotations.Nullable()
    public final java.lang.Object saveDrop(@org.jetbrains.annotations.NotNull()
    java.lang.String text, @org.jetbrains.annotations.NotNull()
    java.lang.String imagePath, double latitude, double longitude, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super com.somewhere.app.data.model.Drop> $completion) {
        return null;
    }
    
    @org.jetbrains.annotations.Nullable()
    public final java.lang.Object deleteDrop(@org.jetbrains.annotations.NotNull()
    com.somewhere.app.data.model.Drop drop, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super kotlin.Unit> $completion) {
        return null;
    }
    
    @org.jetbrains.annotations.Nullable()
    public final java.lang.Object deleteAllDrops(@org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super kotlin.Unit> $completion) {
        return null;
    }
    
    public final void deleteLocalImage(@org.jetbrains.annotations.NotNull()
    java.lang.String path) {
    }
    
    /**
     * Returns drops within [radiusMeters] of the given coordinates,
     * sorted by distance (closest first), limited to [maxItems].
     */
    @org.jetbrains.annotations.Nullable()
    public final java.lang.Object getDropsNear(double lat, double lon, float radiusMeters, int maxItems, @org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super java.util.List<kotlin.Pair<com.somewhere.app.data.model.Drop, java.lang.Float>>> $completion) {
        return null;
    }
    
    @org.jetbrains.annotations.Nullable()
    public final java.lang.Object cleanupOrphanImages(@org.jetbrains.annotations.NotNull()
    kotlin.coroutines.Continuation<? super kotlin.Unit> $completion) {
        return null;
    }
    
    private final void deleteImageIfLocal(java.lang.String path) {
    }
}