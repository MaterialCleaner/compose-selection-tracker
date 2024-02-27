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

package me.gm.selection.list

import android.annotation.SuppressLint
import androidx.annotation.FloatRange
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Indication
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.platform.debugInspectorInfo
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.IntOffset
import me.gm.selection.IntervalHelper
import me.gm.selection.SelectionState
import androidx.compose.foundation.clickable as foundationClickable

interface SelectableLazyItemScope : LazyItemScope {

    fun Modifier.clickable(
        enabled: Boolean = true,
        onClickLabel: String? = null,
        role: Role? = null,
        onClick: () -> Unit
    ): Modifier

    fun Modifier.clickable(
        interactionSource: MutableInteractionSource,
        indication: Indication?,
        enabled: Boolean = true,
        onClickLabel: String? = null,
        role: Role? = null,
        onClick: () -> Unit
    ): Modifier
}

@SuppressLint("ModifierFactoryUnreferencedReceiver")
class SelectableLazyItemScopeImpl(
    private val delegate: LazyItemScope,
    private val state: SelectionState<*, *>,
    private val helper: IntervalHelper<*>,
) : SelectableLazyItemScope {

    override fun Modifier.clickable(
        enabled: Boolean,
        onClickLabel: String?,
        role: Role?,
        onClick: () -> Unit
    ) = composed(
        inspectorInfo = debugInspectorInfo {
            name = "clickable"
            properties["enabled"] = enabled
            properties["onClickLabel"] = onClickLabel
            properties["role"] = role
            properties["onClick"] = onClick
        }
    ) {
        Modifier.clickable(
            enabled = enabled,
            onClickLabel = onClickLabel,
            onClick = onClick,
            role = role,
            indication = LocalIndication.current,
            interactionSource = remember { MutableInteractionSource() }
        )
    }

    override fun Modifier.clickable(
        interactionSource: MutableInteractionSource,
        indication: Indication?,
        enabled: Boolean,
        onClickLabel: String?,
        role: Role?,
        onClick: () -> Unit
    ) = foundationClickable(interactionSource, indication, enabled, onClickLabel, role) {
        if (state.hasSelection()) {
            helper.toggleThis()
        } else {
            onClick()
        }
    }

    override fun Modifier.fillParentMaxSize(
        @FloatRange(from = 0.0, to = 1.0)
        fraction: Float
    ): Modifier = with(delegate) {
        fillParentMaxSize(fraction)
    }

    override fun Modifier.fillParentMaxWidth(
        @FloatRange(from = 0.0, to = 1.0)
        fraction: Float
    ): Modifier = with(delegate) {
        fillParentMaxWidth(fraction)
    }

    override fun Modifier.fillParentMaxHeight(
        @FloatRange(from = 0.0, to = 1.0)
        fraction: Float
    ): Modifier = with(delegate) {
        fillParentMaxHeight(fraction)
    }

    @ExperimentalFoundationApi
    override fun Modifier.animateItemPlacement(
        animationSpec: FiniteAnimationSpec<IntOffset>
    ): Modifier = with(delegate) {
        animateItemPlacement(animationSpec)
    }
}
