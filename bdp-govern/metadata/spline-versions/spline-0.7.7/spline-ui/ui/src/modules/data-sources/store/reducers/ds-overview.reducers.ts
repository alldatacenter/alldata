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

import { SplineDataSourceInfo } from 'spline-api'
import { ProcessingStore } from 'spline-utils'

import { DsOverviewStoreActions } from '../actions'
import ActionTypes = DsOverviewStoreActions.ActionTypes


export namespace DsOverviewStore {

    export const STORE_FEATURE_NAME = 'overview'

    export type State = {
        dataSourceInfo: SplineDataSourceInfo
        loading: ProcessingStore.EventProcessingState
    }

    export function getInitState(): State {
        return {
            dataSourceInfo: null,
            loading: ProcessingStore.getDefaultProcessingState(true),
        }
    }

    export function reducer(state = getInitState(),
                            action: DsOverviewStoreActions.Actions): State {

        switch (action.type) {
            case ActionTypes.InitRequest:
                return {
                    ...state,
                    loading: ProcessingStore.eventProcessingStart(state.loading)
                }

            case ActionTypes.InitSuccess:
                return {
                    ...state,
                    dataSourceInfo: action.payload.dataSourceInfo,
                    loading: ProcessingStore.eventProcessingFinish(state.loading)
                }

            case ActionTypes.InitError:
                return {
                    ...state,
                    loading: ProcessingStore.eventProcessingFinish(state.loading, action.payload.error)
                }

            case ActionTypes.ResetState:
                return {
                    ...getInitState()
                }
            default:
                return { ...state }
        }

    }
}
