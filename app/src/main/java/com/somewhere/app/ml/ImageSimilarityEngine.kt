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

        val l1s = FloatArray(resolution * resolution)
        val l2s = FloatArray(resolution * resolution)
        
        var sum1 = 0f
        var sum2 = 0f

        // 2. Extract luminance and calculate means
        var index = 0
        for (x in 0 until resolution) {
            for (y in 0 until resolution) {
                val p1 = scaled1.getPixel(x, y)
                val p2 = scaled2.getPixel(x, y)

                val l1 = (Color.red(p1) * 0.299f + Color.green(p1) * 0.587f + Color.blue(p1) * 0.114f)
                val l2 = (Color.red(p2) * 0.299f + Color.green(p2) * 0.587f + Color.blue(p2) * 0.114f)

                l1s[index] = l1
                l2s[index] = l2
                
                sum1 += l1
                sum2 += l2
                index++
            }
        }

        scaled1.recycle()
        scaled2.recycle()

        val mean1 = sum1 / (resolution * resolution)
        val mean2 = sum2 / (resolution * resolution)

        // 3. Compute Zero-Mean Normalized Cross-Correlation (ZNCC)
        // This is perfectly invariant to lighting changes and robust against random noise!
        var numerator = 0f
        var sumSq1 = 0f
        var sumSq2 = 0f

        for (i in 0 until resolution * resolution) {
            val dev1 = l1s[i] - mean1
            val dev2 = l2s[i] - mean2

            numerator += (dev1 * dev2)
            sumSq1 += (dev1 * dev1)
            sumSq2 += (dev2 * dev2)
        }

        if (sumSq1 == 0f || sumSq2 == 0f) return 0f

        // Correlation ranges from -1.0 (inverted) to 0.0 (random) to 1.0 (perfect match)
        val correlation = (numerator / Math.sqrt((sumSq1 * sumSq2).toDouble())).toFloat()

        // Map correlation > 0 to 0.0 - 1.0 (discard negative correlations)
        val similarity = Math.max(0f, correlation)

        // Give a slight boost so an 80% correlation feels like a ~90% match
        return (similarity * 1.15f).coerceIn(0f, 1f)
    }

    fun close() {
        // No longer using TFLite, so nothing to close!
    }
}
