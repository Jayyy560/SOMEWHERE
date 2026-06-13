# Hilt / Dagger
-keep class dagger.** { *; }
-keep class javax.inject.** { *; }
-keep class * extends dagger.hilt.android.internal.managers.ViewComponentManager$FragmentContextWrapper { *; }
-keep class * extends dagger.hilt.android.internal.managers.ViewComponentManager$ActivityContextWrapper { *; }

# Suppress warnings for optional/absent classes
-dontwarn java.lang.management.**
-dontwarn org.slf4j.impl.**
-dontwarn org.codehaus.mojo.animal_sniffer.IgnoreJRERequirement

# Room
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-keep @androidx.room.Dao class *
-dontwarn androidx.room.paging.**

# Supabase / Ktor / kotlinx.serialization
-keep class io.github.jan.supabase.** { *; }
-keep class io.ktor.** { *; }
-keepattributes *Annotation*, InnerClasses
-keepclassmembers class kotlinx.serialization.json.** { *** Companion; }
-keep,includedescriptorclasses class com.somewhere.app.**$$serializer { *; }
-keepclassmembers class com.somewhere.app.data.remote.** { *** Companion; }
-keep,allowshrinking class com.somewhere.app.data.remote.NearbyDrop
-keep,allowshrinking class com.somewhere.app.data.remote.DropInsert

# Keep model classes used with serialization
-keep class com.somewhere.app.data.model.Drop { *; }

# Compose
-keep class androidx.compose.** { *; }

# Coil
-keep class coil.** { *; }

# Keep ViewModels
-keep class * extends androidx.lifecycle.ViewModel { *; }

# Keep custom Application class
-keep class com.somewhere.app.SomewhereApplication { *; }

# Kotlin Coroutines
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
