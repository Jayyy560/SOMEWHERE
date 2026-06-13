package com.somewhere.app.di;

import com.somewhere.app.data.local.AppDatabase;
import com.somewhere.app.data.local.DropDao;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.Preconditions;
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
public final class AppModule_ProvideDropDaoFactory implements Factory<DropDao> {
  private final Provider<AppDatabase> databaseProvider;

  public AppModule_ProvideDropDaoFactory(Provider<AppDatabase> databaseProvider) {
    this.databaseProvider = databaseProvider;
  }

  @Override
  public DropDao get() {
    return provideDropDao(databaseProvider.get());
  }

  public static AppModule_ProvideDropDaoFactory create(
      javax.inject.Provider<AppDatabase> databaseProvider) {
    return new AppModule_ProvideDropDaoFactory(Providers.asDaggerProvider(databaseProvider));
  }

  public static AppModule_ProvideDropDaoFactory create(Provider<AppDatabase> databaseProvider) {
    return new AppModule_ProvideDropDaoFactory(databaseProvider);
  }

  public static DropDao provideDropDao(AppDatabase database) {
    return Preconditions.checkNotNullFromProvides(AppModule.INSTANCE.provideDropDao(database));
  }
}
