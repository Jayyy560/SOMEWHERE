package com.somewhere.app.viewmodel;

import android.net.Uri;
import androidx.lifecycle.ViewModel;
import com.somewhere.app.data.repository.DropRepository;
import dagger.hilt.android.lifecycle.HiltViewModel;
import kotlinx.coroutines.flow.StateFlow;
import javax.inject.Inject;

/**
 * ViewModel for the Drop screen.
 * Manages the captured photo URI, text input, and save operation.
 */
@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000D\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0003\n\u0002\u0010\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0010\u000e\n\u0002\b\u0003\n\u0002\u0010\u0006\n\u0002\b\u0003\b\u0007\u0018\u00002\u00020\u0001:\u0001\u0019B\u000f\b\u0007\u0012\u0006\u0010\u0002\u001a\u00020\u0003\u00a2\u0006\u0002\u0010\u0004J\u0006\u0010\f\u001a\u00020\rJ\u000e\u0010\u000e\u001a\u00020\r2\u0006\u0010\u000f\u001a\u00020\u0010J\u000e\u0010\u0011\u001a\u00020\r2\u0006\u0010\u0012\u001a\u00020\u0013J\u0006\u0010\u0014\u001a\u00020\rJ\u0016\u0010\u0015\u001a\u00020\r2\u0006\u0010\u0016\u001a\u00020\u00172\u0006\u0010\u0018\u001a\u00020\u0017R\u0014\u0010\u0005\u001a\b\u0012\u0004\u0012\u00020\u00070\u0006X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0002\u001a\u00020\u0003X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u0017\u0010\b\u001a\b\u0012\u0004\u0012\u00020\u00070\t\u00a2\u0006\b\n\u0000\u001a\u0004\b\n\u0010\u000b\u00a8\u0006\u001a"}, d2 = {"Lcom/somewhere/app/viewmodel/DropViewModel;", "Landroidx/lifecycle/ViewModel;", "repository", "Lcom/somewhere/app/data/repository/DropRepository;", "(Lcom/somewhere/app/data/repository/DropRepository;)V", "_uiState", "Lkotlinx/coroutines/flow/MutableStateFlow;", "Lcom/somewhere/app/viewmodel/DropViewModel$DropUiState;", "uiState", "Lkotlinx/coroutines/flow/StateFlow;", "getUiState", "()Lkotlinx/coroutines/flow/StateFlow;", "clearError", "", "onPhotoCaptured", "uri", "Landroid/net/Uri;", "onTextChanged", "text", "", "reset", "saveDrop", "latitude", "", "longitude", "DropUiState", "app_debug"})
@dagger.hilt.android.lifecycle.HiltViewModel()
public final class DropViewModel extends androidx.lifecycle.ViewModel {
    @org.jetbrains.annotations.NotNull()
    private final com.somewhere.app.data.repository.DropRepository repository = null;
    @org.jetbrains.annotations.NotNull()
    private final kotlinx.coroutines.flow.MutableStateFlow<com.somewhere.app.viewmodel.DropViewModel.DropUiState> _uiState = null;
    @org.jetbrains.annotations.NotNull()
    private final kotlinx.coroutines.flow.StateFlow<com.somewhere.app.viewmodel.DropViewModel.DropUiState> uiState = null;
    
    @javax.inject.Inject()
    public DropViewModel(@org.jetbrains.annotations.NotNull()
    com.somewhere.app.data.repository.DropRepository repository) {
        super();
    }
    
    @org.jetbrains.annotations.NotNull()
    public final kotlinx.coroutines.flow.StateFlow<com.somewhere.app.viewmodel.DropViewModel.DropUiState> getUiState() {
        return null;
    }
    
    public final void onPhotoCaptured(@org.jetbrains.annotations.NotNull()
    android.net.Uri uri) {
    }
    
    public final void onTextChanged(@org.jetbrains.annotations.NotNull()
    java.lang.String text) {
    }
    
    public final void saveDrop(double latitude, double longitude) {
    }
    
    public final void reset() {
    }
    
    public final void clearError() {
    }
    
    @kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000&\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\u000e\n\u0000\n\u0002\u0010\u000b\n\u0002\b\u0012\n\u0002\u0010\b\n\u0002\b\u0002\b\u0087\b\u0018\u00002\u00020\u0001B;\u0012\n\b\u0002\u0010\u0002\u001a\u0004\u0018\u00010\u0003\u0012\b\b\u0002\u0010\u0004\u001a\u00020\u0005\u0012\b\b\u0002\u0010\u0006\u001a\u00020\u0007\u0012\b\b\u0002\u0010\b\u001a\u00020\u0007\u0012\n\b\u0002\u0010\t\u001a\u0004\u0018\u00010\u0005\u00a2\u0006\u0002\u0010\nJ\u000b\u0010\u0011\u001a\u0004\u0018\u00010\u0003H\u00c6\u0003J\t\u0010\u0012\u001a\u00020\u0005H\u00c6\u0003J\t\u0010\u0013\u001a\u00020\u0007H\u00c6\u0003J\t\u0010\u0014\u001a\u00020\u0007H\u00c6\u0003J\u000b\u0010\u0015\u001a\u0004\u0018\u00010\u0005H\u00c6\u0003J?\u0010\u0016\u001a\u00020\u00002\n\b\u0002\u0010\u0002\u001a\u0004\u0018\u00010\u00032\b\b\u0002\u0010\u0004\u001a\u00020\u00052\b\b\u0002\u0010\u0006\u001a\u00020\u00072\b\b\u0002\u0010\b\u001a\u00020\u00072\n\b\u0002\u0010\t\u001a\u0004\u0018\u00010\u0005H\u00c6\u0001J\u0013\u0010\u0017\u001a\u00020\u00072\b\u0010\u0018\u001a\u0004\u0018\u00010\u0001H\u00d6\u0003J\t\u0010\u0019\u001a\u00020\u001aH\u00d6\u0001J\t\u0010\u001b\u001a\u00020\u0005H\u00d6\u0001R\u0013\u0010\u0002\u001a\u0004\u0018\u00010\u0003\u00a2\u0006\b\n\u0000\u001a\u0004\b\u000b\u0010\fR\u0013\u0010\t\u001a\u0004\u0018\u00010\u0005\u00a2\u0006\b\n\u0000\u001a\u0004\b\r\u0010\u000eR\u0011\u0010\b\u001a\u00020\u0007\u00a2\u0006\b\n\u0000\u001a\u0004\b\b\u0010\u000fR\u0011\u0010\u0006\u001a\u00020\u0007\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0006\u0010\u000fR\u0011\u0010\u0004\u001a\u00020\u0005\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0010\u0010\u000e\u00a8\u0006\u001c"}, d2 = {"Lcom/somewhere/app/viewmodel/DropViewModel$DropUiState;", "", "capturedImageUri", "Landroid/net/Uri;", "text", "", "isSaving", "", "isSaved", "error", "(Landroid/net/Uri;Ljava/lang/String;ZZLjava/lang/String;)V", "getCapturedImageUri", "()Landroid/net/Uri;", "getError", "()Ljava/lang/String;", "()Z", "getText", "component1", "component2", "component3", "component4", "component5", "copy", "equals", "other", "hashCode", "", "toString", "app_debug"})
    public static final class DropUiState {
        @org.jetbrains.annotations.Nullable()
        private final android.net.Uri capturedImageUri = null;
        @org.jetbrains.annotations.NotNull()
        private final java.lang.String text = null;
        private final boolean isSaving = false;
        private final boolean isSaved = false;
        @org.jetbrains.annotations.Nullable()
        private final java.lang.String error = null;
        
        @org.jetbrains.annotations.Nullable()
        public final android.net.Uri component1() {
            return null;
        }
        
        @org.jetbrains.annotations.NotNull()
        public final java.lang.String component2() {
            return null;
        }
        
        public final boolean component3() {
            return false;
        }
        
        public final boolean component4() {
            return false;
        }
        
        @org.jetbrains.annotations.Nullable()
        public final java.lang.String component5() {
            return null;
        }
        
        @org.jetbrains.annotations.NotNull()
        public final com.somewhere.app.viewmodel.DropViewModel.DropUiState copy(@org.jetbrains.annotations.Nullable()
        android.net.Uri capturedImageUri, @org.jetbrains.annotations.NotNull()
        java.lang.String text, boolean isSaving, boolean isSaved, @org.jetbrains.annotations.Nullable()
        java.lang.String error) {
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
        
        public DropUiState(@org.jetbrains.annotations.Nullable()
        android.net.Uri capturedImageUri, @org.jetbrains.annotations.NotNull()
        java.lang.String text, boolean isSaving, boolean isSaved, @org.jetbrains.annotations.Nullable()
        java.lang.String error) {
            super();
        }
        
        @org.jetbrains.annotations.Nullable()
        public final android.net.Uri getCapturedImageUri() {
            return null;
        }
        
        @org.jetbrains.annotations.NotNull()
        public final java.lang.String getText() {
            return null;
        }
        
        public final boolean isSaving() {
            return false;
        }
        
        public final boolean isSaved() {
            return false;
        }
        
        @org.jetbrains.annotations.Nullable()
        public final java.lang.String getError() {
            return null;
        }
        
        public DropUiState() {
            super();
        }
    }
}