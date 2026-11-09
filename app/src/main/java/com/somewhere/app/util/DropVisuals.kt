package com.somewhere.app.util

object DropVisuals {
    // Keep perspective, but distant cards must remain recognizable, legible, and comfortably sized.
    fun perspectiveScale(distanceMeters: Float): Float =
        (5.0f / (distanceMeters.coerceAtLeast(1f) + 3.5f) + 0.45f).coerceIn(0.80f, 1.25f)
}
