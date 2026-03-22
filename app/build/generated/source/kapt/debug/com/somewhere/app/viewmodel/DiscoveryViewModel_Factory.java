package com.somewhere.app.viewmodel;

import android.hardware.SensorManager;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.somewhere.app.data.repository.DropRepository;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.Provider;
import dagger.internal.Providers;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;

@ScopeMetadata
@QualifierMetadata
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
public final class DiscoveryViewModel_Factory implements Factory<DiscoveryViewModel> {
  private final Provider<DropRepository> repositoryProvider;

  private final Provider<SensorManager> sensorManagerProvider;

  private final Provider<FusedLocationProviderClient> fusedLocationClientProvider;

  public DiscoveryViewModel_Factory(Provider<DropRepository> repositoryProvider,
      Provider<SensorManager> sensorManagerProvider,
      Provider<FusedLocationProviderClient> fusedLocationClientProvider) {
    this.repositoryProvider = repositoryProvider;
    this.sensorManagerProvider = sensorManagerProvider;
    this.fusedLocationClientProvider = fusedLocationClientProvider;
  }

  @Override
  public DiscoveryViewModel get() {
    return newInstance(repositoryProvider.get(), sensorManagerProvider.get(), fusedLocationClientProvider.get());
  }

  public static DiscoveryViewModel_Factory create(
      javax.inject.Provider<DropRepository> repositoryProvider,
      javax.inject.Provider<SensorManager> sensorManagerProvider,
      javax.inject.Provider<FusedLocationProviderClient> fusedLocationClientProvider) {
    return new DiscoveryViewModel_Factory(Providers.asDaggerProvider(repositoryProvider), Providers.asDaggerProvider(sensorManagerProvider), Providers.asDaggerProvider(fusedLocationClientProvider));
  }

  public static DiscoveryViewModel_Factory create(Provider<DropRepository> repositoryProvider,
      Provider<SensorManager> sensorManagerProvider,
      Provider<FusedLocationProviderClient> fusedLocationClientProvider) {
    return new DiscoveryViewModel_Factory(repositoryProvider, sensorManagerProvider, fusedLocationClientProvider);
  }

  public static DiscoveryViewModel newInstance(DropRepository repository,
      SensorManager sensorManager, FusedLocationProviderClient fusedLocationClient) {
    return new DiscoveryViewModel(repository, sensorManager, fusedLocationClient);
  }
}
