/*
 * Copyright 2021 ABSA Group Limited
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

import { createSelector } from '@ngrx/store'
import { SplineDataSourceInfo } from 'spline-api'

import { DsOverviewStore } from '../reducers'
import { SplineDataSourceStore } from '../reducers/base'


export namespace DsOverviewStoreSelectors {

    export const rootState = createSelector(
        SplineDataSourceStore.rootState,
        (_state: SplineDataSourceStore.State) => _state[DsOverviewStore.STORE_FEATURE_NAME]
    )

    export const dataSourceInfo = createSelector<any, DsOverviewStore.State, SplineDataSourceInfo>(
        rootState,
        (_state) => _state.dataSourceInfo,
    )

    export const isLoading = createSelector<any, DsOverviewStore.State, boolean>(
        rootState,
        (_state) => _state.loading.processing,
    )

    export const isInitialized = createSelector<any, SplineDataSourceInfo, boolean, boolean>(
        [dataSourceInfo, isLoading],
        (_dataSourceInfo, _isLoading) => !!_dataSourceInfo && !_isLoading
    )

}
