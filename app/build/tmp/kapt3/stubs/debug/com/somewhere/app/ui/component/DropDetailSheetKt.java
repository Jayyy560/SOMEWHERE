package com.somewhere.app.ui.component;

import android.net.Uri;
import androidx.compose.animation.core.*;
import androidx.compose.foundation.gestures.Orientation;
import androidx.compose.foundation.layout.*;
import androidx.compose.material.icons.Icons;
import androidx.compose.runtime.*;
import androidx.compose.ui.Alignment;
import androidx.compose.ui.Modifier;
import androidx.compose.ui.layout.ContentScale;
import coil.request.ImageRequest;
import com.somewhere.app.ui.theme.SomewhereColors;
import com.somewhere.app.util.LocationUtils;
import com.somewhere.app.viewmodel.DiscoveredDrop;
import java.text.SimpleDateFormat;
import java.util.*;

@kotlin.Metadata(mv = {1, 9, 0}, k = 2, xi = 48, d1 = {"\u0000\"\n\u0000\n\u0002\u0010\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0003\n\u0002\u0010\u000e\n\u0000\n\u0002\u0010\t\n\u0000\u001a:\u0010\u0000\u001a\u00020\u00012\u0006\u0010\u0002\u001a\u00020\u00032\f\u0010\u0004\u001a\b\u0012\u0004\u0012\u00020\u00010\u00052\f\u0010\u0006\u001a\b\u0012\u0004\u0012\u00020\u00010\u00052\f\u0010\u0007\u001a\b\u0012\u0004\u0012\u00020\u00010\u0005H\u0007\u001a\u0010\u0010\b\u001a\u00020\t2\u0006\u0010\n\u001a\u00020\u000bH\u0002\u00a8\u0006\f"}, d2 = {"DropDetailSheet", "", "item", "Lcom/somewhere/app/viewmodel/DiscoveredDrop;", "onDismiss", "Lkotlin/Function0;", "onDelete", "onReport", "formatTimestamp", "", "timestamp", "", "app_debug"})
public final class DropDetailSheetKt {
    
    /**
     * Expanded detail view for a discovered drop.
     * Animates in from overlay → center with dimmed backdrop.
     * Shows photo, message text, distance, and timestamp.
     */
    @androidx.compose.runtime.Composable()
    public static final void DropDetailSheet(@org.jetbrains.annotations.NotNull()
    com.somewhere.app.viewmodel.DiscoveredDrop item, @org.jetbrains.annotations.NotNull()
    kotlin.jvm.functions.Function0<kotlin.Unit> onDismiss, @org.jetbrains.annotations.NotNull()
    kotlin.jvm.functions.Function0<kotlin.Unit> onDelete, @org.jetbrains.annotations.NotNull()
    kotlin.jvm.functions.Function0<kotlin.Unit> onReport) {
    }
    
    private static final java.lang.String formatTimestamp(long timestamp) {
        return null;
    }
}