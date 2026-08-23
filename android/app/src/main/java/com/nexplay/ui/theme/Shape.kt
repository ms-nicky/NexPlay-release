package com.nexplay.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/**
 * Six radii, down from the thirteen distinct values that were previously spread across ~109
 * inline `RoundedCornerShape(...)` call sites.
 */
object NexPlayRadius {
    val xs = 4.dp
    val sm = 8.dp
    val md = 12.dp
    val lg = 16.dp
    val xl = 24.dp
    val full = 999.dp
}

/** Wired into `MaterialTheme(shapes = ...)`, which previously received no shapes at all. */
val NexPlayShapes = Shapes(
    extraSmall = RoundedCornerShape(NexPlayRadius.xs),
    small = RoundedCornerShape(NexPlayRadius.sm),
    medium = RoundedCornerShape(NexPlayRadius.md),
    large = RoundedCornerShape(NexPlayRadius.lg),
    extraLarge = RoundedCornerShape(NexPlayRadius.xl),
)
