package com.somewhere.app

import android.app.Application
import com.somewhere.app.data.repository.DropRepository
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Application-level singleton providing database and repository instances.
 */
@HiltAndroidApp
class SomewhereApplication : Application() {

    @Inject
    lateinit var repository: DropRepository

    override fun onCreate() {
        super.onCreate()
        CoroutineScope(Dispatchers.IO).launch {
            repository.cleanupOrphanImages()
        }
    }
}
