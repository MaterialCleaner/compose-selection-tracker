/*
 * Copyright 2024 Green Mushroom
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package me.gm.selection

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.drag
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.AwaitPointerEventScope
import androidx.compose.ui.input.pointer.PointerEvent
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventTimeoutCancellationException
import androidx.compose.ui.input.pointer.PointerId
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.changedToUp
import androidx.compose.ui.input.pointer.changedToUpIgnoreConsumed
import androidx.compose.ui.input.pointer.isOutOfBounds
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.util.fastAll
import androidx.compose.ui.util.fastAny
import androidx.compose.ui.util.fastFirstOrNull
import androidx.compose.ui.util.fastForEach
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.coroutineScope

/**
 * Consumes all pointer events until nothing is pressed and then returns. This method assumes
 * that something is currently pressed.
 */
private suspend fun AwaitPointerEventScope.consumeUntilUp(pass: PointerEventPass) {
    do {
        val event = awaitPointerEvent(pass = pass)
        event.changes.fastForEach { it.consume() }
    } while (event.changes.fastAny { it.pressed })
}

/**
 * The difference between this and [androidx.compose.foundation.gestures.detectTapGestures] is that
 * this detects tap gestures during the [PointerEventPass.Initial] and allow to choose whether to
 * prevent the gesture from propagating to descendants.
 */
internal suspend fun PointerInputScope.detectTapGestures(
    onLongPress: ((Offset) -> Boolean)? = null,
    onTap: ((Offset) -> Boolean)? = null,
) = coroutineScope {
    awaitEachGesture {
        val longPressTimeout = viewConfiguration.longPressTimeoutMillis
        val pass = PointerEventPass.Initial

        // wait for the first down press
        val down = awaitFirstDown(pass = pass)

        var upOrCancel: PointerInputChange? = null
        try {
            // listen to if there is up gesture
            // within the longPressTimeout limit
            upOrCancel = withTimeout(longPressTimeout) {
                waitForUpOrCancellation(pass = pass)
            }
            if (upOrCancel == null) {
                // tap-up was canceled
            } else {

            }
        } catch (_: PointerEventTimeoutCancellationException) {
            // handle long press
            if (onLongPress?.invoke(down.position) == true) {
                // consume the children's click handling
                consumeUntilUp(pass = pass)
            }
        }

        if (upOrCancel != null) {
            // tap was successful.
            if (onTap?.invoke(upOrCancel.position) == true) {
                upOrCancel.consume()
            }
        }
    }
}

/**
 * @see [androidx.compose.foundation.gestures.awaitLongPressOrCancellation]
 */
private suspend fun AwaitPointerEventScope.awaitLongPressOrCancellation(
    pointerId: PointerId, pass: PointerEventPass
): PointerInputChange? {
    if (currentEvent.isPointerUp(pointerId)) {
        return null // The pointer has already been lifted, so the long press is cancelled.
    }

    val initialDown =
        currentEvent.changes.fastFirstOrNull { it.id == pointerId } ?: return null

    var longPress: PointerInputChange? = null
    var currentDown = initialDown
    val longPressTimeout = viewConfiguration.longPressTimeoutMillis
    return try {
        // wait for first tap up or long press
        withTimeout(longPressTimeout) {
            var finished = false
            while (!finished) {
                val event = awaitPointerEvent(pass)
                if (event.changes.fastAll { it.changedToUpIgnoreConsumed() }) {
                    // All pointers are up
                    finished = true
                }

                if (
                    event.changes.fastAny {
                        it.isConsumed || it.isOutOfBounds(size, extendedTouchPadding)
                    }
                ) {
                    finished = true // Canceled
                }

                // Check for cancel by position consumption. We can look on the Final pass of
                // the existing pointer event because it comes after the Main pass we checked
                // above.
                val consumeCheck = awaitPointerEvent(PointerEventPass.Final)
                if (consumeCheck.changes.fastAny { it.isConsumed }) {
                    finished = true
                }
                if (event.isPointerUp(currentDown.id)) {
                    val newPressed = event.changes.fastFirstOrNull { it.pressed }
                    if (newPressed != null) {
                        currentDown = newPressed
                        longPress = currentDown
                    } else {
                        // should technically never happen as we checked it above
                        finished = true
                    }
                    // Pointer (id) stayed down.
                } else {
                    longPress = event.changes.fastFirstOrNull { it.id == currentDown.id }
                }
            }
        }
        null
    } catch (_: PointerEventTimeoutCancellationException) {
        longPress ?: initialDown
    }
}

/**
 * @see [androidx.compose.foundation.gestures.isPointerUp]
 */
private fun PointerEvent.isPointerUp(pointerId: PointerId): Boolean =
    changes.fastFirstOrNull { it.id == pointerId }?.pressed != true

/**
 * @see [androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress]
 */
internal suspend fun PointerInputScope.detectDragGesturesAfterLongPress(
    onDragStart: (Offset) -> Boolean = { true },
    onDragEnd: () -> Unit = { },
    onDragCancel: () -> Unit = { },
    onDrag: (change: PointerInputChange, dragAmount: Offset) -> Unit
) {
    awaitEachGesture {
        val pass = PointerEventPass.Initial

        try {
            val down = awaitFirstDown(pass = pass)
            val drag = awaitLongPressOrCancellation(down.id, pass)
            if (drag != null) {
                if (!onDragStart(drag.position)) {
                    return@awaitEachGesture
                }
                val firstEventAfterLongPress = awaitPointerEvent(pass = pass)
                // Since a long press event has already been triggered here,
                // it is necessary for us to consume all subsequent events.
                firstEventAfterLongPress.changes.fastForEach {
                    if (it.changedToUpIgnoreConsumed()) it.consume()
                }

                if (
                    drag(drag.id) {
                        onDrag(it, it.positionChange())
                        it.consume()
                    }
                ) {
                    // consume up if we quit drag gracefully with the up
                    currentEvent.changes.fastForEach {
                        if (it.changedToUp()) it.consume()
                    }
                    onDragEnd()
                } else {
                    onDragCancel()
                }
            }
        } catch (c: CancellationException) {
            onDragCancel()
            throw c
        }
    }
}
