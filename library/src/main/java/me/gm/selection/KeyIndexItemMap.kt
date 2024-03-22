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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.referentialEqualityPolicy
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState

/**
 * @see [androidx.compose.foundation.lazy.layout.LazyLayoutKeyIndexMap]
 */
sealed interface KeyIndexItemMap<K, V> {
    val items: List<V>

    fun getItem(e: K): V?

    fun getFullItemMappings(): Iterable<Pair<K, V>>
}

interface KeyItemMap<V> : KeyIndexItemMap<Any, V> {
    val key: (index: Int) -> Any
}

interface IndexItemMap<V> : KeyIndexItemMap<Int, V>

/**
 * Automatically deselect removed keys when items change.
 */
@Composable
private fun <V> AutoDeselectEffect(
    state: SelectionState<Any, V>,
    items: List<V>,
    key: (index: Int, item: V) -> Any,
) = LaunchedEffect(items) {
    val currentSelectedKeys = state.selectedKeys()
    val newKeys = items.mapIndexed(key).toSet()
    currentSelectedKeys.filterNot { it in newKeys }.forEach { key ->
        if (state !is KeySelectionState || key !in state.dangingKeys) {
            state.deselect(key)
        }
    }
    val deselectedItemCount = currentSelectedKeys.size - state.selectedKeys().size
    if (deselectedItemCount > 0) {
        Log.d("AutoDeselectEffect", "Automatically deselect $deselectedItemCount items.")
    }
}

/**
 * @see [androidx.compose.foundation.lazy.rememberLazyListItemProviderLambda]
 * @param state a [KeySelectionState] object that store the selection state.
 * If you set up this parameter, removed keys will be automatically deselected when items change.
 * It's recommended to pass `null` if you're using multiple [KeyIndexItemMap]s.
 * @param items the data list
 * @param key a factory of stable and unique keys representing the item. Using the same key
 * for multiple items in the list is not allowed. Type of the key should be saveable
 * via Bundle on Android. If null is passed the position in the list will represent the key.
 * When you specify the key the scroll position will be maintained based on the key, which
 * means if you add/remove items before the current visible item the item with the given key
 * will be kept as the first visible one.
 */
@Composable
fun <V> rememberKeyItemMapLambda(
    state: KeySelectionState<V>? = null,
    items: List<V>,
    key: (index: Int, item: V) -> Any,
): () -> KeyItemMap<V> {
    if (state != null) {
        AutoDeselectEffect(state, items, key)
    }

    val latestItems = rememberUpdatedState(items)
    val latestKey = rememberUpdatedState(key)
    return remember {
        val itemMapState = derivedStateOf(referentialEqualityPolicy()) {
            KeyItemMapImpl(latestItems.value) { index ->
                latestKey.value(index, latestItems.value[index])
            }
        }
        itemMapState::value
    }.apply {
        if (state?.dangingKeys?.isNotEmpty() == true) {
            val iterator = state.dangingKeys.iterator()
            while (iterator.hasNext()) {
                val dangingKey = iterator.next()
                val item = this().getItem(dangingKey)
                if (item != null) {
                    state.select(dangingKey, item)
                    iterator.remove()
                }
            }
        }
    }
}

@Composable
fun <V> rememberKeyItemMapLambda(
    state: KeySelectionState<V>? = null,
    items: List<V>,
    key: (item: V) -> Any,
): () -> KeyItemMap<V> {
    return rememberKeyItemMapLambda(state, items) { index, item -> key(item) }
}

/**
 * @see [androidx.compose.foundation.lazy.layout.NearestRangeKeyIndexMap]
 */
open class KeyItemMapImpl<V>(
    override val items: List<V>,
    override val key: (index: Int) -> Any,
) : KeyItemMap<V> {
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
fun <V> rememberIndexItemMapLambda(
    state: IndexSelectionState<V>? = null,
    items: List<V>,
): () -> IndexItemMap<V> {
    if (state != null) {
        AutoDeselectEffect(state as SelectionState<Any, V>, items) { index, item -> index }
    }

    val latestItems = rememberUpdatedState(items)
    return remember(items) {
        val itemMapState = derivedStateOf(referentialEqualityPolicy()) {
            IndexItemMapImpl(latestItems.value)
        }
        itemMapState::value
    }
}

open class IndexItemMapImpl<V>(
    override val items: List<V>
) : IndexItemMap<V> {

    override fun getItem(e: Int): V? = items[e]

    override fun getFullItemMappings(): Iterable<Pair<Int, V>> =
        items.mapIndexed { index, item -> index to item }
}

/**
 * This can only be used as a parameter for modifiers.
 */
private class CombinedKeyItemMap<V>(
    val maps: MutableList<() -> KeyItemMap<V>>,
    override val items: List<V> = emptyList(),
    override val key: (Int) -> Any = { throw UnsupportedOperationException() }
) : KeyItemMap<V> {

    override fun getItem(e: Any): V? {
        for (map in maps) {
            val item = map().getItem(e)
            if (item != null) {
                return item
            }
        }
        return null
    }

    override fun getFullItemMappings(): Iterable<Pair<Int, V>> =
        throw UnsupportedOperationException()
}

operator fun <V> (() -> KeyItemMap<V>).plus(other: () -> KeyItemMap<V>): () -> KeyItemMap<V> {
    val thiz = this()
    return if (thiz is CombinedKeyItemMap) {
        thiz.maps.add(other)
        this
    } else {
        { CombinedKeyItemMap(mutableListOf(this, other)) }
    }
}

/*
 * Combining [IndexItemMap] requires knowing the position of each interval, which is similar to
 * [androidx.compose.foundation.lazy.layout.MutableIntervalList].
 * This introduces some implicit restrictions on usage, which we do not favor, and therefore,
 * we do not provide a built-in implementation.
 */
