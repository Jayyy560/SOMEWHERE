package com.somewhere.app.ui.component;

import android.content.Context;
import androidx.compose.foundation.layout.*;
import androidx.compose.runtime.*;
import androidx.compose.ui.Alignment;
import androidx.compose.ui.Modifier;
import com.google.accompanist.permissions.ExperimentalPermissionsApi;
import com.google.accompanist.permissions.PermissionStatus;
import com.somewhere.app.ui.theme.SomewhereColors;
import com.somewhere.app.util.SettingsUtils;

@kotlin.Metadata(mv = {1, 9, 0}, k = 2, xi = 48, d1 = {"\u00002\n\u0000\n\u0002\u0010\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\u000e\n\u0002\b\u0002\n\u0002\u0010\u000b\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0010 \n\u0000\n\u0002\u0018\u0002\n\u0000\u001a>\u0010\u0000\u001a\u00020\u00012\u0006\u0010\u0002\u001a\u00020\u00032\u0006\u0010\u0004\u001a\u00020\u00052\u0006\u0010\u0006\u001a\u00020\u00052\u0006\u0010\u0007\u001a\u00020\b2\u0006\u0010\t\u001a\u00020\b2\f\u0010\n\u001a\b\u0012\u0004\u0012\u00020\u00010\u000bH\u0003\u001a9\u0010\f\u001a\u00020\u00012\u0006\u0010\u0004\u001a\u00020\u00052\u0006\u0010\u0006\u001a\u00020\u00052\f\u0010\r\u001a\b\u0012\u0004\u0012\u00020\u00050\u000e2\u0011\u0010\u000f\u001a\r\u0012\u0004\u0012\u00020\u00010\u000b\u00a2\u0006\u0002\b\u0010H\u0007\u00a8\u0006\u0011"}, d2 = {"PermissionFallback", "", "context", "Landroid/content/Context;", "title", "", "description", "showRationale", "", "permanentlyDenied", "onRequest", "Lkotlin/Function0;", "PermissionGate", "permissions", "", "onGranted", "Landroidx/compose/runtime/Composable;", "app_debug"})
public final class PermissionGateKt {
    
    @kotlin.OptIn(markerClass = {com.google.accompanist.permissions.ExperimentalPermissionsApi.class})
    @androidx.compose.runtime.Composable()
    public static final void PermissionGate(@org.jetbrains.annotations.NotNull()
    java.lang.String title, @org.jetbrains.annotations.NotNull()
    java.lang.String description, @org.jetbrains.annotations.NotNull()
    java.util.List<java.lang.String> permissions, @org.jetbrains.annotations.NotNull()
    kotlin.jvm.functions.Function0<kotlin.Unit> onGranted) {
    }
    
    @androidx.compose.runtime.Composable()
    private static final void PermissionFallback(android.content.Context context, java.lang.String title, java.lang.String description, boolean showRationale, boolean permanentlyDenied, kotlin.jvm.functions.Function0<kotlin.Unit> onRequest) {
    }
}