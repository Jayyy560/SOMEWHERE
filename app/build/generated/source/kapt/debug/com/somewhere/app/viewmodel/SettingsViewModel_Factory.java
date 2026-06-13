package com.somewhere.app.viewmodel;

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
public final class SettingsViewModel_Factory implements Factory<SettingsViewModel> {
  private final Provider<DropRepository> repositoryProvider;

  public SettingsViewModel_Factory(Provider<DropRepository> repositoryProvider) {
    this.repositoryProvider = repositoryProvider;
  }

  @Override
  public SettingsViewModel get() {
    return newInstance(repositoryProvider.get());
  }

  public static SettingsViewModel_Factory create(
      javax.inject.Provider<DropRepository> repositoryProvider) {
    return new SettingsViewModel_Factory(Providers.asDaggerProvider(repositoryProvider));
  }

  public static SettingsViewModel_Factory create(Provider<DropRepository> repositoryProvider) {
    return new SettingsViewModel_Factory(repositoryProvider);
  }

  public static SettingsViewModel newInstance(DropRepository repository) {
    return new SettingsViewModel(repository);
  }
}
