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

package me.gm.selection.list

import android.annotation.SuppressLint
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.lazy.LazyListItemInfo
import androidx.compose.foundation.lazy.LazyListState
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
import me.gm.selection.IndexItemMap
import me.gm.selection.IndexSelectionState
import me.gm.selection.KeyIndexItemMap
import me.gm.selection.KeyItemMap
import me.gm.selection.KeySelectionState
import me.gm.selection.RangeHelper
import me.gm.selection.SelectionState
import me.gm.selection.detectDragGesturesAfterLongPress
import me.gm.selection.detectTapGestures

abstract class LazyListDetailsLookup<V> : DetailsLookup<LazyListItemInfo, V>

private class FullyInteractiveLazyListDetailsLookup<V>(
    private val map: () -> KeyIndexItemMap<Any, V>,
    private val key: (itemInfo: LazyListItemInfo) -> Any = when (map()) {
        is KeyItemMap -> { itemInfo -> itemInfo.key }
        is IndexItemMap -> { itemInfo -> itemInfo.index }
    }
) : LazyListDetailsLookup<V>() {

    override fun getItem(itemInfo: LazyListItemInfo): V? = map().getItem(key(itemInfo))
}

private fun touchInfo(
    listState: LazyListState,
    touchPosition: Offset,
): Pair<LazyListItemInfo, Offset>? {
    val offset = if (listState.layoutInfo.orientation == Orientation.Vertical) {
        listState.layoutInfo.viewportStartOffset + touchPosition.y
    } else {
        listState.layoutInfo.viewportStartOffset + touchPosition.x
    }
    val touchedItem = listState.layoutInfo.visibleItemsInfo.find { itemInfo ->
        offset.toInt() in itemInfo.offset until (itemInfo.offset + itemInfo.size)
    } ?: return null
    val itemOffset = if (listState.layoutInfo.orientation == Orientation.Vertical) {
        Offset(touchPosition.x, offset - touchedItem.offset)
    } else {
        Offset(offset - touchedItem.offset, touchPosition.y)
    }
    return touchedItem to itemOffset
}

private fun <V> itemDetails(
    listState: LazyListState,
    touchPosition: Offset,
    detailsLookup: LazyListDetailsLookup<V>,
): Pair<LazyListItemInfo, V>? {
    val (touchedItem, touchOffset) = touchInfo(listState, touchPosition)
        ?: return null
    if (detailsLookup.inPressRegion(touchedItem, touchOffset)) {
        val item = detailsLookup.getItem(touchedItem) ?: return null
        return touchedItem to item
    }
    return null
}

private fun <K> SelectionState<K, *>.key(touchedItem: LazyListItemInfo): K =
    when (this) {
        is KeySelectionState -> touchedItem.key
        is IndexSelectionState -> touchedItem.index
        else -> throw IllegalArgumentException()
    } as K

fun <V> Modifier.longPressToToggleGesture(
    listState: LazyListState,
    selectionState: SelectionState<Any, V>,
    detailsLookup: LazyListDetailsLookup<V>,
): Modifier = composed {
    @Suppress("NAME_SHADOWING") val listState by rememberUpdatedState(listState)
    pointerInput(Unit) {
        detectTapGestures(
            onLongPress = { offset ->
                val (touchedItem, item) = itemDetails(listState, offset, detailsLookup)
                    ?: return@detectTapGestures false
                selectionState.toggle(selectionState.key(touchedItem), item)
            }
        )
    }
}

fun <V> Modifier.longPressToToggleGesture(
    listState: LazyListState,
    selectionState: SelectionState<Any, V>,
    map: () -> KeyIndexItemMap<Any, V>,
): Modifier = longPressToToggleGesture(
    listState,
    selectionState,
    FullyInteractiveLazyListDetailsLookup(map)
)

fun <V> Modifier.tapInActionModeToToggleGesture(
    listState: LazyListState,
    selectionState: SelectionState<Any, V>,
    detailsLookup: LazyListDetailsLookup<V>,
): Modifier = composed {
    @Suppress("NAME_SHADOWING") val listState by rememberUpdatedState(listState)
    pointerInput(selectionState.hasSelection()) {
        if (!selectionState.hasSelection()) {
            return@pointerInput
        }
        detectTapGestures(
            onTap = { offset ->
                val (touchedItem, item) = itemDetails(listState, offset, detailsLookup)
                    ?: return@detectTapGestures false
                selectionState.toggle(selectionState.key(touchedItem), item)
                return@detectTapGestures true
            }
        )
    }
}

fun <V> Modifier.tapInActionModeToToggleGesture(
    listState: LazyListState,
    selectionState: SelectionState<Any, V>,
    map: () -> KeyIndexItemMap<Any, V>,
): Modifier = tapInActionModeToToggleGesture(
    listState,
    selectionState,
    FullyInteractiveLazyListDetailsLookup(map)
)

private class RangeSupport<V>(
    private val listState: LazyListState,
    private val detailsLookup: LazyListDetailsLookup<V>,
    private val selectionState: SelectionState<Any, V>,
    private val anchor: Int,
    initialPosition: Offset,
    val scroller: AutoScroller = AutoScroller(listState, initialPosition),
) {
    private val initialSelection: Set<Any?> = selectionState.selectedKeys().toSet()
    private var extend: Int by mutableIntStateOf(anchor)

    private fun range(): IntRange {
        return RangeHelper.buildRange(anchor, extend)
    }

    private fun lookupItemInfoForIndex(index: Int): LazyListItemInfo? {
        val itemInfoIndex = listState.layoutInfo.visibleItemsInfo.binarySearchBy(index) { it.index }
        return if (itemInfoIndex >= 0) listState.layoutInfo.visibleItemsInfo[itemInfoIndex] else null
    }

    fun extendRange(extendToItem: LazyListItemInfo) {
        val oldRange = range()
        extend = extendToItem.index
        val newRange = range()

        val removed = oldRange - newRange
        if (removed.isNotEmpty()) {
            removed.forEach { index ->
                val itemInfo = lookupItemInfoForIndex(index) ?: return@forEach
                if (!initialSelection.contains(selectionState.key(itemInfo))) {
                    selectionState.deselect(selectionState.key(itemInfo))
                }
            }
        } else {
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
}

/**
 * Note that this cannot be used together with [longPressToToggleGesture].
 */
fun <V> Modifier.dragAfterLongPressToSelectGesture(
    listState: LazyListState,
    selectionState: SelectionState<Any, V>,
    detailsLookup: LazyListDetailsLookup<V>,
): Modifier = composed {
    @Suppress("NAME_SHADOWING") val listState by rememberUpdatedState(listState)
    pointerInput(Unit) {
        coroutineScope {
            var rangeSupport: RangeSupport<V>? = null
            detectDragGesturesAfterLongPress(
                onDragStart = onDragStart@{ offset ->
                    val (touchedItem, item) = itemDetails(listState, offset, detailsLookup)
                        ?: return@onDragStart false
                    val selected = selectionState.toggle(selectionState.key(touchedItem), item)
                    if (selected) {
                        rangeSupport = RangeSupport(
                            listState, detailsLookup, selectionState, touchedItem.index, offset
                        )
                    }
                    return@onDragStart true
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
                    val (touchedItem, touchOffset) = touchInfo(listState, change.position)
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
    listState: LazyListState,
    selectionState: SelectionState<Any, V>,
    map: () -> KeyIndexItemMap<Any, V>,
): Modifier = dragAfterLongPressToSelectGesture(
    listState,
    selectionState,
    FullyInteractiveLazyListDetailsLookup(map)
)
