package com.travelingtunes.app.core.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.travelingtunes.app.core.model.SlideDirection

@Composable
fun SlidingOverlay(
    visible: Boolean,
    slideDirection: SlideDirection,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    if (visible) {
        BackHandler {
            onDismiss()
        }
    }

    val (enterTransition, exitTransition) = remember(slideDirection) {
        when (slideDirection) {
            SlideDirection.TOP -> {
                slideInVertically(
                    initialOffsetY = { -it },
                    animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing)
                ) + fadeIn(animationSpec = tween(300)) to
                slideOutVertically(
                    targetOffsetY = { -it },
                    animationSpec = tween(durationMillis = 250, easing = FastOutSlowInEasing)
                ) + fadeOut(animationSpec = tween(250))
            }
            SlideDirection.BOTTOM -> {
                slideInVertically(
                    initialOffsetY = { it },
                    animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing)
                ) + fadeIn(animationSpec = tween(300)) to
                slideOutVertically(
                    targetOffsetY = { it },
                    animationSpec = tween(durationMillis = 250, easing = FastOutSlowInEasing)
                ) + fadeOut(animationSpec = tween(250))
            }
            SlideDirection.LEFT -> {
                slideInHorizontally(
                    initialOffsetX = { -it },
                    animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing)
                ) + fadeIn(animationSpec = tween(300)) to
                slideOutHorizontally(
                    targetOffsetX = { -it },
                    animationSpec = tween(durationMillis = 250, easing = FastOutSlowInEasing)
                ) + fadeOut(animationSpec = tween(250))
            }
            SlideDirection.RIGHT -> {
                slideInHorizontally(
                    initialOffsetX = { it },
                    animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing)
                ) + fadeIn(animationSpec = tween(300)) to
                slideOutHorizontally(
                    targetOffsetX = { it },
                    animationSpec = tween(durationMillis = 250, easing = FastOutSlowInEasing)
                ) + fadeOut(animationSpec = tween(250))
            }
        }
    }

    AnimatedVisibility(
        visible = visible,
        enter = enterTransition,
        exit = exitTransition,
        modifier = modifier.fillMaxSize()
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.5f))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) { onDismiss() }
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { /* consume tap inside sheet */ },
                color = MaterialTheme.colorScheme.background,
                tonalElevation = 8.dp
            ) {
                content()
            }
        }
    }
}
