package com.somewhere.app.viewmodel;

import androidx.lifecycle.ViewModel;
import com.somewhere.app.data.repository.DropRepository;
import dagger.hilt.android.lifecycle.HiltViewModel;
import kotlinx.coroutines.flow.StateFlow;
import javax.inject.Inject;

@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000,\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0003\n\u0002\u0010\u0002\n\u0002\b\u0003\b\u0007\u0018\u00002\u00020\u0001:\u0001\u000fB\u000f\b\u0007\u0012\u0006\u0010\u0002\u001a\u00020\u0003\u00a2\u0006\u0002\u0010\u0004J\u0006\u0010\f\u001a\u00020\rJ\u0006\u0010\u000e\u001a\u00020\rR\u0014\u0010\u0005\u001a\b\u0012\u0004\u0012\u00020\u00070\u0006X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0002\u001a\u00020\u0003X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u0017\u0010\b\u001a\b\u0012\u0004\u0012\u00020\u00070\t\u00a2\u0006\b\n\u0000\u001a\u0004\b\n\u0010\u000b\u00a8\u0006\u0010"}, d2 = {"Lcom/somewhere/app/viewmodel/SettingsViewModel;", "Landroidx/lifecycle/ViewModel;", "repository", "Lcom/somewhere/app/data/repository/DropRepository;", "(Lcom/somewhere/app/data/repository/DropRepository;)V", "_uiState", "Lkotlinx/coroutines/flow/MutableStateFlow;", "Lcom/somewhere/app/viewmodel/SettingsViewModel$SettingsUiState;", "uiState", "Lkotlinx/coroutines/flow/StateFlow;", "getUiState", "()Lkotlinx/coroutines/flow/StateFlow;", "clearMessage", "", "deleteAllDrops", "SettingsUiState", "app_debug"})
@dagger.hilt.android.lifecycle.HiltViewModel()
public final class SettingsViewModel extends androidx.lifecycle.ViewModel {
    @org.jetbrains.annotations.NotNull()
    private final com.somewhere.app.data.repository.DropRepository repository = null;
    @org.jetbrains.annotations.NotNull()
    private final kotlinx.coroutines.flow.MutableStateFlow<com.somewhere.app.viewmodel.SettingsViewModel.SettingsUiState> _uiState = null;
    @org.jetbrains.annotations.NotNull()
    private final kotlinx.coroutines.flow.StateFlow<com.somewhere.app.viewmodel.SettingsViewModel.SettingsUiState> uiState = null;
    
    @javax.inject.Inject()
    public SettingsViewModel(@org.jetbrains.annotations.NotNull()
    com.somewhere.app.data.repository.DropRepository repository) {
        super();
    }
    
    @org.jetbrains.annotations.NotNull()
    public final kotlinx.coroutines.flow.StateFlow<com.somewhere.app.viewmodel.SettingsViewModel.SettingsUiState> getUiState() {
        return null;
    }
    
    public final void deleteAllDrops() {
    }
    
    public final void clearMessage() {
    }
    
    @kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000 \n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0000\n\u0002\u0010\u000b\n\u0000\n\u0002\u0010\u000e\n\u0002\b\n\n\u0002\u0010\b\n\u0002\b\u0002\b\u0087\b\u0018\u00002\u00020\u0001B\u001b\u0012\b\b\u0002\u0010\u0002\u001a\u00020\u0003\u0012\n\b\u0002\u0010\u0004\u001a\u0004\u0018\u00010\u0005\u00a2\u0006\u0002\u0010\u0006J\t\u0010\n\u001a\u00020\u0003H\u00c6\u0003J\u000b\u0010\u000b\u001a\u0004\u0018\u00010\u0005H\u00c6\u0003J\u001f\u0010\f\u001a\u00020\u00002\b\b\u0002\u0010\u0002\u001a\u00020\u00032\n\b\u0002\u0010\u0004\u001a\u0004\u0018\u00010\u0005H\u00c6\u0001J\u0013\u0010\r\u001a\u00020\u00032\b\u0010\u000e\u001a\u0004\u0018\u00010\u0001H\u00d6\u0003J\t\u0010\u000f\u001a\u00020\u0010H\u00d6\u0001J\t\u0010\u0011\u001a\u00020\u0005H\u00d6\u0001R\u0011\u0010\u0002\u001a\u00020\u0003\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0002\u0010\u0007R\u0013\u0010\u0004\u001a\u0004\u0018\u00010\u0005\u00a2\u0006\b\n\u0000\u001a\u0004\b\b\u0010\t\u00a8\u0006\u0012"}, d2 = {"Lcom/somewhere/app/viewmodel/SettingsViewModel$SettingsUiState;", "", "isClearing", "", "message", "", "(ZLjava/lang/String;)V", "()Z", "getMessage", "()Ljava/lang/String;", "component1", "component2", "copy", "equals", "other", "hashCode", "", "toString", "app_debug"})
    public static final class SettingsUiState {
        private final boolean isClearing = false;
        @org.jetbrains.annotations.Nullable()
        private final java.lang.String message = null;
        
        public final boolean component1() {
            return false;
        }
        
        @org.jetbrains.annotations.Nullable()
        public final java.lang.String component2() {
            return null;
        }
        
        @org.jetbrains.annotations.NotNull()
        public final com.somewhere.app.viewmodel.SettingsViewModel.SettingsUiState copy(boolean isClearing, @org.jetbrains.annotations.Nullable()
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
        
        public SettingsUiState(boolean isClearing, @org.jetbrains.annotations.Nullable()
        java.lang.String message) {
            super();
        }
        
        public final boolean isClearing() {
            return false;
        }
        
        @org.jetbrains.annotations.Nullable()
        public final java.lang.String getMessage() {
            return null;
        }
        
        public SettingsUiState() {
            super();
        }
    }
}