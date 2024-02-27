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

package me.gm.selection.list

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import me.gm.selection.grid.ScrollMode

/*
 * For an unreversed LazyColumn:
 * ==============================
 *          FastBackward
 * ==============================
 *        ModerateBackward
 * ==============================
 *
 *
 *
 *              Idle
 *
 *
 *
 * ==============================
 *        ModerateForward
 * ==============================
 *          FastForward
 * ==============================
 */
enum class ScrollMode {
    FastBackward, ModerateBackward, Idle, ModerateForward, FastForward
}

/**
 * Provides support for auto-scrolling a LazyList.
 *
 * @hide
 */
class AutoScroller(
    private val listState: LazyListState,
    initialPosition: Offset,
    private val scrollThresholdRatio: Float = 0.2F,
    private val fastScrollThresholdRatio: Float = scrollThresholdRatio / 2,
    private val moderateScrollStep: Float = 500F,
    private val fastScrollStep: Float = moderateScrollStep * 2,
) {
    /**
     * Setting the initial [ScrollMode] is to ensure that scrolling is never triggered within the
     * initially pressed area. This can prevent triggering [ScrollMode.FastBackward] when dragging
     * from the starting side of the [androidx.compose.foundation.lazy.LazyList] to the other side,
     * and vice versa.
     */
    private var scrollMode: ScrollMode by mutableStateOf(scrollModeForPosition(initialPosition))

    private fun scrollModeForOffset(offset: Float): ScrollMode {
        with(listState.layoutInfo) {
            val size = viewportStartOffset + viewportEndOffset
            if (offset <= beforeContentPadding + size * scrollThresholdRatio) {
                if (offset <= beforeContentPadding + size * fastScrollThresholdRatio) {
                    return ScrollMode.FastBackward
                }
                return ScrollMode.ModerateBackward
            }
            if (offset >= viewportEndOffset - size * scrollThresholdRatio) {
                if (offset >= viewportEndOffset - size * fastScrollThresholdRatio) {
                    return ScrollMode.FastForward
                }
                return ScrollMode.ModerateForward
            }
            return ScrollMode.Idle
        }
    }

    private fun scrollModeForPosition(position: Offset): ScrollMode {
        val offset =
            if (listState.layoutInfo.orientation == Orientation.Vertical) {
                position.y
            } else {
                position.x
            }
        return scrollModeForOffset(offset)
    }

    private var scrollJob: Job? = null
    suspend fun computeScroll(position: Offset, onPositionChange: () -> Unit) {
        val newMode = scrollModeForPosition(position)
        if (newMode == scrollMode) {
            return
        }
        scrollJob?.cancel()
        scrollMode = newMode
        if (newMode == ScrollMode.Idle) {
            return
        }
        coroutineScope {
            scrollJob = launch {
                while (isActive) {
                    val value = when (newMode) {
                        ScrollMode.FastBackward -> -fastScrollStep
                        ScrollMode.ModerateBackward -> -moderateScrollStep
                        ScrollMode.ModerateForward -> moderateScrollStep
                        ScrollMode.FastForward -> fastScrollStep
                        else -> throw IllegalStateException("should not happen")
                    }
                    listState.animateScrollBy(value, tween(easing = LinearEasing))
                    onPositionChange()
                }
            }
        }
    }

    fun stopScroll() {
        scrollJob?.cancel()
        scrollJob = null
    }
}
