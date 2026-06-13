package com.somewhere.app.data.repository

import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import coil.ImageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import com.somewhere.app.ml.ImageSimilarityEngine
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ImageMatchRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val engine = ImageSimilarityEngine(context)
    private val imageLoader = ImageLoader(context)
    
    // Cache the original bitmap to avoid downloading it every frame
    private var cachedOriginalBitmap: Bitmap? = null
    private var currentOriginalUrl: String? = null

    suspend fun loadOriginalImage(originalImageUrl: String): Boolean {
        if (originalImageUrl == currentOriginalUrl && cachedOriginalBitmap != null) {
            return true
        }
        
        return withContext(Dispatchers.IO) {
            try {
                val request = ImageRequest.Builder(context)
                    .data(originalImageUrl)
                    .allowHardware(false)
                    .build()
                
                val result = imageLoader.execute(request)
                if (result is SuccessResult) {
                    cachedOriginalBitmap = (result.drawable as BitmapDrawable).bitmap
                    currentOriginalUrl = originalImageUrl
                    true
                } else {
                    false
                }
            } catch (e: Exception) {
                e.printStackTrace()
                false
            }
        }
    }

    suspend fun computeLiveScore(capturedBitmap: Bitmap): Int {
        val originalBitmap = cachedOriginalBitmap ?: return -1
        return withContext(Dispatchers.Default) {
            val similarity = engine.computeSimilarity(originalBitmap, capturedBitmap)
            (similarity * 100).toInt().coerceIn(0, 100)
        }
    }
}
