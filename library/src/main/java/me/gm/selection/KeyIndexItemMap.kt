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
import androidx.compose.foundation.lazy.LazyListItemInfo
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember

/**
 * @see [androidx.compose.foundation.lazy.layout.LazyLayoutKeyIndexMap]
 */
interface KeyIndexItemMap<T> {

    fun getItemForKey(key: Any): T?

    fun getItemForIndex(index: Int): T?

    fun getFullKeyItemMappings(): Iterable<Pair<Any, T>>

    fun getFullIndexItemMappings(): Iterable<Pair<Int, T>>
}

/**
 * @see [androidx.compose.foundation.lazy.rememberLazyListItemProviderLambda]
 */
@Composable
fun <T> rememberKeyItemMap(
    items: List<T>,
    key: (item: T) -> Any,
    state: KeySelectionState<T>? = null,
): DefaultKeyItemMap<T> {
    if (state != null) {
        // Deselect removed items.
        AutoDeselectEffect(items, key, state)
    }

    return remember(items, key) {
        DefaultKeyItemMap(items, key)
    }
}

/**
 * Automatically deselect removed keys when items change.
 */
@Composable
fun <T> AutoDeselectEffect(
    items: List<T>,
    key: (item: T) -> Any,
    state: KeySelectionState<T>,
) = LaunchedEffect(items) {
    val currentSelectedKeys = state.selectedKeys()
    val newKeys = items.map(key).toSet()
    currentSelectedKeys.filterNot { it in newKeys }.forEach { key -> state.deselect(key) }
}

@Composable
fun <T> AutoDeselectEffect(
    map: DefaultKeyItemMap<T>,
    state: KeySelectionState<T>,
) = AutoDeselectEffect(map.items, map.key, state)

/**
 * @see [androidx.compose.foundation.lazy.layout.NearestRangeKeyIndexMap]
 */
class DefaultKeyItemMap<T>(
    internal val items: List<T>,
    internal val key: (item: T) -> Any,
) : DetailsLookup<T>(), KeyIndexItemMap<T> {
    private val map: MutableScatterMap<Any, T> = MutableScatterMap()

    override fun getItemForKey(key: Any): T? = map.getOrElse(key) {
        // TODO: It is possible to implement a heuristic function to make the search faster.
        for (i in map.size until items.size) {
            val item = items[i]
            val keyForItem = key(item)
            map.put(keyForItem, item)
            if (key == keyForItem) {
                return item
            }
        }
        return null
    }

    override fun getItemForIndex(index: Int): T? = throw UnsupportedOperationException()

    override fun getFullKeyItemMappings(): Iterable<Pair<Any, T>> =
        items.map { item -> key(item) to item }

    override fun getFullIndexItemMappings(): Iterable<Pair<Int, T>> =
        throw UnsupportedOperationException()

    override fun getItem(itemInfo: LazyListItemInfo): T? = getItemForKey(itemInfo.key)
}

public operator fun <T> Collection<T>.plus(element: T): List<T> {
    val result = ArrayList<T>(size + 1)
    result.addAll(this)
    result.add(element)
    return result
}
