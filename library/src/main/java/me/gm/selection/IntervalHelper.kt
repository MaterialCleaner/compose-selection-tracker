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

class IntervalHelper<T>(
    internal val state: SelectionState<Any, T>,
    internal val index: Int,
    internal val itemProvider: (Int) -> T,
    private val keyProvider: ((Int) -> Any)?,
) {

    init {
        if (keyProvider != null) {
            state as KeySelectionState
        } else {
            state as IndexSelectionState
        }
    }

    internal fun key(indexInThisInterval: Int): Any =
        keyProvider?.invoke(indexInThisInterval) ?: indexInThisInterval

    fun selectThis(): Boolean = state.select(key(index), itemProvider(index)!!)

    fun deselectThis(): Boolean = state.deselect(key(index))

    fun toggleThis(): Boolean = state.toggle(key(index), itemProvider(index)!!)

    fun isThisSelected(): Boolean = state.isSelected(key(index))

    fun selectedKeys(): Set<Any> = state.selectedKeys()

    fun clearSelection() = state.clearSelection()

    fun selectedItemCount(): Int = state.selectedItemCount()

    fun hasSelection(): Boolean = state.hasSelection()

    fun selectedItems(): Set<T> = state.selectedItems()

    fun endSelection(): List<T> = state.endSelection()
}

// TODO: Is it worth adding a caching mechanism here?
fun <T> IntervalHelper<T>.lookupIndexForKey(
    key: Any,
): Int {
    for (index in 0 until Int.MAX_VALUE) {
        try {
            if (key == key(index)) {
                return index
            }
        } catch (e: Exception) {
            return -1
        }
    }
    return -1
}

private fun <T> IntervalHelper<T>.lookupItemForKey(
    key: Any,
): T = itemProvider(lookupIndexForKey(key))

fun <T> IntervalHelper<T>.selectIndex(
    indexInThisInterval: Int,
): Boolean = state.select(key(indexInThisInterval), itemProvider(indexInThisInterval)!!)

fun <T> IntervalHelper<T>.selectKey(
    key: Any,
): Boolean = (state as KeySelectionState).select(key, lookupItemForKey(key)!!)

fun <T> IntervalHelper<T>.deselectIndex(
    indexInThisInterval: Int,
): Boolean = state.deselect(key(indexInThisInterval))

fun <T> IntervalHelper<T>.deselectKey(
    key: Any,
): Boolean = (state as KeySelectionState).deselect(key)

fun <T> IntervalHelper<T>.toggleIndex(
    indexInThisInterval: Int,
): Boolean = state.toggle(key(indexInThisInterval), itemProvider(indexInThisInterval)!!)

fun <T> IntervalHelper<T>.toggleKey(
    key: Any,
): Boolean = (state as KeySelectionState).toggle(key, lookupItemForKey(key)!!)

fun <T> IntervalHelper<T>.isIndexSelected(
    indexInThisInterval: Int,
): Boolean = state.isSelected(key(indexInThisInterval))

fun <T> IntervalHelper<T>.isKeySelected(
    key: Any,
): Boolean = (state as KeySelectionState).isSelected(key)
