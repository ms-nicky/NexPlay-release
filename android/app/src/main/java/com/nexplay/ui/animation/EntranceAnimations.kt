package com.nexplay.ui.animation

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.graphicsLayer
import com.nexplay.ui.theme.LocalReduceMotion
import com.nexplay.ui.theme.NexPlayMotion

private val EasingEmphasizedDecelerate = CubicBezierEasing(0.05f, 0.7f, 0.1f, 1f)

/**
 * Modifier that animates the entrance of a composable with a staggered fade + slide-up effect.
 *
 * @param index Position index of the item in the list/grid (0-based)
 * @param enabled Whether the animation is enabled
 */
fun Modifier.staggeredEntrance(
    index: Int,
    enabled: Boolean = true,
): Modifier = composed {
    if (!enabled) return@composed this
    val animatable = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        val delayMs = (index * 40L).coerceAtMost(400L)
        kotlinx.coroutines.delay(delayMs)
        animatable.animateTo(
            targetValue = 1f,
            animationSpec = tween(
                durationMillis = NexPlayMotion.DurationEmphasized,
                easing = EasingEmphasizedDecelerate,
            ),
        )
    }
    graphicsLayer {
        val progress = animatable.value
        alpha = progress
        translationY = (1f - progress) * 24f
    }
}

/**
 * Modifier that animates a composable's entrance with a smooth scale + fade effect.
 */
fun Modifier.popInEntrance(
    enabled: Boolean = true,
): Modifier = composed {
    if (!enabled) return@composed this
    val animatable = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        animatable.animateTo(
            targetValue = 1f,
            animationSpec = tween(
                durationMillis = 380,
                easing = EasingEmphasizedDecelerate,
            ),
        )
    }
    graphicsLayer {
        val progress = animatable.value
        alpha = progress
        scaleX = 0.92f + 0.08f * progress
        scaleY = 0.92f + 0.08f * progress
    }
}
