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

    suspend fun computeMatchScore(originalImageUrl: String, capturedBitmap: Bitmap): Int {
        return withContext(Dispatchers.IO) {
            try {
                // Fetch the original image from the network using Coil
                val request = ImageRequest.Builder(context)
                    .data(originalImageUrl)
                    .allowHardware(false) // Must be false to extract software bitmap pixels
                    .build()
                
                val result = imageLoader.execute(request)
                if (result is SuccessResult) {
                    val originalBitmap = (result.drawable as BitmapDrawable).bitmap
                    
                    val similarity = engine.computeSimilarity(originalBitmap, capturedBitmap)
                    
                    // Convert cosine similarity (-1.0 to 1.0) to a percentage (0 to 100)
                    // For image classification probability vectors, it's usually between 0.0 and 1.0 already
                    val score = (similarity * 100).toInt().coerceIn(0, 100)
                    score
                } else {
                    -1 // Failed to load original image
                }
            } catch (e: Exception) {
                e.printStackTrace()
                -1
            }
        }
    }
}
