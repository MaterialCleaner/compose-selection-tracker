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
import androidx.compose.runtime.saveable.SaverScope
import androidx.lifecycle.ViewModel
import java.util.UUID

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

/**
 * [viewModelSaver] is a compromise solution that ensures data is not lost during
 * configuration changes, and its usage is not much more complicated than [noOpSaver].
 * However, data will still be lost after process recreation
 * (you can test this by enabling 'Don't keep activities' in the developer options).
 *
 * Usage:
 *
 * Place the [viewModelSaver] object in your [ViewModel].
 * ```
 * class MyViewModel : ViewModel() {
 *     val saver: Saver<List<V>, Any> = viewModelSaver()
 * }
 * ```
 *
 * In any composable function, pass the [viewModelSaver] object to
 * [rememberKeySelectionState] or [rememberIndexSelectionState].
 * ```
 * val viewModel = viewModel<MyViewModel>()
 * val selectionState = rememberKeySelectionState(viewModel.saver)
 * ```
 */
fun <T> viewModelSaver(): Saver<T, Any> =
    @Suppress("UNCHECKED_CAST")
    (ViewModelSaver<T>() as Saver<T, Any>)

class ViewModelSaver<T> : Saver<Pair<List<Any>, List<T>>, UUID> {
    private val saveableStateRegistry: MutableMap<UUID, Pair<List<Any>, List<T>>> = mutableMapOf()

    override fun SaverScope.save(value: Pair<List<Any>, List<T>>): UUID {
        val uuid = UUID.randomUUID()
        saveableStateRegistry[uuid] = value
        return uuid
    }

    override fun restore(value: UUID): Pair<List<Any>, List<T>>? = saveableStateRegistry[value]
}
