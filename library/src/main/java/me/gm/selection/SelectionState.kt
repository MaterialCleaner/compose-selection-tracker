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
 * An interface used to control whether the [SelectionState] can be modified.
 *
 * @return `true` to allow modification; otherwise, do not allow it.
 */
interface SelectionStateController<K, V> {

    fun canSelect(e: K, item: V): Boolean

    fun canDeselect(e: K): Boolean

    fun canClearSelection(): Boolean
}

/**
 * The default [SelectionStateController] always allows modification.
 */
private class DefaultSelectionStateController<K, V> : SelectionStateController<K, V> {

    override fun canSelect(e: K, item: V): Boolean = true

    override fun canDeselect(e: K): Boolean = true

    override fun canClearSelection(): Boolean = true
}

/**
 * Creates a [KeySelectionState] that is remembered across compositions.
 *
 * Changes to the provided initial values will **not** result in the state being recreated or
 * changed in any way if it has already been created.
 *
 * @param saver is used to save the data of selected items. You can use the built-in
 * [noOpSaver] or [danglingSaver] to keep the implementation simple. Alternatively, you can use
 * [autoSaver] or a custom [Saver] to ensure data is not lost
 * @param initialSelection the initial value for [SelectionSupport.selection]
 * @param mutable control whether the [SelectionState] can be modified
 */
@Composable
fun <V> rememberKeySelectionState(
    saver: Saver<Pair<List<Any>, List<V>>, Any> = noOpSaver(),
    initialSelection: Iterable<Pair<Any, V>> = emptyList(),
    mutable: SelectionStateController<Any, V> = DefaultSelectionStateController(),
): KeySelectionState<V> {
    return rememberSaveable(
        saver = KeySelectionState.Saver(saver)
    ) {
        KeySelectionState(initialSelection, mutable)
    }
}

/**
 * Creates a [IndexSelectionState] that is remembered across compositions.
 *
 * Changes to the provided initial values will **not** result in the state being recreated or
 * changed in any way if it has already been created.
 *
 * @param saver is used to save the data of selected items. You can use the built-in
 * [noOpSaver] or [danglingSaver] to keep the implementation simple. Alternatively, you can use
 * [autoSaver] or a custom [Saver] to ensure data is not lost
 * @param initialSelection the initial value for [SelectionSupport.selection]
 * @param mutable control whether the [SelectionState] can be modified
 */
@Composable
fun <V> rememberIndexSelectionState(
    saver: Saver<Pair<List<Int>, List<V>>, Any> = noOpSaver(),
    initialSelection: Iterable<Pair<Int, V>> = emptyList(),
    mutable: SelectionStateController<Int, V> = DefaultSelectionStateController(),
): IndexSelectionState<V> {
    return rememberSaveable(
        saver = IndexSelectionState.Saver(saver)
    ) {
        IndexSelectionState(initialSelection, mutable)
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
    initialSelection: Iterable<Pair<K, V>>,
    var mutable: SelectionStateController<K, V>,
) : SelectionState<K, V> {
    private val selection: SnapshotStateMap<K, V> = initialSelection.toMutableStateMap()

    final override fun select(e: K, item: V): Boolean =
        mutable.canSelect(e, item) && selection.put(e, item) == null

    final override fun deselect(e: K): Boolean =
        mutable.canDeselect(e) && selection.remove(e) != null

    final override fun isSelected(e: K): Boolean = selection.containsKey(e)

    final override fun selectedKeys(): List<K> = selection.keys.toList()

    final override fun clearSelection() {
        if (mutable.canClearSelection()) {
            selection.clear()
        }
    }

    final override fun selectedItemCount(): Int = selection.size

    final override fun selectedItems(): List<V> = selection.values.toList()
}

abstract class DanglingKeysSupport<K, V>(
    initialSelection: Iterable<Pair<K, V>>,
    mutable: SelectionStateController<K, V>,
    internal val danglingKeys: MutableList<K>
) : SelectionSupport<K, V>(initialSelection, mutable)

class KeySelectionState<V>(
    initialSelection: Iterable<Pair<Any, V>> = emptyList(),
    mutable: SelectionStateController<Any, V> = DefaultSelectionStateController(),
    danglingKeys: MutableList<Any> = mutableListOf()
) : DanglingKeysSupport<Any, V>(initialSelection, mutable, danglingKeys) {

    companion object {
        /** The default [Saver] for [KeySelectionState]. */
        fun <V> Saver(saver: Saver<Pair<List<Any>, List<V>>, Any>): Saver<KeySelectionState<V>, *> =
            Saver(
                save = { with(saver) { save(it.selectedKeys() to it.selectedItems())!! } },
                restore = {
                    val (selectedKeys, selectedItems) = saver.restore(it)!!
                    if (saver === DanglingSaver) {
                        KeySelectionState(danglingKeys = selectedKeys.toMutableList())
                    } else if (selectedKeys.size != selectedItems.size) {
                        // Data cleared or corrupted, drop it.
                        KeySelectionState()
                    } else {
                        KeySelectionState(initialSelection = selectedKeys.zip(selectedItems))
                    }
                }
            )
    }
}

class IndexSelectionState<V>(
    initialSelection: Iterable<Pair<Int, V>> = emptyList(),
    mutable: SelectionStateController<Int, V> = DefaultSelectionStateController(),
) : SelectionSupport<Int, V>(initialSelection, mutable) {

    companion object {
        /** The default [Saver] for [IndexSelectionState]. */
        fun <V> Saver(saver: Saver<Pair<List<Int>, List<V>>, Any>): Saver<IndexSelectionState<V>, *> =
            Saver(
                save = { with(saver) { save(it.selectedKeys() to it.selectedItems())!! } },
                restore = {
                    val (selectedKeys, selectedItems) = saver.restore(it)!!
                    if (selectedKeys.size != selectedItems.size) {
                        // Data cleared or corrupted, drop it.
                        IndexSelectionState()
                    } else {
                        IndexSelectionState(initialSelection = selectedKeys.zip(selectedItems))
                    }
                }
            )
    }
}
