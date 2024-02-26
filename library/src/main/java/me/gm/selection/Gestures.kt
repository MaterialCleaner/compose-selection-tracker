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

package me.gm.selection

import android.annotation.SuppressLint
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.lazy.LazyListItemInfo
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

abstract class DetailsLookup<T> {

    /**
     * The Selection library calls this function when it needs the item
     * that meets specific criteria.
     */
    abstract fun getItem(itemInfo: LazyListItemInfo): T?

    fun inPressRegion(itemInfo: LazyListItemInfo, position: Offset): Boolean = true

    /**
     * "Item Drag Region" identifies areas of an item that are not considered when the library
     * evaluates whether or not to initiate band-selection for mouse input. The drag region
     * will usually correspond to an area of an item that represents user visible content.
     * Mouse driven band selection operations are only ever initiated in non-drag-regions.
     * This is a consideration as many layouts may not include empty space between
     * RecyclerView items where band selection can be initiated.
     *
     *
     *
     * For example. You may present a single column list of contact names in a
     * RecyclerView instance in which the individual view items expand to fill all
     * available space.
     * But within the expanded view item after the contact name there may be empty space that a
     * user would reasonably expect to initiate band selection. When a MotionEvent occurs
     * in such an area, you should return identify this as NOT in a drag region.
     *
     *
     *
     * Further more, within a drag region, a mouse click and drag will immediately
     * initiate drag and drop (if supported by your configuration).
     *
     * @return true if the item is in an area of the item that can result in dragging
     * the item. List items frequently have a white area that is not draggable allowing
     * mouse driven band selection to be initiated in that area.
     */
    fun inDragRegion(itemInfo: LazyListItemInfo, position: Offset): Boolean = true
}

private fun touchInfo(
    listState: LazyListState,
    offset: Offset,
): Pair<LazyListItemInfo, Offset>? {
    val touchedItem = listState.layoutInfo.visibleItemsInfo.find { itemInfo ->
        if (listState.layoutInfo.orientation == Orientation.Vertical) {
            (offset.y + listState.layoutInfo.viewportStartOffset).toInt() in
                    itemInfo.offset until (itemInfo.offset + itemInfo.size)
        } else {
            (offset.x + listState.layoutInfo.viewportStartOffset).toInt() in
                    itemInfo.offset until (itemInfo.offset + itemInfo.size)
        }
    } ?: return null
    val touchOffset =
        if (listState.layoutInfo.orientation == Orientation.Vertical) {
            Offset(
                offset.x,
                offset.y + listState.layoutInfo.viewportStartOffset - touchedItem.offset
            )
        } else {
            Offset(
                offset.x + listState.layoutInfo.viewportStartOffset - touchedItem.offset,
                offset.y
            )
        }
    return touchedItem to touchOffset
}

private fun <T> itemDetails(
    listState: LazyListState,
    offset: Offset,
    detailsLookup: DetailsLookup<T>,
): Pair<LazyListItemInfo, T>? {
    val (touchedItem, touchOffset) = touchInfo(listState, offset)
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

fun <T> Modifier.longPressToToggleGesture(
    listState: LazyListState,
    selectionState: SelectionState<Any, T>,
    detailsLookup: DetailsLookup<T>,
): Modifier = pointerInput(Unit) {
    detectTapGestures(
        onLongPress = { offset ->
            val (touchedItem, item) = itemDetails(listState, offset, detailsLookup)
                ?: return@detectTapGestures false
            selectionState.toggle(selectionState.key(touchedItem), item)
        }
    )
}

fun <T> Modifier.tapInActionModeToToggleGesture(
    listState: LazyListState,
    selectionState: SelectionState<Any, T>,
    detailsLookup: DetailsLookup<T>,
): Modifier = pointerInput(selectionState.hasSelection()) {
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

private class RangeSupport<T>(
    private val listState: LazyListState,
    private val detailsLookup: DetailsLookup<T>,
    private val selectionState: SelectionState<Any, T>,
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
fun <T> Modifier.dragAfterLongPressToSelectGesture(
    listState: LazyListState,
    selectionState: SelectionState<Any, T>,
    detailsLookup: DetailsLookup<T>,
): Modifier = pointerInput(Unit) {
    coroutineScope {
        var rangeSupport: RangeSupport<T>? = null
        detectDragGesturesAfterLongPress(
            onDragStart = onDragStart@{ offset ->
                val (touchedItem, item) = itemDetails(listState, offset, detailsLookup)
                    ?: return@onDragStart
                val selected = selectionState.toggle(selectionState.key(touchedItem), item)
                if (selected) {
                    rangeSupport = RangeSupport(
                        listState, detailsLookup, selectionState, touchedItem.index, offset
                    )
                }
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
