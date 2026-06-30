package com.somewhere.app.util

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.vector.ImageVector

object CategoryUtils {
    val CATEGORIES = listOf(
        "Story", "Memory", "Food", "Music", "Photography", 
        "History", "Hidden Spot", "Event", "Recommendation"
    )
}

fun getCategoryIcon(category: String?, isAnonymous: Boolean): ImageVector {
    if (isAnonymous) return Icons.Default.HelpOutline
    return when (category) {
        "Story" -> Icons.Default.Book
        "Memory" -> Icons.Default.History
        "Food" -> Icons.Default.Restaurant
        "Music" -> Icons.Default.MusicNote
        "Photography" -> Icons.Default.CameraAlt
        "History" -> Icons.Default.AccountBalance
        "Hidden Spot" -> Icons.Default.VisibilityOff
        "Event" -> Icons.Default.Event
        "Recommendation" -> Icons.Default.Star
        else -> Icons.Default.LocationOn
    }
}
