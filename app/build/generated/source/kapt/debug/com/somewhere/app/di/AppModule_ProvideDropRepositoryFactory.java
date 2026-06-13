package com.somewhere.app.di;

import android.content.Context;
import com.somewhere.app.data.local.DropDao;
import com.somewhere.app.data.repository.DropRepository;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.Preconditions;
import dagger.internal.Provider;
import dagger.internal.Providers;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;

@ScopeMetadata("javax.inject.Singleton")
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
public final class AppModule_ProvideDropRepositoryFactory implements Factory<DropRepository> {
  private final Provider<Context> contextProvider;

  private final Provider<DropDao> daoProvider;

  public AppModule_ProvideDropRepositoryFactory(Provider<Context> contextProvider,
      Provider<DropDao> daoProvider) {
    this.contextProvider = contextProvider;
    this.daoProvider = daoProvider;
  }

  @Override
  public DropRepository get() {
    return provideDropRepository(contextProvider.get(), daoProvider.get());
  }

  public static AppModule_ProvideDropRepositoryFactory create(
      javax.inject.Provider<Context> contextProvider, javax.inject.Provider<DropDao> daoProvider) {
    return new AppModule_ProvideDropRepositoryFactory(Providers.asDaggerProvider(contextProvider), Providers.asDaggerProvider(daoProvider));
  }

  public static AppModule_ProvideDropRepositoryFactory create(Provider<Context> contextProvider,
      Provider<DropDao> daoProvider) {
    return new AppModule_ProvideDropRepositoryFactory(contextProvider, daoProvider);
  }

  public static DropRepository provideDropRepository(Context context, DropDao dao) {
    return Preconditions.checkNotNullFromProvides(AppModule.INSTANCE.provideDropRepository(context, dao));
  }
}
