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

private val NoOpSaver: Saver<List<Any>, Any> = Saver(
    save = { 114514 },
    restore = { emptyList() }
)

fun <T> dangingSaver(): Saver<T, Any> =
    @Suppress("UNCHECKED_CAST")
    (DangingSaver as Saver<T, Any>)

internal val DangingSaver: Saver<List<Any>, Any> = Saver(
    save = { 1919810 },
    restore = { emptyList() }
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
    (ViewModelSaver() as Saver<T, Any>)

class ViewModelSaver : Saver<List<Any>, UUID> {
    private val saveableStateRegistry: MutableMap<UUID, List<Any>> = mutableMapOf()

    override fun SaverScope.save(value: List<Any>): UUID {
        val uuid = UUID.randomUUID()
        saveableStateRegistry[uuid] = value
        return uuid
    }

    override fun restore(value: UUID): List<Any>? = saveableStateRegistry[value]
}
