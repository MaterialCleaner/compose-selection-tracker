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

package me.gm.selection.grid

import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridItemScope
import androidx.compose.foundation.lazy.grid.LazyGridItemSpanScope
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.runtime.Composable
import me.gm.selection.IndexItemMap
import me.gm.selection.IntervalHelper
import me.gm.selection.KeyItemMap
import me.gm.selection.SelectionState

/** List */
inline fun <T> LazyGridScope.selectableItems(
    state: SelectionState<Any, T>,
    items: List<T>,
    noinline key: ((item: T) -> Any)? = null,
    noinline span: (LazyGridItemSpanScope.(item: T) -> GridItemSpan)? = null,
    noinline contentType: (item: T) -> Any? = { null },
    crossinline itemContent: @Composable LazyGridItemScope.(helper: IntervalHelper<T>, item: T) -> Unit
) {
    val itemProvider = { index: Int -> items[index] }
    val keyProvider = if (key != null) { index: Int -> key(items[index]) } else null
    items(
        count = items.size,
        key = keyProvider,
        span = if (span != null) {
            { span(items[it]) }
        } else null,
        contentType = { index: Int -> contentType(items[index]) }
    ) { index ->
        val helper = IntervalHelper(state, index, itemProvider, keyProvider)
        itemContent(helper, items[index])
    }
}

inline fun <T> LazyGridScope.selectableItems(
    state: SelectionState<Any, T>,
    map: () -> KeyItemMap<T>,
    noinline span: (LazyGridItemSpanScope.(item: T) -> GridItemSpan)? = null,
    noinline contentType: (item: T) -> Any? = { null },
    crossinline itemContent: @Composable LazyGridItemScope.(helper: IntervalHelper<T>, item: T) -> Unit
) {
    val items = map().items
    val itemProvider = { index: Int -> items[index] }
    val keyProvider = map().key
    items(
        count = items.size,
        key = keyProvider,
        span = if (span != null) {
            { span(items[it]) }
        } else null,
        contentType = { index: Int -> contentType(items[index]) }
    ) { index ->
        val helper = IntervalHelper(state, index, itemProvider, keyProvider)
        itemContent(helper, items[index])
    }
}

@JvmName("selectableItemsWithIndexItemMap")
inline fun <T> LazyGridScope.selectableItems(
    state: SelectionState<Any, T>,
    map: () -> IndexItemMap<T>,
    noinline span: (LazyGridItemSpanScope.(item: T) -> GridItemSpan)? = null,
    noinline contentType: (item: T) -> Any? = { null },
    crossinline itemContent: @Composable LazyGridItemScope.(helper: IntervalHelper<T>, item: T) -> Unit
) = selectableItems(state, map().items, null, span, contentType, itemContent)

/** ListIndexed */
inline fun <T> LazyGridScope.selectableItemsIndexed(
    state: SelectionState<Any, T>,
    items: List<T>,
    noinline key: ((index: Int, item: T) -> Any)? = null,
    noinline span: (LazyGridItemSpanScope.(index: Int, item: T) -> GridItemSpan)? = null,
    crossinline contentType: (index: Int, item: T) -> Any? = { _, _ -> null },
    crossinline itemContent: @Composable LazyGridItemScope.(helper: IntervalHelper<T>, index: Int, item: T) -> Unit
) {
    val itemProvider = { index: Int -> items[index] }
    val keyProvider = if (key != null) { index: Int -> key(index, items[index]) } else null
    items(
        count = items.size,
        key = if (key != null) { index: Int -> key(index, items[index]) } else null,
        span = if (span != null) {
            { span(it, items[it]) }
        } else null,
        contentType = { index -> contentType(index, items[index]) }
    ) { index ->
        val helper = IntervalHelper(state, index, itemProvider, keyProvider)
        itemContent(helper, index, items[index])
    }
}

inline fun <T> LazyGridScope.selectableItemsIndexed(
    state: SelectionState<Any, T>,
    map: () -> KeyItemMap<T>,
    noinline span: (LazyGridItemSpanScope.(index: Int, item: T) -> GridItemSpan)? = null,
    crossinline contentType: (index: Int, item: T) -> Any? = { _, _ -> null },
    crossinline itemContent: @Composable LazyGridItemScope.(helper: IntervalHelper<T>, index: Int, item: T) -> Unit
) {
    val items = map().items
    val itemProvider = { index: Int -> items[index] }
    val keyProvider = map().key
    items(
        count = items.size,
        key = keyProvider,
        span = if (span != null) {
            { span(it, items[it]) }
        } else null,
        contentType = { index -> contentType(index, items[index]) }
    ) { index ->
        val helper = IntervalHelper(state, index, itemProvider, keyProvider)
        itemContent(helper, index, items[index])
    }
}

@JvmName("selectableItemsWithIndexItemMap")
inline fun <T> LazyGridScope.selectableItemsIndexed(
    state: SelectionState<Any, T>,
    map: () -> IndexItemMap<T>,
    noinline span: (LazyGridItemSpanScope.(index: Int, item: T) -> GridItemSpan)? = null,
    crossinline contentType: (index: Int, item: T) -> Any? = { _, _ -> null },
    crossinline itemContent: @Composable LazyGridItemScope.(helper: IntervalHelper<T>, index: Int, item: T) -> Unit
) = selectableItemsIndexed(state, map().items, null, span, contentType, itemContent)

/** Array */
inline fun <T> LazyGridScope.selectableItems(
    state: SelectionState<Any, T>,
    items: Array<T>,
    noinline key: ((item: T) -> Any)? = null,
    noinline span: (LazyGridItemSpanScope.(item: T) -> GridItemSpan)? = null,
    noinline contentType: (item: T) -> Any? = { null },
    crossinline itemContent: @Composable LazyGridItemScope.(helper: IntervalHelper<T>, item: T) -> Unit
) {
    val itemProvider = { index: Int -> items[index] }
    val keyProvider = if (key != null) { index: Int -> key(items[index]) } else null
    items(
        count = items.size,
        key = if (key != null) { index: Int -> key(items[index]) } else null,
        span = if (span != null) {
            { span(items[it]) }
        } else null,
        contentType = { index: Int -> contentType(items[index]) }
    ) { index ->
        val helper = IntervalHelper(state, index, itemProvider, keyProvider)
        itemContent(helper, items[index])
    }
}

/** ArrayIndexed */
inline fun <T> LazyGridScope.selectableItemsIndexed(
    state: SelectionState<Any, T>,
    items: Array<T>,
    noinline key: ((index: Int, item: T) -> Any)? = null,
    noinline span: (LazyGridItemSpanScope.(index: Int, item: T) -> GridItemSpan)? = null,
    crossinline contentType: (index: Int, item: T) -> Any? = { _, _ -> null },
    crossinline itemContent: @Composable LazyGridItemScope.(helper: IntervalHelper<T>, index: Int, item: T) -> Unit
) {
    val itemProvider = { index: Int -> items[index] }
    val keyProvider = if (key != null) { index: Int -> key(index, items[index]) } else null
    items(
        count = items.size,
        key = if (key != null) { index: Int -> key(index, items[index]) } else null,
        span = if (span != null) {
            { span(it, items[it]) }
        } else null,
        contentType = { index -> contentType(index, items[index]) }
    ) { index ->
        val helper = IntervalHelper(state, index, itemProvider, keyProvider)
        itemContent(helper, index, items[index])
    }
}
