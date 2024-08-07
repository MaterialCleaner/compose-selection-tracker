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

// Should NOT add "this then "! This lint is not true!
@file:SuppressLint("ModifierFactoryUnreferencedReceiver")

package me.gm.selection.grid

import android.annotation.SuppressLint
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.lazy.grid.LazyGridItemInfo
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import me.gm.selection.DetailsLookup
import me.gm.selection.IndexSelectionState
import me.gm.selection.KeySelectionState
import me.gm.selection.RangeHelper
import me.gm.selection.SelectionState
import me.gm.selection.SelectionSupport
import me.gm.selection.detectDragGesturesAfterLongPress
import me.gm.selection.detectTapGestures

abstract class LazyGridDetailsLookup<V> : DetailsLookup<LazyGridItemInfo, V>

private fun <K> SelectionState<K, *>.key(touchedItem: LazyGridItemInfo): K =
    when (this) {
        is KeySelectionState -> touchedItem.key
        is IndexSelectionState -> touchedItem.index
        else -> throw IllegalArgumentException()
    } as K

private class FullyInteractiveLazyGridDetailsLookup<V>(
    private val selectionState: SelectionState<Any, V>,
    private val enabled: (LazyGridItemInfo) -> Boolean,
) : LazyGridDetailsLookup<V>() {

    override fun getItem(itemInfo: LazyGridItemInfo): V? =
        if (enabled(itemInfo)) {
            (selectionState as? SelectionSupport)?.selectableItemsContent?.getItemForKey(
                selectionState.key(itemInfo)
            )
        } else {
            null
        }
}

private fun touchInfo(
    gridState: LazyGridState,
    touchPosition: Offset,
): Pair<LazyGridItemInfo, Offset>? {
    val offset = if (gridState.layoutInfo.orientation == Orientation.Vertical) {
        Offset(touchPosition.x, gridState.layoutInfo.viewportStartOffset + touchPosition.y)
    } else {
        Offset(gridState.layoutInfo.viewportStartOffset + touchPosition.x, touchPosition.y)
    }
    val touchedItem = gridState.layoutInfo.visibleItemsInfo.find { itemInfo ->
        offset.x.toInt() in itemInfo.offset.x until (itemInfo.offset.x + itemInfo.size.width) &&
                offset.y.toInt() in itemInfo.offset.y until (itemInfo.offset.y + itemInfo.size.height)
    } ?: return null
    val itemOffset = Offset(offset.x - touchedItem.offset.x, offset.y - touchedItem.offset.y)
    return touchedItem to itemOffset
}

private fun <V> itemDetails(
    gridState: LazyGridState,
    touchPosition: Offset,
    detailsLookup: LazyGridDetailsLookup<V>,
): Pair<LazyGridItemInfo, V>? {
    val (touchedItem, touchOffset) = touchInfo(gridState, touchPosition)
        ?: return null
    if (detailsLookup.inPressRegion(touchedItem, touchOffset)) {
        val item = detailsLookup.getItem(touchedItem) ?: return null
        return touchedItem to item
    }
    return null
}

fun <V> Modifier.longPressToToggleGesture(
    gridState: LazyGridState,
    selectionState: SelectionState<Any, V>,
    detailsLookup: LazyGridDetailsLookup<V>,
): Modifier = composed {
    @Suppress("NAME_SHADOWING") val gridState by rememberUpdatedState(gridState)
    pointerInput(Unit) {
        detectTapGestures(
            onLongPress = { offset ->
                val (touchedItem, item) = itemDetails(gridState, offset, detailsLookup)
                    ?: return@detectTapGestures false
                selectionState.toggle(selectionState.key(touchedItem), item)
            }
        )
    }
}

fun <V> Modifier.longPressToToggleGesture(
    gridState: LazyGridState,
    selectionState: SelectionState<Any, V>,
    enabled: (LazyGridItemInfo) -> Boolean = { true },
): Modifier = longPressToToggleGesture(
    gridState,
    selectionState,
    FullyInteractiveLazyGridDetailsLookup(selectionState, enabled)
)

fun <V> Modifier.tapInActionModeToToggleGesture(
    gridState: LazyGridState,
    selectionState: SelectionState<Any, V>,
    detailsLookup: LazyGridDetailsLookup<V>,
): Modifier = composed {
    @Suppress("NAME_SHADOWING") val gridState by rememberUpdatedState(gridState)
    pointerInput(selectionState.hasSelection()) {
        if (!selectionState.hasSelection()) {
            return@pointerInput
        }
        detectTapGestures(
            onTap = { offset ->
                val (touchedItem, item) = itemDetails(gridState, offset, detailsLookup)
                    ?: return@detectTapGestures false
                selectionState.toggle(selectionState.key(touchedItem), item)
                return@detectTapGestures true
            }
        )
    }
}

fun <V> Modifier.tapInActionModeToToggleGesture(
    gridState: LazyGridState,
    selectionState: SelectionState<Any, V>,
    enabled: (LazyGridItemInfo) -> Boolean = { true },
): Modifier = tapInActionModeToToggleGesture(
    gridState,
    selectionState,
    FullyInteractiveLazyGridDetailsLookup(selectionState, enabled)
)

private class RangeSupport<V>(
    private val gridState: LazyGridState,
    private val detailsLookup: LazyGridDetailsLookup<V>,
    private val selectionState: SelectionState<Any, V>,
    private val anchor: Int,
    initialPosition: Offset,
    val scroller: AutoScroller = AutoScroller(gridState, initialPosition),
) {
    private val initialSelection: Set<Any?> = selectionState.selectedKeys().toSet()
    private var extend: Int by mutableIntStateOf(anchor)

    private fun range(): IntRange {
        return RangeHelper.buildRange(anchor, extend)
    }

    private fun lookupItemInfoForIndex(index: Int): LazyGridItemInfo? {
        val itemInfoIndex = gridState.layoutInfo.visibleItemsInfo.binarySearchBy(index) { it.index }
        return if (itemInfoIndex >= 0) gridState.layoutInfo.visibleItemsInfo[itemInfoIndex] else null
    }

    fun extendRange(extendToItem: LazyGridItemInfo) {
        val oldRange = range()
        extend = extendToItem.index
        val newRange = range()

        val removed = oldRange - newRange
        removed.forEach { index ->
            val itemInfo = lookupItemInfoForIndex(index) ?: return@forEach
            if (!initialSelection.contains(selectionState.key(itemInfo))) {
                selectionState.deselect(selectionState.key(itemInfo))
            }
        }

        val added = newRange - oldRange
        added.forEach { index ->
            val itemInfo =
                if (index == extendToItem.index) extendToItem
                else lookupItemInfoForIndex(index) ?: return@forEach
            val item = detailsLookup.getItem(itemInfo) ?: return@forEach
            selectionState.select(selectionState.key(itemInfo), item)
        }
    }
}

/**
 * Note that this cannot be used together with [longPressToToggleGesture].
 */
fun <V> Modifier.dragAfterLongPressToSelectGesture(
    gridState: LazyGridState,
    selectionState: SelectionState<Any, V>,
    detailsLookup: LazyGridDetailsLookup<V>,
): Modifier = composed {
    @Suppress("NAME_SHADOWING") val gridState by rememberUpdatedState(gridState)
    pointerInput(Unit) {
        coroutineScope {
            var rangeSupport: RangeSupport<V>? = null
            detectDragGesturesAfterLongPress(
                onDragStart = onDragStart@{ offset ->
                    val (touchedItem, item) = itemDetails(gridState, offset, detailsLookup)
                        ?: return@onDragStart false
                    val selected = selectionState.toggle(selectionState.key(touchedItem), item)
                    if (selected) {
                        rangeSupport = RangeSupport(
                            gridState, detailsLookup, selectionState, touchedItem.index, offset
                        )
                    }
                    return@onDragStart selected
                },
                onDragEnd = {
                    rangeSupport?.scroller?.stopScroll()
                    rangeSupport = null
                },
                onDragCancel = {
                    rangeSupport?.scroller?.stopScroll()
                    rangeSupport = null
                },
            ) { change, _ ->
                @Suppress("NAME_SHADOWING")
                val rangeSupport = rangeSupport
                rangeSupport ?: return@detectDragGesturesAfterLongPress
                val onPositionChange = onPositionChange@{
                    val (touchedItem, touchOffset) = touchInfo(gridState, change.position)
                        ?: return@onPositionChange
                    if (detailsLookup.inDragRegion(touchedItem, touchOffset)) {
                        rangeSupport.extendRange(touchedItem)
                    }
                }
                launch {
                    rangeSupport.scroller.computeScroll(change.position, onPositionChange)
                }
                onPositionChange()
            }
        }
    }
}

fun <V> Modifier.dragAfterLongPressToSelectGesture(
    gridState: LazyGridState,
    selectionState: SelectionState<Any, V>,
    enabled: (LazyGridItemInfo) -> Boolean = { true },
): Modifier = dragAfterLongPressToSelectGesture(
    gridState,
    selectionState,
    FullyInteractiveLazyGridDetailsLookup(selectionState, enabled)
)
