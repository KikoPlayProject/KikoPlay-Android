package com.kiko.kikoplay.ui.player

import android.graphics.Rect
import android.util.Rational

data class PlayerPictureInPictureState(
    val canEnter: Boolean = false,
    val sourceRectHint: Rect? = null,
    val aspectRatio: Rational = Rational(16, 9)
)
