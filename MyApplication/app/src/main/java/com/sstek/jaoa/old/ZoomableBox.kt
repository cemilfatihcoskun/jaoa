package com.sstek.jaoa.old

import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput

@Composable
fun ZoomGestureWrapper(
    modifier: Modifier = Modifier,
    onZoomIn: () -> Unit,
    onZoomOut: () -> Unit,
    content: @Composable BoxScope.() -> Unit
) {
    var lastScale by remember { mutableStateOf(1f) }

    Box(
        modifier = modifier
            .pointerInput(Unit) {
                detectTransformGestures { _, _, zoom, _ ->
                    if (zoom > 1.1f) {
                        onZoomIn()
                    } else if (zoom < 0.9f) {
                        onZoomOut()
                    }
                    lastScale = zoom
                }
            }
    ) {
        content()
    }
}
