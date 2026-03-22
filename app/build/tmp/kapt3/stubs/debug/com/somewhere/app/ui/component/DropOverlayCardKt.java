package com.somewhere.app.ui.component;

import androidx.compose.animation.core.*;
import androidx.compose.foundation.layout.*;
import androidx.compose.runtime.*;
import androidx.compose.ui.Alignment;
import androidx.compose.ui.Modifier;
import com.somewhere.app.ui.theme.SomewhereColors;
import com.somewhere.app.util.LocationUtils;
import com.somewhere.app.viewmodel.DiscoveredDrop;

@kotlin.Metadata(mv = {1, 9, 0}, k = 2, xi = 48, d1 = {"\u0000\u001a\n\u0000\n\u0002\u0010\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\u001a(\u0010\u0000\u001a\u00020\u00012\u0006\u0010\u0002\u001a\u00020\u00032\f\u0010\u0004\u001a\b\u0012\u0004\u0012\u00020\u00010\u00052\b\b\u0002\u0010\u0006\u001a\u00020\u0007H\u0007\u00a8\u0006\b"}, d2 = {"DropOverlayCard", "", "item", "Lcom/somewhere/app/viewmodel/DiscoveredDrop;", "onTap", "Lkotlin/Function0;", "modifier", "Landroidx/compose/ui/Modifier;", "app_debug"})
public final class DropOverlayCardKt {
    
    /**
     * Floating overlay card shown on the camera preview.
     *
     * Two visual states:
     * - LOCKED (far): blurred, faint, shows "move closer", non-tappable
     * - UNLOCKED (near): sharp, shows distance, tappable
     *
     * Primary items get full opacity; secondary items are slightly faded.
     */
    @androidx.compose.runtime.Composable()
    public static final void DropOverlayCard(@org.jetbrains.annotations.NotNull()
    com.somewhere.app.viewmodel.DiscoveredDrop item, @org.jetbrains.annotations.NotNull()
    kotlin.jvm.functions.Function0<kotlin.Unit> onTap, @org.jetbrains.annotations.NotNull()
    androidx.compose.ui.Modifier modifier) {
    }
}