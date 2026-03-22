package com.somewhere.app;

import android.app.Application;
import com.somewhere.app.data.repository.DropRepository;
import dagger.hilt.android.HiltAndroidApp;
import kotlinx.coroutines.Dispatchers;
import javax.inject.Inject;

/**
 * Application-level singleton providing database and repository instances.
 */
@dagger.hilt.android.HiltAndroidApp()
@kotlin.Metadata(mv = {1, 9, 0}, k = 1, xi = 48, d1 = {"\u0000\u001a\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\b\u0005\n\u0002\u0010\u0002\n\u0000\b\u0007\u0018\u00002\u00020\u0001B\u0005\u00a2\u0006\u0002\u0010\u0002J\b\u0010\t\u001a\u00020\nH\u0016R\u001e\u0010\u0003\u001a\u00020\u00048\u0006@\u0006X\u0087.\u00a2\u0006\u000e\n\u0000\u001a\u0004\b\u0005\u0010\u0006\"\u0004\b\u0007\u0010\b\u00a8\u0006\u000b"}, d2 = {"Lcom/somewhere/app/SomewhereApplication;", "Landroid/app/Application;", "()V", "repository", "Lcom/somewhere/app/data/repository/DropRepository;", "getRepository", "()Lcom/somewhere/app/data/repository/DropRepository;", "setRepository", "(Lcom/somewhere/app/data/repository/DropRepository;)V", "onCreate", "", "app_debug"})
public final class SomewhereApplication extends android.app.Application {
    @javax.inject.Inject()
    public com.somewhere.app.data.repository.DropRepository repository;
    
    public SomewhereApplication() {
        super();
    }
    
    @org.jetbrains.annotations.NotNull()
    public final com.somewhere.app.data.repository.DropRepository getRepository() {
        return null;
    }
    
    public final void setRepository(@org.jetbrains.annotations.NotNull()
    com.somewhere.app.data.repository.DropRepository p0) {
    }
    
    @java.lang.Override()
    public void onCreate() {
    }
}