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

package me.gm.selection.sample

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.autoSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import me.gm.selection.grid.dragAfterLongPressToSelectGesture
import me.gm.selection.grid.selectableItems
import me.gm.selection.grid.tapInActionModeToToggleGesture
import me.gm.selection.list.dragAfterLongPressToSelectGesture
import me.gm.selection.list.selectableItems
import me.gm.selection.list.tapInActionModeToToggleGesture
import me.gm.selection.rememberKeyItemMap
import me.gm.selection.rememberKeySelectionState

@OptIn(
    ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class,
    ExperimentalLayoutApi::class
)
@Composable
fun SampleScreen() {
    val itemsA = remember {
        List(10) { index ->
            index
        }
    }
    val itemsB = remember {
        List(10) { index ->
            index
        }
    }
    val selectionState = rememberKeySelectionState<Int>(autoSaver())
    BackHandler(enabled = selectionState.hasSelection()) {
        selectionState.clearSelection()
    }

    val windowInsets = WindowInsets.safeDrawing
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            val context = LocalContext.current
            val appLabel =
                if (LocalView.current.isInEditMode) {
                    "Sample"
                } else {
                    context.applicationInfo.loadLabel(context.packageManager).toString()
                }
            TopAppBar(
                title = { Text(text = appLabel) },
                modifier = Modifier.fillMaxWidth(),
                windowInsets =
                windowInsets.only(WindowInsetsSides.Horizontal + WindowInsetsSides.Top),
                scrollBehavior = scrollBehavior
            )
        },
        contentWindowInsets = windowInsets
    ) { contentPadding ->
        var selectedOption by rememberSaveable { mutableStateOf("LazyColumn") }

        val mapA = rememberKeyItemMap(
            items = itemsA,
            key = { item -> "A" to item },
            state = selectionState
        )
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(contentPadding)
        ) {
            when (selectedOption) {
                "LazyColumn" -> {
                    val listState = rememberLazyListState()
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .weight(1f)
                            .tapInActionModeToToggleGesture(listState, selectionState, mapA)
                            .dragAfterLongPressToSelectGesture(listState, selectionState, mapA),
                        state = listState
                    ) {
                        selectableItems(
                            state = selectionState,
                            map = mapA
                        ) { helper, item ->
                            ListItem(
                                headlineContent = { Text(text = "Item $item") },
                                modifier = Modifier.animateItemPlacement(),
                                colors = ListItemDefaults.colors(
                                    containerColor = if (helper.isThisSelected()) {
                                        MaterialTheme.colorScheme.primaryContainer
                                    } else {
                                        Color.Transparent
                                    }
                                ),
                            )
                        }
                    }
                }

                "LazyVerticalGrid" -> {
                    val gridState = rememberLazyGridState()
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(4),
                        modifier = Modifier
                            .fillMaxSize()
                            .weight(1f)
                            .tapInActionModeToToggleGesture(gridState, selectionState, mapA)
                            .dragAfterLongPressToSelectGesture(gridState, selectionState, mapA),
                        state = gridState
                    ) {
                        selectableItems(
                            state = selectionState,
                            map = mapA,
                            span = { GridItemSpan(2) },
                        ) { helper, item ->
                            ListItem(
                                headlineContent = { Text(text = "Item $item") },
                                modifier = Modifier.animateItemPlacement(),
                                colors = ListItemDefaults.colors(
                                    containerColor = if (helper.isThisSelected()) {
                                        MaterialTheme.colorScheme.primaryContainer
                                    } else {
                                        Color.Transparent
                                    }
                                ),
                            )
                        }
                    }
                }
            }
            FlowRow(
                modifier = Modifier.padding(16.dp)
            ) {
                arrayOf("LazyColumn", "LazyRow", "LazyVerticalGrid", "LazyHorizontalGrid").forEach {
                    Row(
                        modifier = Modifier.clickable { selectedOption = it },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = selectedOption == it,
                            onClick = null
                        )
                        Text(text = it)
                    }
                }
            }
            FlowRow(
                modifier = Modifier.padding(16.dp)
            ) {
                TextButton(onClick = { /*TODO*/ }) {
                    Text(text = "Shuffle")
                }
                TextButton(onClick = { /*TODO*/ }) {
                    Text(text = "Show selected items")
                }
            }
        }
    }
}
