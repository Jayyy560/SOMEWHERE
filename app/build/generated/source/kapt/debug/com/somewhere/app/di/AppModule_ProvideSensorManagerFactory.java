package com.somewhere.app.di;

import android.content.Context;
import android.hardware.SensorManager;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.Preconditions;
import dagger.internal.Provider;
import dagger.internal.Providers;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;

@ScopeMetadata
@QualifierMetadata("dagger.hilt.android.qualifiers.ApplicationContext")
@DaggerGenerated
@Generated(
    value = "dagger.internal.codegen.ComponentProcessor",
    comments = "https://dagger.dev"
)
@SuppressWarnings({
    "unchecked",
    "rawtypes",
    "KotlinInternal",
    "KotlinInternalInJava",
    "cast",
    "deprecation",
    "nullness:initialization.field.uninitialized"
})
public final class AppModule_ProvideSensorManagerFactory implements Factory<SensorManager> {
  private final Provider<Context> contextProvider;

  public AppModule_ProvideSensorManagerFactory(Provider<Context> contextProvider) {
    this.contextProvider = contextProvider;
  }

  @Override
  public SensorManager get() {
    return provideSensorManager(contextProvider.get());
  }

  public static AppModule_ProvideSensorManagerFactory create(
      javax.inject.Provider<Context> contextProvider) {
    return new AppModule_ProvideSensorManagerFactory(Providers.asDaggerProvider(contextProvider));
  }

  public static AppModule_ProvideSensorManagerFactory create(Provider<Context> contextProvider) {
    return new AppModule_ProvideSensorManagerFactory(contextProvider);
  }

  public static SensorManager provideSensorManager(Context context) {
    return Preconditions.checkNotNullFromProvides(AppModule.INSTANCE.provideSensorManager(context));
  }
}
