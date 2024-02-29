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

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyHorizontalGrid
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.AlertDialog
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
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import me.gm.selection.IntervalHelper
import me.gm.selection.KeyIndexItemMap
import me.gm.selection.SelectionState
import me.gm.selection.grid.dragAfterLongPressToSelectGesture
import me.gm.selection.grid.selectableItems
import me.gm.selection.grid.tapInActionModeToToggleGesture
import me.gm.selection.list.dragAfterLongPressToSelectGesture
import me.gm.selection.list.selectableItems
import me.gm.selection.list.tapInActionModeToToggleGesture
import me.gm.selection.plus
import me.gm.selection.rememberIndexItemMapLambda
import me.gm.selection.rememberIndexSelectionState
import me.gm.selection.rememberKeyItemMapLambda
import me.gm.selection.rememberKeySelectionState

@OptIn(
    ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class,
    ExperimentalLayoutApi::class
)
@Composable
fun SampleScreen() {
    val itemsA = remember {
        List(8) { index ->
            index
        }.toMutableStateList()
    }
    val itemsB = remember {
        List(8) { index ->
            index
        }.toMutableStateList()
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

        val mapA = rememberKeyItemMapLambda(
            items = itemsA,
            key = { item -> "A" to item },
        )
        val mapB = rememberKeyItemMapLambda(
            items = itemsB,
            key = { item -> "B" to item },
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
                            .dragAfterLongPressToSelectGesture(
                                listState, selectionState, mapA + mapB
                            ),
                        state = listState
                    ) {
                        selectableItems(
                            state = selectionState,
                            map = mapA
                        ) { helper, item ->
                            val context = LocalContext.current
                            SampleListItem(
                                modifier = Modifier
                                    .animateItemPlacement()
                                    .clickable {
                                        Toast
                                            .makeText(context, "ItemA $item", Toast.LENGTH_SHORT)
                                            .show()
                                    },
                                helper = helper,
                                text = "ItemA $item",
                            )
                        }
                        selectableItems(
                            state = selectionState,
                            map = mapB
                        ) { helper, item ->
                            val context = LocalContext.current
                            SampleListItem(
                                modifier = Modifier
                                    .animateItemPlacement()
                                    .clickable {
                                        Toast
                                            .makeText(context, "ItemB $item", Toast.LENGTH_SHORT)
                                            .show()
                                    },
                                helper = helper,
                                text = "ItemB $item",
                            )
                        }
                    }
                }

                "LazyRow" -> {
                    val listState = rememberLazyListState()
                    val indexSelectionState = rememberIndexSelectionState<Int>(autoSaver())
                    BackHandler(enabled = indexSelectionState.hasSelection()) {
                        indexSelectionState.clearSelection()
                    }
                    val indexMapA = rememberIndexItemMapLambda(
                        state = indexSelectionState,
                        items = itemsA
                    )
                    LazyRow(
                        modifier = Modifier
                            .fillMaxSize()
                            .weight(1f)
                            .tapInActionModeToToggleGesture(
                                listState,
                                indexSelectionState as SelectionState<Any, Int>,
                                indexMapA as () -> KeyIndexItemMap<Any, Int>
                            )
                            .dragAfterLongPressToSelectGesture(
                                listState,
                                indexSelectionState as SelectionState<Any, Int>,
                                indexMapA as () -> KeyIndexItemMap<Any, Int>
                            ),
                        state = listState
                    ) {
                        selectableItems(
                            state = indexSelectionState,
                            map = indexMapA,
                        ) { helper, item ->
                            SampleListItem(
                                modifier = Modifier.animateItemPlacement(),
                                helper = helper,
                                text = "ItemA $item",
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
                            .tapInActionModeToToggleGesture(
                                gridState, selectionState, mapA + mapB
                            )
                            .dragAfterLongPressToSelectGesture(
                                gridState, selectionState, mapA + mapB
                            ),
                        state = gridState
                    ) {
                        selectableItems(
                            state = selectionState,
                            map = mapA,
                            span = { GridItemSpan(2) },
                        ) { helper, item ->
                            SampleListItem(
                                modifier = Modifier.animateItemPlacement(),
                                helper = helper,
                                text = "ItemA $item",
                            )
                        }
                        selectableItems(
                            state = selectionState,
                            map = mapB,
                            span = { GridItemSpan(1) },
                        ) { helper, item ->
                            SampleListItem(
                                modifier = Modifier.animateItemPlacement(),
                                helper = helper,
                                text = "ItemB $item",
                            )
                        }
                    }
                }

                "LazyHorizontalGrid" -> {
                    val gridState = rememberLazyGridState()
                    LazyHorizontalGrid(
                        rows = GridCells.Fixed(4),
                        modifier = Modifier
                            .fillMaxSize()
                            .weight(1f)
                            .tapInActionModeToToggleGesture(
                                gridState, selectionState, mapA + mapB
                            )
                            .dragAfterLongPressToSelectGesture(
                                gridState, selectionState, mapA + mapB
                            ),
                        state = gridState
                    ) {
                        selectableItems(
                            state = selectionState,
                            map = mapA,
                            span = { GridItemSpan(2) },
                        ) { helper, item ->
                            SampleListItem(
                                modifier = Modifier.animateItemPlacement(),
                                helper = helper,
                                text = "ItemA $item",
                            )
                        }
                        selectableItems(
                            state = selectionState,
                            map = mapB,
                            span = { GridItemSpan(1) },
                        ) { helper, item ->
                            SampleListItem(
                                modifier = Modifier.animateItemPlacement(),
                                helper = helper,
                                text = "ItemB $item",
                            )
                        }
                    }
                }
            }
            FlowRow(
                modifier = Modifier.padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
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
                modifier = Modifier.padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                TextButton(onClick = {
                    itemsA.shuffle()
                    itemsB.shuffle()
                }) {
                    Text(text = "Shuffle")
                }
                var showSelectedItemsDialog by rememberSaveable { mutableStateOf(false) }
                if (showSelectedItemsDialog) {
                    AlertDialog(
                        onDismissRequest = { showSelectedItemsDialog = false },
                        confirmButton = {
                            TextButton(onClick = { showSelectedItemsDialog = false }) {
                                Text(text = stringResource(android.R.string.ok))
                            }
                        },
                        text = {
                            Text(
                                text = selectionState.selectedKeys()
                                    .zip(selectionState.selectedItems())
                                    .joinToString("\n") { (key, item) ->
                                        "$key - $item"
                                    }
                            )
                        }
                    )
                }
                TextButton(onClick = {
                    showSelectedItemsDialog = true
                }) {
                    Text(text = "Show selected items")
                }
            }
        }
    }
}

@Composable
private fun SampleListItem(
    modifier: Modifier = Modifier,
    helper: IntervalHelper<*>,
    text: String
) {
    ListItem(
        headlineContent = { Text(text = text) },
        modifier = modifier,
        colors = ListItemDefaults.colors(
            containerColor = if (helper.isThisSelected()) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                Color.Transparent
            }
        ),
    )
}
