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

import android.util.Log
import androidx.collection.MutableScatterMap
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.lazy.layout.MutableIntervalList
import androidx.compose.foundation.lazy.layout.getDefaultLazyLayoutKey

@OptIn(ExperimentalFoundationApi::class)
class SelectableItemsIntervalContent<K, V>(
    private val selectionSupport: SelectionSupport<K, V>,
    private val autoDeselectMode: AutoDeselectMode
) {
    private var lazyDslScope: Any? = null
    private var intervals: MutableIntervalList<SelectableItemsInterval<V>> = MutableIntervalList()
    private val map: MutableScatterMap<Any, V> = MutableScatterMap()

    fun updateInterval(
        lazyDslScope: Any?,
        count: Int,
        item: (Int) -> V,
        key: ((index: Int) -> Any)?
    ) {
        fun deselectForNewKeys(newKeys: Set<K>) {
            var deselectedItemCount = 0
            selectionSupport.selectedKeys()
                .filterNot { it in newKeys }
                .forEach { key ->
                    deselectedItemCount++
                    selectionSupport.deselect(key)
                }
            if (deselectedItemCount > 0) {
                Log.d("AutoDeselect", "Automatically deselect $deselectedItemCount items.")
            }
        }
        // equivalent to referentialEqualityPolicy()
        val isScopeChanged = this.lazyDslScope !== lazyDslScope
        if (isScopeChanged) {
            this.lazyDslScope = lazyDslScope
            if (intervals.size != 0) {
                intervals = MutableIntervalList()
            }
            map.clear()
            if (selectionSupport.hasSelection() && selectionSupport is KeySelectionState) {
                when (autoDeselectMode) {
                    is AutoDeselectMode.Disabled -> {}

                    is AutoDeselectMode.SingleInterval -> if (key != null) {
                        deselectForNewKeys(
                            buildSet {
                                for (i in 0 until count) {
                                    val keyForIndex = key(i)
                                    add(keyForIndex as K)
                                }
                            }
                        )
                    }

                    is AutoDeselectMode.Enabled -> {
                        deselectForNewKeys(autoDeselectMode.latestKeys() as Set<K>)
                    }
                }
            }
        }
        intervals.addInterval(
            count,
            SelectableItemsInterval(
                item = item,
                key = key
            )
        )
        if ((selectionSupport as? DanglingKeysSupport)?.danglingKeys?.isNotEmpty() == true) {
            val iterator = selectionSupport.danglingKeys.iterator()
            while (iterator.hasNext()) {
                val danglingKey = iterator.next()
                val itemForKey = getItemForKey(danglingKey!!)
                if (itemForKey != null) {
                    selectionSupport.select(danglingKey, itemForKey)
                    iterator.remove()
                }
            }
        }
    }

    private inline fun <T> withInterval(
        globalIndex: Int,
        block: (localIntervalIndex: Int, content: SelectableItemsInterval<V>) -> T
    ): T {
        val interval = intervals[globalIndex]
        val localIntervalIndex = globalIndex - interval.startIndex
        return block(localIntervalIndex, interval.value)
    }

    private fun getKey(index: Int): Any =
        withInterval(index) { localIndex, content ->
            content.key?.invoke(localIndex) ?: getDefaultLazyLayoutKey(index)
        }

    internal fun getItemForKey(key: Any): V? =
        map.getOrElse(key) {
            // TODO: It is possible to implement a heuristic function to make the search faster.
            for (i in map.size until intervals.size) {
                val keyForIndex = getKey(i)
                val itemForIndex = getItemForIndex(i)
                map.put(keyForIndex, itemForIndex)
                if (key == keyForIndex) {
                    return itemForIndex
                }
            }
            return null
        }

    internal fun getItemForIndex(index: Int): V =
        withInterval(index) { localIndex, content ->
            content.item.invoke(localIndex)
        }

    val itemCount: Int get() = intervals.size

    fun getSelectableContent(): Iterable<Pair<Any, V>> =
        buildList {
            for (i in 0 until intervals.size) {
                val keyForIndex = getKey(i)
                val itemForIndex = getItemForIndex(i)
                add(keyForIndex to itemForIndex)
            }
        }
}

internal class SelectableItemsInterval<V>(
    val item: (Int) -> V,
    val key: ((index: Int) -> Any)?
)
