package com.example.dindoripranityadnyiki.core.design

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/**
 * Standard rounded rectangle shapes.
 * आयताकृती पण कोपरे गोलाकार (As per user requirement)
 * Providing a clean, balanced professional look.
 */
val DivineShapes = Shapes(
    extraSmall = RoundedCornerShape(4.dp),
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(12.dp), // Perfect for Rectangular fields with rounded corners
    large = RoundedCornerShape(16.dp),
    extraLarge = RoundedCornerShape(20.dp)
)
