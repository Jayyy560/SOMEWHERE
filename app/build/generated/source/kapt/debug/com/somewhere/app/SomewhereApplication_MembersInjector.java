package com.somewhere.app;

import com.somewhere.app.data.repository.DropRepository;
import dagger.MembersInjector;
import dagger.internal.DaggerGenerated;
import dagger.internal.InjectedFieldSignature;
import dagger.internal.Provider;
import dagger.internal.Providers;
import dagger.internal.QualifierMetadata;
import javax.annotation.processing.Generated;

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
public final class SomewhereApplication_MembersInjector implements MembersInjector<SomewhereApplication> {
  private final Provider<DropRepository> repositoryProvider;

  public SomewhereApplication_MembersInjector(Provider<DropRepository> repositoryProvider) {
    this.repositoryProvider = repositoryProvider;
  }

  public static MembersInjector<SomewhereApplication> create(
      Provider<DropRepository> repositoryProvider) {
    return new SomewhereApplication_MembersInjector(repositoryProvider);
  }

  public static MembersInjector<SomewhereApplication> create(
      javax.inject.Provider<DropRepository> repositoryProvider) {
    return new SomewhereApplication_MembersInjector(Providers.asDaggerProvider(repositoryProvider));
  }

  @Override
  public void injectMembers(SomewhereApplication instance) {
    injectRepository(instance, repositoryProvider.get());
  }

  @InjectedFieldSignature("com.somewhere.app.SomewhereApplication.repository")
  public static void injectRepository(SomewhereApplication instance, DropRepository repository) {
    instance.repository = repository;
  }
}
