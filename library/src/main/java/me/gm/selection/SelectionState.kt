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

import androidx.compose.runtime.Composable
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.autoSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.compose.runtime.toMutableStateMap

/**
 * Creates a [KeySelectionState] that is remembered across compositions.
 *
 * Changes to the provided initial values will **not** result in the state being recreated or
 * changed in any way if it has already been created.
 *
 * @param selectedItemsSaver is used to save the data of selected items. You can use the built-in
 * [noOpSaver] or [viewModelSaver] to keep the implementation simple. Alternatively, you can use
 * [autoSaver] or a custom [Saver] to ensure data is not lost
 * @param initialSelection the initial value for [SelectionSupport.selection]
 */
@Composable
fun <V> rememberKeySelectionState(
    selectedItemsSaver: Saver<List<V>, Any> = dangingSaver(),
    initialSelection: Iterable<Pair<Any, V>> = emptyList(),
): KeySelectionState<V> {
    return rememberSaveable(
        saver = KeySelectionState.Saver(selectedItemsSaver)
    ) {
        KeySelectionState(initialSelection)
    }
}

/**
 * Creates a [IndexSelectionState] that is remembered across compositions.
 *
 * Changes to the provided initial values will **not** result in the state being recreated or
 * changed in any way if it has already been created.
 *
 * @param selectedItemsSaver is used to save the data of selected items. You can use the built-in
 * [noOpSaver] or [viewModelSaver] to keep the implementation simple. Alternatively, you can use
 * [autoSaver] or a custom [Saver] to ensure data is not lost
 * @param initialSelection the initial value for [SelectionSupport.selection]
 */
@Composable
fun <V> rememberIndexSelectionState(
    selectedItemsSaver: Saver<List<V>, Any> = noOpSaver(),
    initialSelection: Iterable<Pair<Int, V>> = emptyList(),
): IndexSelectionState<V> {
    return rememberSaveable(
        saver = IndexSelectionState.Saver(selectedItemsSaver)
    ) {
        IndexSelectionState(initialSelection)
    }
}

sealed interface SelectionState<K, V> {

    fun select(e: K, item: V): Boolean

    fun deselect(e: K): Boolean

    fun toggle(e: K, item: V): Boolean {
        if (!select(e, item)) {
            deselect(e)
            return false
        }
        return true
    }

    fun isSelected(e: K): Boolean

    fun selectedKeys(): List<K>

    fun clearSelection()

    fun selectedItemCount(): Int

    fun hasSelection(): Boolean = selectedItemCount() > 0

    fun selectedItems(): List<V>

    fun endSelection(): List<V> =
        try {
            selectedItems()
        } finally {
            clearSelection()
        }
}

abstract class SelectionSupport<K, V>(
    initialSelection: Iterable<Pair<K, V>>
) : SelectionState<K, V> {
    private val selection: SnapshotStateMap<K, V> = initialSelection.toMutableStateMap()

    final override fun select(e: K, item: V): Boolean = selection.put(e, item) == null

    final override fun deselect(e: K): Boolean = selection.remove(e) != null

    final override fun isSelected(e: K): Boolean = selection.containsKey(e)

    final override fun selectedKeys(): List<K> = selection.keys.toList()

    final override fun clearSelection() = selection.clear()

    final override fun selectedItemCount(): Int = selection.size

    final override fun selectedItems(): List<V> = selection.values.toList()
}

abstract class DangingKeysSupport<K, V>(
    initialSelection: Iterable<Pair<K, V>>,
    internal val dangingKeys: MutableList<K>
) : SelectionSupport<K, V>(initialSelection)

class KeySelectionState<V>(
    initialSelection: Iterable<Pair<Any, V>>,
    dangingKeys: MutableList<Any> = mutableListOf()
) : DangingKeysSupport<Any, V>(initialSelection, dangingKeys) {

    companion object {
        /** The default [Saver] for [KeySelectionState]. */
        fun <V> Saver(selectedItemsSaver: Saver<List<V>, Any>): Saver<KeySelectionState<V>, *> =
            Saver(
                save = { it.selectedKeys() to with(selectedItemsSaver) { save(it.selectedItems())!! } },
                restore = {
                    val selectedKeys = it.first
                    val selectedItems = selectedItemsSaver.restore(it.second)
                    if (selectedItemsSaver === DangingSaver) {
                        KeySelectionState(emptyList(), selectedKeys.toMutableList())
                    } else if (selectedKeys.size != selectedItems?.size) {
                        // Data cleared or corrupted, drop it.
                        KeySelectionState(emptyList())
                    } else {
                        KeySelectionState(selectedKeys.zip(selectedItems))
                    }
                }
            )
    }
}

class IndexSelectionState<V>(
    initialSelection: Iterable<Pair<Int, V>>
) : SelectionSupport<Int, V>(initialSelection) {

    companion object {
        /** The default [Saver] for [IndexSelectionState]. */
        fun <V> Saver(selectedItemsSaver: Saver<List<V>, Any>): Saver<IndexSelectionState<V>, *> =
            Saver(
                save = { it.selectedKeys() to with(selectedItemsSaver) { save(it.selectedItems())!! } },
                restore = {
                    val selectedKeys = it.first
                    val selectedItems = selectedItemsSaver.restore(it.second)
                    if (selectedKeys.size != selectedItems?.size) {
                        // Data cleared or corrupted, drop it.
                        IndexSelectionState(emptyList())
                    } else {
                        IndexSelectionState(selectedKeys.zip(selectedItems))
                    }
                }
            )
    }
}
