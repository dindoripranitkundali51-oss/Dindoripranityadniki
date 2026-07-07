package com.example.dindoripranityadnyiki.core.design

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween

/**
 * Shared motion tokens for app transitions and small interactions.
 */
object DivineMotion {
    val StandardEasing = CubicBezierEasing(0.16f, 1f, 0.3f, 1f)

    fun <T> snappySpring() = spring<T>(
        dampingRatio = Spring.DampingRatioLowBouncy,
        stiffness = Spring.StiffnessLow
    )

    fun <T> pageTween() = tween<T>(
        durationMillis = 500,
        easing = StandardEasing
    )
}
