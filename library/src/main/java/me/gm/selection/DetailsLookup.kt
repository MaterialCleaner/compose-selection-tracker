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

import androidx.compose.ui.geometry.Offset

internal interface DetailsLookup<K, V> {

    /**
     * The Selection library calls this function when it needs the item
     * that meets specific criteria.
     */
    fun getItem(itemInfo: K): V?

    fun inPressRegion(itemInfo: K, position: Offset): Boolean = true

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
    fun inDragRegion(itemInfo: K, position: Offset): Boolean = true
}
