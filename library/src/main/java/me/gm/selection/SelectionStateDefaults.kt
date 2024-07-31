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

import androidx.compose.runtime.saveable.Saver

/**
 * If you are indifferent to data loss,
 * then you can use [noOpSaver] to keep the implementation simple.
 */
fun <T> noOpSaver(): Saver<T, Any> =
    @Suppress("UNCHECKED_CAST")
    (NoOpSaver as Saver<T, Any>)

private val NoOpSaver: Saver<Pair<List<Any>, List<Any>>, Any> = Saver(
    save = {
        /*
          java.lang.IllegalArgumentException: kotlin.Unit cannot be saved using the current
          SaveableStateRegistry. The default implementation only supports types which can be stored
          inside the Bundle. Please consider implementing a custom Saver for this class and pass
          it to rememberSaveable().
         */
        114514
    },
    restore = { emptyList<Any>() to emptyList() }
)

/**
 * Save the key and automatically restore the selection after
 * [SelectableItemsIntervalContent.updateInterval].
 */
fun <T> danglingSaver(): Saver<T, Any> =
    @Suppress("UNCHECKED_CAST")
    (DanglingSaver as Saver<T, Any>)

internal val DanglingSaver: Saver<Pair<List<Any>, List<Any>>, List<Any>> = Saver(
    save = { (keys, _) ->
        keys
    },
    restore = { keys ->
        keys to emptyList()
    }
)

sealed class AutoDeselectMode {
    data object Disabled : AutoDeselectMode()
    data object SingleInterval : AutoDeselectMode()

    /**
     * I haven't figured out a way to determine that [SelectableItemsIntervalContent.updateInterval]
     * has added all selectable items in lazyDslScope.
     * I suspect it might be impossible, so you need to manually specify [latestKeys].
     * If you come up with a method, please let me know. Thank you!
     */
    data class Enabled(val latestKeys: () -> Set<*>) : AutoDeselectMode()
}
