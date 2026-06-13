package com.somewhere.app.di

import android.content.Context
import android.hardware.SensorManager
import androidx.room.Room
import com.somewhere.app.data.local.AppDatabase
import com.somewhere.app.data.local.DropDao
import com.somewhere.app.data.repository.DropRepository
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideDatabase(
        @ApplicationContext context: Context
    ): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "somewhere_db"
        )
            .addMigrations(AppDatabase.MIGRATION_1_2)
            .build()
    }

    @Provides
    fun provideDropDao(database: AppDatabase): DropDao = database.dropDao()

    @Provides
    @Singleton
    fun provideDropRepository(
        @ApplicationContext context: Context,
        dao: DropDao
    ): DropRepository {
        return DropRepository(context, dao)
    }

    @Provides
    fun provideSensorManager(
        @ApplicationContext context: Context
    ): SensorManager = context.getSystemService(SensorManager::class.java)

    @Provides
    fun provideFusedLocationClient(
        @ApplicationContext context: Context
    ): FusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(context)
}
