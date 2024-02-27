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

import androidx.collection.MutableScatterMap
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember

/**
 * @see [androidx.compose.foundation.lazy.layout.LazyLayoutKeyIndexMap]
 */
sealed interface KeyIndexItemMap<K, V> {

    fun getItem(e: K): V?

    fun getFullItemMappings(): Iterable<Pair<K, V>>
}

/**
 * Automatically deselect removed keys when items change.
 */
@Composable
private fun <V> AutoDeselectEffect(
    state: KeySelectionState<V>,
    items: List<V>,
    key: (index: Int, item: V) -> Any,
) = LaunchedEffect(items) {
    val currentSelectedKeys = state.selectedKeys()
    val newKeys = items.mapIndexed(key).toSet()
    currentSelectedKeys.filterNot { it in newKeys }.forEach { key -> state.deselect(key) }
}

/**
 * @see [androidx.compose.foundation.lazy.rememberLazyListItemProviderLambda]
 */
@Composable
fun <V> rememberKeyItemMap(
    state: KeySelectionState<V>? = null,
    items: List<V>,
    key: (item: V) -> Any,
): KeyItemMap<V> {
    if (state != null) {
        // Deselect removed items.
        AutoDeselectEffect(state, items) { index, item -> key(item) }
    }

    return remember(items, key) {
        KeyItemMap(items) { index -> key(items[index]) }
    }
}

@Composable
fun <V> rememberKeyItemMap(
    state: KeySelectionState<V>? = null,
    items: List<V>,
    key: (index: Int, item: V) -> Any,
): KeyItemMap<V> {
    if (state != null) {
        // Deselect removed items.
        AutoDeselectEffect(state, items, key)
    }

    return remember(items, key) {
        KeyItemMap(items) { index -> key(index, items[index]) }
    }
}

/**
 * @see [androidx.compose.foundation.lazy.layout.NearestRangeKeyIndexMap]
 */
open class KeyItemMap<V>(
    val items: List<V>,
    val key: ((index: Int) -> Any),
) : KeyIndexItemMap<Any, V> {
    private val map: MutableScatterMap<Any, V> = MutableScatterMap()

    override fun getItem(e: Any): V? = map.getOrElse(e) {
        // TODO: It is possible to implement a heuristic function to make the search faster.
        for (i in map.size until items.size) {
            val keyForItem = key(i)
            val item = items[i]
            map.put(keyForItem, item)
            if (e == keyForItem) {
                return item
            }
        }
        return null
    }

    override fun getFullItemMappings(): Iterable<Pair<Any, V>> =
        items.mapIndexed { index, item -> key(index) to item }
}

@Composable
fun <V> rememberIndexItemMap(
    state: KeySelectionState<V>? = null,
    items: List<V>,
): IndexItemMap<V> {
    if (state != null) {
        // Deselect removed items.
        AutoDeselectEffect(state, items) { index, item -> index }
    }

    return remember(items) {
        IndexItemMap(items)
    }
}

open class IndexItemMap<V>(
    val items: List<V>
) : KeyIndexItemMap<Int, V> {

    override fun getItem(e: Int): V? = items[e]

    override fun getFullItemMappings(): Iterable<Pair<Int, V>> =
        items.mapIndexed { index, item -> index to item }
}

operator fun KeyIndexItemMap<*, *>.plus(other: KeyIndexItemMap<*, *>): KeyIndexItemMap<*, *> {
    TODO()
}
