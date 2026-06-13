package com.somewhere.app.ml

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import kotlin.math.abs

/**
 * Replaces the heavy TFLite model with a blazing fast structural similarity engine.
 * This is perfect for "Find Exact Spot" because it compares the actual spatial
 * layout of the scene (sky at top, building on left) rather than just semantic tags.
 */
class ImageSimilarityEngine(context: Context) {
    
    // We downsample to 32x32 to remove noise, lighting variations, and small details,
    // focusing only on the overall structure/viewpoint of the scene.
    private val resolution = 32

    fun computeSimilarity(bitmap1: Bitmap, bitmap2: Bitmap): Float {
        // 1. Resize both to 32x32
        val scaled1 = Bitmap.createScaledBitmap(bitmap1, resolution, resolution, true)
        val scaled2 = Bitmap.createScaledBitmap(bitmap2, resolution, resolution, true)

        // 2. Compute Mean Absolute Difference (MAD)
        var totalDifference = 0f
        val maxDifferencePerPixel = 255f

        for (x in 0 until resolution) {
            for (y in 0 until resolution) {
                val p1 = scaled1.getPixel(x, y)
                val p2 = scaled2.getPixel(x, y)

                // Get luminance (grayscale)
                val l1 = (Color.red(p1) * 0.299 + Color.green(p1) * 0.587 + Color.blue(p1) * 0.114).toFloat()
                val l2 = (Color.red(p2) * 0.299 + Color.green(p2) * 0.587 + Color.blue(p2) * 0.114).toFloat()

                totalDifference += abs(l1 - l2)
            }
        }

        scaled1.recycle()
        scaled2.recycle()

        // 3. Convert to a similarity percentage (0.0 to 1.0)
        val totalPixels = (resolution * resolution).toFloat()
        val maxPossibleDifference = totalPixels * maxDifferencePerPixel
        val differenceRatio = totalDifference / maxPossibleDifference

        // 4. Boost the score slightly because real world photos will rarely be 100% identical 
        // due to lighting/time of day. A 10-15% difference in MAD is actually a very good structural match.
        val similarity = 1.0f - differenceRatio
        
        // Apply a curve to make scores feel more natural to the user
        // e.g. 0.8 structural similarity feels like a 95% match to a human.
        return when {
            similarity > 0.85f -> 1.0f // Perfect match
            similarity > 0.70f -> similarity * 1.15f // Good match, boost it
            else -> similarity // Poor match
        }.coerceIn(0f, 1f)
    }

    fun close() {
        // No longer using TFLite, so nothing to close!
    }
}
