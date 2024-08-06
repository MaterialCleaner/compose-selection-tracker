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

import androidx.annotation.RestrictTo
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
 * @param autoDeselectMode automatically deselect removed keys when items change
 * @param initialSelection the initial value for [SelectionSupport.selection]
 * @param mutable control whether the [SelectionState] can be modified
 */
@Composable
fun <V> rememberKeySelectionState(
    saver: Saver<Pair<List<Any>, List<V>>, Any> = noOpSaver(),
    autoDeselectMode: AutoDeselectMode = AutoDeselectMode.Disabled,
    initialSelection: Iterable<Pair<Any, V>> = emptyList(),
    mutable: SelectionStateController<Any, V> = DefaultSelectionStateController()
): KeySelectionState<V> {
    return rememberSaveable(
        saver = KeySelectionState.Saver(saver, autoDeselectMode, mutable)
    ) {
        KeySelectionState(autoDeselectMode, initialSelection, mutable)
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
    mutable: SelectionStateController<Int, V> = DefaultSelectionStateController()
): IndexSelectionState<V> {
    return rememberSaveable(
        saver = IndexSelectionState.Saver(saver, mutable)
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

    fun selectedKeys(): Set<K>

    fun clearSelection()

    fun selectedItemCount(): Int

    fun hasSelection(): Boolean = selectedItemCount() > 0

    fun selectedItems(): Set<V>

    fun endSelection(): List<V> =
        try {
            // After clearSelection(), SnapshotMapValueSet will become empty,
            // so we create a copy here.
            selectedItems().toList()
        } finally {
            clearSelection()
        }

    fun selectableItemCount(): Int

    fun selectableContent(): Iterable<Pair<Any, V>>
}

abstract class SelectionSupport<K, V>(
    autoDeselectMode: AutoDeselectMode,
    initialSelection: Iterable<Pair<K, V>>,
    var mutable: SelectionStateController<K, V>
) : SelectionState<K, V> {
    private val selection: SnapshotStateMap<K, V> = initialSelection.toMutableStateMap()

    @delegate:RestrictTo(RestrictTo.Scope.LIBRARY)
    val selectableItemsContent: SelectableItemsIntervalContent<K, V> by lazy {
        SelectableItemsIntervalContent(this, autoDeselectMode)
    }

    final override fun select(e: K, item: V): Boolean =
        mutable.canSelect(e, item) && selection.put(e, item) == null

    final override fun deselect(e: K): Boolean =
        mutable.canDeselect(e) && selection.remove(e) != null

    final override fun isSelected(e: K): Boolean = selection.containsKey(e)

    final override fun selectedKeys(): Set<K> = selection.keys

    final override fun clearSelection() {
        if (mutable.canClearSelection()) {
            selection.clear()
        }
    }

    final override fun selectedItemCount(): Int = selection.size

    @Suppress("UNCHECKED_CAST")
    final override fun selectedItems(): Set<V> = selection.values as Set<V>

    final override fun selectableItemCount(): Int = selectableItemsContent.itemCount

    final override fun selectableContent(): Iterable<Pair<Any, V>> =
        selectableItemsContent.getSelectableContent()
}

abstract class DanglingKeysSupport<K, V>(
    autoDeselectMode: AutoDeselectMode,
    initialSelection: Iterable<Pair<K, V>>,
    mutable: SelectionStateController<K, V>,
    internal val danglingKeys: MutableList<K>
) : SelectionSupport<K, V>(autoDeselectMode, initialSelection, mutable)

class KeySelectionState<V>(
    autoDeselectMode: AutoDeselectMode = AutoDeselectMode.Disabled,
    initialSelection: Iterable<Pair<Any, V>> = emptyList(),
    mutable: SelectionStateController<Any, V> = DefaultSelectionStateController(),
    danglingKeys: MutableList<Any> = mutableListOf()
) : DanglingKeysSupport<Any, V>(autoDeselectMode, initialSelection, mutable, danglingKeys) {

    companion object {
        /** The default [Saver] for [KeySelectionState]. */
        fun <V> Saver(
            saver: Saver<Pair<List<Any>, List<V>>, Any>,
            autoDeselectMode: AutoDeselectMode,
            mutable: SelectionStateController<Any, V>
        ): Saver<KeySelectionState<V>, *> =
            Saver(
                save = {
                    with(saver) {
                        save(it.selectedKeys().toList() to it.selectedItems().toList())!!
                    }
                },
                restore = {
                    val (selectedKeys, selectedItems) = saver.restore(it)!!
                    if (saver === DanglingSaver) {
                        KeySelectionState(
                            autoDeselectMode = autoDeselectMode,
                            mutable = mutable,
                            danglingKeys = selectedKeys.toMutableList()
                        )
                    } else if (selectedKeys.size != selectedItems.size) {
                        // Data cleared or corrupted, drop it.
                        KeySelectionState(
                            autoDeselectMode = autoDeselectMode,
                            mutable = mutable
                        )
                    } else {
                        KeySelectionState(
                            autoDeselectMode = autoDeselectMode,
                            initialSelection = selectedKeys.zip(selectedItems),
                            mutable = mutable
                        )
                    }
                }
            )
    }
}

class IndexSelectionState<V>(
    initialSelection: Iterable<Pair<Int, V>> = emptyList(),
    mutable: SelectionStateController<Int, V> = DefaultSelectionStateController()
) : SelectionSupport<Int, V>(AutoDeselectMode.Disabled, initialSelection, mutable) {

    companion object {
        /** The default [Saver] for [IndexSelectionState]. */
        fun <V> Saver(
            saver: Saver<Pair<List<Int>, List<V>>, Any>,
            mutable: SelectionStateController<Int, V>
        ): Saver<IndexSelectionState<V>, *> =
            Saver(
                save = {
                    with(saver) {
                        save(it.selectedKeys().toList() to it.selectedItems().toList())!!
                    }
                },
                restore = {
                    val (selectedKeys, selectedItems) = saver.restore(it)!!
                    if (selectedKeys.size != selectedItems.size) {
                        // Data cleared or corrupted, drop it.
                        IndexSelectionState(
                            mutable = mutable
                        )
                    } else {
                        IndexSelectionState(
                            initialSelection = selectedKeys.zip(selectedItems),
                            mutable = mutable
                        )
                    }
                }
            )
    }
}
