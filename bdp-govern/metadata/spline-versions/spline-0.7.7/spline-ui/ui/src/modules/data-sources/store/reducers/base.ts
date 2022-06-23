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

import { ActionReducerMap, createFeatureSelector } from '@ngrx/store'

import { DsOverviewDetailsStore } from './ds-overview-details.reducers'
import { DsOverviewStore } from './ds-overview.reducers'


export namespace SplineDataSourceStore {

    export const STORE_FEATURE_NAME = 'dataSources'

    export type State = {
        [DsOverviewStore.STORE_FEATURE_NAME]: DsOverviewStore.State
        [DsOverviewDetailsStore.STORE_FEATURE_NAME]: DsOverviewDetailsStore.State
    }

    export const reducers: ActionReducerMap<State> = {
        [DsOverviewStore.STORE_FEATURE_NAME]: DsOverviewStore.reducer,
        [DsOverviewDetailsStore.STORE_FEATURE_NAME]: DsOverviewDetailsStore.reducer
    }

    export const rootState = createFeatureSelector<State>(STORE_FEATURE_NAME)
}
