package com.somewhere.app.ui.screen;

import android.content.Context;
import android.net.Uri;
import android.widget.Toast;
import androidx.camera.core.*;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.compose.animation.core.*;
import androidx.compose.foundation.layout.*;
import androidx.compose.material3.*;
import androidx.compose.runtime.*;
import androidx.compose.ui.Alignment;
import androidx.compose.ui.Modifier;
import androidx.compose.ui.graphics.SolidColor;
import androidx.core.content.ContextCompat;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.android.gms.tasks.CancellationTokenSource;
import com.somewhere.app.ui.theme.SomewhereColors;
import com.somewhere.app.util.LocationUtils;
import com.somewhere.app.viewmodel.DropViewModel;
import coil.request.ImageRequest;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.*;

@kotlin.Metadata(mv = {1, 9, 0}, k = 2, xi = 48, d1 = {"\u00004\n\u0000\n\u0002\u0010\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\u001a \u0010\u0000\u001a\u00020\u00012\f\u0010\u0002\u001a\b\u0012\u0004\u0012\u00020\u00010\u00032\b\b\u0002\u0010\u0004\u001a\u00020\u0005H\u0007\u001a$\u0010\u0006\u001a\u00020\u00012\u0006\u0010\u0007\u001a\u00020\b2\u0012\u0010\t\u001a\u000e\u0012\u0004\u0012\u00020\u000b\u0012\u0004\u0012\u00020\u00010\nH\u0002\u001a.\u0010\f\u001a\u00020\u00012\u0006\u0010\u0007\u001a\u00020\b2\b\u0010\r\u001a\u0004\u0018\u00010\u000e2\u0012\u0010\u000f\u001a\u000e\u0012\u0004\u0012\u00020\u0010\u0012\u0004\u0012\u00020\u00010\nH\u0002\u00a8\u0006\u0011"}, d2 = {"DropScreen", "", "onComplete", "Lkotlin/Function0;", "viewModel", "Lcom/somewhere/app/viewmodel/DropViewModel;", "getCurrentLocation", "context", "Landroid/content/Context;", "onLocation", "Lkotlin/Function1;", "Lcom/somewhere/app/ui/screen/LocationResult;", "takePhoto", "imageCapture", "Landroidx/camera/core/ImageCapture;", "onCaptured", "Landroid/net/Uri;", "app_debug"})
public final class DropScreenKt {
    
    /**
     * Drop screen — capture a photo, write a message, mark this place.
     *
     * Flow:
     * 1. Fullscreen camera preview
     * 2. Capture button → freezes image
     * 3. Text input appears
     * 4. "Mark this place" → saves drop with current GPS
     */
    @androidx.compose.runtime.Composable()
    public static final void DropScreen(@org.jetbrains.annotations.NotNull()
    kotlin.jvm.functions.Function0<kotlin.Unit> onComplete, @org.jetbrains.annotations.NotNull()
    com.somewhere.app.viewmodel.DropViewModel viewModel) {
    }
    
    /**
     * Captures a photo to internal storage and returns the URI.
     */
    private static final void takePhoto(android.content.Context context, androidx.camera.core.ImageCapture imageCapture, kotlin.jvm.functions.Function1<? super android.net.Uri, kotlin.Unit> onCaptured) {
    }
    
    private static final void getCurrentLocation(android.content.Context context, kotlin.jvm.functions.Function1<? super com.somewhere.app.ui.screen.LocationResult, kotlin.Unit> onLocation) {
    }
}