# Compose Selection Tracker

[![Android CI status](https://github.com/MaterialCleaner/compose-selection-tracker/workflows/Android%20CI/badge.svg)](https://github.com/MaterialCleaner/compose-selection-tracker/actions)

Selection library for the Jetpack Compose LazyList and LazyGrid.

## Preview

## Background

Selection is easy to implement. However, is it easy to stay elegant? I've seen some developers
manage selection logic within the ViewModel. If there's only one place in the entire project that
requires selection, this approach seems fine. However, if multiple areas require it, the selection
logic will be duplicated across multiple ViewModels. Why not have these selection logics appear just
once? This is the most important reason I created this library: to modularize selection for reuse.

## Features

## Integration

Gradle:

```gradle
implementation 'me.gm.selection:library:1.0.0'
```

## Usage

Use it.

## License

    Copyright 2024 Green Mushroom

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

        http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.
